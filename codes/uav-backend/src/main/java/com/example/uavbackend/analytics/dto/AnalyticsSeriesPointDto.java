package com.example.uavbackend.analytics.dto;

import java.time.Instant;

public record AnalyticsSeriesPointDto(Instant timestamp, Double value) {}
