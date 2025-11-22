package com.example.uavbackend.monitoring;

import com.example.uavbackend.monitoring.dto.MonitoringTaskDto;
import com.example.uavbackend.monitoring.dto.MonitoringTaskDto.RuleDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {
  private final MonitoringService monitoringService;

  @GetMapping("/tasks")
  public List<MonitoringTaskDto> tasks(@RequestParam(required = false) String status) {
    return monitoringService.list(status);
  }

  @GetMapping("/tasks/{taskCode}")
  public ResponseEntity<MonitoringTaskDto> detail(@PathVariable String taskCode) {
    return monitoringService.detail(taskCode).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/tasks/{taskCode}/rules")
  public ResponseEntity<RuleDto> addRule(
      @PathVariable String taskCode, @Valid @RequestBody RuleDto request) {
    return ResponseEntity.status(201).body(monitoringService.addRule(taskCode, request));
  }

  @DeleteMapping("/tasks/{taskCode}/rules/{ruleId}")
  public ResponseEntity<Void> deleteRule(@PathVariable String taskCode, @PathVariable Long ruleId) {
    monitoringService.deleteRule(ruleId);
    return ResponseEntity.noContent().build();
  }
}
