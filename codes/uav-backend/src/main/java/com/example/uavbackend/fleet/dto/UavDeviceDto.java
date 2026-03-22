package com.example.uavbackend.fleet.dto;

import java.util.List;

public record UavDeviceDto(
    Long id,
    String uavCode,
    String model,
    String pilotName,
    String departmentName,
    String ownerUsername,
    List<String> sensors) {}
