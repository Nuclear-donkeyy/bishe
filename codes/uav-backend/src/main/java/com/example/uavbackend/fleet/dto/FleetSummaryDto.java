package com.example.uavbackend.fleet.dto;

public record FleetSummaryDto(long online, long warning, long alerts, long avgRtt) {}
