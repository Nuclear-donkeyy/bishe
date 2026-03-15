package com.example.uavbackend.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.alert.AlertRecord;
import com.example.uavbackend.alert.AlertRecordMapper;
import com.example.uavbackend.auth.AccessScope;
import com.example.uavbackend.auth.AccessScopeService;
import com.example.uavbackend.analytics.dto.AnalyticsDefinitionDto;
import com.example.uavbackend.analytics.dto.AnalyticsMetricOptionDto;
import com.example.uavbackend.analytics.dto.AnalyticsReplayDto;
import com.example.uavbackend.analytics.dto.AnalyticsReplayEventDto;
import com.example.uavbackend.analytics.dto.AnalyticsReplayPointDto;
import com.example.uavbackend.analytics.dto.AnalyticsReplaySampleDto;
import com.example.uavbackend.analytics.dto.AnalyticsSeriesDto;
import com.example.uavbackend.analytics.dto.AnalyticsSeriesPointDto;
import com.example.uavbackend.analytics.dto.AnalyticsTimeSeriesDto;
import com.example.uavbackend.analytics.dto.MissionComparisonDto;
import com.example.uavbackend.analytics.dto.MissionDataRecordDto;
import com.example.uavbackend.analytics.dto.TaskExecutionDto;
import com.example.uavbackend.configcenter.MetricDefinition;
import com.example.uavbackend.configcenter.MetricDefinitionMapper;
import com.example.uavbackend.configcenter.MissionTypeMetric;
import com.example.uavbackend.configcenter.MissionTypeMetricMapper;
import com.example.uavbackend.fleet.UavDevice;
import com.example.uavbackend.fleet.UavDeviceMapper;
import com.example.uavbackend.fleet.UavTelemetry;
import com.example.uavbackend.fleet.UavTelemetryMapper;
import com.example.uavbackend.mission.Mission;
import com.example.uavbackend.mission.MissionEvent;
import com.example.uavbackend.mission.MissionEventMapper;
import com.example.uavbackend.mission.MissionMapper;
import com.example.uavbackend.mission.MissionRoutePoint;
import com.example.uavbackend.mission.MissionRoutePointMapper;
import com.example.uavbackend.mission.MissionUavAssignment;
import com.example.uavbackend.mission.MissionUavAssignmentMapper;
import com.example.uavbackend.mission.MissionTypeDefinition;
import com.example.uavbackend.mission.MissionTypeMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
  private static final ZoneId ZONE_ID = ZoneId.systemDefault();
  private static final String KEY_DURATION = "derived::durationMinutes";
  private static final String KEY_AVG_SPEED = "derived::avgSpeedKmh";
  private static final String KEY_BATTERY_CONSUMPTION = "derived::batteryConsumption";
  private static final String KEY_ALERT_COUNT = "derived::alertCount";
  private static final String KEY_SUCCESS_RATE = "derived::successRate";
  private static final double DEFAULT_SPEED_KMH = 108d;
  private static final double DEFAULT_BATTERY_DRAIN_PER_SECOND = 0.1d;

  private final AnalyticsDefinitionMapper definitionMapper;
  private final TaskExecutionMapper taskExecutionMapper;
  private final MissionDataRecordMapper recordMapper;
  private final MissionMapper missionMapper;
  private final MissionRoutePointMapper routePointMapper;
  private final MissionEventMapper missionEventMapper;
  private final MissionUavAssignmentMapper missionUavAssignmentMapper;
  private final AlertRecordMapper alertRecordMapper;
  private final MissionTypeMapper missionTypeMapper;
  private final MissionTypeMetricMapper missionTypeMetricMapper;
  private final MetricDefinitionMapper metricDefinitionMapper;
  private final UavTelemetryMapper uavTelemetryMapper;
  private final UavDeviceMapper uavDeviceMapper;
  private final ObjectMapper objectMapper;
  private final AccessScopeService accessScopeService;

  public List<AnalyticsDefinitionDto> definitions(String missionType) {
    LambdaQueryWrapper<AnalyticsDefinition> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(missionType)) {
      wrapper.and(
          w ->
              w.eq(AnalyticsDefinition::getMissionType, missionType)
                  .or()
                  .eq(AnalyticsDefinition::getMissionType, resolveMissionTypeCode(missionType)));
    }
    wrapper.orderByAsc(AnalyticsDefinition::getDisplayOrder);
    List<AnalyticsDefinition> defs = definitionMapper.selectList(wrapper);
    if (!defs.isEmpty()) {
      return defs.stream()
          .map(
              def ->
                  new AnalyticsDefinitionDto(
                      def.getId(),
                      def.getMissionType(),
                      def.getTitle(),
                      def.getDescription(),
                      def.getSeriesConfig()))
          .toList();
    }
    return buildFallbackDefinitions(missionType);
  }

  public List<TaskExecutionDto> taskExecutions(String missionType, Instant from, Instant to) {
    AccessScope scope = accessScopeService.currentScope();
    List<TaskExecutionDto> persisted = loadPersistedTaskExecutions(missionType, from, to);
    List<TaskExecutionDto> scopedPersisted =
        persisted.stream().filter(item -> canAccessOwner(scope, item.ownerName())).toList();
    if (!scopedPersisted.isEmpty()) {
      return scopedPersisted;
    }
    return deriveTaskExecutions(missionType, from, to, scope);
  }

  public List<MissionComparisonDto> compare(List<String> missionCodes) {
    AccessScope scope = accessScopeService.currentScope();
    if (missionCodes == null || missionCodes.size() < 2 || missionCodes.size() > 5) {
      throw new IllegalArgumentException("请选择 2 到 5 个任务进行对比");
    }
    List<String> normalizedMissionCodes =
        missionCodes.stream().filter(StringUtils::hasText).distinct().toList();
    normalizedMissionCodes = filterAccessibleMissionCodes(normalizedMissionCodes, scope);
    if (normalizedMissionCodes.size() < 2 || normalizedMissionCodes.size() > 5) {
      throw new IllegalArgumentException("请选择 2 到 5 个任务进行对比");
    }
    List<MissionDataRecord> records =
        recordMapper.selectList(
            new LambdaQueryWrapper<MissionDataRecord>()
                .in(MissionDataRecord::getMissionCode, normalizedMissionCodes)
                .orderByDesc(MissionDataRecord::getEndTime));
    if (records.isEmpty()) {
      return compareFromMissionFallback(normalizedMissionCodes);
    }
    Map<String, MissionDataRecord> latestRecordByMissionCode = latestRecordByMissionCode(records);
    Map<String, Mission> missionByCode = loadMissionsByCodes(latestRecordByMissionCode.keySet());
    Map<String, Integer> alertCounts = loadAlertCounts(latestRecordByMissionCode.keySet());
    Map<Long, Double> routeDistanceByMissionId = loadRouteDistanceByMissionId(missionByCode.values());

    List<MissionComparisonDto> comparisons = new ArrayList<>();
    for (String missionCode : normalizedMissionCodes) {
      MissionDataRecord record = latestRecordByMissionCode.get(missionCode);
      if (record == null) {
        continue;
      }
      Mission mission = missionByCode.get(missionCode);
      comparisons.add(toComparison(record, mission, alertCounts.getOrDefault(missionCode, 0), routeDistanceByMissionId));
    }
    return comparisons;
  }

  public AnalyticsTimeSeriesDto timeseries(String missionCode, List<String> metrics) {
    ReplayContext context = loadReplayContext(missionCode);
    List<AnalyticsReplaySampleDto> samples = context.samples();
    List<AnalyticsMetricOptionDto> metricOptions = resolveReplayMetricOptions(samples);

    Set<String> selectedMetrics = new LinkedHashSet<>();
    if (metrics != null) {
      metrics.stream().filter(StringUtils::hasText).forEach(selectedMetrics::add);
    }
    if (selectedMetrics.isEmpty()) {
      metricOptions.stream().limit(4).map(AnalyticsMetricOptionDto::metricCode).forEach(selectedMetrics::add);
    }

    List<AnalyticsSeriesDto> series =
        selectedMetrics.stream()
            .map(metricCode -> buildSeries(metricCode, samples))
            .filter(seriesDto -> !seriesDto.points().isEmpty())
            .toList();

    return new AnalyticsTimeSeriesDto(
        context.mission().getMissionCode(),
        context.mission().getName(),
        context.mission().getMissionType(),
        context.uavCode(),
        context.startTime(),
        context.endTime(),
        metricOptions,
        series);
  }

  public AnalyticsReplayDto replay(String missionCode) {
    ReplayContext context = loadReplayContext(missionCode);
    List<AnalyticsReplaySampleDto> samples = context.samples();
    List<AnalyticsReplayPointDto> plannedRoute =
        context.routePoints().stream()
            .map(
                point ->
                    new AnalyticsReplayPointDto(
                        point.getSeq(),
                        point.getLat() == null ? null : point.getLat().doubleValue(),
                        point.getLng() == null ? null : point.getLng().doubleValue(),
                        point.getAltitude() == null ? null : point.getAltitude().doubleValue(),
                        "PLANNED",
                        null))
            .toList();
    List<AnalyticsReplayPointDto> actualTrack =
        samples.stream()
            .filter(sample -> sample.lat() != null && sample.lng() != null)
            .map(
                sample ->
                    new AnalyticsReplayPointDto(
                        null,
                        sample.lat(),
                        sample.lng(),
                        sample.altitude(),
                        "ACTUAL",
                        sample.reportedAt()))
            .toList();

    return new AnalyticsReplayDto(
        context.mission().getMissionCode(),
        context.mission().getName(),
        context.mission().getMissionType(),
        context.mission().getStatus(),
        context.uavCode(),
        context.startTime(),
        context.endTime(),
        context.durationMinutes(),
        round(computeRouteDistanceKm(context.routePoints())),
        samples.size(),
        resolveReplayMetricOptions(samples),
        plannedRoute,
        actualTrack,
        buildReplayTimeline(context),
        samples);
  }

  private List<TaskExecutionDto> loadPersistedTaskExecutions(String missionType, Instant from, Instant to) {
    if (!StringUtils.hasText(missionType)) {
      return List.of();
    }
    LambdaQueryWrapper<TaskExecution> wrapper = new LambdaQueryWrapper<>();
    wrapper.and(
        w ->
            w.eq(TaskExecution::getMissionType, missionType)
                .or()
                .eq(TaskExecution::getMissionType, resolveMissionTypeCode(missionType)));
    if (from != null && to != null) {
      wrapper.between(TaskExecution::getCompletedAt, from, to);
    } else if (from != null) {
      wrapper.ge(TaskExecution::getCompletedAt, from);
    } else if (to != null) {
      wrapper.le(TaskExecution::getCompletedAt, to);
    }
    wrapper.orderByAsc(TaskExecution::getCompletedAt);
    return taskExecutionMapper.selectList(wrapper).stream().map(this::toDto).toList();
  }

  private List<TaskExecutionDto> deriveTaskExecutions(String missionType, Instant from, Instant to, AccessScope scope) {
    if (!StringUtils.hasText(missionType)) {
      return List.of();
    }
    LambdaQueryWrapper<MissionDataRecord> wrapper =
        new LambdaQueryWrapper<MissionDataRecord>()
            .and(
                w ->
                    w.eq(MissionDataRecord::getMissionType, missionType)
                        .or()
                        .eq(MissionDataRecord::getMissionType, resolveMissionTypeCode(missionType)));
    if (from != null) {
      wrapper.ge(MissionDataRecord::getEndTime, LocalDateTime.ofInstant(from, ZONE_ID));
    }
    if (to != null) {
      wrapper.le(MissionDataRecord::getEndTime, LocalDateTime.ofInstant(to, ZONE_ID));
    }
    wrapper.orderByAsc(MissionDataRecord::getEndTime);
    List<MissionDataRecord> records = recordMapper.selectList(wrapper);
    if (!scope.superAdmin()) {
      records =
          records.stream()
              .filter(record -> canAccessOwner(scope, record.getOperatorName(), record.getPilotName()))
              .toList();
    }
    if (records.isEmpty()) {
      return deriveTaskExecutionsFromMissions(missionType, scope);
    }
    Map<String, MissionDataRecord> latestRecordByMissionCode = latestRecordByMissionCode(records);
    Map<String, Mission> missionByCode = loadMissionsByCodes(latestRecordByMissionCode.keySet());
    Map<String, Integer> alertCounts = loadAlertCounts(latestRecordByMissionCode.keySet());
    Map<Long, Double> routeDistanceByMissionId = loadRouteDistanceByMissionId(missionByCode.values());

    return latestRecordByMissionCode.values().stream()
        .sorted(Comparator.comparing(MissionDataRecord::getEndTime, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(record -> toTaskExecution(record, missionByCode.get(record.getMissionCode()), alertCounts.getOrDefault(record.getMissionCode(), 0), routeDistanceByMissionId))
        .toList();
  }

  private List<AnalyticsDefinitionDto> buildFallbackDefinitions(String missionType) {
    List<AnalyticsDefinitionDto> definitions = new ArrayList<>();
    definitions.add(
        definition(
            missionType,
            "任务时长趋势",
            "按任务完成时间展示任务时长变化",
            "line",
            List.of(series("任务时长", KEY_DURATION))));
    definitions.add(
        definition(
            missionType,
            "告警次数与成功率",
            "对比各次任务的告警负载与执行成功情况",
            "bar",
            List.of(series("告警次数", KEY_ALERT_COUNT), series("执行成功率", KEY_SUCCESS_RATE))));
    definitions.add(
        definition(
            missionType,
            "平均速度与电量消耗",
            "对比不同任务的飞行效率与能耗表现",
            "line",
            List.of(series("平均速度(km/h)", KEY_AVG_SPEED), series("电量消耗(%)", KEY_BATTERY_CONSUMPTION))));

    for (MetricDefinition metric : resolveMetricDefinitions(missionType)) {
      definitions.add(
          definition(
              missionType,
              metric.getName(),
              StringUtils.hasText(metric.getDescription()) ? metric.getDescription() : "按任务展示指标的平均值与峰值",
              "line",
              List.of(
                  series("平均值", metricKey("avg", metric.getMetricCode())),
                  series("峰值", metricKey("max", metric.getMetricCode())),
                  series("低值", metricKey("min", metric.getMetricCode())))));
    }
    if (definitions.size() > 3) {
      return definitions;
    }

    Set<String> dynamicMetricCodes = loadMetricCodesFromRecords(missionType);
    for (String metricCode : dynamicMetricCodes) {
      definitions.add(
          definition(
              missionType,
              metricCode,
              "按任务展示指标的平均值与峰值",
              "line",
              List.of(
                  series("平均值", metricKey("avg", metricCode)),
                  series("峰值", metricKey("max", metricCode)),
                  series("低值", metricKey("min", metricCode)))));
    }
    return definitions;
  }

  private List<MetricDefinition> resolveMetricDefinitions(String missionType) {
    MissionTypeDefinition missionTypeDefinition = resolveMissionType(missionType);
    if (missionTypeDefinition == null) {
      return List.of();
    }
    List<MissionTypeMetric> missionTypeMetrics =
        missionTypeMetricMapper.selectList(
            new LambdaQueryWrapper<MissionTypeMetric>()
                .eq(MissionTypeMetric::getMissionTypeId, missionTypeDefinition.getId())
                .orderByAsc(MissionTypeMetric::getDisplayOrder));
    if (missionTypeMetrics.isEmpty()) {
      return List.of();
    }
    List<Long> metricIds = missionTypeMetrics.stream().map(MissionTypeMetric::getMetricId).distinct().toList();
    Map<Long, MetricDefinition> metricById =
        metricDefinitionMapper.selectBatchIds(metricIds).stream()
            .collect(Collectors.toMap(MetricDefinition::getId, metric -> metric));
    return missionTypeMetrics.stream()
        .map(item -> metricById.get(item.getMetricId()))
        .filter(Objects::nonNull)
        .toList();
  }

  private Set<String> loadMetricCodesFromRecords(String missionType) {
    LambdaQueryWrapper<MissionDataRecord> wrapper =
        new LambdaQueryWrapper<MissionDataRecord>()
            .and(
                w ->
                    w.eq(MissionDataRecord::getMissionType, missionType)
                        .or()
                        .eq(MissionDataRecord::getMissionType, resolveMissionTypeCode(missionType)))
            .orderByDesc(MissionDataRecord::getEndTime)
            .last("limit 20");
    return recordMapper.selectList(wrapper).stream()
        .flatMap(record -> parseJsonMap(record.getDataAvg()).keySet().stream())
        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
  }

  private MissionComparisonDto toComparison(
      MissionDataRecord record,
      Mission mission,
      int alertCount,
      Map<Long, Double> routeDistanceByMissionId) {
    Map<String, Object> maxMap = parseJsonMap(record.getDataMax());
    Map<String, Object> minMap = parseJsonMap(record.getDataMin());
    Map<String, Object> avgMap = parseJsonMap(record.getDataAvg());
    DurationAndSpeed durationAndSpeed = computeDurationAndSpeed(record, mission, routeDistanceByMissionId);
    Double batteryConsumption = computeBatteryConsumption(maxMap, minMap);
    double successRate = mission != null && "COMPLETED".equalsIgnoreCase(mission.getStatus()) ? 100d : 0d;
    return new MissionComparisonDto(
        record.getMissionCode(),
        mission == null ? record.getMissionCode() : mission.getName(),
        record.getMissionType(),
        mission == null ? null : mission.getStatus(),
        record.getPilotName(),
        record.getUavCode(),
        record.getStartTime(),
        record.getEndTime(),
        durationAndSpeed.durationMinutes(),
        durationAndSpeed.avgSpeedKmh(),
        batteryConsumption,
        alertCount,
        successRate,
        avgMap,
        maxMap,
        minMap);
  }

  private List<MissionComparisonDto> compareFromMissionFallback(List<String> missionCodes) {
    Map<String, Mission> missionByCode = loadMissionsByCodes(missionCodes);
    Map<String, Integer> alertCounts = loadAlertCounts(missionCodes);
    Map<Long, Double> routeDistanceByMissionId = loadRouteDistanceByMissionId(missionByCode.values());
    Map<Long, List<MissionEvent>> missionEventsByMissionId = loadMissionEventsByMissionId(missionByCode.values());

    return missionCodes.stream()
        .map(missionByCode::get)
        .filter(Objects::nonNull)
        .map(
            mission -> {
              DerivedMissionExecution derived =
                  deriveMissionExecution(
                      mission,
                      routeDistanceByMissionId.getOrDefault(mission.getId(), 0d),
                      alertCounts.getOrDefault(mission.getMissionCode(), 0),
                      missionEventsByMissionId.getOrDefault(mission.getId(), List.of()));
              return new MissionComparisonDto(
                  mission.getMissionCode(),
                  mission.getName(),
                  mission.getMissionType(),
                  mission.getStatus(),
                  mission.getPilotName(),
                  null,
                  derived.startTime(),
                  derived.endTime(),
                  derived.durationMinutes(),
                  derived.avgSpeedKmh(),
                  derived.batteryConsumption(),
                  derived.alertCount(),
                  derived.successRate(),
                  Map.of(),
                  Map.of(),
                  Map.of());
            })
        .toList();
  }

  private List<TaskExecutionDto> deriveTaskExecutionsFromMissions(String missionType, AccessScope scope) {
    String missionTypeCode = resolveMissionTypeCode(missionType);
    List<Mission> missions =
        missionMapper.selectList(
            new LambdaQueryWrapper<Mission>()
                .and(
                    w ->
                        w.eq(Mission::getMissionType, missionType)
                            .or()
                            .eq(Mission::getMissionType, missionTypeCode))
                .orderByAsc(Mission::getId));
    if (!scope.superAdmin()) {
      missions = missions.stream().filter(mission -> canAccessMission(scope, mission)).toList();
    }
    if (missions.isEmpty()) {
      return List.of();
    }
    Map<String, Integer> alertCounts =
        loadAlertCounts(missions.stream().map(Mission::getMissionCode).toList());
    Map<Long, Double> routeDistanceByMissionId = loadRouteDistanceByMissionId(missions);
    Map<Long, List<MissionEvent>> missionEventsByMissionId = loadMissionEventsByMissionId(missions);

    return missions.stream()
        .map(
            mission -> {
              DerivedMissionExecution derived =
                  deriveMissionExecution(
                      mission,
                      routeDistanceByMissionId.getOrDefault(mission.getId(), 0d),
                      alertCounts.getOrDefault(mission.getMissionCode(), 0),
                      missionEventsByMissionId.getOrDefault(mission.getId(), List.of()));
              Map<String, Object> metrics = new LinkedHashMap<>();
              metrics.put(KEY_DURATION, derived.durationMinutes());
              metrics.put(KEY_AVG_SPEED, derived.avgSpeedKmh());
              metrics.put(KEY_BATTERY_CONSUMPTION, derived.batteryConsumption());
              metrics.put(KEY_ALERT_COUNT, derived.alertCount());
              metrics.put(KEY_SUCCESS_RATE, derived.successRate());
              return new TaskExecutionDto(
                  mission.getId(),
                  mission.getMissionCode(),
                  mission.getName(),
                  mission.getMissionType(),
                  null,
                  mission.getPilotName(),
                  toInstant(derived.endTime()),
                  toJson(metrics));
            })
        .toList();
  }

  private TaskExecutionDto toTaskExecution(
      MissionDataRecord record,
      Mission mission,
      int alertCount,
      Map<Long, Double> routeDistanceByMissionId) {
    Map<String, Object> maxMap = parseJsonMap(record.getDataMax());
    Map<String, Object> minMap = parseJsonMap(record.getDataMin());
    Map<String, Object> avgMap = parseJsonMap(record.getDataAvg());
    DurationAndSpeed durationAndSpeed = computeDurationAndSpeed(record, mission, routeDistanceByMissionId);
    Double batteryConsumption = computeBatteryConsumption(maxMap, minMap);
    double successRate = mission != null && "COMPLETED".equalsIgnoreCase(mission.getStatus()) ? 100d : 0d;

    Map<String, Object> derivedMetrics = new LinkedHashMap<>();
    derivedMetrics.put(KEY_DURATION, durationAndSpeed.durationMinutes());
    derivedMetrics.put(KEY_AVG_SPEED, durationAndSpeed.avgSpeedKmh());
    derivedMetrics.put(KEY_BATTERY_CONSUMPTION, batteryConsumption);
    derivedMetrics.put(KEY_ALERT_COUNT, alertCount);
    derivedMetrics.put(KEY_SUCCESS_RATE, successRate);
    avgMap.forEach((key, value) -> derivedMetrics.put(metricKey("avg", key), value));
    maxMap.forEach((key, value) -> derivedMetrics.put(metricKey("max", key), value));
    minMap.forEach((key, value) -> derivedMetrics.put(metricKey("min", key), value));

    return new TaskExecutionDto(
        record.getId(),
        record.getMissionCode(),
        mission == null ? record.getMissionCode() : mission.getName(),
        record.getMissionType(),
        record.getUavCode(),
        record.getPilotName(),
        toInstant(record.getEndTime()),
        toJson(derivedMetrics));
  }

  private TaskExecutionDto toDto(TaskExecution exec) {
    return new TaskExecutionDto(
        exec.getId(),
        exec.getExecutionCode(),
        exec.getMissionName(),
        exec.getMissionType(),
        exec.getLocation(),
        exec.getOwnerName(),
        exec.getCompletedAt(),
        exec.getMetrics());
  }

  private MissionTypeDefinition resolveMissionType(String missionType) {
    if (!StringUtils.hasText(missionType)) {
      return null;
    }
    return missionTypeMapper.selectOne(
        new LambdaQueryWrapper<MissionTypeDefinition>()
            .and(
                w ->
                    w.eq(MissionTypeDefinition::getDisplayName, missionType)
                        .or()
                        .eq(MissionTypeDefinition::getTypeCode, missionType))
            .last("limit 1"));
  }

  private String resolveMissionTypeCode(String missionType) {
    MissionTypeDefinition definition = resolveMissionType(missionType);
    return definition == null ? missionType : definition.getTypeCode();
  }

  private Map<String, MissionDataRecord> latestRecordByMissionCode(List<MissionDataRecord> records) {
    Map<String, MissionDataRecord> latestRecordByMissionCode = new LinkedHashMap<>();
    for (MissionDataRecord record : records) {
      latestRecordByMissionCode.merge(
          record.getMissionCode(),
          record,
          (left, right) -> {
            LocalDateTime leftEnd = left.getEndTime();
            LocalDateTime rightEnd = right.getEndTime();
            if (leftEnd == null) {
              return right;
            }
            if (rightEnd == null) {
              return left;
            }
            return rightEnd.isAfter(leftEnd) ? right : left;
          });
    }
    return latestRecordByMissionCode;
  }

  private Map<String, Mission> loadMissionsByCodes(Collection<String> missionCodes) {
    if (missionCodes.isEmpty()) {
      return Map.of();
    }
    AccessScope scope = accessScopeService.currentScope();
    return missionMapper
        .selectList(new LambdaQueryWrapper<Mission>().in(Mission::getMissionCode, missionCodes))
        .stream()
        .filter(mission -> canAccessMission(scope, mission))
        .collect(Collectors.toMap(Mission::getMissionCode, mission -> mission, (left, right) -> left));
  }

  private Map<String, Integer> loadAlertCounts(Collection<String> missionCodes) {
    if (missionCodes.isEmpty()) {
      return Map.of();
    }
    return alertRecordMapper
        .selectList(new LambdaQueryWrapper<AlertRecord>().in(AlertRecord::getMissionCode, missionCodes))
        .stream()
        .collect(Collectors.groupingBy(AlertRecord::getMissionCode, Collectors.summingInt(item -> 1)));
  }

  private Map<Long, Double> loadRouteDistanceByMissionId(Collection<Mission> missions) {
    List<Long> missionIds = missions.stream().map(Mission::getId).filter(Objects::nonNull).toList();
    if (missionIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, List<MissionRoutePoint>> routePointsByMissionId =
        routePointMapper
            .selectList(
                new LambdaQueryWrapper<MissionRoutePoint>()
                    .in(MissionRoutePoint::getMissionId, missionIds)
                    .orderByAsc(MissionRoutePoint::getMissionId, MissionRoutePoint::getSeq))
            .stream()
            .collect(Collectors.groupingBy(MissionRoutePoint::getMissionId, LinkedHashMap::new, Collectors.toList()));

    Map<Long, Double> routeDistanceByMissionId = new HashMap<>();
    routePointsByMissionId.forEach(
        (missionId, routePoints) -> routeDistanceByMissionId.put(missionId, computeRouteDistanceKm(routePoints)));
    return routeDistanceByMissionId;
  }

  private Map<Long, List<MissionEvent>> loadMissionEventsByMissionId(Collection<Mission> missions) {
    List<Long> missionIds = missions.stream().map(Mission::getId).filter(Objects::nonNull).toList();
    if (missionIds.isEmpty()) {
      return Map.of();
    }
    return missionEventMapper
        .selectList(
            new LambdaQueryWrapper<MissionEvent>()
                .in(MissionEvent::getMissionId, missionIds)
                .orderByAsc(MissionEvent::getOccurredAt))
        .stream()
        .collect(Collectors.groupingBy(MissionEvent::getMissionId, LinkedHashMap::new, Collectors.toList()));
  }

  private double computeRouteDistanceKm(List<MissionRoutePoint> routePoints) {
    if (routePoints == null || routePoints.size() < 2) {
      return 0d;
    }
    double totalMeters = 0d;
    for (int i = 1; i < routePoints.size(); i++) {
      MissionRoutePoint prev = routePoints.get(i - 1);
      MissionRoutePoint current = routePoints.get(i);
      totalMeters += distanceMeters(prev.getLat(), prev.getLng(), current.getLat(), current.getLng());
    }
    return totalMeters / 1000d;
  }

  private double distanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
    if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
      return 0d;
    }
    double lat1Rad = Math.toRadians(lat1.doubleValue());
    double lng1Rad = Math.toRadians(lng1.doubleValue());
    double lat2Rad = Math.toRadians(lat2.doubleValue());
    double lng2Rad = Math.toRadians(lng2.doubleValue());
    double dLat = lat2Rad - lat1Rad;
    double dLng = lng2Rad - lng1Rad;
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return 6371000d * c;
  }

  private DurationAndSpeed computeDurationAndSpeed(
      MissionDataRecord record, Mission mission, Map<Long, Double> routeDistanceByMissionId) {
    Double durationMinutes = null;
    if (record.getStartTime() != null && record.getEndTime() != null) {
      long seconds =
          java.time.Duration.between(record.getStartTime(), record.getEndTime()).getSeconds();
      durationMinutes = Math.max(seconds / 60d, 0d);
    }
    Double avgSpeedKmh = null;
    Map<String, Object> avgMap = parseJsonMap(record.getDataAvg());
    avgSpeedKmh = firstNumeric(avgMap, List.of("speed", "velocity", "velocityMs", "speedMps", "speed_kmh"));
    if (avgSpeedKmh != null) {
      if (avgSpeedKmh <= 100) {
        avgSpeedKmh = avgSpeedKmh * 3.6d;
      }
    } else if (durationMinutes != null && durationMinutes > 0 && mission != null) {
      double distanceKm = routeDistanceByMissionId.getOrDefault(mission.getId(), 0d);
      if (distanceKm > 0) {
        avgSpeedKmh = distanceKm / (durationMinutes / 60d);
      }
    }
    return new DurationAndSpeed(round(durationMinutes), round(avgSpeedKmh));
  }

  private DerivedMissionExecution deriveMissionExecution(
      Mission mission, double routeDistanceKm, int alertCount, List<MissionEvent> missionEvents) {
    LocalDateTime startTime = null;
    LocalDateTime endTime = null;
    if (missionEvents != null && !missionEvents.isEmpty()) {
      startTime = toLocalDateTime(missionEvents.get(0).getOccurredAt());
      endTime = toLocalDateTime(missionEvents.get(missionEvents.size() - 1).getOccurredAt());
    }
    if (startTime == null) {
      startTime = LocalDateTime.now().minusMinutes(5);
    }

    double estimatedDurationMinutes = routeDistanceKm > 0 ? (routeDistanceKm / DEFAULT_SPEED_KMH) * 60d : 3d;
    if (endTime == null || !endTime.isAfter(startTime)) {
      endTime = startTime.plusSeconds(Math.max((long) (estimatedDurationMinutes * 60d), 60L));
    }

    double durationMinutes =
        Math.max(java.time.Duration.between(startTime, endTime).toSeconds() / 60d, 1d);
    double avgSpeedKmh =
        routeDistanceKm > 0 ? routeDistanceKm / Math.max(durationMinutes / 60d, 0.01d) : DEFAULT_SPEED_KMH;
    double batteryConsumption = Math.min(durationMinutes * 60d * DEFAULT_BATTERY_DRAIN_PER_SECOND, 100d);
    double successRate = "COMPLETED".equalsIgnoreCase(mission.getStatus()) ? 100d : 0d;

    return new DerivedMissionExecution(
        startTime,
        endTime,
        round(durationMinutes),
        round(avgSpeedKmh),
        round(batteryConsumption),
        alertCount,
        successRate);
  }

  private Double computeBatteryConsumption(Map<String, Object> maxMap, Map<String, Object> minMap) {
    Double maxBattery = firstNumeric(maxMap, List.of("battery", "batteryPercent"));
    Double minBattery = firstNumeric(minMap, List.of("battery", "batteryPercent"));
    if (maxBattery == null || minBattery == null) {
      return null;
    }
    return round(Math.max(maxBattery - minBattery, 0d));
  }

  private Double firstNumeric(Map<String, Object> map, List<String> keys) {
    for (String key : keys) {
      Object value = map.get(key);
      if (value instanceof Number number) {
        return number.doubleValue();
      }
      if (value != null) {
        try {
          return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
          // ignore
        }
      }
    }
    return null;
  }

  private Map<String, Object> parseJsonMap(String json) {
    if (!StringUtils.hasText(json)) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      return Map.of();
    }
  }

  private String toJson(Map<String, Object> map) {
    try {
      return objectMapper.writeValueAsString(map);
    } catch (Exception e) {
      return "{}";
    }
  }

  private Instant toInstant(LocalDateTime time) {
    return time == null ? null : time.atZone(ZONE_ID).toInstant();
  }

  private LocalDateTime toLocalDateTime(Instant time) {
    return time == null ? null : LocalDateTime.ofInstant(time, ZONE_ID);
  }

  private ReplayContext loadReplayContext(String missionCode) {
    AccessScope scope = accessScopeService.currentScope();
    if (!StringUtils.hasText(missionCode)) {
      throw new IllegalArgumentException("任务编码不能为空");
    }
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode).last("limit 1"));
    if (mission == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    if (!canAccessMission(scope, mission)) {
      throw new IllegalArgumentException("无权访问该任务");
    }

    MissionDataRecord record =
        recordMapper.selectOne(
            new LambdaQueryWrapper<MissionDataRecord>()
                .eq(MissionDataRecord::getMissionCode, missionCode)
                .orderByDesc(MissionDataRecord::getEndTime)
                .last("limit 1"));

    List<MissionRoutePoint> routePoints =
        routePointMapper.selectList(
            new LambdaQueryWrapper<MissionRoutePoint>()
                .eq(MissionRoutePoint::getMissionId, mission.getId())
                .orderByAsc(MissionRoutePoint::getSeq));
    List<MissionEvent> missionEvents =
        missionEventMapper.selectList(
            new LambdaQueryWrapper<MissionEvent>()
                .eq(MissionEvent::getMissionId, mission.getId())
                .orderByAsc(MissionEvent::getOccurredAt));
    List<AlertRecord> alerts =
        alertRecordMapper.selectList(
            new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getMissionCode, missionCode)
                .orderByAsc(AlertRecord::getTriggeredAt));

    String uavCode = record != null ? record.getUavCode() : null;
    if (!StringUtils.hasText(uavCode)) {
      uavCode = resolveUavCodeFromMissionEvents(missionEvents);
    }
    if (!StringUtils.hasText(uavCode) && mission.getId() != null) {
      uavCode = resolveUavCodeFromAssignments(mission.getId());
    }

    Instant startTime = record != null ? toInstant(record.getStartTime()) : null;
    Instant endTime = record != null ? toInstant(record.getEndTime()) : null;
    if (startTime == null && !missionEvents.isEmpty()) {
      startTime = missionEvents.get(0).getOccurredAt();
    }
    if (endTime == null && !missionEvents.isEmpty()) {
      endTime = missionEvents.get(missionEvents.size() - 1).getOccurredAt();
    }

    List<UavTelemetry> telemetry = loadTelemetryForReplay(missionCode, uavCode, startTime, endTime);
    List<AnalyticsReplaySampleDto> samples = telemetry.stream().map(this::toReplaySample).toList();
    Double durationMinutes = null;
    if (startTime != null && endTime != null) {
      durationMinutes = round(Math.max(java.time.Duration.between(startTime, endTime).toSeconds() / 60d, 0d));
    }
    return new ReplayContext(mission, record, uavCode, startTime, endTime, durationMinutes, routePoints, missionEvents, alerts, samples);
  }

  private List<UavTelemetry> loadTelemetryForReplay(
      String missionCode, String uavCode, Instant startTime, Instant endTime) {
    List<UavTelemetry> telemetry =
        uavTelemetryMapper.selectList(
            new LambdaQueryWrapper<UavTelemetry>()
                .eq(UavTelemetry::getSessionCode, missionCode)
                .orderByAsc(UavTelemetry::getReportedAt));
    if (!telemetry.isEmpty()) {
      return telemetry;
    }
    if (!StringUtils.hasText(uavCode)) {
      return List.of();
    }
    UavDevice uav =
        uavDeviceMapper.selectOne(
            new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode).last("limit 1"));
    if (uav == null) {
      return List.of();
    }
    LambdaQueryWrapper<UavTelemetry> wrapper =
        new LambdaQueryWrapper<UavTelemetry>()
            .eq(UavTelemetry::getUavId, uav.getId())
            .orderByAsc(UavTelemetry::getReportedAt);
    if (startTime != null) {
      wrapper.ge(UavTelemetry::getReportedAt, startTime.minusSeconds(60));
    }
    if (endTime != null) {
      wrapper.le(UavTelemetry::getReportedAt, endTime.plusSeconds(60));
    }
    return uavTelemetryMapper.selectList(wrapper);
  }

  public List<MissionDataRecordDto> listMissionData(
      String missionType,
      String uavCode,
      String operatorName,
      String missionCode,
      LocalDateTime from,
      LocalDateTime to) {
    AccessScope scope = accessScopeService.currentScope();
    LambdaQueryWrapper<MissionDataRecord> wrapper =
        new LambdaQueryWrapper<MissionDataRecord>().eq(MissionDataRecord::getMissionType, missionType);
    if (uavCode != null) {
      wrapper.eq(MissionDataRecord::getUavCode, uavCode);
    }
    if (scope.superAdmin()) {
      if (operatorName != null) {
        wrapper.eq(MissionDataRecord::getOperatorName, operatorName);
      }
    } else {
      wrapper.and(
          w ->
              w.eq(MissionDataRecord::getOperatorName, scope.displayName())
                  .or()
                  .eq(MissionDataRecord::getPilotName, scope.displayName()));
    }
    if (missionCode != null) {
      wrapper.eq(MissionDataRecord::getMissionCode, missionCode);
    }
    if (from != null) {
      wrapper.ge(MissionDataRecord::getEndTime, from);
    }
    if (to != null) {
      wrapper.le(MissionDataRecord::getEndTime, to);
    }
    wrapper.orderByDesc(MissionDataRecord::getEndTime);
    return recordMapper.selectList(wrapper).stream().map(this::toMissionDataDto).toList();
  }

  private MissionDataRecordDto toMissionDataDto(MissionDataRecord r) {
    Map<String, Object> maxMap = MissionDataAggregator.jsonToMap(objectMapper, r.getDataMax());
    Map<String, Object> minMap = MissionDataAggregator.jsonToMap(objectMapper, r.getDataMin());
    Map<String, Object> avgMap = MissionDataAggregator.jsonToMap(objectMapper, r.getDataAvg());
    return new MissionDataRecordDto(
        r.getId(),
        r.getMissionId(),
        r.getMissionCode(),
        r.getMissionType(),
        r.getPilotName(),
        r.getUavCode(),
        r.getOperatorName(),
        r.getStartTime(),
        r.getEndTime(),
        maxMap,
        minMap,
        avgMap);
  }

  private boolean canAccessOwner(AccessScope scope, String... ownerNames) {
    if (scope.superAdmin()) {
      return true;
    }
    for (String ownerName : ownerNames) {
      if (StringUtils.hasText(ownerName) && ownerName.equals(scope.displayName())) {
        return true;
      }
    }
    return false;
  }

  private boolean canAccessMission(AccessScope scope, Mission mission) {
    return scope.superAdmin() || (mission != null && StringUtils.hasText(mission.getPilotName()) && mission.getPilotName().equals(scope.displayName()));
  }

  private List<String> filterAccessibleMissionCodes(List<String> missionCodes, AccessScope scope) {
    if (scope.superAdmin() || missionCodes.isEmpty()) {
      return missionCodes;
    }
    return missionMapper
        .selectList(
            new LambdaQueryWrapper<Mission>()
                .in(Mission::getMissionCode, missionCodes)
                .eq(Mission::getPilotName, scope.displayName()))
        .stream()
        .map(Mission::getMissionCode)
        .distinct()
        .toList();
  }

  private String resolveUavCodeFromMissionEvents(List<MissionEvent> missionEvents) {
    for (MissionEvent event : missionEvents) {
      Map<String, Object> payload = parseJsonMap(event.getPayload());
      Object uavCode = payload.get("uavCode");
      if (uavCode != null && StringUtils.hasText(uavCode.toString())) {
        return uavCode.toString();
      }
    }
    return null;
  }

  private String resolveUavCodeFromAssignments(Long missionId) {
    List<MissionUavAssignment> assignments =
        missionUavAssignmentMapper.selectList(
            new LambdaQueryWrapper<MissionUavAssignment>()
                .eq(MissionUavAssignment::getMissionId, missionId)
                .orderByAsc(MissionUavAssignment::getAssignedAt));
    return assignments.stream()
        .filter(item -> item.getReleasedAt() == null)
        .findFirst()
        .or(() -> assignments.stream().findFirst())
        .map(MissionUavAssignment::getUavId)
        .map(uavDeviceMapper::selectById)
        .map(UavDevice::getUavCode)
        .orElse(null);
  }

  private AnalyticsReplaySampleDto toReplaySample(UavTelemetry telemetry) {
    Map<String, Object> metrics = parseJsonMap(telemetry.getPayload());
    return new AnalyticsReplaySampleDto(
        telemetry.getReportedAt(),
        telemetry.getLocationLat() == null ? null : telemetry.getLocationLat().doubleValue(),
        telemetry.getLocationLng() == null ? null : telemetry.getLocationLng().doubleValue(),
        telemetry.getLocationAlt() == null ? null : telemetry.getLocationAlt().doubleValue(),
        telemetry.getBatteryPercent() == null ? null : telemetry.getBatteryPercent().doubleValue(),
        telemetry.getVelocityMs() == null ? null : telemetry.getVelocityMs().doubleValue(),
        metrics);
  }

  private List<AnalyticsMetricOptionDto> resolveReplayMetricOptions(List<AnalyticsReplaySampleDto> samples) {
    LinkedHashSet<String> metricCodes = new LinkedHashSet<>();
    if (samples.stream().anyMatch(sample -> sample.batteryPercent() != null)) {
      metricCodes.add("battery");
    }
    if (samples.stream().anyMatch(sample -> sample.velocityMs() != null)) {
      metricCodes.add("velocityMs");
    }
    if (samples.stream().anyMatch(sample -> sample.altitude() != null)) {
      metricCodes.add("altitude");
    }
    samples.forEach(sample -> metricCodes.addAll(sample.metrics().keySet()));
    return metricCodes.stream().map(this::toMetricOption).toList();
  }

  private AnalyticsMetricOptionDto toMetricOption(String metricCode) {
    String definitionCode = switch (metricCode) {
      case "battery", "batteryPercent" -> "BATTERY";
      case "velocityMs", "speed" -> "VELOCITY_MS";
      default -> metricCode;
    };
    MetricDefinition definition =
        metricDefinitionMapper.selectOne(
            new LambdaQueryWrapper<MetricDefinition>()
                .eq(MetricDefinition::getMetricCode, definitionCode)
                .last("limit 1"));
    if (definition != null) {
      return new AnalyticsMetricOptionDto(metricCode, definition.getName(), definition.getUnit());
    }
    return switch (metricCode) {
      case "battery", "batteryPercent" -> new AnalyticsMetricOptionDto(metricCode, "飞行电量", "%");
      case "velocityMs", "speed" -> new AnalyticsMetricOptionDto(metricCode, "飞行速度", "m/s");
      case "altitude" -> new AnalyticsMetricOptionDto(metricCode, "飞行高度", "m");
      default -> new AnalyticsMetricOptionDto(metricCode, metricCode, null);
    };
  }

  private AnalyticsSeriesDto buildSeries(String metricCode, List<AnalyticsReplaySampleDto> samples) {
    AnalyticsMetricOptionDto option = toMetricOption(metricCode);
    List<AnalyticsSeriesPointDto> points =
        samples.stream()
            .map(sample -> new AnalyticsSeriesPointDto(sample.reportedAt(), extractReplayMetricValue(sample, metricCode)))
            .filter(point -> point.value() != null)
            .toList();
    return new AnalyticsSeriesDto(metricCode, option.displayName(), option.unit(), points);
  }

  private Double extractReplayMetricValue(AnalyticsReplaySampleDto sample, String metricCode) {
    return switch (metricCode) {
      case "battery", "batteryPercent" -> sample.batteryPercent();
      case "velocityMs", "speed" -> sample.velocityMs();
      case "altitude" -> sample.altitude();
      default -> toNumber(sample.metrics().get(metricCode));
    };
  }

  private List<AnalyticsReplayEventDto> buildReplayTimeline(ReplayContext context) {
    List<AnalyticsReplayEventDto> timeline = new ArrayList<>();
    for (MissionEvent event : context.missionEvents()) {
      timeline.add(
          new AnalyticsReplayEventDto(
              "MISSION",
              event.getEventType(),
              missionEventTitle(event.getEventType()),
              missionEventDescription(event),
              event.getOccurredAt()));
    }
    for (AlertRecord alert : context.alerts()) {
      String description =
          String.format(
              "%s=%.2f，联动状态 %s",
              alert.getMetricCode(),
              alert.getMetricValue() == null ? 0d : alert.getMetricValue(),
              alert.getLinkageStatus() == null ? "PENDING" : alert.getLinkageStatus());
      timeline.add(
          new AnalyticsReplayEventDto(
              "ALERT",
              "ALERT_TRIGGERED",
              "触发报警",
              description,
              toInstant(alert.getTriggeredAt())));
    }
    if (timeline.isEmpty() && context.startTime() != null) {
      timeline.add(new AnalyticsReplayEventDto("MISSION", "MISSION_STARTED", "任务开始", "任务已进入复盘窗口", context.startTime()));
      if (context.endTime() != null) {
        timeline.add(new AnalyticsReplayEventDto("MISSION", "MISSION_COMPLETED", "任务结束", "任务已完成", context.endTime()));
      }
    }
    return timeline.stream()
        .sorted(Comparator.comparing(AnalyticsReplayEventDto::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private String missionEventTitle(String eventType) {
    return switch (eventType) {
      case "MISSION_ENQUEUED" -> "任务入队";
      case "MISSION_DISPATCHED" -> "任务派发";
      case "MISSION_PREEMPTED" -> "任务被抢占";
      case "MISSION_PREEMPTION_REQUESTED" -> "发起抢占";
      case "MISSION_COMPLETED" -> "任务完成";
      default -> eventType;
    };
  }

  private String missionEventDescription(MissionEvent event) {
    Map<String, Object> payload = parseJsonMap(event.getPayload());
    if (payload.isEmpty()) {
      return "无附加信息";
    }
    return payload.entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("，"));
  }

  private Double toNumber(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    try {
      return Double.parseDouble(value.toString());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private AnalyticsDefinitionDto definition(
      String missionType, String title, String description, String chartType, List<Map<String, String>> series) {
    Map<String, Object> config = new LinkedHashMap<>();
    config.put("chartType", chartType);
    config.put("series", series);
    return new AnalyticsDefinitionDto(null, missionType, title, description, toJson(config));
  }

  private Map<String, String> series(String name, String dataKey) {
    Map<String, String> config = new LinkedHashMap<>();
    config.put("name", name);
    config.put("dataKey", dataKey);
    return config;
  }

  private String metricKey(String aggregate, String metricCode) {
    return aggregate + "::" + metricCode;
  }

  private Double round(Double value) {
    if (value == null) {
      return null;
    }
    return Math.round(value * 100d) / 100d;
  }

  private record DurationAndSpeed(Double durationMinutes, Double avgSpeedKmh) {}

  private record DerivedMissionExecution(
      LocalDateTime startTime,
      LocalDateTime endTime,
      Double durationMinutes,
      Double avgSpeedKmh,
      Double batteryConsumption,
      Integer alertCount,
      Double successRate) {}

  private record ReplayContext(
      Mission mission,
      MissionDataRecord record,
      String uavCode,
      Instant startTime,
      Instant endTime,
      Double durationMinutes,
      List<MissionRoutePoint> routePoints,
      List<MissionEvent> missionEvents,
      List<AlertRecord> alerts,
      List<AnalyticsReplaySampleDto> samples) {}
}
