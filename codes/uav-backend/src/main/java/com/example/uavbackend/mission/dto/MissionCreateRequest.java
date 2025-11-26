package com.example.uavbackend.mission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建任务的请求体，对应 /api/missions。
 * 任务编号、状态、进度由后端自动生成，前端无需提供。
 */
public record MissionCreateRequest(
    @NotBlank String name,
    @NotBlank String missionType,
    /** 提交责任人用户名，后端校验并映射为真实姓名。 */
    @NotBlank String pilotUsername,
    @NotBlank String priority,
    List<String> milestones,
    @NotEmpty List<List<Double>> route,
    List<String> assignedUavs) {}
