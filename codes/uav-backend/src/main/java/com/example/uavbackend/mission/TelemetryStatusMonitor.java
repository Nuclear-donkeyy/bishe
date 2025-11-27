package com.example.uavbackend.mission;

import com.example.uavbackend.fleet.TelemetryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class TelemetryStatusMonitor {
  private final TelemetryService telemetryService;
  private final MissionQueueService missionQueueService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Scheduled(fixedDelay = 1000)
  public void pollTelemetryStatus() {
    Map<String, String> all = telemetryService.readAllTelemetry();
    if (all.isEmpty()) {
      return;
    }
    all.forEach((uavCode, payload) -> {
      try {
        JsonNode node = objectMapper.readTree(payload);
        String status = node.hasNonNull("status") ? node.get("status").asText(null) : null;
        if (StringUtils.hasText(status)) {
          missionQueueService.onTelemetryStatus(uavCode, status);
        }
      } catch (Exception ignored) {
      }
    });
  }
}
