package com.example.uavbackend.fleet.dto;

import com.example.uavbackend.fleet.UavStatus;
import java.util.List;

public record UavDeviceDto(
    Long id,
    String uavCode,
    String model,
    String pilotName,
    UavStatus status,
    List<String> sensors) {}
