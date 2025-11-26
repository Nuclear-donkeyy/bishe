package com.example.uavbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login payload for /api/auth/login：前端登录表单提交的账号和密码。
 * Java record = 简洁的不可变数据载体，用于声明 DTO。
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
