package com.example.uavbackend.fleet;

import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class TelemetryMockController {
  private final TelemetryService telemetryService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @PostMapping("/mock-telemetry")
  public ResponseEntity<Void> mockTelemetry(@RequestBody Map<String, Object> payload) {
    Object uavCodeObj = payload.get("uavCode");
    if (uavCodeObj == null) {
      return ResponseEntity.badRequest().build();
    }
    String uavCode = uavCodeObj.toString();
    try {
      String json = objectMapper.writeValueAsString(payload);
      telemetryService.upsertTelemetry(uavCode, json);
      return ResponseEntity.ok().build();
    } catch (JsonProcessingException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
