package com.example.uavbackend.alert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.alert.dto.AlertRecordDto;
import com.example.uavbackend.alert.dto.AlertRuleCreateRequest;
import com.example.uavbackend.alert.dto.AlertRuleDto;
import com.example.uavbackend.alert.dto.ConditionDto;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    return mapRules(rules);
  }

  public List<AlertRuleDto> listTemplates() {
    List<AlertRule> templates =
        ruleMapper.selectList(new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getTemplateEnabled, true));
    return mapRules(templates);
  }

  private List<AlertRuleDto> mapRules(List<AlertRule> rules) {
    Map<Long, Long> unreadMap =
        recordMapper
            .selectList(
                new LambdaQueryWrapper<AlertRecord>().eq(AlertRecord::getProcessed, false))
            .stream()
            .collect(Collectors.groupingBy(AlertRecord::getRuleId, Collectors.counting()));
    Map<Long, AlertRule> templatesById = loadTemplatesById(rules);
    return rules.stream()
        .map(r -> toDto(r, unreadMap.getOrDefault(r.getId(), 0L).intValue(), templatesById.get(r.getTemplateId())))
        .toList();
  }

  public List<AlertRecordDto> listRecords(Long ruleId, List<String> missionCodes) {
    LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
    if (ruleId != null) {
      wrapper.eq(AlertRecord::getRuleId, ruleId);
    }
    if (missionCodes != null) {
      List<String> normalizedMissionCodes = missionCodes.stream().filter(code -> code != null && !code.isBlank()).distinct().toList();
      if (!normalizedMissionCodes.isEmpty()) {
        wrapper.in(AlertRecord::getMissionCode, normalizedMissionCodes);
      }
    }
    wrapper.orderByDesc(AlertRecord::getTriggeredAt);
    return recordMapper.selectList(wrapper).stream().map(this::toRecordDto).toList();
  }

  @Transactional
  public AlertRuleDto createRule(AlertRuleCreateRequest req) {
    if (Boolean.TRUE.equals(req.templateEnabled())) {
      AlertRule template = new AlertRule();
      applyTemplateFields(template, req);
      template.setCreatedAt(LocalDateTime.now());
      template.setUpdatedAt(LocalDateTime.now());
      ruleMapper.insert(template);
      saveConditions(template.getId(), resolveTemplateConditions(req.conditions()));
      return toDto(template, 0, null);
    }
    AlertRule template = resolveTemplate(req.templateId());
    AlertRule rule = new AlertRule();
    applyRuleFields(rule, req, template);
    rule.setCreatedAt(LocalDateTime.now());
    rule.setUpdatedAt(LocalDateTime.now());
    ruleMapper.insert(rule);
    saveConditions(rule.getId(), resolveConditions(req.conditions(), template));
    return toDto(rule, 0, template);
  }

  @Transactional
  public AlertRuleDto updateRule(Long ruleId, AlertRuleCreateRequest req) {
    AlertRule rule = ruleMapper.selectById(ruleId);
    if (rule == null) {
      throw new IllegalArgumentException("规则不存在");
    }
    if (Boolean.TRUE.equals(rule.getTemplateEnabled())) {
      applyTemplateFields(rule, req);
      rule.setUpdatedAt(LocalDateTime.now());
      ruleMapper.updateById(rule);
      conditionMapper.delete(new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, ruleId));
      saveConditions(ruleId, resolveTemplateConditions(req.conditions()));
      return toDto(rule, unreadCount(ruleId), null);
    }
    AlertRule template = resolveTemplate(req.templateId());
    applyRuleFields(rule, req, template);
    rule.setUpdatedAt(LocalDateTime.now());
    ruleMapper.updateById(rule);
    conditionMapper.delete(new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, ruleId));
    saveConditions(ruleId, resolveConditions(req.conditions(), template));
    return toDto(rule, unreadCount(ruleId), template);
  }

  @Transactional
  public void deleteRule(Long ruleId) {
    AlertRule rule = ruleMapper.selectById(ruleId);
    if (rule != null && Boolean.TRUE.equals(rule.getTemplateEnabled())) {
      Long referencedCount =
          ruleMapper.selectCount(new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getTemplateId, ruleId));
      if (referencedCount != null && referencedCount > 0) {
        throw new IllegalArgumentException("模板已被普通规则引用，无法删除");
      }
    }
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

  private AlertRuleDto toDto(AlertRule rule, int unread, AlertRule template) {
    List<ConditionDto> conditions =
        conditionMapper
            .selectList(
                new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, rule.getId()))
            .stream()
            .map(c -> new ConditionDto(c.getId(), c.getMetricCode(), c.getComparator(), c.getThreshold()))
            .toList();
    return new AlertRuleDto(
        rule.getId(),
        rule.getName(),
        rule.getDescription(),
        rule.getLogicOperator(),
        Boolean.TRUE.equals(rule.getTemplateEnabled()),
        rule.getTemplateId(),
        template == null ? null : template.getName(),
        Boolean.TRUE.equals(rule.getTemplateEnabled()) ? rule.getTemplateCode() : null,
        Boolean.TRUE.equals(rule.getTemplateEnabled()) ? rule.getTemplateCategory() : null,
        Boolean.TRUE.equals(rule.getAutoInterrupt()),
        Boolean.TRUE.equals(rule.getNotifyEnabled()),
        rule.getNotifyChannels(),
        rule.getNotifyTargets(),
        rule.getNotifyTemplate(),
        conditions,
        unread);
  }

  private Map<Long, AlertRule> loadTemplatesById(List<AlertRule> rules) {
    List<Long> templateIds =
        rules.stream().map(AlertRule::getTemplateId).filter(id -> id != null && id > 0).distinct().toList();
    if (templateIds.isEmpty()) {
      return Map.of();
    }
    return ruleMapper.selectBatchIds(templateIds).stream()
        .collect(Collectors.toMap(AlertRule::getId, template -> template, (left, right) -> left, HashMap::new));
  }

  private AlertRule resolveTemplate(Long templateId) {
    if (templateId == null) {
      return null;
    }
    AlertRule template = ruleMapper.selectById(templateId);
    if (template == null || !Boolean.TRUE.equals(template.getTemplateEnabled())) {
      throw new IllegalArgumentException("所选模板不存在");
    }
    return template;
  }

  private void applyTemplateFields(AlertRule template, AlertRuleCreateRequest req) {
    String name = trimToNull(req.name());
    if (name == null) {
      throw new IllegalArgumentException("模板名称不能为空");
    }
    template.setName(name);
    template.setTemplateEnabled(true);
    template.setTemplateId(null);
    template.setDescription(trimToNull(req.description()));
    template.setLogicOperator(firstNonBlank(req.logicOperator(), "AND"));
    template.setTemplateCode(trimToNull(req.templateCode()));
    template.setTemplateCategory(trimToNull(req.templateCategory()));
    template.setAutoInterrupt(resolveBoolean(req.autoInterrupt(), null, false));
    template.setNotifyEnabled(resolveBoolean(req.notifyEnabled(), null, false));
    template.setNotifyChannels(trimToNull(req.notifyChannels()));
    template.setNotifyTargets(trimToNull(req.notifyTargets()));
    template.setNotifyTemplate(trimToNull(req.notifyTemplate()));
  }

  private void applyRuleFields(AlertRule rule, AlertRuleCreateRequest req, AlertRule template) {
    String name = trimToNull(req.name());
    if (name == null) {
      throw new IllegalArgumentException("规则名称不能为空");
    }
    rule.setName(name);
    rule.setTemplateEnabled(false);
    rule.setTemplateId(template == null ? null : template.getId());
    rule.setTemplateCode(null);
    rule.setTemplateCategory(null);
    rule.setDescription(firstNonBlank(req.description(), template == null ? null : template.getDescription()));
    rule.setLogicOperator(firstNonBlank(req.logicOperator(), template == null ? null : template.getLogicOperator(), "AND"));
    rule.setAutoInterrupt(resolveBoolean(req.autoInterrupt(), template == null ? null : template.getAutoInterrupt(), false));
    rule.setNotifyEnabled(resolveBoolean(req.notifyEnabled(), template == null ? null : template.getNotifyEnabled(), false));
    rule.setNotifyChannels(firstNonBlank(req.notifyChannels(), template == null ? null : template.getNotifyChannels()));
    rule.setNotifyTargets(firstNonBlank(req.notifyTargets(), template == null ? null : template.getNotifyTargets()));
    rule.setNotifyTemplate(firstNonBlank(req.notifyTemplate(), template == null ? null : template.getNotifyTemplate()));
  }

  private List<ConditionDto> resolveTemplateConditions(List<ConditionDto> requestConditions) {
    if (requestConditions != null && !requestConditions.isEmpty()) {
      return requestConditions;
    }
    throw new IllegalArgumentException("模板至少需要配置一条条件");
  }

  private List<ConditionDto> resolveConditions(List<ConditionDto> requestConditions, AlertRule template) {
    if (requestConditions != null && !requestConditions.isEmpty()) {
      return requestConditions;
    }
    if (template != null) {
      return conditionMapper
          .selectList(new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, template.getId()))
          .stream()
          .map(c -> new ConditionDto(null, c.getMetricCode(), c.getComparator(), c.getThreshold()))
          .toList();
    }
    throw new IllegalArgumentException("请至少配置一条条件，或选择带条件的模板");
  }

  private void saveConditions(Long ruleId, List<ConditionDto> conditions) {
    for (ConditionDto c : conditions) {
      AlertRuleCondition cond = new AlertRuleCondition();
      cond.setRuleId(ruleId);
      cond.setMetricCode(c.metricCode());
      cond.setComparator(c.comparator());
      cond.setThreshold(c.threshold());
      conditionMapper.insert(cond);
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      String normalized = trimToNull(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  private boolean resolveBoolean(Boolean requested, Boolean inherited, boolean defaultValue) {
    if (requested != null) {
      return requested;
    }
    if (inherited != null) {
      return inherited;
    }
    return defaultValue;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
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
        r.getProcessed(),
        r.getLinkageStatus(),
        r.getLinkageSummary(),
        r.getNotificationStatus());
  }
}
