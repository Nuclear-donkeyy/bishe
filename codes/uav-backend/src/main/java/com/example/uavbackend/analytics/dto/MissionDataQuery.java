package com.example.uavbackend.analytics.dto;

import java.time.LocalDateTime;

public record MissionDataQuery(
    String missionType,
    String uavCode,
    String operatorName,
    String missionCode,
    LocalDateTime from,
    LocalDateTime to) {}

