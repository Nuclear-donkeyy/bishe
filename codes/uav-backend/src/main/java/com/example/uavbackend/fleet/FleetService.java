package com.example.uavbackend.fleet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.uavbackend.fleet.dto.FleetSummaryDto;
import com.example.uavbackend.fleet.dto.UavDeviceDto;
import com.example.uavbackend.fleet.dto.UavRequest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FleetService {
  private final UavDeviceMapper deviceMapper;

  public FleetSummaryDto summary() {
    List<UavDevice> all = deviceMapper.selectList(null);
    long online = all.stream().filter(d -> d.getStatus() == UavStatus.ONLINE).count();
    long warning =
        all.stream().filter(d -> d.getStatus() == UavStatus.WARNING || d.getStatus() == UavStatus.CRITICAL).count();
    double avgRtt =
        all.stream().map(UavDevice::getRttMs).filter(v -> v != null && v > 0).mapToInt(Integer::intValue).average().orElse(0);
    return new FleetSummaryDto(online, warning, 0L, Math.round(avgRtt));
  }

  public org.springframework.data.domain.Page<UavDeviceDto> list(List<UavStatus> statuses, int page, int size) {
    LambdaQueryWrapper<UavDevice> wrapper = new LambdaQueryWrapper<>();
    if (statuses != null && !statuses.isEmpty()) {
      wrapper.in(UavDevice::getStatus, statuses);
    }
    Page<UavDevice> mpPage = deviceMapper.selectPage(Page.of(Math.max(page, 1), size), wrapper);
    List<UavDeviceDto> content = mpPage.getRecords().stream().map(this::toDto).toList();
    return new PageImpl<>(content, PageRequest.of(Math.max(page - 1, 0), size), mpPage.getTotal());
  }

  public List<UavDeviceDto> available(List<String> excludeMissionIds) {
    // excludeMissionIds currently unused; filter by status only
    return deviceMapper
        .selectList(new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getStatus, UavStatus.ONLINE))
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public UavDeviceDto register(UavRequest request) {
    UavDevice device = new UavDevice();
    device.setUavCode(request.uavCode());
    device.setModel(request.model());
    device.setPilotName(request.pilotName());
    device.setStatus(UavStatus.PENDING_CONNECT);
    device.setConnectionEndpoint(request.connection().endpoint());
    device.setConnectionProtocol(request.connection().protocol());
    device.setConnectionSecret(request.connection().secret());
    device.setTelemetryTopics(request.connection().telemetryTopics());
    device.setMqttUsername(request.connection().mqttUsername());
    device.setMqttPassword(request.connection().mqttPassword());
    device.setMetadata(request.metadata());
    deviceMapper.insert(device);
    return toDto(device);
  }

  public void applyTelemetry(String uavCode, UavTelemetry telemetry) {
    UavDevice device =
        deviceMapper.selectOne(
            new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode));
    if (device != null) {
      device.setBatteryPercent(telemetry.getBatteryPercent());
      device.setRangeKm(telemetry.getRangeKm());
      device.setLocationLat(telemetry.getLocationLat());
      device.setLocationLng(telemetry.getLocationLng());
      device.setLocationAlt(telemetry.getLocationAlt());
      device.setRttMs(telemetry.getRttMs());
      device.setLastHeartbeatAt(Instant.now());
      device.setStatus(UavStatus.ONLINE);
      deviceMapper.updateById(device);
    }
  }

  private UavDeviceDto toDto(UavDevice entity) {
    return new UavDeviceDto(
        entity.getId(),
        entity.getUavCode(),
        entity.getModel(),
        entity.getPilotName(),
        entity.getStatus(),
        entity.getBatteryPercent(),
        entity.getRangeKm(),
        entity.getLinkQuality(),
        entity.getRttMs(),
        entity.getLocationLat(),
        entity.getLocationLng(),
        entity.getLocationAlt(),
        entity.getLastHeartbeatAt());
  }
}
