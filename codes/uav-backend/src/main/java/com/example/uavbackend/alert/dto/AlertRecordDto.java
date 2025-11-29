package com.example.uavbackend.alert.dto;

import java.time.LocalDateTime;

public record AlertRecordDto(
    Long id,
    Long ruleId,
    String missionCode,
    String uavCode,
    String metricCode,
    Double metricValue,
    LocalDateTime triggeredAt,
    Boolean processed) {}

