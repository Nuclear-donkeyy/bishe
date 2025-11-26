package com.example.uavbackend.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.uavbackend.analytics.dto.AnalyticsDefinitionDto;
import com.example.uavbackend.analytics.dto.TaskExecutionDto;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
  private final AnalyticsDefinitionMapper definitionMapper;
  private final TaskExecutionMapper taskExecutionMapper;

  public List<AnalyticsDefinitionDto> definitions(String missionType) {
    LambdaQueryWrapper<AnalyticsDefinition> wrapper = new LambdaQueryWrapper<>();
    if (missionType != null) {
      wrapper.eq(AnalyticsDefinition::getMissionType, missionType);
    }
    wrapper.orderByAsc(AnalyticsDefinition::getDisplayOrder);
    List<AnalyticsDefinition> defs = definitionMapper.selectList(wrapper);
    return defs.stream()
        .map(
            def ->
                new AnalyticsDefinitionDto(
                    def.getId(),
                    def.getMissionType(),
                    def.getTitle(),
                    def.getDescription(),
                    def.getSeriesConfig()))
        .collect(Collectors.toList());
  }

  public List<TaskExecutionDto> taskExecutions(String missionType, Instant from, Instant to) {
    if (from != null && to != null) {
      LambdaQueryWrapper<TaskExecution> wrapper =
          new LambdaQueryWrapper<TaskExecution>()
              .eq(TaskExecution::getMissionType, missionType)
              .between(TaskExecution::getCompletedAt, from, to)
              .orderByAsc(TaskExecution::getCompletedAt);
      return taskExecutionMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }
    Page<TaskExecution> page =
        taskExecutionMapper.selectPage(
            Page.of(1, 200, false),
            new LambdaQueryWrapper<TaskExecution>()
                .eq(TaskExecution::getMissionType, missionType)
                .orderByDesc(TaskExecution::getCompletedAt));
    return page.getRecords().stream().map(this::toDto).toList();
  }

  private TaskExecutionDto toDto(TaskExecution exec) {
    return new TaskExecutionDto(
        exec.getId(),
        exec.getExecutionCode(),
        exec.getMissionName(),
        exec.getMissionType(),
        exec.getLocation(),
        exec.getOwnerName(),
        exec.getCompletedAt(),
        exec.getMetrics());
  }
}
