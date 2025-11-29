package com.example.uavbackend.alert.dto;

import java.util.List;

public record AlertRuleCreateRequest(
    String name, String description, String logicOperator, List<ConditionDto> conditions) {}

