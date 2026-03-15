package com.example.uavbackend.analytics;

import com.example.uavbackend.analytics.dto.AnalyticsCompareRequest;
import com.example.uavbackend.analytics.dto.AnalyticsDefinitionDto;
import com.example.uavbackend.analytics.dto.AnalyticsReplayDto;
import com.example.uavbackend.analytics.dto.AnalyticsTimeSeriesDto;
import com.example.uavbackend.analytics.dto.MissionComparisonDto;
import com.example.uavbackend.analytics.dto.MissionDataRecordDto;
import com.example.uavbackend.analytics.dto.TaskExecutionDto;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  public List<TaskExecutionDto> taskExecutions(
      @RequestParam("missionType") String missionType,
      @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return analyticsService.taskExecutions(missionType, from, to);
  }

  @PostMapping("/compare")
  public List<MissionComparisonDto> compare(@RequestBody AnalyticsCompareRequest request) {
    return analyticsService.compare(request.missionCodes());
  }

  @GetMapping("/timeseries")
  public AnalyticsTimeSeriesDto timeseries(
      @RequestParam("missionCode") String missionCode,
      @RequestParam(value = "metrics", required = false) List<String> metrics) {
    return analyticsService.timeseries(missionCode, metrics);
  }

  @GetMapping("/replay")
  public AnalyticsReplayDto replay(@RequestParam("missionCode") String missionCode) {
    return analyticsService.replay(missionCode);
  }

  @GetMapping("/data")
  public List<MissionDataRecordDto> list(
      @RequestParam("missionType") String missionType,
      @RequestParam(value = "uavCode", required = false) String uavCode,
      @RequestParam(value = "operatorName", required = false) String operatorName,
      @RequestParam(value = "missionCode", required = false) String missionCode,
      @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to) {
    return analyticsService.listMissionData(missionType, uavCode, operatorName, missionCode, from, to);
  }
}
