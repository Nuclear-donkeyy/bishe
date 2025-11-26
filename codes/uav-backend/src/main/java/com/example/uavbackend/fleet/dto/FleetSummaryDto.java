package com.example.uavbackend.fleet.dto;

/**
 * 机群总览卡片数据：在线/告警/告警数与平均 RTT，展示在前端总览页。
 */
public record FleetSummaryDto(long online, long warning, long alerts, long avgRtt) {}
