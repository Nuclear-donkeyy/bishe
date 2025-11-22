package com.example.uavbackend.analytics.dto;

import java.time.Instant;

public record TaskExecutionDto(
    Long id,
    String executionCode,
    String missionName,
    String missionType,
    String location,
    String ownerName,
    Instant completedAt,
    String metrics) {}
