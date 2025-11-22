package com.example.uavbackend.fleet.dto;

import com.example.uavbackend.fleet.UavStatus;
import java.math.BigDecimal;
import java.time.Instant;

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
