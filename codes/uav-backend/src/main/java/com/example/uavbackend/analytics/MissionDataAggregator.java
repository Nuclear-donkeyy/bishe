package com.example.uavbackend.analytics;

import com.example.uavbackend.mission.Mission;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MissionDataAggregator {
  private final MissionDataRecordMapper recordMapper;
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
}

