package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultUserInitializer implements ApplicationRunner {
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    ensureUser("superadmin", "超级管理员", UserRole.SUPERADMIN);
    ensureUser("张三", "张三", UserRole.OPERATOR);
    ensureUser("李四", "李四", UserRole.OPERATOR);
    ensureUser("王五", "王五", UserRole.OPERATOR);
  }

  private void ensureUser(String username, String name, UserRole role) {
    Long exists =
        userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    if (exists != null && exists > 0) {
      return;
    }

    User user = new User();
    user.setUsername(username);
    user.setName(name);
    user.setPasswordHash(passwordEncoder.encode("123456"));
    user.setRole(role);
    user.setStatus(UserStatus.ACTIVE);
    userMapper.insert(user);
  }
}
