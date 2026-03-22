package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessScopeService {
  private final UserMapper userMapper;

  public AccessScope currentScope() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication == null ? null : authentication.getName();
    if (username == null || username.isBlank()) {
      throw new IllegalStateException("当前请求缺少登录身份");
    }
    User user =
        Optional.ofNullable(
                userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)))
            .orElseThrow(() -> new IllegalStateException("当前用户不存在"));
    return new AccessScope(
        user.getRole() == UserRole.SUPERADMIN,
        user.getUsername(),
        user.getName(),
        user.getRole(),
        user.getDepartmentId(),
        user.getDepartmentName());
  }

  public boolean isSuperAdmin() {
    return currentScope().superAdmin();
  }
}
