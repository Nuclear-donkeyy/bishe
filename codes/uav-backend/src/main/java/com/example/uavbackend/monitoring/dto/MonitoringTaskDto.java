package com.example.uavbackend.monitoring.dto;

import java.util.List;

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
  public record RuleDto(Long id, String name, String metric, String threshold, String level) {}
}
