package com.example.uavbackend.alert.dto;

import java.util.List;

public record AlertRuleDto(
    Long id,
    String name,
    String description,
    String logicOperator,
    Boolean templateEnabled,
    Long templateId,
    String templateName,
    Long departmentId,
    String departmentName,
    String templateCode,
    String templateCategory,
    Boolean autoInterrupt,
    Boolean notifyEnabled,
    String notifyChannels,
    String notifyTargets,
    String notifyTemplate,
    List<ConditionDto> conditions,
    int unreadCount) {}
