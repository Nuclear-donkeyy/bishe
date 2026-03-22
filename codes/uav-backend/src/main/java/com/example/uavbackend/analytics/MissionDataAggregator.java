package com.example.uavbackend.analytics;

import com.example.uavbackend.mission.Mission;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MissionDataAggregator {
  private static final String KEY_DURATION = "derived::durationMinutes";
  private static final String KEY_AVG_SPEED = "derived::avgSpeedKmh";
  private static final String KEY_BATTERY_CONSUMPTION = "derived::batteryConsumption";
  private static final String KEY_SUCCESS_RATE = "derived::successRate";

  private final MissionDataRecordMapper recordMapper;
  private final TaskExecutionMapper taskExecutionMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static class Stat {
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    double sum = 0;
    long count = 0;

    void update(double v) {
      min = Math.min(min, v);
      max = Math.max(max, v);
      sum += v;
      count++;
    }

    double avg() {
      return count == 0 ? 0 : sum / count;
    }
  }

  private static class Agg {
    LocalDateTime start = LocalDateTime.now();
    LocalDateTime end = null;
    String uavCode;
    String pilotName;
    String operatorName;
    String missionType;
    Long departmentId;
    String departmentName;
    Map<String, Stat> stats = new HashMap<>();
  }

  private final Map<String, Agg> cache = new ConcurrentHashMap<>();

  public void ingest(Mission mission, String uavCode, Map<String, Object> data) {
    if (mission == null || data == null || data.isEmpty()) return;
    Agg agg = cache.computeIfAbsent(mission.getMissionCode(), k -> new Agg());
    agg.uavCode = uavCode;
    agg.pilotName = mission.getPilotName();
    agg.operatorName = mission.getPilotName(); // 简化为同 pilotName，可按需调整
    agg.missionType = mission.getMissionType();
    agg.departmentId = mission.getDepartmentId();
    agg.departmentName = mission.getDepartmentName();
    data.forEach(
        (k, v) -> {
          if (v == null) return;
          double d;
          if (v instanceof Number n) {
            d = n.doubleValue();
          } else {
            try {
              d = Double.parseDouble(v.toString());
            } catch (Exception e) {
              return;
            }
          }
          Stat s = agg.stats.computeIfAbsent(k, key -> new Stat());
          s.update(d);
        });
  }

  public void complete(Mission mission) {
    if (mission == null) return;
    Agg agg = cache.remove(mission.getMissionCode());
    if (agg == null) return;
    agg.end = LocalDateTime.now();
    Map<String, Object> maxMap = new HashMap<>();
    Map<String, Object> minMap = new HashMap<>();
    Map<String, Object> avgMap = new HashMap<>();
    agg.stats.forEach(
        (k, s) -> {
          if (s.count == 0) return;
          maxMap.put(k, s.max);
          minMap.put(k, s.min);
          avgMap.put(k, s.avg());
        });
    MissionDataRecord record = new MissionDataRecord();
    record.setMissionId(mission.getId());
    record.setMissionCode(mission.getMissionCode());
    record.setMissionType(agg.missionType);
    record.setDepartmentId(agg.departmentId);
    record.setDepartmentName(agg.departmentName);
    record.setPilotName(agg.pilotName);
    record.setUavCode(agg.uavCode);
    record.setOperatorName(agg.operatorName);
    record.setStartTime(agg.start);
    record.setEndTime(agg.end);
    try {
      record.setDataMax(objectMapper.writeValueAsString(maxMap));
      record.setDataMin(objectMapper.writeValueAsString(minMap));
      record.setDataAvg(objectMapper.writeValueAsString(avgMap));
    } catch (Exception e) {
      // ignore serialization errors
    }
    recordMapper.insert(record);
    upsertTaskExecution(mission, agg, maxMap, minMap, avgMap, agg.end);
  }

  public void clear(String missionCode) {
    cache.remove(missionCode);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> jsonToMap(ObjectMapper mapper, String json) {
    if (json == null) return Map.of();
    try {
      return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      return Map.of();
    }
  }

  private void upsertTaskExecution(
      Mission mission,
      Agg agg,
      Map<String, Object> maxMap,
      Map<String, Object> minMap,
      Map<String, Object> avgMap,
      LocalDateTime completedAt) {
    TaskExecution execution =
        taskExecutionMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskExecution>()
                .eq(TaskExecution::getExecutionCode, mission.getMissionCode())
                .last("limit 1"));
    if (execution == null) {
      execution = new TaskExecution();
      execution.setExecutionCode(mission.getMissionCode());
    }

    double durationMinutes =
        Math.max(java.time.Duration.between(agg.start, completedAt).toSeconds() / 60d, 1d);
    Double avgSpeedKmh = firstNumeric(avgMap, "speed", "velocityMs");
    if (avgSpeedKmh != null && avgSpeedKmh <= 100d) {
      avgSpeedKmh = avgSpeedKmh * 3.6d;
    }
    Double batteryConsumption = computeBatteryConsumption(maxMap, minMap);

    Map<String, Object> metrics = new LinkedHashMap<>();
    metrics.put(KEY_DURATION, round(durationMinutes));
    metrics.put(KEY_AVG_SPEED, avgSpeedKmh == null ? null : round(avgSpeedKmh));
    metrics.put(KEY_BATTERY_CONSUMPTION, batteryConsumption == null ? null : round(batteryConsumption));
    metrics.put(KEY_SUCCESS_RATE, 100d);
    avgMap.forEach((key, value) -> metrics.put("avg::" + key, value));
    maxMap.forEach((key, value) -> metrics.put("max::" + key, value));
    minMap.forEach((key, value) -> metrics.put("min::" + key, value));

    execution.setMissionName(mission.getName());
    execution.setMissionType(mission.getMissionType());
    execution.setDepartmentId(agg.departmentId);
    execution.setDepartmentName(agg.departmentName);
    execution.setLocation(agg.uavCode);
    execution.setOwnerName(agg.operatorName);
    execution.setCompletedAt(completedAt.atZone(ZoneId.systemDefault()).toInstant());
    try {
      execution.setMetrics(objectMapper.writeValueAsString(metrics));
    } catch (Exception e) {
      execution.setMetrics("{}");
    }

    if (execution.getId() == null) {
      taskExecutionMapper.insert(execution);
    } else {
      taskExecutionMapper.updateById(execution);
    }
  }

  private Double computeBatteryConsumption(Map<String, Object> maxMap, Map<String, Object> minMap) {
    Double maxBattery = firstNumeric(maxMap, "battery", "batteryPercent");
    Double minBattery = firstNumeric(minMap, "battery", "batteryPercent");
    if (maxBattery == null || minBattery == null) {
      return null;
    }
    return Math.max(maxBattery - minBattery, 0d);
  }

  private Double firstNumeric(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      Object value = map.get(key);
      if (value instanceof Number number) {
        return number.doubleValue();
      }
      if (value != null) {
        try {
          return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
          // ignore malformed metric
        }
      }
    }
    return null;
  }

  private double round(double value) {
    return Math.round(value * 100d) / 100d;
  }
}
