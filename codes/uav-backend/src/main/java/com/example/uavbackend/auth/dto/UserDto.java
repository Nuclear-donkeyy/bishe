package com.example.uavbackend.auth.dto;

import com.example.uavbackend.auth.UserRole;

/**
 * 登录态中的用户信息（供前端导航/权限展示），来自 users 表。
 */
public record UserDto(Long id, String username, String name, UserRole role) {}
