package com.example.uavbackend.mission.dto;

import java.util.List;

/**
 * 任务列表/详情返回体，覆盖任务基础信息、航线点、里程碑与指标等数据。
 */
public record MissionDto(
    Long id,
    String missionCode,
    String name,
    String missionType,
    String pilotName,
    String status,
    String priority,
    Integer progress,
    String colorHex,
    List<List<Double>> route,
    List<String> milestones,
    List<String> metrics,
    List<String> assignedUavs) {}
