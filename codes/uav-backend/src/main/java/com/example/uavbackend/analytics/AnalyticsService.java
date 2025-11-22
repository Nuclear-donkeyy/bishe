package com.example.uavbackend.analytics;

import com.example.uavbackend.analytics.dto.AnalyticsDefinitionDto;
import com.example.uavbackend.analytics.dto.TaskExecutionDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
  private final AnalyticsDefinitionRepository definitionRepository;
  private final TaskExecutionRepository taskExecutionRepository;

  public List<AnalyticsDefinitionDto> definitions(String missionType) {
    List<AnalyticsDefinition> defs =
        missionType == null
            ? definitionRepository.findAll()
            : definitionRepository.findByMissionTypeOrderByDisplayOrderAsc(missionType);
    return defs.stream()
        .map(def -> new AnalyticsDefinitionDto(def.getId(), def.getMissionType(), def.getTitle(), def.getDescription(), def.getSeriesConfig()))
        .collect(Collectors.toList());
  }

  public List<TaskExecutionDto> taskExecutions(String missionType, Instant from, Instant to) {
    List<TaskExecution> executions;
    if (from != null && to != null) {
      executions =
          taskExecutionRepository.findByMissionTypeAndCompletedAtBetweenOrderByCompletedAtAsc(
              missionType, from, to);
    } else {
      executions = taskExecutionRepository.findTop200ByMissionTypeOrderByCompletedAtDesc(missionType);
    }
    return executions.stream()
        .map(
            exec ->
                new TaskExecutionDto(
                    exec.getId(),
                    exec.getExecutionCode(),
                    exec.getMissionName(),
                    exec.getMissionType(),
                    exec.getLocation(),
                    exec.getOwnerName(),
                    exec.getCompletedAt(),
                    exec.getMetrics()))
        .collect(Collectors.toList());
  }
}
