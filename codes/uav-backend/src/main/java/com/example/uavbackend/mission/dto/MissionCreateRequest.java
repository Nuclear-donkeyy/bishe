package com.example.uavbackend.mission.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建/更新任务的请求体，对应 /api/missions。
 * 携带任务基础信息、航线、指标/里程碑配置以及分配的无人机。
 */
public record MissionCreateRequest(
    String missionCode,
    @NotBlank String name,
    @NotBlank String missionType,
    @NotBlank String pilotName,
    @NotBlank String status,
    @NotBlank String priority,
    @Min(0) Integer progress,
    List<String> metrics,
    List<String> milestones,
    @NotEmpty List<List<Double>> route,
    List<String> assignedUavs) {}
