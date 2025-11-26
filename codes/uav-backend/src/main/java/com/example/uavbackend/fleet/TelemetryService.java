package com.example.uavbackend.fleet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TelemetryService {
  private static final String KEY_PREFIX = "uav:telemetry:";
  private final StringRedisTemplate redisTemplate;

  public TelemetryService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void upsertTelemetry(String uavCode, String payloadJson) {
    redisTemplate.opsForValue().set(KEY_PREFIX + uavCode, payloadJson);
  }

  public Map<String, String> readAllTelemetry() {
    var keys = redisTemplate.keys(KEY_PREFIX + "*");
    if (keys == null || keys.isEmpty()) {
      return Map.of();
    }
    List<String> keyList = keys.stream().toList();
    List<String> values = redisTemplate.opsForValue().multiGet(keyList);
    return keyList.stream()
        .collect(Collectors.toMap(k -> k.substring(KEY_PREFIX.length()), k -> values.get(keyList.indexOf(k))));
  }
}
