package com.example.uavbackend.analytics.dto;

import java.util.List;

public record AnalyticsSeriesDto(
    String metricCode, String displayName, String unit, List<AnalyticsSeriesPointDto> points) {}
