package com.example.uavbackend.configcenter.dto;

import java.util.List;

public record MetricItem(
    Long id, String metricCode, String name, String unit, String description, List<Long> sensorTypeIds) {}
