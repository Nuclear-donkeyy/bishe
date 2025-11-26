package com.example.uavbackend.configcenter.dto;

import java.util.List;

public record MissionTypeItem(
    Long id, String typeCode, String displayName, String description, List<Long> metricIds) {}
