package com.example.uavbackend.analytics.dto;

import java.time.Instant;
import java.util.List;

public record AnalyticsReplayDto(
    String missionCode,
    String missionName,
    String missionType,
    String status,
    String uavCode,
    Instant startTime,
    Instant endTime,
    Double durationMinutes,
    Double distanceKm,
    Integer sampleCount,
    List<AnalyticsMetricOptionDto> metricOptions,
    List<AnalyticsReplayPointDto> plannedRoute,
    List<AnalyticsReplayPointDto> actualTrack,
    List<AnalyticsReplayEventDto> timeline,
    List<AnalyticsReplaySampleDto> samples) {}
