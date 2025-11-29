package com.example.uavbackend.fleet;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.example.uavbackend.fleet.UavStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TelemetryService {
  private static final String KEY_PREFIX = "uav:telemetry:";
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();
  // TTL in milliseconds
  private static final long TTL_MS = 2000;

  public TelemetryService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void upsertTelemetry(String uavCode, String payloadJson) {
    try {
      redisTemplate.opsForValue().set(KEY_PREFIX + uavCode, payloadJson, TTL_MS, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      // 忽略 Redis 异常，避免接口 500
    }
  }

  public Map<String, String> readAllTelemetry() {
    try {
      var keys = redisTemplate.keys(KEY_PREFIX + "*");
      if (keys == null || keys.isEmpty()) {
        return Map.of();
      }
      List<String> keyList = keys.stream().toList();
      List<String> values = redisTemplate.opsForValue().multiGet(keyList);
      return keyList.stream()
          .collect(Collectors.toMap(k -> k.substring(KEY_PREFIX.length()), k -> values.get(keyList.indexOf(k))));
    } catch (Exception e) {
      return Map.of();
    }
  }

  public String readTelemetry(String uavCode) {
    try {
      return redisTemplate.opsForValue().get(KEY_PREFIX + uavCode);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Resolve a status from cached telemetry according to rules:
   * - No telemetry => OFFLINE
   * - Telemetry with "status" => mapped enum (case-insensitive), fallback to ONLINE if unknown
   * - Telemetry present without status => ONLINE
   */
  public UavStatus resolveStatus(String uavCode) {
    String payload = readTelemetry(uavCode);
    if (payload == null) {
      return UavStatus.OFFLINE;
    }
    try {
      JsonNode node = objectMapper.readTree(payload);
      if (node.hasNonNull("status")) {
        String status = node.get("status").asText("");
        try {
          return UavStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ignore) {
          return UavStatus.ONLINE;
        }
      }
    } catch (Exception ignored) {
      // ignore parse issues, treat as ONLINE when telemetry exists
    }
    return UavStatus.ONLINE;
  }

  public boolean isOnline(String payload) {
      if (payload == null) {
          return false;
      } else {
          return true;
      }
  }
}
