package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.auth.AccessScope;
import com.example.uavbackend.auth.AccessScopeService;
import com.example.uavbackend.auth.User;
import com.example.uavbackend.auth.UserMapper;
import com.example.uavbackend.auth.UserRole;
import com.example.uavbackend.auth.UserStatus;
import com.example.uavbackend.fleet.UavDevice;
import com.example.uavbackend.fleet.UavDeviceMapper;
import com.example.uavbackend.mission.dto.MissionCreateRequest;
import com.example.uavbackend.mission.dto.MissionDto;
import com.example.uavbackend.mqtt.MqttCommandPublisher;
import com.example.uavbackend.alert.AlertRule;
import com.example.uavbackend.alert.AlertRuleMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionService {
  private final MissionMapper missionMapper;
  private final MissionRoutePointMapper routePointMapper;
  private final MissionUavAssignmentMapper assignmentMapper;
  private final UavDeviceMapper uavDeviceMapper;
  private final UserMapper userMapper;
  private final MissionQueueService missionQueueService;
  private final SimpMessagingTemplate messagingTemplate;
  private final MqttCommandPublisher mqttCommandPublisher;
  private final AlertRuleMapper alertRuleMapper;
  private final AccessScopeService accessScopeService;

  public List<MissionDto> list(List<String> statuses) {
    AccessScope scope = accessScopeService.currentScope();
    LambdaQueryWrapper<Mission> wrapper = new LambdaQueryWrapper<>();
    if (statuses != null && !statuses.isEmpty()) {
      wrapper.in(Mission::getStatus, statuses);
    }
    applyPilotScope(wrapper, scope);
    List<Mission> missions = missionMapper.selectList(wrapper);
    return missions.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Optional<MissionDto> findByCode(String code) {
    AccessScope scope = accessScopeService.currentScope();
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, code));
    if (mission == null || !canAccessMission(scope, mission)) {
      return Optional.empty();
    }
    return Optional.of(toDtoWithRoute(mission));
  }

  @Transactional
  public MissionDto create(MissionCreateRequest request) {
    AccessScope scope = accessScopeService.currentScope();
    Mission mission = new Mission();
    mission.setMissionCode(generateMissionCode());
    mission.setName(request.name());
    User pilot = findPilot(scope.superAdmin() ? request.pilotUsername() : scope.username());
    mission.setMissionType(request.missionType());
    mission.setPilotName(pilot.getName());
    mission.setPriority(request.priority());
    if (request.ruleId() != null) {
      Long templateRuleCount =
          alertRuleMapper.selectCount(
              new LambdaQueryWrapper<AlertRule>()
                  .eq(AlertRule::getId, request.ruleId())
                  .eq(AlertRule::getTemplateEnabled, true));
      if (templateRuleCount != null && templateRuleCount > 0) {
        throw new IllegalArgumentException("任务只能绑定普通规则，不能直接绑定规则模板");
      }
      AlertRule rule = alertRuleMapper.selectById(request.ruleId());
      if (rule == null) {
        throw new IllegalArgumentException("报警规则不存在");
      }
      mission.setRuleId(rule.getId());
    }
    mission.setProgress(0);
    List<UavDevice> assignedDevices = findAssignedDevices(request.assignedUavs(), scope);
    mission.setStatus(MissionStatus.QUEUE.name());
    mission.setColorHex("#22c55e");
    mission.setMetrics(null);
    mission.setMilestones(JsonUtils.toJson(request.milestones()));
    missionMapper.insert(mission);
    saveRoutePoints(mission.getId(), request.route());
    saveAssignments(mission.getId(), assignedDevices);
    missionQueueService.enqueue(mission, request.route(), assignedDevices, request.priority());
    pushStatusUpdate(mission);
    return toDtoWithRoute(mission);
  }

  @Transactional
  public MissionDto updateProgress(String missionCode, Integer progress) {
    AccessScope scope = accessScopeService.currentScope();
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    ensureMissionAccess(scope, mission);
    mission.setProgress(progress);
    missionMapper.updateById(mission);
    return toDto(mission);
  }

  @Transactional
  public void interrupt(String missionCode) {
    AccessScope scope = accessScopeService.currentScope();
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission != null) {
      ensureMissionAccess(scope, mission);
      List<String> uavCodes = findAssignedUavCodes(mission.getId());
      mission.setStatus(MissionStatus.INTERRUPTED.name());
      missionMapper.updateById(mission);
      missionQueueService.removeFromQueue(mission.getMissionCode());
      releaseAssignments(mission.getId());
      // push interrupt command to assigned UAVs
      for (String code : uavCodes) {
        try {
          mqttCommandPublisher.publish(
              code, java.util.Map.of("type", "interrupt", "missionCode", missionCode));
          log.info("Sent interrupt to UAV {} for mission {}", code, missionCode);
        } catch (Exception e) {
          log.warn("Failed to send interrupt to UAV {} for mission {}", code, missionCode, e);
        }
      }
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
        assignedUavCodes,
        entity.getRuleId());
  }

  private MissionDto toDtoWithRoute(Mission mission) {
    return toDto(mission);
  }

  private String generateMissionCode() {
    return "M-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private List<UavDevice> findAssignedDevices(List<String> assignedUavCodes, AccessScope scope) {
    if (assignedUavCodes == null || assignedUavCodes.isEmpty()) {
      return List.of();
    }
    LambdaQueryWrapper<UavDevice> wrapper =
        new LambdaQueryWrapper<UavDevice>().in(UavDevice::getUavCode, assignedUavCodes);
    if (!scope.superAdmin()) {
      wrapper.eq(UavDevice::getPilotName, scope.displayName());
    }
    List<UavDevice> devices = uavDeviceMapper.selectList(wrapper);
    if (devices.size() != assignedUavCodes.size()) {
      throw new IllegalArgumentException("存在无权使用或不存在的无人机");
    }
    return devices;
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
      assignment.setReleasedAt(null);
      assignmentMapper.insert(assignment);
    }
  }

  private List<String> findAssignedUavCodes(Long missionId) {
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getMissionId, missionId)
                .isNull(MissionUavAssignment::getReleasedAt));
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

  private void releaseAssignments(Long missionId) {
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getMissionId, missionId)
                .isNull(MissionUavAssignment::getReleasedAt));
    for (MissionUavAssignment assignment : assignments) {
      assignment.setReleasedAt(Instant.now());
      assignmentMapper.updateById(assignment);
    }
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

  private void pushStatusUpdate(Mission mission) {
    messagingTemplate.convertAndSend(
        "/topic/mission-updates",
        new MissionStatusPayload(mission.getMissionCode(), mission.getStatus()));
  }

  private void applyPilotScope(LambdaQueryWrapper<Mission> wrapper, AccessScope scope) {
    if (!scope.superAdmin()) {
      wrapper.eq(Mission::getPilotName, scope.displayName());
    }
  }

  private boolean canAccessMission(AccessScope scope, Mission mission) {
    return scope.superAdmin() || scope.displayName().equals(mission.getPilotName());
  }

  private void ensureMissionAccess(AccessScope scope, Mission mission) {
    if (!canAccessMission(scope, mission)) {
      throw new IllegalArgumentException("无权访问该任务");
    }
  }
}
