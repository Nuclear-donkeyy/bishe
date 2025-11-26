package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  public UserController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping
  public List<User> list() {
    return userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.ACTIVE));
  }

  @PostMapping
  public User create(@RequestBody CreateUserRequest req) {
    if (req.username() == null || req.username().isBlank()) {
      throw new IllegalArgumentException("用户名不能为空");
    }
    if (req.password() == null || req.password().isBlank()) {
      throw new IllegalArgumentException("密码不能为空");
    }
    Long exists =
        userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.username()));
    if (exists != null && exists > 0) {
      throw new IllegalArgumentException("用户名已存在");
    }
    User user = new User();
    user.setUsername(req.username());
    user.setName(req.name() != null ? req.name() : req.username());
    user.setPasswordHash(passwordEncoder.encode(req.password()));
    UserRole role =
        req.role() != null && req.role().equalsIgnoreCase("superadmin")
            ? UserRole.SUPERADMIN
            : UserRole.OPERATOR;
    user.setRole(role);
    user.setStatus(UserStatus.ACTIVE);
    userMapper.insert(user);
    return user;
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable("id") Long id) {
    User target = userMapper.selectById(id);
    if (target == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String currentUsername = auth != null ? auth.getName() : null;
    User current =
        currentUsername != null
            ? userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, currentUsername))
            : null;

    if (target.getRole() == UserRole.SUPERADMIN) {
      // 注销超级管理员
      if (current == null || !Objects.equals(current.getId(), target.getId())) {
        throw new IllegalStateException("只能注销当前登录的超级管理员");
      }
      Long otherAdmins =
          userMapper.selectCount(
              new LambdaQueryWrapper<User>()
                  .eq(User::getRole, UserRole.SUPERADMIN)
                  .eq(User::getStatus, UserStatus.ACTIVE)
                  .ne(User::getId, target.getId()));
      if (otherAdmins == null || otherAdmins == 0) {
        throw new IllegalStateException("至少需要保留一名超级管理员，注销失败");
      }
      target.setStatus(UserStatus.DISABLED);
      userMapper.updateById(target);
    } else {
      // 删除操作员
      userMapper.deleteById(id);
    }
  }

  @PostMapping("/{id}/reset-password")
  public void resetPassword(@PathVariable("id") Long id) {
    User target = userMapper.selectById(id);
    if (target == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    target.setPasswordHash(passwordEncoder.encode("123456"));
    userMapper.updateById(target);
  }

  public record CreateUserRequest(String username, String password, String name, String role) {}
}
