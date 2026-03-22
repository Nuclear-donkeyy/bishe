package com.example.uavbackend.alert.dto;

import java.util.List;

public record AlertRuleCreateRequest(
    String name,
    String description,
    String logicOperator,
    Boolean templateEnabled,
    Long templateId,
    Long departmentId,
    String templateCode,
    String templateCategory,
    Boolean autoInterrupt,
    Boolean notifyEnabled,
    String notifyChannels,
    String notifyTargets,
    String notifyTemplate,
    List<ConditionDto> conditions) {}
