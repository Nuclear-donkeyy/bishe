package com.example.uavbackend.fleet.dto;

import com.example.uavbackend.fleet.UavStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * UAV 列表/详情返回体，映射 uav_devices 表并服务于 /api/fleet 相关页面。
 */
public record UavDeviceDto(
    Long id,
    String uavCode,
    String model,
    String pilotName,
    UavStatus status,
    Integer batteryPercent,
    BigDecimal rangeKm,
    String linkQuality,
    Integer rttMs,
    BigDecimal locationLat,
    BigDecimal locationLng,
    BigDecimal locationAlt,
    Instant lastHeartbeatAt) {}
