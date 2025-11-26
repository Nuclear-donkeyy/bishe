package com.example.uavbackend.fleet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.uavbackend.auth.User;
import com.example.uavbackend.auth.UserMapper;
import com.example.uavbackend.auth.UserStatus;
import com.example.uavbackend.configcenter.SensorType;
import com.example.uavbackend.configcenter.SensorTypeMapper;
import com.example.uavbackend.fleet.dto.FleetSummaryDto;
import com.example.uavbackend.fleet.dto.UavDeviceDto;
import com.example.uavbackend.fleet.dto.UavRequest;
import java.util.List;
import java.util.Optional;
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
  private final UserMapper userMapper;
  private final UavSensorMapper uavSensorMapper;
  private final SensorTypeMapper sensorTypeMapper;

  public FleetSummaryDto summary() {
    List<UavDevice> all = deviceMapper.selectList(null);
    long online = all.stream().filter(d -> d.getStatus() == UavStatus.ONLINE).count();
    long warning =
        all.stream().filter(d -> d.getStatus() == UavStatus.WARNING || d.getStatus() == UavStatus.CRITICAL).count();
    return new FleetSummaryDto(online, warning, 0L, 0);
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
    User pilot =
        Optional.ofNullable(
                userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.pilotUsername())
                        .eq(User::getStatus, UserStatus.ACTIVE)))
            .orElseThrow(() -> new IllegalArgumentException("责任人不存在或已禁用"));
    UavDevice device = new UavDevice();
    device.setUavCode(request.uavCode());
    device.setModel(request.model());
    device.setPilotName(pilot.getName());
    device.setStatus(UavStatus.PENDING_CONNECT);
    deviceMapper.insert(device);
    saveSensors(device.getId(), request.sensors());
    return toDto(device);
  }

  public void applyTelemetry(String uavCode, UavTelemetry telemetry) {
    UavDevice device =
        deviceMapper.selectOne(new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode));
    if (device != null) {
      device.setStatus(UavStatus.ONLINE);
      deviceMapper.updateById(device);
    }
  }

  private UavDeviceDto toDto(UavDevice entity) {
    List<String> sensors = loadSensorCodes(entity.getId());
    return new UavDeviceDto(
        entity.getId(),
        entity.getUavCode(),
        entity.getModel(),
        entity.getPilotName(),
        entity.getStatus(),
        sensors);
  }

  private void saveSensors(Long uavId, List<String> sensorCodes) {
    if (sensorCodes == null || sensorCodes.isEmpty()) {
      return;
    }
    List<SensorType> sensors =
        sensorTypeMapper.selectList(new LambdaQueryWrapper<SensorType>().in(SensorType::getSensorCode, sensorCodes));
    for (SensorType s : sensors) {
      UavSensor rel = new UavSensor();
      rel.setUavId(uavId);
      rel.setSensorTypeId(s.getId());
      uavSensorMapper.insert(rel);
    }
  }

  private List<String> loadSensorCodes(Long uavId) {
    List<UavSensor> rels =
        uavSensorMapper.selectList(new LambdaQueryWrapper<UavSensor>().eq(UavSensor::getUavId, uavId));
    if (rels.isEmpty()) {
      return List.of();
    }
    List<Long> sensorIds = rels.stream().map(UavSensor::getSensorTypeId).toList();
    List<SensorType> sensors = sensorTypeMapper.selectBatchIds(sensorIds);
    if (sensors == null) {
      return List.of();
    }
    return sensors.stream().map(SensorType::getSensorCode).toList();
  }
}
