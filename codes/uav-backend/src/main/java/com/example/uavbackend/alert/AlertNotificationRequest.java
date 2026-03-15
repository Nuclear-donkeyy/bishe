package com.example.uavbackend.alert;

import com.example.uavbackend.mission.Mission;

public record AlertNotificationRequest(
    AlertRule rule,
    AlertRecord record,
    Mission mission,
    AlertRuleCondition matchedCondition,
    String uavCode) {}
