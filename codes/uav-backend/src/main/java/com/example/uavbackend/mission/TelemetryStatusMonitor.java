package com.example.uavbackend.mission;

import com.example.uavbackend.fleet.TelemetryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
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
        String missionId = node.hasNonNull("missionId") ? node.get("missionId").asText(null) : null;
        if (StringUtils.hasText(status) || StringUtils.hasText(missionId)) {
          if (log.isDebugEnabled()) {
            log.debug("Telemetry status detect uav={}, status={}, missionId={}", uavCode, status, missionId);
          }
          missionQueueService.onTelemetryStatus(uavCode, status, missionId);
        }
      } catch (Exception ignored) {
      }
    });
  }
}
