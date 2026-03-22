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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
  private static final double DEFAULT_SPEED_MPS = 18.0;
  private static final double DISCHARGE_PER_MINUTE = 2.5;
  private static final double SAFETY_BATTERY_RESERVE = 15.0;
  private static final double MIN_REQUIRED_BATTERY = 20.0;
  private static final int MIN_HEALTH_SCORE = 45;
  private static final int DYNAMIC_HEALTH_FLOOR = 35;
  private static final double DYNAMIC_BATTERY_BUFFER = 10.0;
  private static final long DISPATCH_RETRY_COOLDOWN_MS = 8_000L;
  private static final long PREEMPT_COOLDOWN_MS = 30_000L;
  private static final int MAX_PREEMPTIONS = 3;

  private final StringRedisTemplate redisTemplate;
  private final MissionMapper missionMapper;
  private final MissionRoutePointMapper routePointMapper;
  private final MissionUavAssignmentMapper assignmentMapper;
  private final MissionEventMapper missionEventMapper;
  private final UavDeviceMapper uavDeviceMapper;
  private final TelemetryService telemetryService;
  private final MqttCommandPublisher mqttCommandPublisher;
  private final SimpMessagingTemplate messagingTemplate;
  private final com.example.uavbackend.analytics.MissionDataAggregator dataAggregator;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public Optional<String> validateAssignedDispatchReadiness(
      List<List<Double>> route, List<UavDevice> devices, String priority) {
    if (devices == null || devices.isEmpty()) {
      return Optional.empty();
    }
    MissionQueueItem preview =
        buildQueueItem(
            "preview",
            route == null ? List.of() : route,
            devices.stream().map(UavDevice::getUavCode).toList(),
            priority,
            Instant.now().toEpochMilli(),
            0,
            null,
            "preflight_validation");
    List<String> blockedReasons = new ArrayList<>();
    for (UavDevice device : devices) {
      DispatchCandidate candidate = buildCandidate(preview, device.getUavCode(), Set.of());
      if (candidate == null) {
        blockedReasons.add(device.getUavCode() + " 当前离线或未上报遥测");
        continue;
      }
      if (candidate.isEligible() && !candidate.isOccupied()) {
        return Optional.empty();
      }
      blockedReasons.add(formatBlockedReason(candidate, preview));
    }
    if (blockedReasons.isEmpty()) {
      return Optional.of("指定无人机当前无法执行任务");
    }
    return Optional.of(String.join("；", blockedReasons));
  }

  public void enqueue(
      Mission mission, List<List<Double>> route, List<UavDevice> devices, String priority) {
    MissionQueueItem item =
        buildQueueItem(
            mission.getMissionCode(),
            route,
            devices.stream().map(UavDevice::getUavCode).toList(),
            priority,
            Instant.now().toEpochMilli(),
            0,
            null,
            "mission_created");
    saveQueueItem(item);
    writeEvent(
        mission,
        "MISSION_ENQUEUED",
        Map.of(
            "priority", item.getPriority(),
            "routeDistanceKm", item.getRouteDistanceKm(),
            "estimatedDurationMinutes", item.getEstimatedDurationMinutes(),
            "requiredBatteryPercent", item.getRequiredBatteryPercent(),
            "candidateUavCodes", item.getCandidateUavCodes()));
  }

  public void removeFromQueue(String missionCode) {
    redisTemplate.delete(KEY_PREFIX + missionCode);
  }

  public void restoreQueuedMission(
      Mission mission, List<List<Double>> route, List<String> candidateUavCodes, String reason) {
    if (mission == null || !StringUtils.hasText(mission.getMissionCode())) {
      return;
    }
    if (loadQueueItem(mission.getMissionCode()).isPresent()) {
      return;
    }
    long enqueuedAt =
        mission.getCreatedAt() == null ? Instant.now().toEpochMilli() : mission.getCreatedAt().toEpochMilli();
    MissionQueueItem item =
        buildQueueItemFromMission(
            mission,
            route == null ? List.of() : route,
            candidateUavCodes == null ? List.of() : candidateUavCodes,
            mission.getPriority(),
            enqueuedAt,
            MissionStatus.PREEMPTED.name().equalsIgnoreCase(mission.getStatus()) ? 1 : 0,
            null,
            reason);
    saveQueueItem(item);
  }

  @Scheduled(initialDelay = 5000, fixedDelay = 3000)
  public void processQueue() {
    reconcileRunningMissionStates();
    rebalanceRunningMissions();

    List<MissionQueueItem> items = loadQueueItems();
    if (items.isEmpty()) {
      return;
    }

    items.sort(
        Comparator.comparingDouble((MissionQueueItem item) -> queuePriorityScore(item)).reversed());

    Set<String> reservedUavs = new HashSet<>();
    for (MissionQueueItem item : items) {
      Mission mission = findMission(item.getMissionCode()).orElse(null);
      if (mission == null) {
        removeFromQueue(item.getMissionCode());
        continue;
      }
      if (MissionStatus.COMPLETED.name().equalsIgnoreCase(mission.getStatus())
          || MissionStatus.INTERRUPTED.name().equalsIgnoreCase(mission.getStatus())) {
        removeFromQueue(item.getMissionCode());
        continue;
      }

      QueueEvaluation evaluation = evaluateQueueItem(item, reservedUavs);
      if (evaluation.dispatchCandidate() != null) {
        if (dispatchMission(item, evaluation.dispatchCandidate())) {
          reservedUavs.add(evaluation.dispatchCandidate().getUavCode());
        }
        continue;
      }

      if (evaluation.preemptCandidate() != null) {
        if (preemptForQueuedMission(item, evaluation.preemptCandidate())) {
          continue;
        }
      }

      item.setLastSchedulingReason(evaluation.blockedReason());
      saveQueueItem(item);
    }
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

  private void reconcileRunningMissionStates() {
    List<Mission> runningMissions =
        missionMapper.selectList(
            new LambdaQueryWrapper<Mission>().eq(Mission::getStatus, MissionStatus.RUNNING.name()));
    for (Mission mission : runningMissions) {
      List<String> activeUavCodes = findActiveAssignedUavCodes(mission.getId());
      if (activeUavCodes.isEmpty()) {
        transitionMissionToPreempted(
            mission, null, "stale_running:no_active_assignment", false, null);
        continue;
      }
      boolean hasOnlineTelemetry = activeUavCodes.stream().anyMatch(uavCode -> readSnapshot(uavCode) != null);
      if (!hasOnlineTelemetry) {
        transitionMissionToPreempted(
            mission, null, "stale_running:all_uavs_offline", false, null);
      }
    }
  }

  private void rebalanceRunningMissions() {
    List<Mission> runningMissions =
        missionMapper.selectList(
            new LambdaQueryWrapper<Mission>().eq(Mission::getStatus, MissionStatus.RUNNING.name()));
    for (Mission mission : runningMissions) {
      MissionQueueItem profile =
          buildQueueItemFromMission(
              mission,
              findMissionRoute(mission.getId()),
              findActiveAssignedUavCodes(mission.getId()),
              mission.getPriority(),
              mission.getCreatedAt() != null
                  ? mission.getCreatedAt().toEpochMilli()
                  : Instant.now().toEpochMilli(),
              0,
              null,
              "running_profile");
      for (String uavCode : findActiveAssignedUavCodes(mission.getId())) {
        TelemetrySnapshot snapshot = readSnapshot(uavCode);
        if (snapshot == null) {
          continue;
        }
        if (shouldRebalanceRunningMission(profile, snapshot)) {
          transitionMissionToPreempted(
              mission,
              uavCode,
              "dynamic_rebalance:battery_or_health",
              false,
              null);
          break;
        }
      }
    }
  }

  private boolean shouldRebalanceRunningMission(
      MissionQueueItem missionProfile, TelemetrySnapshot snapshot) {
    if (snapshot.batteryPercent() == null) {
      return false;
    }
    double rebalanceBatteryThreshold =
        Math.max(
            MIN_REQUIRED_BATTERY,
            missionProfile.getRequiredBatteryPercent() - DYNAMIC_BATTERY_BUFFER);
    return snapshot.batteryPercent() < rebalanceBatteryThreshold
        || snapshot.healthScore() < DYNAMIC_HEALTH_FLOOR;
  }

  private QueueEvaluation evaluateQueueItem(MissionQueueItem item, Set<String> reservedUavs) {
    List<String> candidates = resolveCandidateUavCodes(item);
    DispatchCandidate bestDispatch = null;
    DispatchCandidate bestPreempt = null;
    List<String> blockedReasons = new ArrayList<>();

    for (String uavCode : candidates) {
      DispatchCandidate candidate = buildCandidate(item, uavCode, reservedUavs);
      if (candidate == null) {
        blockedReasons.add(uavCode + ":missing_snapshot");
        continue;
      }
      if (!candidate.isEligible()) {
        blockedReasons.add(uavCode + ":" + candidate.getReason());
        continue;
      }
      if (!candidate.isOccupied()) {
        if (bestDispatch == null || candidate.getTotalScore() > bestDispatch.getTotalScore()) {
          bestDispatch = candidate;
        }
        continue;
      }
      if (canPreempt(item, candidate)
          && (bestPreempt == null || candidate.getTotalScore() > bestPreempt.getTotalScore())) {
        bestPreempt = candidate;
      }
    }

    return new QueueEvaluation(
        bestDispatch,
        bestPreempt,
        blockedReasons.isEmpty() ? "no_candidate_uav" : String.join(", ", blockedReasons));
  }

  private DispatchCandidate buildCandidate(
      MissionQueueItem item, String uavCode, Set<String> reservedUavs) {
    if (reservedUavs.contains(uavCode)) {
      return null;
    }
    UavDevice device =
        uavDeviceMapper.selectOne(
            new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode));
    if (device == null) {
      return null;
    }
    TelemetrySnapshot snapshot = readSnapshot(uavCode);
    if (snapshot == null) {
      return null;
    }

    Mission occupiedMission = findRunningMissionByUav(device.getId()).orElse(null);
    double batteryFitness = computeBatteryFitness(item, snapshot);
    double durationFitness = computeDurationFitness(item);
    double totalScore =
        priorityBaseScore(item.getPriority()) * 0.35
            + agingScore(item) * 0.10
            + batteryFitness * 0.25
            + snapshot.healthScore() * 0.20
            + durationFitness * 0.10
            - (occupiedMission == null ? 0 : 35);

    boolean batteryOk =
        snapshot.batteryPercent() != null
            && snapshot.batteryPercent() >= item.getRequiredBatteryPercent();
    boolean healthOk = snapshot.healthScore() >= MIN_HEALTH_SCORE;
    boolean statusOk = snapshot.dispatchable() || occupiedMission != null;
    String reason = "ok";
    if (!statusOk) {
      reason = "status_not_dispatchable";
    } else if (!batteryOk) {
      reason =
          "battery_insufficient("
              + snapshot.batteryPercent()
              + "<"
              + item.getRequiredBatteryPercent()
              + ")";
    } else if (!healthOk) {
      reason = "health_low(" + snapshot.healthScore() + ")";
    } else if (occupiedMission != null
        && priorityWeight(occupiedMission.getPriority()) >= priorityWeight(item.getPriority())) {
      reason = "occupied_by_higher_or_equal_priority";
    }

    return new DispatchCandidate(
        uavCode,
        device,
        snapshot,
        occupiedMission,
        occupiedMission != null,
        statusOk && batteryOk && healthOk,
        batteryFitness,
        durationFitness,
        totalScore,
        reason);
  }

  private boolean dispatchMission(MissionQueueItem item, DispatchCandidate candidate) {
    long now = Instant.now().toEpochMilli();
    if (item.getDispatchedAt() != null && now - item.getDispatchedAt() < DISPATCH_RETRY_COOLDOWN_MS) {
      return false;
    }
    try {
      Map<String, Object> payload =
          Map.of(
              "type", "mission.start",
              "missionCode", item.getMissionCode(),
              "uavCode", candidate.getUavCode(),
              "route", item.getRoute(),
              "schedule", Map.of(
                  "score", Math.round(candidate.getTotalScore() * 100.0) / 100.0,
                  "requiredBatteryPercent", item.getRequiredBatteryPercent(),
                  "healthScore", candidate.getSnapshot().healthScore(),
                  "dispatchReason", candidate.getReason()));
      mqttCommandPublisher.publish(candidate.getUavCode(), payload);

      Mission mission = findMission(item.getMissionCode()).orElseThrow();
      ensureActiveAssignment(mission.getId(), candidate.getDevice().getId());
      releaseAssignmentsForMission(mission.getId(), candidate.getDevice().getId());
      item.setDispatchedAt(now);
      item.setLastSchedulingReason("dispatched_to_" + candidate.getUavCode());
      saveQueueItem(item);

      writeEvent(
          mission,
          "MISSION_DISPATCHED",
          Map.of(
              "uavCode", candidate.getUavCode(),
              "score", candidate.getTotalScore(),
              "batteryFitness", candidate.getBatteryFitness(),
              "healthScore", candidate.getSnapshot().healthScore(),
              "durationFitness", candidate.getDurationFitness()));
      log.info(
          "Dispatch mission {} to uav {} score={} battery={} health={}",
          item.getMissionCode(),
          candidate.getUavCode(),
          candidate.getTotalScore(),
          candidate.getSnapshot().batteryPercent(),
          candidate.getSnapshot().healthScore());
      return true;
    } catch (Exception e) {
      item.setLastSchedulingReason("dispatch_failed:" + e.getClass().getSimpleName());
      saveQueueItem(item);
      log.warn(
          "Failed to dispatch mission {} to uav {}",
          item.getMissionCode(),
          candidate.getUavCode(),
          e);
      return false;
    }
  }

  private boolean preemptForQueuedMission(MissionQueueItem waitingItem, DispatchCandidate candidate) {
    Mission runningMission = candidate.getOccupiedMission();
    if (runningMission == null) {
      return false;
    }
    boolean changed =
        transitionMissionToPreempted(
            runningMission,
            candidate.getUavCode(),
            "preempted_by:" + waitingItem.getMissionCode(),
            true,
            waitingItem);
    if (!changed) {
      return false;
    }
    waitingItem.setLastSchedulingReason("awaiting_release_from_" + candidate.getUavCode());
    saveQueueItem(waitingItem);
    writeEvent(
        findMission(waitingItem.getMissionCode()).orElse(null),
        "MISSION_PREEMPTION_REQUESTED",
        Map.of(
            "waitingMissionCode", waitingItem.getMissionCode(),
            "preemptedMissionCode", runningMission.getMissionCode(),
            "uavCode", candidate.getUavCode()));
    return true;
  }

  private boolean transitionMissionToPreempted(
      Mission mission,
      String uavCode,
      String reason,
      boolean incrementPreemptCounter,
      MissionQueueItem triggeringMission) {
    if (mission == null || !MissionStatus.RUNNING.name().equalsIgnoreCase(mission.getStatus())) {
      return false;
    }

    MissionQueueItem item =
        loadQueueItem(mission.getMissionCode())
            .orElseGet(
                () ->
                    buildQueueItemFromMission(
                        mission,
                        findMissionRoute(mission.getId()),
                        findActiveAssignedUavCodes(mission.getId()),
                        mission.getPriority(),
                        Instant.now().toEpochMilli(),
                        0,
                        null,
                        reason));

    long now = Instant.now().toEpochMilli();
    if (incrementPreemptCounter) {
      if (item.getLastPreemptedAt() != null && now - item.getLastPreemptedAt() < PREEMPT_COOLDOWN_MS) {
        return false;
      }
      if (item.getPreemptedCount() >= MAX_PREEMPTIONS) {
        return false;
      }
      item.setPreemptedCount(item.getPreemptedCount() + 1);
      item.setLastPreemptedAt(now);
    }
    item.setDispatchedAt(null);
    item.setLastSchedulingReason(reason);
    if (item.getEnqueuedAt() == 0) {
      item.setEnqueuedAt(now);
    }
    saveQueueItem(item);

    try {
      if (StringUtils.hasText(uavCode)) {
        mqttCommandPublisher.publish(
            uavCode,
            Map.of(
                "type", "interrupt",
                "missionCode", mission.getMissionCode(),
                "reason", reason));
      }
    } catch (Exception e) {
      log.warn("Failed to send preempt interrupt to uav {} mission {}", uavCode, mission.getMissionCode(), e);
    }

    markMissionStatus(mission.getMissionCode(), MissionStatus.PREEMPTED);
    writeEvent(
        mission,
        "MISSION_PREEMPTED",
        payloadOf(
            "uavCode", uavCode,
            "reason", reason,
            "triggeringMissionCode",
            triggeringMission != null ? triggeringMission.getMissionCode() : null,
            "preemptedCount", item.getPreemptedCount()));
    return true;
  }

  private boolean canPreempt(MissionQueueItem waitingItem, DispatchCandidate candidate) {
    Mission runningMission = candidate.getOccupiedMission();
    if (runningMission == null) {
      return false;
    }
    if (priorityWeight(waitingItem.getPriority()) < priorityWeight("HIGH")) {
      return false;
    }
    if (priorityWeight(runningMission.getPriority()) >= priorityWeight(waitingItem.getPriority())) {
      return false;
    }
    if ((runningMission.getProgress() != null ? runningMission.getProgress() : 0) >= 80) {
      return false;
    }
    return candidate.getSnapshot().batteryPercent() != null
        && candidate.getSnapshot().batteryPercent() >= waitingItem.getRequiredBatteryPercent();
  }

  private void markMissionByUav(String uavCode, MissionStatus target) {
    UavDevice device =
        uavDeviceMapper.selectOne(
            new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode));
    if (device == null) {
      return;
    }
    Optional<Mission> missionOpt = findRunningMissionByUav(device.getId());
    if (missionOpt.isEmpty()) {
      missionOpt = findPendingMissionByUav(device.getId());
    }
    missionOpt.ifPresent(mission -> applyTelemetryTransition(mission, target));
  }

  private void markMissionById(String missionCode, MissionStatus target) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission == null) {
      return;
    }
    applyTelemetryTransition(mission, target);
  }

  private void applyTelemetryTransition(Mission mission, MissionStatus target) {
    switch (target) {
      case RUNNING -> {
        if ((MissionStatus.QUEUE.name().equals(mission.getStatus())
                || MissionStatus.PREEMPTED.name().equals(mission.getStatus()))
            && isAwaitingExecutionAck(mission.getMissionCode())) {
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
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission == null) {
      return;
    }
    mission.setStatus(status.name());
    switch (status) {
      case COMPLETED -> {
        mission.setProgress(100);
        dataAggregator.complete(mission);
        releaseAssignmentsForMission(mission.getId(), null);
      }
      case INTERRUPTED, PREEMPTED -> {
        dataAggregator.clear(missionCode);
        releaseAssignmentsForMission(mission.getId(), null);
      }
      case QUEUE -> dataAggregator.clear(missionCode);
      default -> {}
    }
    missionMapper.updateById(mission);
    pushStatusUpdate(mission);
    if (status == MissionStatus.COMPLETED || status == MissionStatus.INTERRUPTED) {
      removeFromQueue(missionCode);
    }
  }

  private Optional<Mission> findMission(String missionCode) {
    return Optional.ofNullable(
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode)));
  }

  private boolean isAwaitingExecutionAck(String missionCode) {
    Optional<MissionQueueItem> itemOpt = loadQueueItem(missionCode);
    if (itemOpt.isEmpty()) {
      return false;
    }
    Long dispatchedAt = itemOpt.get().getDispatchedAt();
    if (dispatchedAt == null) {
      return false;
    }
    return Instant.now().toEpochMilli() - dispatchedAt < 60_000L;
  }

  private MissionQueueItem buildQueueItemFromMission(
      Mission mission,
      List<List<Double>> route,
      List<String> candidateUavCodes,
      String priority,
      long enqueuedAt,
      int preemptedCount,
      Long lastPreemptedAt,
      String reason) {
    return buildQueueItem(
        mission.getMissionCode(),
        route,
        candidateUavCodes,
        priority,
        enqueuedAt,
        preemptedCount,
        lastPreemptedAt,
        reason);
  }

  private MissionQueueItem buildQueueItem(
      String missionCode,
      List<List<Double>> route,
      List<String> candidateUavCodes,
      String priority,
      long enqueuedAt,
      int preemptedCount,
      Long lastPreemptedAt,
      String reason) {
    double routeDistanceKm = estimateRouteDistanceKm(route);
    double estimatedDurationMinutes =
        routeDistanceKm <= 0 ? 1.0 : (routeDistanceKm * 1000.0) / DEFAULT_SPEED_MPS / 60.0;
    double requiredBatteryPercent =
        Math.max(
            MIN_REQUIRED_BATTERY,
            Math.min(95.0, estimatedDurationMinutes * DISCHARGE_PER_MINUTE + SAFETY_BATTERY_RESERVE));

    MissionQueueItem item = new MissionQueueItem();
    item.setMissionCode(missionCode);
    item.setCandidateUavCodes(candidateUavCodes == null ? List.of() : candidateUavCodes);
    item.setRoute(route);
    item.setPriority(normalizePriority(priority));
    item.setEnqueuedAt(enqueuedAt);
    item.setDispatchedAt(null);
    item.setRouteDistanceKm(routeDistanceKm);
    item.setEstimatedDurationMinutes(estimatedDurationMinutes);
    item.setRequiredBatteryPercent(requiredBatteryPercent);
    item.setPreemptedCount(preemptedCount);
    item.setLastPreemptedAt(lastPreemptedAt);
    item.setLastSchedulingReason(reason);
    return item;
  }

  private void saveQueueItem(MissionQueueItem item) {
    try {
      redisTemplate.opsForValue().set(KEY_PREFIX + item.getMissionCode(), objectMapper.writeValueAsString(item));
    } catch (Exception e) {
      log.warn("Failed to persist mission queue item {}", item.getMissionCode(), e);
    }
  }

  private Optional<MissionQueueItem> loadQueueItem(String missionCode) {
    try {
      String payload = redisTemplate.opsForValue().get(KEY_PREFIX + missionCode);
      if (!StringUtils.hasText(payload)) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(payload, MissionQueueItem.class));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private List<MissionQueueItem> loadQueueItems() {
    Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
    if (keys == null || keys.isEmpty()) {
      return List.of();
    }
    List<MissionQueueItem> items = new ArrayList<>();
    for (String key : keys) {
      try {
        String payload = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(payload)) {
          continue;
        }
        items.add(objectMapper.readValue(payload, MissionQueueItem.class));
      } catch (Exception e) {
        log.debug("Ignore malformed queue item {}", key, e);
      }
    }
    return items;
  }

  private List<String> resolveCandidateUavCodes(MissionQueueItem item) {
    if (!CollectionUtils.isEmpty(item.getCandidateUavCodes())) {
      return item.getCandidateUavCodes();
    }
    return uavDeviceMapper.selectList(null).stream().map(UavDevice::getUavCode).toList();
  }

  private double queuePriorityScore(MissionQueueItem item) {
    return priorityBaseScore(item.getPriority()) + agingScore(item);
  }

  private double agingScore(MissionQueueItem item) {
    long now = Instant.now().toEpochMilli();
    double waitMinutes = Math.max(0, now - item.getEnqueuedAt()) / 60_000.0;
    return Math.min(40.0, waitMinutes * 2.0);
  }

  private double priorityBaseScore(String priority) {
    return switch (normalizePriority(priority)) {
      case "HIGH" -> 100.0;
      case "LOW" -> 30.0;
      default -> 60.0;
    };
  }

  private double computeBatteryFitness(MissionQueueItem item, TelemetrySnapshot snapshot) {
    if (snapshot.batteryPercent() == null) {
      return 0;
    }
    double margin = snapshot.batteryPercent() - item.getRequiredBatteryPercent();
    return clamp(40.0 + margin * 2.0, 0.0, 100.0);
  }

  private double computeDurationFitness(MissionQueueItem item) {
    return clamp(100.0 - item.getEstimatedDurationMinutes() * 2.0, 20.0, 100.0);
  }

  private TelemetrySnapshot readSnapshot(String uavCode) {
    String payload = telemetryService.readTelemetry(uavCode);
    if (!StringUtils.hasText(payload)) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(payload);
      String status = text(node, "status");
      Double battery = number(node, "battery");
      if (battery == null) {
        battery = number(node, "batteryPercent");
      }
      Double linkQuality = number(node, "linkQuality");
      Double rangeKm = number(node, "rangeKm");
      Double lat = number(node, "lat");
      Double lng = number(node, "lng");
      String missionId = text(node, "missionId");
      if (!StringUtils.hasText(missionId)) {
        missionId = text(node, "missionCode");
      }
      int healthScore = calculateHealthScore(status, battery, linkQuality, lat, lng);
      return new TelemetrySnapshot(uavCode, status, battery, linkQuality, rangeKm, lat, lng, missionId, healthScore);
    } catch (Exception e) {
      return null;
    }
  }

  private int calculateHealthScore(
      String status, Double battery, Double linkQuality, Double lat, Double lng) {
    String normalizedStatus = status != null ? status.trim().toUpperCase() : "";
    if ("OFFLINE".equals(normalizedStatus) || "CRITICAL".equals(normalizedStatus)) {
      return 0;
    }
    int score = 30;
    if (battery != null) {
      if (battery >= 80) {
        score += 30;
      } else if (battery >= 60) {
        score += 24;
      } else if (battery >= 40) {
        score += 16;
      } else if (battery >= 25) {
        score += 8;
      }
    }
    if (linkQuality != null) {
      score += (int) Math.round(clamp(linkQuality, 0, 1) * 25.0);
    } else {
      score += "WARNING".equals(normalizedStatus) ? 10 : 18;
    }
    if (battery != null && lat != null && lng != null && StringUtils.hasText(status)) {
      score += 15;
    } else if (battery != null && StringUtils.hasText(status)) {
      score += 8;
    }
    if ("WARNING".equals(normalizedStatus)) {
      score -= 10;
    }
    return Math.max(score, 0);
  }

  private Optional<Mission> findRunningMissionByUav(Long uavId) {
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getUavId, uavId)
                .isNull(MissionUavAssignment::getReleasedAt));
    if (assignments.isEmpty()) {
      return Optional.empty();
    }
    List<Long> missionIds = assignments.stream().map(MissionUavAssignment::getMissionId).toList();
    return missionMapper.selectList(
            new LambdaQueryWrapper<Mission>()
                .in(Mission::getId, missionIds)
                .eq(Mission::getStatus, MissionStatus.RUNNING.name()))
        .stream()
        .findFirst();
  }

  private Optional<Mission> findPendingMissionByUav(Long uavId) {
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getUavId, uavId)
                .isNull(MissionUavAssignment::getReleasedAt));
    if (assignments.isEmpty()) {
      return Optional.empty();
    }
    List<Long> missionIds = assignments.stream().map(MissionUavAssignment::getMissionId).toList();
    return missionMapper.selectList(
            new LambdaQueryWrapper<Mission>()
                .in(Mission::getId, missionIds)
                .in(
                    Mission::getStatus,
                    List.of(MissionStatus.QUEUE.name(), MissionStatus.PREEMPTED.name())))
        .stream()
        .findFirst();
  }

  private List<String> findActiveAssignedUavCodes(Long missionId) {
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getMissionId, missionId)
                .isNull(MissionUavAssignment::getReleasedAt));
    if (assignments.isEmpty()) {
      return List.of();
    }
    List<Long> uavIds = assignments.stream().map(MissionUavAssignment::getUavId).toList();
    return uavDeviceMapper.selectBatchIds(uavIds).stream().map(UavDevice::getUavCode).toList();
  }

  private void ensureActiveAssignment(Long missionId, Long uavId) {
    Long count =
        assignmentMapper.selectCount(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getMissionId, missionId)
                .eq(MissionUavAssignment::getUavId, uavId)
                .isNull(MissionUavAssignment::getReleasedAt));
    if (count != null && count > 0) {
      return;
    }
    MissionUavAssignment assignment = new MissionUavAssignment();
    assignment.setMissionId(missionId);
    assignment.setUavId(uavId);
    assignment.setAssignedAt(Instant.now());
    assignmentMapper.insert(assignment);
  }

  private void releaseAssignmentsForMission(Long missionId, Long keepUavId) {
    List<MissionUavAssignment> assignments =
        assignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getMissionId, missionId)
                .isNull(MissionUavAssignment::getReleasedAt));
    for (MissionUavAssignment assignment : assignments) {
      if (keepUavId != null && keepUavId.equals(assignment.getUavId())) {
        continue;
      }
      assignment.setReleasedAt(Instant.now());
      assignmentMapper.updateById(assignment);
    }
  }

  private List<List<Double>> findMissionRoute(Long missionId) {
    return routePointMapper.selectList(
            new LambdaQueryWrapper<MissionRoutePoint>()
                .eq(MissionRoutePoint::getMissionId, missionId)
                .orderByAsc(MissionRoutePoint::getSeq))
        .stream()
        .map(point -> List.of(point.getLat().doubleValue(), point.getLng().doubleValue()))
        .toList();
  }

  private void writeEvent(Mission mission, String eventType, Map<String, Object> payload) {
    if (mission == null) {
      return;
    }
    try {
      MissionEvent event = new MissionEvent();
      event.setMissionId(mission.getId());
      event.setEventType(eventType);
      event.setPayload(objectMapper.writeValueAsString(payload));
      event.setOccurredAt(Instant.now());
      missionEventMapper.insert(event);
    } catch (Exception e) {
      log.debug("Failed to write mission event {} for {}", eventType, mission.getMissionCode(), e);
    }
  }

  private int priorityWeight(String priority) {
    return switch (normalizePriority(priority)) {
      case "HIGH" -> 3;
      case "LOW" -> 1;
      default -> 2;
    };
  }

  private String normalizePriority(String priority) {
    if (!StringUtils.hasText(priority)) {
      return "MEDIUM";
    }
    String normalized = priority.trim().toUpperCase();
    return switch (normalized) {
      case "HIGH", "高", "P1", "URGENT" -> "HIGH";
      case "LOW", "低", "P3" -> "LOW";
      default -> "MEDIUM";
    };
  }

  private double estimateRouteDistanceKm(List<List<Double>> route) {
    if (route == null || route.size() < 2) {
      return 0;
    }
    double distanceKm = 0;
    for (int i = 1; i < route.size(); i++) {
      List<Double> prev = route.get(i - 1);
      List<Double> curr = route.get(i);
      distanceKm += haversineKm(prev.get(0), prev.get(1), curr.get(0), curr.get(1));
    }
    return distanceKm;
  }

  private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
    double earthRadiusKm = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
  }

  private String text(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.get(field).asText(null) : null;
  }

  private Double number(JsonNode node, String field) {
    return node.hasNonNull(field) ? node.get(field).asDouble() : null;
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private String formatBlockedReason(DispatchCandidate candidate, MissionQueueItem item) {
    String reason = candidate.getReason();
    return switch (reason) {
      case "status_not_dispatchable" ->
          candidate.getUavCode() + " 当前状态为 " + readableStatus(candidate.getSnapshot().status());
      case "occupied_by_higher_or_equal_priority" ->
          candidate.getUavCode() + " 已被同级或更高优先级任务占用";
      default -> {
        if (reason != null && reason.startsWith("battery_insufficient")) {
          double current = candidate.getSnapshot().batteryPercent() == null ? 0.0 : candidate.getSnapshot().batteryPercent();
          yield candidate.getUavCode()
              + " 电量不足（"
              + Math.round(current * 10.0) / 10.0
              + "% < "
              + Math.round(item.getRequiredBatteryPercent() * 10.0) / 10.0
              + "%）";
        }
        if (reason != null && reason.startsWith("health_low")) {
          yield candidate.getUavCode() + " 健康度过低（" + candidate.getSnapshot().healthScore() + "）";
        }
        yield candidate.getUavCode() + " 当前不可调度";
      }
    };
  }

  private String readableStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return "未知";
    }
    return switch (status.trim().toUpperCase()) {
      case "IDLE" -> "空闲待命";
      case "ONLINE" -> "在线";
      case "OFFLINE" -> "离线";
      case "WARNING" -> "链路预警";
      case "CRITICAL" -> "严重异常";
      case "RUNNING", "EXECUTING" -> "任务执行中";
      case "RETURNING" -> "返航中";
      case "PENDING_CONNECT" -> "待接入";
      default -> status;
    };
  }

  private Map<String, Object> payloadOf(Object... keyValues) {
    java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
    for (int i = 0; i + 1 < keyValues.length; i += 2) {
      payload.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
    }
    return payload;
  }

  private record QueueEvaluation(
      DispatchCandidate dispatchCandidate,
      DispatchCandidate preemptCandidate,
      String blockedReason) {}

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class MissionQueueItem {
    private String missionCode;
    private List<String> candidateUavCodes;
    private List<List<Double>> route;
    private String priority;
    private long enqueuedAt;
    private Long dispatchedAt;
    private double routeDistanceKm;
    private double estimatedDurationMinutes;
    private double requiredBatteryPercent;
    private int preemptedCount;
    private Long lastPreemptedAt;
    private String lastSchedulingReason;
  }

  @Data
  @AllArgsConstructor
  private static class DispatchCandidate {
    private String uavCode;
    private UavDevice device;
    private TelemetrySnapshot snapshot;
    private Mission occupiedMission;
    private boolean occupied;
    private boolean eligible;
    private double batteryFitness;
    private double durationFitness;
    private double totalScore;
    private String reason;
  }

  private record TelemetrySnapshot(
      String uavCode,
      String status,
      Double batteryPercent,
      Double linkQuality,
      Double rangeKm,
      Double lat,
      Double lng,
      String missionId,
      int healthScore) {
    private boolean dispatchable() {
      if (!StringUtils.hasText(status)) {
        return true;
      }
      String normalized = status.trim().toUpperCase();
      return !"OFFLINE".equals(normalized)
          && !"CRITICAL".equals(normalized)
          && !"RETURNING".equals(normalized)
          && !"EXECUTING".equals(normalized)
          && !"RUNNING".equals(normalized);
    }
  }
}
