package com.example.uavbackend.analytics.dto;

import java.time.Instant;
import java.util.Map;

public record AnalyticsReplaySampleDto(
    Instant reportedAt,
    Double lat,
    Double lng,
    Double altitude,
    Double batteryPercent,
    Double velocityMs,
    Map<String, Object> metrics) {}
