package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.fleet.TelemetryService;
import com.example.uavbackend.fleet.UavDevice;
import com.example.uavbackend.fleet.UavDeviceMapper;
import com.example.uavbackend.mqtt.MqttCommandPublisher;
import com.fasterxml.jackson.databind.JsonNode;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
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
  private final com.example.uavbackend.analytics.MissionDataAggregator dataAggregator;

  public void enqueue(Mission mission, List<List<Double>> route, List<UavDevice> devices, String priority) {
        MissionQueueItem item = new MissionQueueItem();
        item.setMissionCode(mission.getMissionCode());
        item.setUavCodes(devices.stream().map(UavDevice::getUavCode).toList());
        item.setRoute(route);
        item.setPriority(normalizePriority(priority)); // HIGH/MEDIUM/LOW
        item.setEnqueuedAt(Instant.now().toEpochMilli());
        item.setDispatchedAt(null);
        try {
            String json = objectMapper.writeValueAsString(item);
            log.info("Queue mission payload: {}", json);
            redisTemplate.opsForValue().set(KEY_PREFIX + mission.getMissionCode(), json);
            log.info("Mission queued missionCode={}, uavs={}, priority={}", mission.getMissionCode(), item.getUavCodes(),
                    item.getPriority());
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
    // 获取所有排队任务
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
    log.info("排序后的任务队列:"+items.toString());
    var chosen = new java.util.HashSet<String>();
    for (MissionQueueItem item : items) {
      Optional<String> readyUav =
          item.getUavCodes().stream().filter(u -> !chosen.contains(u)).filter(this::isUavReady).findFirst();
      if (readyUav.isEmpty()) {
        log.debug("No ready UAV for mission {}", item.getMissionCode());
        continue;
      }
      log.info("可供选择的无人机"+readyUav.toString());
      String uavCode = readyUav.get();
      chosen.add(uavCode);
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
      log.info("Dispatch mission.start missionCode={} to uav={} points={}", item.getMissionCode(), uavCode, item.getRoute().size());
      item.setDispatchedAt(Instant.now().toEpochMilli());
      // 写回 redis，标记已下发但仍处于排队键，等待遥测确认
      redisTemplate
          .opsForValue()
          .set(KEY_PREFIX + item.getMissionCode(), objectMapper.writeValueAsString(item), QUEUE_TTL_MS);
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

      try{
          JsonNode node = objectMapper.readTree(payload);
          if (node.hasNonNull("status")) {
              String s = node.get("status").asText("");
              return "ONLINE".equalsIgnoreCase(s)
                      || "IDLE".equalsIgnoreCase(s);
          }else{
              return true;
          }
      }catch (Exception e){
         // ignore
      }

    return missions.stream().noneMatch(m -> MissionStatus.RUNNING.name().equals(m.getStatus()));
  }

  public void onTelemetryStatus(String uavCode, String status, String missionId) {
    String upper = status != null ? status.toUpperCase() : null;
    if (StringUtils.hasText(missionId)) {
      if ("EXECUTING".equals(upper) || "RUNNING".equals(upper)) {
        markMissionById(missionId, MissionStatus.RUNNING);
      } else if ("RETURNING".equals(upper) || "IDLE".equals(upper)) {
        markMissionById(missionId, MissionStatus.COMPLETED);
      }
    } else if (StringUtils.hasText(upper)) {
      if ("EXECUTING".equals(upper) || "RUNNING".equals(upper)) {
        markMissionByUav(uavCode, MissionStatus.RUNNING);
      } else if ("RETURNING".equals(upper) || "IDLE".equals(upper)) {
        markMissionByUav(uavCode, MissionStatus.COMPLETED);
      }
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

  private void markMissionById(String missionCode, MissionStatus target) {
    Mission mission =
        missionMapper.selectOne(new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission == null) {
      return;
    }
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
      // 完成时生成数据采集记录
      dataAggregator.complete(mission);
    } else if (status == MissionStatus.INTERRUPTED || status == MissionStatus.QUEUE) {
      dataAggregator.clear(missionCode);
    }
    missionMapper.updateById(mission);
    pushStatusUpdate(mission);
    if (status != MissionStatus.QUEUE) {
      removeFromQueue(missionCode);
    }
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
    private Long dispatchedAt;
  }

    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) return "MEDIUM";
        String p = priority.trim().toUpperCase();
        return switch (p) {
            case "HIGH", "高" -> "HIGH";
            case "LOW", "低" -> "LOW";
            default -> "MEDIUM";
        };
    }
}
