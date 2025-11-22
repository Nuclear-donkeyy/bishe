package com.example.uavbackend.monitoring;

import com.example.uavbackend.monitoring.dto.MonitoringTaskDto;
import com.example.uavbackend.monitoring.dto.MonitoringTaskDto.RuleDto;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonitoringService {
  private final MonitoringTaskRepository taskRepository;
  private final MonitoringRuleRepository ruleRepository;

  public List<MonitoringTaskDto> list(String status) {
    List<MonitoringTask> tasks =
        status == null ? taskRepository.findAll() : taskRepository.findByStatus(status);
    return tasks.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Optional<MonitoringTaskDto> detail(String taskCode) {
    return taskRepository.findByTaskCode(taskCode).map(this::toDto);
  }

  @Transactional
  public RuleDto addRule(String taskCode, RuleDto ruleDto) {
    MonitoringTask task =
      taskRepository
          .findByTaskCode(taskCode)
          .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    MonitoringRule rule = new MonitoringRule();
    rule.setTaskId(task.getId());
    rule.setName(ruleDto.name());
    rule.setMetric(ruleDto.metric());
    rule.setThreshold(ruleDto.threshold());
    rule.setLevel(ruleDto.level());
    MonitoringRule saved = ruleRepository.save(rule);
    return new RuleDto(saved.getId(), saved.getName(), saved.getMetric(), saved.getThreshold(), saved.getLevel());
  }

  @Transactional
  public void deleteRule(Long ruleId) {
    ruleRepository.deleteById(ruleId);
  }

  private MonitoringTaskDto toDto(MonitoringTask task) {
    List<RuleDto> rules =
        ruleRepository.findByTaskId(task.getId()).stream()
            .map(r -> new RuleDto(r.getId(), r.getName(), r.getMetric(), r.getThreshold(), r.getLevel()))
            .collect(Collectors.toList());
    return new MonitoringTaskDto(
        task.getId(),
        task.getTaskCode(),
        task.getMissionName(),
        task.getMissionType(),
        task.getOwnerName(),
        task.getStatus(),
        task.getLocationDesc(),
        task.getDevicesCount(),
        rules);
  }
}
