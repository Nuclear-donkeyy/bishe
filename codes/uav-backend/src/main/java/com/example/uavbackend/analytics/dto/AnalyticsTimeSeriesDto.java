package com.example.uavbackend.analytics.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsTimeSeriesDto(
    String missionCode,
    String missionName,
    String missionType,
    String uavCode,
    Instant startTime,
    Instant endTime,
    List<AnalyticsMetricOptionDto> metricOptions,
    List<AnalyticsSeriesDto> series) {}
