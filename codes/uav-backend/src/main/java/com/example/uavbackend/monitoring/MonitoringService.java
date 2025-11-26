package com.example.uavbackend.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.monitoring.dto.MonitoringTaskDto;
import com.example.uavbackend.monitoring.dto.MonitoringTaskDto.RuleDto;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MonitoringService {
  private final MonitoringTaskMapper taskMapper;
  private final MonitoringRuleMapper ruleMapper;

  public List<MonitoringTaskDto> list(String status) {
    LambdaQueryWrapper<MonitoringTask> wrapper = new LambdaQueryWrapper<>();
    if (status != null) {
      wrapper.eq(MonitoringTask::getStatus, status);
    }
    List<MonitoringTask> tasks = taskMapper.selectList(wrapper);
    return tasks.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Optional<MonitoringTaskDto> detail(String taskCode) {
    MonitoringTask task =
        taskMapper.selectOne(
            new LambdaQueryWrapper<MonitoringTask>().eq(MonitoringTask::getTaskCode, taskCode));
    return Optional.ofNullable(task).map(this::toDto);
  }

  @Transactional
  public RuleDto addRule(String taskCode, RuleDto ruleDto) {
    MonitoringTask task =
        taskMapper.selectOne(
            new LambdaQueryWrapper<MonitoringTask>().eq(MonitoringTask::getTaskCode, taskCode));
    if (task == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    MonitoringRule rule = new MonitoringRule();
    rule.setTaskId(task.getId());
    rule.setName(ruleDto.name());
    rule.setMetric(ruleDto.metric());
    rule.setThreshold(ruleDto.threshold());
    rule.setLevel(ruleDto.level());
    ruleMapper.insert(rule);
    return new RuleDto(rule.getId(), rule.getName(), rule.getMetric(), rule.getThreshold(), rule.getLevel());
  }

  @Transactional
  public void deleteRule(Long ruleId) {
    ruleMapper.deleteById(ruleId);
  }

  private MonitoringTaskDto toDto(MonitoringTask task) {
    List<RuleDto> rules =
        ruleMapper
            .selectList(
                new LambdaQueryWrapper<MonitoringRule>().eq(MonitoringRule::getTaskId, task.getId()))
            .stream()
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
