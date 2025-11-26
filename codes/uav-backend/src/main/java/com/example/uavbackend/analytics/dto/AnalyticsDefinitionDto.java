package com.example.uavbackend.analytics.dto;

/**
 * 前端配置每种任务类型的指标卡与图表定义，用于 /api/analytics/definitions。
 */
public record AnalyticsDefinitionDto(
    Long id, String missionType, String title, String description, String seriesConfig) {}
