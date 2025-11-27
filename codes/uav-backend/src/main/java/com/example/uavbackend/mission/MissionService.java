package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.auth.User;
import com.example.uavbackend.auth.UserMapper;
import com.example.uavbackend.auth.UserRole;
import com.example.uavbackend.auth.UserStatus;
import com.example.uavbackend.fleet.UavDevice;
import com.example.uavbackend.fleet.UavDeviceMapper;
import com.example.uavbackend.mission.MissionStatus;
import com.example.uavbackend.mission.MissionStatusPayload;
import com.example.uavbackend.mission.dto.MissionCreateRequest;
import com.example.uavbackend.mission.dto.MissionDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionService {
  private final MissionMapper missionMapper;
  private final MissionRoutePointMapper routePointMapper;
  private final MissionUavAssignmentMapper assignmentMapper;
  private final UavDeviceMapper uavDeviceMapper;
  private final UserMapper userMapper;
  private final MissionQueueService missionQueueService;
  private final SimpMessagingTemplate messagingTemplate;

  public List<MissionDto> list(List<String> statuses) {
    LambdaQueryWrapper<Mission> wrapper = new LambdaQueryWrapper<>();
    if (statuses != null && !statuses.isEmpty()) {
      wrapper.in(Mission::getStatus, statuses);
    }
    List<Mission> missions = missionMapper.selectList(wrapper);
    return missions.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Optional<MissionDto> findByCode(String code) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, code));
    return Optional.ofNullable(mission).map(this::toDtoWithRoute);
  }

  @Transactional
  public MissionDto create(MissionCreateRequest request) {
    Mission mission = new Mission();
    mission.setMissionCode(generateMissionCode());
    mission.setName(request.name());
    User pilot = findPilot(request.pilotUsername());
    mission.setMissionType(request.missionType());
    mission.setPilotName(pilot.getName());
    mission.setPriority(request.priority());
    mission.setProgress(0);
    List<UavDevice> assignedDevices = findAssignedDevices(request.assignedUavs());
    mission.setStatus(MissionStatus.QUEUE.name());
    mission.setColorHex("#22c55e");
    mission.setMetrics(null);
    mission.setMilestones(JsonUtils.toJson(request.milestones()));
    missionMapper.insert(mission);
    saveRoutePoints(mission.getId(), request.route());
    saveAssignments(mission.getId(), assignedDevices);
    if (!assignedDevices.isEmpty()) {
      missionQueueService.enqueue(mission, request.route(), assignedDevices, request.priority());
    }
    pushStatusUpdate(mission);
    return toDtoWithRoute(mission);
  }

  @Transactional
  public MissionDto updateProgress(String missionCode, Integer progress) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    mission.setProgress(progress);
    missionMapper.updateById(mission);
    return toDto(mission);
  }

  @Transactional
  public void interrupt(String missionCode) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission != null) {
      mission.setStatus(MissionStatus.INTERRUPTED.name());
      missionMapper.updateById(mission);
      missionQueueService.removeFromQueue(mission.getMissionCode());
      pushStatusUpdate(mission);
    }
  }

  private void saveRoutePoints(Long missionId, List<List<Double>> points) {
    routePointMapper.delete(
        new LambdaQueryWrapper<MissionRoutePoint>().eq(MissionRoutePoint::getMissionId, missionId));
    for (int i = 0; i < points.size(); i++) {
      List<Double> p = points.get(i);
      MissionRoutePoint point = new MissionRoutePoint();
      point.setMissionId(missionId);
      point.setSeq(i + 1);
      point.setLat(BigDecimal.valueOf(p.get(0)));
      point.setLng(BigDecimal.valueOf(p.get(1)));
      routePointMapper.insert(point);
    }
  }

  private MissionDto toDto(Mission entity) {
    List<MissionRoutePoint> route =
        routePointMapper.selectList(
            new LambdaQueryWrapper<MissionRoutePoint>()
                .eq(MissionRoutePoint::getMissionId, entity.getId())
                .orderByAsc(MissionRoutePoint::getSeq));
    List<String> assignedUavCodes = findAssignedUavCodes(entity.getId());
    return new MissionDto(
        entity.getId(),
        entity.getMissionCode(),
        entity.getName(),
        entity.getMissionType(),
        entity.getPilotName(),
        entity.getStatus(),
        entity.getPriority(),
        entity.getProgress(),
        entity.getColorHex(),
        route.stream()
            .map(p -> List.of(p.getLat().doubleValue(), p.getLng().doubleValue()))
            .collect(Collectors.toList()),
        JsonUtils.fromJsonArray(entity.getMilestones()),
        JsonUtils.fromJsonArray(entity.getMetrics()),
        assignedUavCodes);
  }

  private MissionDto toDtoWithRoute(Mission mission) {
    return toDto(mission);
  }

  private String generateMissionCode() {
    return "M-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private List<UavDevice> findAssignedDevices(List<String> assignedUavCodes) {
    if (assignedUavCodes == null || assignedUavCodes.isEmpty()) {
      return List.of();
    }
    return uavDeviceMapper.selectList(
        new LambdaQueryWrapper<UavDevice>().in(UavDevice::getUavCode, assignedUavCodes));
  }

  private void saveAssignments(Long missionId, List<UavDevice> devices) {
    if (devices.isEmpty()) {
      return;
    }
    for (UavDevice device : devices) {
      MissionUavAssignment assignment = new MissionUavAssignment();
      assignment.setMissionId(missionId);
      assignment.setUavId(device.getId());
      assignment.setAssignedAt(Instant.now());
      assignmentMapper.insert(assignment);
    }
  }

  private List<String> findAssignedUavCodes(Long missionId) {
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>().eq(MissionUavAssignment::getMissionId, missionId));
    if (assignments.isEmpty()) {
      return List.of();
    }
    List<Long> uavIds = assignments.stream().map(MissionUavAssignment::getUavId).toList();
    List<UavDevice> devices = uavDeviceMapper.selectBatchIds(uavIds);
    if (devices == null) {
      return List.of();
    }
    return devices.stream().map(UavDevice::getUavCode).toList();
  }

  private User findPilot(String pilotUsername) {
    User pilot =
        userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, pilotUsername)
                .eq(User::getStatus, UserStatus.ACTIVE));
    if (pilot == null) {
      throw new IllegalArgumentException("责任人不存在");
    }
    if (pilot.getRole() != UserRole.OPERATOR && pilot.getRole() != UserRole.SUPERADMIN) {
      throw new IllegalArgumentException("责任人角色无效");
    }
    return pilot;
  }

  public void markRunning(String missionCode) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission != null && !MissionStatus.RUNNING.name().equals(mission.getStatus())) {
      mission.setStatus(MissionStatus.RUNNING.name());
      missionMapper.updateById(mission);
      pushStatusUpdate(mission);
    }
  }

  public void markCompleted(String missionCode) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission != null && !MissionStatus.COMPLETED.name().equals(mission.getStatus())) {
      mission.setStatus(MissionStatus.COMPLETED.name());
      mission.setProgress(100);
      missionMapper.updateById(mission);
      pushStatusUpdate(mission);
    }
  }

  private void pushStatusUpdate(Mission mission) {
    messagingTemplate.convertAndSend(
        "/topic/mission-updates",
        new MissionStatusPayload(mission.getMissionCode(), mission.getStatus()));
  }
}
