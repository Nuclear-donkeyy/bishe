package com.example.uavbackend.monitoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.auth.AccessScope;
import com.example.uavbackend.auth.AccessScopeService;
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
  private final AccessScopeService accessScopeService;

  public List<MonitoringTaskDto> list(String status) {
    AccessScope scope = accessScopeService.currentScope();
    LambdaQueryWrapper<MonitoringTask> wrapper = new LambdaQueryWrapper<>();
    if (status != null) {
      wrapper.eq(MonitoringTask::getStatus, status);
    }
    if (!scope.superAdmin()) {
      wrapper.eq(MonitoringTask::getOwnerName, scope.displayName());
    }
    List<MonitoringTask> tasks = taskMapper.selectList(wrapper);
    return tasks.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Optional<MonitoringTaskDto> detail(String taskCode) {
    AccessScope scope = accessScopeService.currentScope();
    MonitoringTask task =
        taskMapper.selectOne(
            new LambdaQueryWrapper<MonitoringTask>().eq(MonitoringTask::getTaskCode, taskCode));
    if (task == null || (!scope.superAdmin() && !scope.displayName().equals(task.getOwnerName()))) {
      return Optional.empty();
    }
    return Optional.of(toDto(task));
  }

  @Transactional
  public RuleDto addRule(String taskCode, RuleDto ruleDto) {
    AccessScope scope = accessScopeService.currentScope();
    MonitoringTask task =
        taskMapper.selectOne(
            new LambdaQueryWrapper<MonitoringTask>().eq(MonitoringTask::getTaskCode, taskCode));
    if (task == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    if (!scope.superAdmin() && !scope.displayName().equals(task.getOwnerName())) {
      throw new IllegalArgumentException("无权操作该监控任务");
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
    AccessScope scope = accessScopeService.currentScope();
    MonitoringRule rule = ruleMapper.selectById(ruleId);
    if (rule == null) {
      return;
    }
    if (!scope.superAdmin()) {
      MonitoringTask task = taskMapper.selectById(rule.getTaskId());
      if (task == null || !scope.displayName().equals(task.getOwnerName())) {
        throw new IllegalArgumentException("无权删除该规则");
      }
    }
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
