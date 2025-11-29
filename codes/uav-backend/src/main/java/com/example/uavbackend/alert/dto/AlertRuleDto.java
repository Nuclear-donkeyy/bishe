package com.example.uavbackend.alert.dto;

import java.util.List;

public record AlertRuleDto(
    Long id,
    String name,
    String description,
    String logicOperator,
    List<ConditionDto> conditions,
    int unreadCount) {}

