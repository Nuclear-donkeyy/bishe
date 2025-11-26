package com.example.uavbackend.analytics.dto;

import java.time.Instant;

/**
 * 单次任务执行的统计数据，供报表/趋势图展示 (/api/analytics/task-executions)。
 */
public record TaskExecutionDto(
    Long id,
    String executionCode,
    String missionName,
    String missionType,
    String location,
    String ownerName,
    Instant completedAt,
    String metrics) {}
