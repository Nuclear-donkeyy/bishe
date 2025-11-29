package com.example.uavbackend.alert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.alert.dto.AlertRecordDto;
import com.example.uavbackend.alert.dto.AlertRuleCreateRequest;
import com.example.uavbackend.alert.dto.AlertRuleDto;
import com.example.uavbackend.alert.dto.ConditionDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {
  private final AlertRuleMapper ruleMapper;
  private final AlertRuleConditionMapper conditionMapper;
  private final AlertRecordMapper recordMapper;

  public List<AlertRuleDto> listRules() {
    List<AlertRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<>());
    Map<Long, Long> unreadMap =
        recordMapper
            .selectList(
                new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getProcessed, false))
            .stream()
            .collect(Collectors.groupingBy(AlertRecord::getRuleId, Collectors.counting()));
    return rules.stream().map(r -> toDto(r, unreadMap.getOrDefault(r.getId(), 0L).intValue())).toList();
  }

  public List<AlertRecordDto> listRecords(Long ruleId) {
    LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
    if (ruleId != null) {
      wrapper.eq(AlertRecord::getRuleId, ruleId);
    }
    wrapper.orderByDesc(AlertRecord::getTriggeredAt);
    return recordMapper.selectList(wrapper).stream().map(this::toRecordDto).toList();
  }

  @Transactional
  public AlertRuleDto createRule(AlertRuleCreateRequest req) {
    AlertRule rule = new AlertRule();
    rule.setName(req.name());
    rule.setDescription(req.description());
    rule.setLogicOperator(req.logicOperator());
    rule.setCreatedAt(LocalDateTime.now());
    rule.setUpdatedAt(LocalDateTime.now());
    ruleMapper.insert(rule);
    if (req.conditions() != null) {
      for (ConditionDto c : req.conditions()) {
        AlertRuleCondition cond = new AlertRuleCondition();
        cond.setRuleId(rule.getId());
        cond.setMetricCode(c.metricCode());
        cond.setComparator(c.comparator());
        cond.setThreshold(c.threshold());
        conditionMapper.insert(cond);
      }
    }
    return toDto(rule, 0);
  }

  @Transactional
  public AlertRuleDto updateRule(Long ruleId, AlertRuleCreateRequest req) {
    AlertRule rule = ruleMapper.selectById(ruleId);
    if (rule == null) {
      throw new IllegalArgumentException("规则不存在");
    }
    rule.setName(req.name());
    rule.setDescription(req.description());
    rule.setLogicOperator(req.logicOperator());
    rule.setUpdatedAt(LocalDateTime.now());
    ruleMapper.updateById(rule);
    conditionMapper.delete(new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, ruleId));
    if (req.conditions() != null) {
      for (ConditionDto c : req.conditions()) {
        AlertRuleCondition cond = new AlertRuleCondition();
        cond.setRuleId(ruleId);
        cond.setMetricCode(c.metricCode());
        cond.setComparator(c.comparator());
        cond.setThreshold(c.threshold());
        conditionMapper.insert(cond);
      }
    }
    return toDto(rule, unreadCount(ruleId));
  }

  @Transactional
  public void deleteRule(Long ruleId) {
    conditionMapper.delete(new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, ruleId));
    recordMapper.delete(new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getRuleId, ruleId));
    ruleMapper.deleteById(ruleId);
  }

  @Transactional
  public void markRecordProcessed(Long recordId) {
    AlertRecord record = recordMapper.selectById(recordId);
    if (record == null) return;
    record.setProcessed(true);
    record.setProcessedAt(LocalDateTime.now());
    recordMapper.updateById(record);
  }

  private AlertRuleDto toDto(AlertRule rule, int unread) {
    List<ConditionDto> conditions =
        conditionMapper
            .selectList(
                new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, rule.getId()))
            .stream()
            .map(c -> new ConditionDto(c.getId(), c.getMetricCode(), c.getComparator(), c.getThreshold()))
            .toList();
    return new AlertRuleDto(rule.getId(), rule.getName(), rule.getDescription(), rule.getLogicOperator(), conditions, unread);
  }

  private int unreadCount(Long ruleId) {
    return Math.toIntExact(
        recordMapper
            .selectCount(
                new LambdaQueryWrapper<AlertRecord>()
                    .eq(AlertRecord::getRuleId, ruleId)
                    .eq(AlertRecord::getProcessed, false)));
  }

  private AlertRecordDto toRecordDto(AlertRecord r) {
    return new AlertRecordDto(
        r.getId(),
        r.getRuleId(),
        r.getMissionCode(),
        r.getUavCode(),
        r.getMetricCode(),
        r.getMetricValue(),
        r.getTriggeredAt(),
        r.getProcessed());
  }
}
