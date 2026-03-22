package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(10)
public class DefaultUserInitializer implements ApplicationRunner {
  private final DepartmentMapper departmentMapper;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    Department forest = ensureDepartment("FOREST", "森林巡查部", "森林火情巡查与热点复核");
    Department air = ensureDepartment("AIR", "空气监测部", "空气质量剖面和污染巡查");
    Department grid = ensureDepartment("GRID", "电网巡检部", "输电线路与通道巡检");

    ensureUser("superadmin", "超级管理员", UserRole.SUPERADMIN, null);

    ensureUser("forest.lead", "森林巡查负责人", UserRole.DEPT_LEAD, forest);
    ensureUser("air.lead", "空气监测负责人", UserRole.DEPT_LEAD, air);
    ensureUser("grid.lead", "电网巡检负责人", UserRole.DEPT_LEAD, grid);

    ensureUser("张三", "张三", UserRole.EXECUTOR, forest);
    ensureUser("李四", "李四", UserRole.EXECUTOR, air);
    ensureUser("王五", "王五", UserRole.EXECUTOR, grid);
    ensureUser("forest.exec2", "森林执行二号", UserRole.EXECUTOR, forest);
    ensureUser("air.exec2", "空气执行二号", UserRole.EXECUTOR, air);
    ensureUser("grid.exec2", "电网执行二号", UserRole.EXECUTOR, grid);
  }

  private Department ensureDepartment(String deptCode, String deptName, String description) {
    Department department =
        departmentMapper.selectOne(
            new LambdaQueryWrapper<Department>().eq(Department::getDeptCode, deptCode).last("limit 1"));
    if (department != null) {
      department.setDeptName(deptName);
      department.setDescription(description);
      department.setStatus(DepartmentStatus.ACTIVE);
      departmentMapper.updateById(department);
      return department;
    }
    department = new Department();
    department.setDeptCode(deptCode);
    department.setDeptName(deptName);
    department.setDescription(description);
    department.setStatus(DepartmentStatus.ACTIVE);
    departmentMapper.insert(department);
    return department;
  }

  private void ensureUser(String username, String name, UserRole role, Department department) {
    User user =
        userMapper.selectOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("limit 1"));
    if (user == null) {
      user = new User();
      user.setUsername(username);
      user.setPasswordHash(passwordEncoder.encode("123456"));
    }
    user.setName(name);
    user.setRole(role);
    user.setDepartmentId(department == null ? null : department.getId());
    user.setDepartmentName(department == null ? null : department.getDeptName());
    user.setStatus(UserStatus.ACTIVE);
    if (user.getId() == null) {
      userMapper.insert(user);
      return;
    }
    userMapper.updateById(user);
  }
}
