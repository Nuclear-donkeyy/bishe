package com.example.uavbackend.analytics.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record MissionComparisonDto(
    String missionCode,
    String missionName,
    String missionType,
    String status,
    String pilotName,
    String uavCode,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Double durationMinutes,
    Double avgSpeedKmh,
    Double batteryConsumption,
    Integer alertCount,
    Double successRate,
    Map<String, Object> dataAvg,
    Map<String, Object> dataMax,
    Map<String, Object> dataMin) {}
