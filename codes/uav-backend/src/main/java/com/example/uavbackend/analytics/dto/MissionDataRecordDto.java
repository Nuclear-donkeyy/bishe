package com.example.uavbackend.analytics.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record MissionDataRecordDto(
    Long id,
    Long missionId,
    String missionCode,
    String missionType,
    String pilotName,
    String uavCode,
    String operatorName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Map<String, Object> dataMax,
    Map<String, Object> dataMin,
    Map<String, Object> dataAvg) {}

