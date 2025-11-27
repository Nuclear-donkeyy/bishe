package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.fleet.TelemetryService;
import com.example.uavbackend.fleet.UavDevice;
import com.example.uavbackend.fleet.UavDeviceMapper;
import com.example.uavbackend.mqtt.MqttCommandPublisher;
import com.example.uavbackend.mission.MissionStatusPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MissionQueueService {
  private static final String KEY_PREFIX = "mission:queue:";
  private static final long QUEUE_TTL_MS = 10 * 60 * 1000;

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final MissionMapper missionMapper;
  private final MissionUavAssignmentMapper assignmentMapper;
  private final UavDeviceMapper uavDeviceMapper;
  private final TelemetryService telemetryService;
  private final MqttCommandPublisher mqttCommandPublisher;
  private final SimpMessagingTemplate messagingTemplate;

  public void enqueue(Mission mission, List<List<Double>> route, List<UavDevice> devices, String priority) {
    MissionQueueItem item = new MissionQueueItem();
    item.setMissionCode(mission.getMissionCode());
    item.setUavCodes(devices.stream().map(UavDevice::getUavCode).toList());
    item.setRoute(route);
    item.setPriority(priority);
    item.setEnqueuedAt(Instant.now().toEpochMilli());
    try {
      redisTemplate
          .opsForValue()
          .set(KEY_PREFIX + mission.getMissionCode(), objectMapper.writeValueAsString(item), QUEUE_TTL_MS);
    } catch (Exception ignored) {
      // swallow; queue failure should not break mission creation
    }
  }

  public void removeFromQueue(String missionCode) {
    redisTemplate.delete(KEY_PREFIX + missionCode);
  }

  @Scheduled(fixedDelay = 3000)
  public void processQueue() {
    Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
    if (keys == null || keys.isEmpty()) {
      return;
    }
    List<MissionQueueItem> items = new ArrayList<>();
    for (String key : keys) {
      try {
        String json = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(json)) {
          continue;
        }
        MissionQueueItem item = objectMapper.readValue(json, MissionQueueItem.class);
        items.add(item);
      } catch (Exception ignored) {
      }
    }
    if (items.isEmpty()) {
      return;
    }
    items.sort(Comparator.comparingInt((MissionQueueItem i) -> priorityWeight(i.getPriority()))
        .reversed()
        .thenComparingLong(MissionQueueItem::getEnqueuedAt));

    for (MissionQueueItem item : items) {
      Optional<String> readyUav = item.getUavCodes().stream().filter(this::isUavReady).findFirst();
      if (readyUav.isEmpty()) {
        continue;
      }
      String uavCode = readyUav.get();
      sendCommandAndStart(item, uavCode);
    }
  }

  private void sendCommandAndStart(MissionQueueItem item, String uavCode) {
    try {
      Map<String, Object> payload =
          Map.of(
              "type", "mission.start",
              "missionCode", item.getMissionCode(),
              "uavCode", uavCode,
              "route", item.getRoute());
      mqttCommandPublisher.publish(uavCode, payload);
      markMissionStatus(item.getMissionCode(), MissionStatus.RUNNING);
      removeFromQueue(item.getMissionCode());
    } catch (Exception e) {
      // 如果发送失败，不要删除队列，等下次调度
    }
  }

  private boolean isUavReady(String uavCode) {
    UavDevice device =
        uavDeviceMapper.selectOne(new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode));
    if (device == null) {
      return false;
    }
    // 在线判定：有遥测且 status 为 ONLINE 或无 status
    String payload = telemetryService.readTelemetry(uavCode);
    if (!telemetryService.isOnline(payload)) {
      return false;
    }
    // 不被 RUNNING 任务占用
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(new LambdaQueryWrapper<MissionUavAssignment>().eq(MissionUavAssignment::getUavId, device.getId()));
    if (assignments.isEmpty()) {
      return true;
    }
    List<Long> missionIds = assignments.stream().map(MissionUavAssignment::getMissionId).toList();
    List<Mission> missions = missionMapper.selectBatchIds(missionIds);
    if (missions == null) {
      return true;
    }
    return missions.stream().noneMatch(m -> MissionStatus.RUNNING.name().equals(m.getStatus()));
  }

  public void onTelemetryStatus(String uavCode, String status) {
    if (!StringUtils.hasText(status)) {
      return;
    }
    String upper = status.toUpperCase();
    if ("EXECUTING".equals(upper)) {
      markMissionByUav(uavCode, MissionStatus.RUNNING);
    } else if ("RETURNING".equals(upper)) {
      markMissionByUav(uavCode, MissionStatus.COMPLETED);
    }
  }

  private void markMissionByUav(String uavCode, MissionStatus target) {
    UavDevice device =
        uavDeviceMapper.selectOne(new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode));
    if (device == null) {
      return;
    }
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(new LambdaQueryWrapper<MissionUavAssignment>().eq(MissionUavAssignment::getUavId, device.getId()));
    if (assignments.isEmpty()) {
      return;
    }
    List<Long> missionIds = assignments.stream().map(MissionUavAssignment::getMissionId).toList();
    List<Mission> missions = missionMapper.selectBatchIds(missionIds);
    if (CollectionUtils.isEmpty(missions)) {
      return;
    }
    for (Mission mission : missions) {
      switch (target) {
        case RUNNING -> {
          if (MissionStatus.QUEUE.name().equals(mission.getStatus())) {
            markMissionStatus(mission.getMissionCode(), MissionStatus.RUNNING);
            removeFromQueue(mission.getMissionCode());
          }
        }
        case COMPLETED -> {
          if (MissionStatus.RUNNING.name().equals(mission.getStatus())) {
            markMissionStatus(mission.getMissionCode(), MissionStatus.COMPLETED);
          }
        }
        default -> {}
      }
    }
  }

  private void pushStatusUpdate(Mission mission) {
    messagingTemplate.convertAndSend(
        "/topic/mission-updates",
        new MissionStatusPayload(mission.getMissionCode(), mission.getStatus()));
  }

  private void markMissionStatus(String missionCode, MissionStatus status) {
    Mission mission =
        missionMapper.selectOne(new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission == null) {
      return;
    }
    mission.setStatus(status.name());
    if (status == MissionStatus.COMPLETED) {
      mission.setProgress(100);
    }
    missionMapper.updateById(mission);
    pushStatusUpdate(mission);
  }

  private int priorityWeight(String priority) {
    if (!StringUtils.hasText(priority)) {
      return 0;
    }
    return switch (priority) {
      case "HIGH", "high" -> 3;
      case "MEDIUM", "medium" -> 2;
      case "LOW", "low" -> 1;
      default -> 0;
    };
  }

  @Data
  @AllArgsConstructor
  @RequiredArgsConstructor
  private static class MissionQueueItem {
    private String missionCode;
    private List<String> uavCodes;
    private List<List<Double>> route;
    private String priority;
    private long enqueuedAt;
  }
}
