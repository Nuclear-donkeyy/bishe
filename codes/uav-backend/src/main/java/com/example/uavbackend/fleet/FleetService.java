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
  private final TelemetryService telemetryService;

  public FleetSummaryDto summary() {
    List<UavDevice> all = deviceMapper.selectList(null);
    long online =
        all.stream()
            .map(d -> telemetryService.resolveStatus(d.getUavCode()))
            .filter(status -> status == UavStatus.ONLINE)
            .count();
    long warning =
        all.stream()
            .map(d -> telemetryService.resolveStatus(d.getUavCode()))
            .filter(status -> status == UavStatus.WARNING || status == UavStatus.CRITICAL)
            .count();
    return new FleetSummaryDto(online, warning, 0L, 0);
  }

  public org.springframework.data.domain.Page<UavDeviceDto> list(List<UavStatus> statuses, int page, int size) {
    LambdaQueryWrapper<UavDevice> wrapper = new LambdaQueryWrapper<>();
    Page<UavDevice> mpPage = deviceMapper.selectPage(Page.of(Math.max(page, 1), size), wrapper);
    List<UavDevice> records = mpPage.getRecords();
    List<UavDevice> filtered =
        (statuses == null || statuses.isEmpty())
            ? records
            : records.stream()
                .filter(d -> statuses.contains(telemetryService.resolveStatus(d.getUavCode())))
                .toList();
    List<UavDeviceDto> mapped = filtered.stream().map(this::toDto).toList();
    return new PageImpl<>(mapped, PageRequest.of(Math.max(page - 1, 0), size), mapped.size());
  }

  public List<UavDeviceDto> available(List<String> excludeMissionIds) {
    // excludeMissionIds currently unused
    return deviceMapper.selectList(null).stream()
        .filter(device -> telemetryService.resolveStatus(device.getUavCode()) == UavStatus.ONLINE)
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public UavDeviceDto register(UavRequest request) {
    if (deviceMapper.exists(new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, request.uavCode()))) {
      throw new IllegalArgumentException("无人机编号已存在");
    }
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
    
    int rows = deviceMapper.insert(device);
    if (rows != 1) {
      throw new IllegalStateException("插入无人机记录失败");
    }
    saveSensors(device.getId(), request.sensors());
    return toDto(device);
  }

  private UavDeviceDto toDto(UavDevice entity) {
    List<String> sensors = loadSensorCodes(entity.getId());
    // 前端状态仅从 WebSocket 遥测推送得出，这里不返回 status
    return new UavDeviceDto(entity.getId(), entity.getUavCode(), entity.getModel(), entity.getPilotName(), sensors);
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
