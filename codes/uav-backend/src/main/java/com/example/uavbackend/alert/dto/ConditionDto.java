package com.example.uavbackend.alert.dto;

public record ConditionDto(Long id, String metricCode, String comparator, Double threshold) {}

