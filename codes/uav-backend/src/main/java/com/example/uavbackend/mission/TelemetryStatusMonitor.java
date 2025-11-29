package com.example.uavbackend.mission;

import com.example.uavbackend.fleet.TelemetryService;
import com.example.uavbackend.alert.AlertRecord;
import com.example.uavbackend.alert.AlertRecordMapper;
import com.example.uavbackend.alert.AlertRule;
import com.example.uavbackend.alert.AlertRuleCondition;
import com.example.uavbackend.alert.AlertRuleConditionMapper;
import com.example.uavbackend.alert.AlertRuleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.example.uavbackend.mission.MissionMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelemetryStatusMonitor {
  private final TelemetryService telemetryService;
  private final MissionQueueService missionQueueService;
  private final MissionMapper missionMapper;
  private final AlertRuleMapper alertRuleMapper;
  private final AlertRuleConditionMapper conditionMapper;
  private final AlertRecordMapper recordMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Scheduled(fixedDelay = 1000)
  public void pollTelemetryStatus() {
    Map<String, String> all = telemetryService.readAllTelemetry();
    if (all.isEmpty()) {
      return;
    }
    all.forEach((uavCode, payload) -> {
      try {
        JsonNode node = objectMapper.readTree(payload);
        String status = node.hasNonNull("status") ? node.get("status").asText(null) : null;
        String missionId = node.hasNonNull("missionId") ? node.get("missionId").asText(null) : null;
        if (StringUtils.hasText(status) || StringUtils.hasText(missionId)) {
          if (log.isDebugEnabled()) {
            log.debug("Telemetry status detect uav={}, status={}, missionId={}", uavCode, status, missionId);
          }
          missionQueueService.onTelemetryStatus(uavCode, status, missionId);
        }
        // 执行中任务的报警检测
        if ("EXECUTING".equalsIgnoreCase(status) && StringUtils.hasText(missionId)) {
          evaluateAlerts(uavCode, missionId, node.path("data"));
        }
      } catch (Exception ignored) {
      }
    });
  }

  private void evaluateAlerts(String uavCode, String missionCodeOrId, JsonNode dataNode) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>()
                .eq(Mission::getMissionCode, missionCodeOrId)
                .or()
                .eq(Mission::getId, missionCodeOrId));
    if (mission == null || mission.getRuleId() == null) {
      return;
    }
    AlertRule rule = alertRuleMapper.selectById(mission.getRuleId());
    if (rule == null) return;
    var conditions =
        conditionMapper.selectList(
            new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, rule.getId()));
    if (conditions.isEmpty()) {
      return;
    }
    boolean matched = "AND".equalsIgnoreCase(rule.getLogicOperator());
    AlertRuleCondition matchedCond = null;
    if ("AND".equalsIgnoreCase(rule.getLogicOperator())) {
      matched = conditions.stream().allMatch(c -> compare(dataNode, c));
      matchedCond = matched ? conditions.get(0) : null;
    } else {
      for (AlertRuleCondition c : conditions) {
        if (compare(dataNode, c)) {
          matched = true;
          matchedCond = c;
          break;
        }
      }
    }
    if (matched && matchedCond != null) {
      // 防止重复刷：若已存在未处理的同规则、同任务记录则跳过
      Long count =
          recordMapper.selectCount(
              new LambdaQueryWrapper<AlertRecord>()
                  .eq(AlertRecord::getRuleId, rule.getId())
                  .eq(AlertRecord::getMissionCode, mission.getMissionCode())
                  .eq(AlertRecord::getProcessed, false));
      if (count != null && count > 0) {
        return;
      }
      AlertRecord record = new AlertRecord();
      record.setRuleId(rule.getId());
      record.setMissionCode(mission.getMissionCode());
      record.setUavCode(uavCode);
      record.setMetricCode(matchedCond.getMetricCode());
      record.setMetricValue(extractNumber(dataNode, matchedCond.getMetricCode()));
      record.setTriggeredAt(LocalDateTime.now());
      record.setProcessed(false);
      recordMapper.insert(record);
      log.info("Alert triggered rule={} mission={} uav={} metric={}", rule.getId(), mission.getMissionCode(), uavCode, matchedCond.getMetricCode());
    }
  }

  private boolean compare(JsonNode dataNode, AlertRuleCondition cond) {
    double value = extractNumber(dataNode, cond.getMetricCode());
    double threshold = cond.getThreshold() == null ? 0 : cond.getThreshold();
    String cmp = cond.getComparator() == null ? "" : cond.getComparator().toUpperCase();
    return switch (cmp) {
      case "GT" -> value > threshold;
      case "GTE" -> value >= threshold;
      case "LT" -> value < threshold;
      case "LTE" -> value <= threshold;
      case "EQ" -> value == threshold;
      default -> false;
    };
  }

  private double extractNumber(JsonNode node, String field) {
    if (node == null || !node.has(field)) return 0;
    try {
      return node.get(field).asDouble();
    } catch (Exception e) {
      return 0;
    }
  }
}
