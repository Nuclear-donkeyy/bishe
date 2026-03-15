package com.example.uavbackend.analytics.dto;

import java.time.Instant;

public record AnalyticsReplayEventDto(
    String category,
    String eventType,
    String title,
    String description,
    Instant occurredAt) {}
