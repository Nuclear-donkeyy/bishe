package com.example.uavbackend.fleet;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelemetryPushScheduler {
  private final TelemetryService telemetryService;
  private final SimpMessagingTemplate messagingTemplate;

  @Scheduled(fixedDelay = 500)
  public void pushTelemetry() {
    Map<String, String> all = telemetryService.readAllTelemetry();
    if (all.isEmpty()) {
      return;
    }
    all.forEach(
        (uavCode, payload) -> {
          messagingTemplate.convertAndSend("/topic/uav-telemetry", payload);
          messagingTemplate.convertAndSend("/topic/uav-telemetry/" + uavCode, payload);
        });
  }
}
