package com.example.uavbackend.analytics;

import com.example.uavbackend.analytics.dto.AnalyticsDefinitionDto;
import com.example.uavbackend.analytics.dto.TaskExecutionDto;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
  private final AnalyticsService analyticsService;

  @GetMapping("/definitions")
  public List<AnalyticsDefinitionDto> definitions(
      @RequestParam(value = "missionType", required = false) String missionType) {
    return analyticsService.definitions(missionType);
  }

  @GetMapping("/task-executions")
  public ResponseEntity<List<TaskExecutionDto>> taskExecutions(
      @RequestParam("missionType") String missionType,
      @RequestParam(value = "from", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(value = "to", required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return ResponseEntity.ok(analyticsService.taskExecutions(missionType, from, to));
  }
}
