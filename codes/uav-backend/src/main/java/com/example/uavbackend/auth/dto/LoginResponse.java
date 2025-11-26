package com.example.uavbackend.auth.dto;

/**
 * 登录成功后返回给前端的会话信息：token 以及当前用户基本信息。
 * 对应 /api/auth/login 响应体。
 */
public record LoginResponse(String token, UserDto user) {}
