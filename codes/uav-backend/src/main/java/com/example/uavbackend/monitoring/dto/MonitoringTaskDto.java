package com.example.uavbackend.monitoring.dto;

import java.util.List;

/**
 * 监控任务（监控页卡片/详情）与关联的告警规则，来源 monitoring_tasks / monitoring_rules。
 */
public record MonitoringTaskDto(
    Long id,
    String taskCode,
    String missionName,
    String missionType,
    String ownerName,
    String status,
    String location,
    Integer devices,
    List<RuleDto> rules) {
  /** 告警规则：指标、阈值与等级，驱动监控规则列表与告警触发。 */
  public record RuleDto(Long id, String name, String metric, String threshold, String level) {}
}
