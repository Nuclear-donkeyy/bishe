package com.example.uavbackend.fleet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryHistoryService {
  private static final long SAMPLE_INTERVAL_MS = 3_000L;
  private static final ZoneId ZONE_ID = ZoneId.systemDefault();

  private final UavDeviceMapper uavDeviceMapper;
  private final UavTelemetryMapper uavTelemetryMapper;
  private final ObjectMapper objectMapper;
  private final Map<String, Long> lastSampleAtByUav = new ConcurrentHashMap<>();

  public void recordSample(String uavCode, String rawPayload) {
    if (!StringUtils.hasText(uavCode) || !StringUtils.hasText(rawPayload)) {
      return;
    }
    try {
      JsonNode node = objectMapper.readTree(rawPayload);
      long reportedAt = resolveReportedAt(node);
      Long lastSampleAt = lastSampleAtByUav.get(uavCode);
      if (lastSampleAt != null && reportedAt - lastSampleAt < SAMPLE_INTERVAL_MS) {
        return;
      }

      UavDevice device =
          uavDeviceMapper.selectOne(
              new LambdaQueryWrapper<UavDevice>()
                  .eq(UavDevice::getUavCode, uavCode)
                  .last("limit 1"));
      if (device == null) {
        return;
      }

      UavTelemetry telemetry = new UavTelemetry();
      telemetry.setUavId(device.getId());
      telemetry.setSessionCode(resolveSessionCode(node, uavCode));
      telemetry.setReportedAt(Instant.ofEpochMilli(reportedAt).atZone(ZONE_ID).toInstant());
      telemetry.setBatteryPercent(resolveBattery(node));
      telemetry.setRangeKm(decimal(node, 2, "rangeKm", "range"));
      telemetry.setLocationLat(decimal(node, 6, "lat", "locationLat"));
      telemetry.setLocationLng(decimal(node, 6, "lng", "locationLng"));
      telemetry.setLocationAlt(decimal(node, 2, "alt", "altitude", "locationAlt"));
      telemetry.setVelocityMs(resolveVelocity(node));
      telemetry.setPayload(extractPayload(node));
      telemetry.setRawMessage(rawPayload);
      uavTelemetryMapper.insert(telemetry);
      lastSampleAtByUav.put(uavCode, reportedAt);
    } catch (Exception e) {
      log.debug("Ignore telemetry history sample for uav={}", uavCode, e);
    }
  }

  private long resolveReportedAt(JsonNode node) {
    if (node != null && node.hasNonNull("ts")) {
      double ts = node.get("ts").asDouble(0d);
      if (ts > 0) {
        return ts > 1_000_000_000_000d ? (long) ts : (long) (ts * 1000d);
      }
    }
    return System.currentTimeMillis();
  }

  private String resolveSessionCode(JsonNode node, String uavCode) {
    if (node != null && node.hasNonNull("missionId") && StringUtils.hasText(node.get("missionId").asText())) {
      return node.get("missionId").asText();
    }
    String status = node != null && node.hasNonNull("status") ? node.get("status").asText("IDLE") : "IDLE";
    return "FREE::" + uavCode + "::" + status;
  }

  private Integer resolveBattery(JsonNode node) {
    BigDecimal battery = decimal(node, 0, "batteryPercent", "battery");
    return battery == null ? null : battery.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
  }

  private BigDecimal resolveVelocity(JsonNode node) {
    BigDecimal velocity = decimal(node, 2, "velocityMs", "speed");
    if (velocity == null) {
      return null;
    }
    if (velocity.compareTo(BigDecimal.valueOf(100)) > 0) {
      return velocity.divide(BigDecimal.valueOf(3.6), 2, BigDecimal.ROUND_HALF_UP);
    }
    return velocity.setScale(2, BigDecimal.ROUND_HALF_UP);
  }

  private String extractPayload(JsonNode node) {
    JsonNode payloadNode = node == null ? null : node.path("data");
    if (payloadNode == null || payloadNode.isMissingNode() || payloadNode.isNull()) {
      return "{}";
    }
    try {
      return objectMapper.writeValueAsString(payloadNode);
    } catch (Exception e) {
      return "{}";
    }
  }

  private BigDecimal decimal(JsonNode node, int scale, String... fields) {
    if (node == null || fields == null) {
      return null;
    }
    for (String field : fields) {
      JsonNode value = node.get(field);
      if (value == null || value.isNull()) {
        continue;
      }
      try {
        return BigDecimal.valueOf(value.asDouble()).setScale(scale, BigDecimal.ROUND_HALF_UP);
      } catch (Exception ignored) {
        // ignore malformed telemetry field
      }
    }
    return null;
  }
}
