package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.auth.dto.UserDto;
import java.util.List;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
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
  private final DepartmentMapper departmentMapper;
  private final PasswordEncoder passwordEncoder;
  private final AccessScopeService accessScopeService;

  public UserController(
      UserMapper userMapper,
      DepartmentMapper departmentMapper,
      PasswordEncoder passwordEncoder,
      AccessScopeService accessScopeService) {
    this.userMapper = userMapper;
    this.departmentMapper = departmentMapper;
    this.passwordEncoder = passwordEncoder;
    this.accessScopeService = accessScopeService;
  }

  @GetMapping
  public List<UserDto> list(Long departmentId) {
    AccessScope scope = accessScopeService.currentScope();
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.ACTIVE);
    if (scope.superAdmin()) {
      if (departmentId != null) {
        wrapper.eq(User::getDepartmentId, departmentId);
      }
    } else if (scope.isDeptLead()) {
      wrapper.eq(User::getDepartmentId, scope.departmentId());
    } else {
      wrapper.eq(User::getUsername, scope.username());
    }
    wrapper.orderByAsc(User::getDepartmentName).orderByAsc(User::getRole).orderByAsc(User::getUsername);
    return userMapper.selectList(wrapper).stream().map(this::toDto).toList();
  }

  @PostMapping
  public UserDto create(@RequestBody CreateUserRequest req) {
    AccessScope scope = accessScopeService.currentScope();
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
    UserRole role = normalizeRole(req.role());
    Department department = resolveDepartmentForCreate(scope, role, req.departmentId());
    User user = new User();
    user.setUsername(req.username());
    user.setName(req.name() != null ? req.name() : req.username());
    user.setPasswordHash(passwordEncoder.encode(req.password()));
    user.setRole(role);
    user.setDepartmentId(department == null ? null : department.getId());
    user.setDepartmentName(department == null ? null : department.getDeptName());
    user.setStatus(UserStatus.ACTIVE);
    userMapper.insert(user);
    return toDto(user);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable("id") Long id) {
    AccessScope scope = accessScopeService.currentScope();
    User target = userMapper.selectById(id);
    if (target == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    ensureManageable(scope, target);

    if (target.getRole() == UserRole.SUPERADMIN) {
      if (!Objects.equals(scope.username(), target.getUsername())) {
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
      return;
    }
    userMapper.deleteById(id);
  }

  @PostMapping("/{id}/reset-password")
  public void resetPassword(@PathVariable("id") Long id) {
    AccessScope scope = accessScopeService.currentScope();
    User target = userMapper.selectById(id);
    if (target == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    ensureManageable(scope, target);
    target.setPasswordHash(passwordEncoder.encode("123456"));
    userMapper.updateById(target);
  }

  private UserDto toDto(User user) {
    return new UserDto(
        user.getId(),
        user.getUsername(),
        user.getName(),
        user.getRole(),
        user.getDepartmentId(),
        user.getDepartmentName());
  }

  private UserRole normalizeRole(String role) {
    if (!StringUtils.hasText(role)) {
      return UserRole.EXECUTOR;
    }
    String normalized = role.trim().toUpperCase();
    return switch (normalized) {
      case "SUPERADMIN" -> UserRole.SUPERADMIN;
      case "DEPT_LEAD", "LEAD", "负责人" -> UserRole.DEPT_LEAD;
      default -> UserRole.EXECUTOR;
    };
  }

  private Department resolveDepartmentForCreate(AccessScope scope, UserRole role, Long departmentId) {
    if (role == UserRole.SUPERADMIN) {
      if (!scope.superAdmin()) {
        throw new IllegalArgumentException("无权创建超级管理员");
      }
      return null;
    }
    if (scope.superAdmin()) {
      if (departmentId == null) {
        throw new IllegalArgumentException("部门不能为空");
      }
      Department department = departmentMapper.selectById(departmentId);
      if (department == null) {
        throw new IllegalArgumentException("部门不存在");
      }
      return department;
    }
    if (scope.isDeptLead()) {
      if (role != UserRole.EXECUTOR) {
        throw new IllegalArgumentException("部门负责人只能创建本部门执行者");
      }
      Department department = departmentMapper.selectById(scope.departmentId());
      if (department == null) {
        throw new IllegalArgumentException("当前部门不存在");
      }
      return department;
    }
    throw new IllegalArgumentException("无权创建用户");
  }

  private void ensureManageable(AccessScope scope, User target) {
    if (scope.superAdmin()) {
      return;
    }
    if (scope.isDeptLead()
        && target.getRole() == UserRole.EXECUTOR
        && Objects.equals(scope.departmentId(), target.getDepartmentId())) {
      return;
    }
    if (target.getRole() == UserRole.SUPERADMIN && Objects.equals(scope.username(), target.getUsername())) {
      return;
    }
    throw new IllegalArgumentException("无权操作该用户");
  }

  public record CreateUserRequest(
      String username, String password, String name, String role, Long departmentId) {}
}
