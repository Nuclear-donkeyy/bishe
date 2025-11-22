package com.example.uavbackend.fleet;

import com.example.uavbackend.fleet.dto.FleetSummaryDto;
import com.example.uavbackend.fleet.dto.UavDeviceDto;
import com.example.uavbackend.fleet.dto.UavRequest;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FleetService {
  private final UavDeviceRepository deviceRepository;

  public FleetSummaryDto summary() {
    List<UavDevice> all = deviceRepository.findAll();
    long online = all.stream().filter(d -> d.getStatus() == UavStatus.ONLINE).count();
    long warning =
        all.stream().filter(d -> d.getStatus() == UavStatus.WARNING || d.getStatus() == UavStatus.CRITICAL).count();
    double avgRtt =
        all.stream().map(UavDevice::getRttMs).filter(v -> v != null && v > 0).mapToInt(Integer::intValue).average().orElse(0);
    return new FleetSummaryDto(online, warning, 0L, Math.round(avgRtt));
  }

  public Page<UavDeviceDto> list(List<UavStatus> statuses, int page, int size) {
    PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), size);
    Page<UavDevice> pageResult =
        statuses == null || statuses.isEmpty()
            ? deviceRepository.findAll(pageable)
            : deviceRepository.findAllByStatusIn(statuses, pageable);
    return pageResult.map(this::toDto);
  }

  public List<UavDeviceDto> available(List<String> excludeMissionIds) {
    return deviceRepository.findByStatus(UavStatus.ONLINE).stream().map(this::toDto).collect(Collectors.toList());
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
    deviceRepository.save(device);
    return toDto(device);
  }

  public void applyTelemetry(String uavCode, UavTelemetry telemetry) {
    deviceRepository
        .findByUavCode(uavCode)
        .ifPresent(
            device -> {
              device.setBatteryPercent(telemetry.getBatteryPercent());
              device.setRangeKm(telemetry.getRangeKm());
              device.setLocationLat(telemetry.getLocationLat());
              device.setLocationLng(telemetry.getLocationLng());
              device.setLocationAlt(telemetry.getLocationAlt());
              device.setRttMs(telemetry.getRttMs());
              device.setLastHeartbeatAt(Instant.now());
              device.setStatus(UavStatus.ONLINE);
            });
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
