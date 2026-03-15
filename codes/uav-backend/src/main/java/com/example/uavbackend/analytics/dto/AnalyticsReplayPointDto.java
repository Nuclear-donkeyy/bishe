package com.example.uavbackend.analytics.dto;

import java.time.Instant;

public record AnalyticsReplayPointDto(
    Integer seq,
    Double lat,
    Double lng,
    Double altitude,
    String source,
    Instant timestamp) {}
