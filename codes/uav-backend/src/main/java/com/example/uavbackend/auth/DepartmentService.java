package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.auth.dto.DepartmentDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DepartmentService {
  private final DepartmentMapper departmentMapper;
  private final UserMapper userMapper;
  private final AccessScopeService accessScopeService;

  public List<DepartmentDto> list() {
    AccessScope scope = accessScopeService.currentScope();
    List<Department> departments;
    if (scope.superAdmin()) {
      departments =
          departmentMapper.selectList(
              new LambdaQueryWrapper<Department>().orderByAsc(Department::getDeptCode));
    } else if (scope.departmentId() != null) {
      Department current = departmentMapper.selectById(scope.departmentId());
      departments = current == null ? List.of() : List.of(current);
    } else {
      departments = List.of();
    }
    return departments.stream().map(this::toDto).toList();
  }

  @Transactional
  public DepartmentDto create(CreateDepartmentRequest request) {
    ensureSuperAdmin();
    if (!StringUtils.hasText(request.deptCode()) || !StringUtils.hasText(request.deptName())) {
      throw new IllegalArgumentException("部门编码和名称不能为空");
    }
    Long exists =
        departmentMapper.selectCount(
            new LambdaQueryWrapper<Department>().eq(Department::getDeptCode, request.deptCode().trim()));
    if (exists != null && exists > 0) {
      throw new IllegalArgumentException("部门编码已存在");
    }
    Department department = new Department();
    department.setDeptCode(request.deptCode().trim());
    department.setDeptName(request.deptName().trim());
    department.setDescription(trimToNull(request.description()));
    department.setStatus(request.status() == null ? DepartmentStatus.ACTIVE : request.status());
    departmentMapper.insert(department);
    return toDto(department);
  }

  @Transactional
  public DepartmentDto update(Long id, CreateDepartmentRequest request) {
    ensureSuperAdmin();
    Department department = departmentMapper.selectById(id);
    if (department == null) {
      throw new IllegalArgumentException("部门不存在");
    }
    if (!StringUtils.hasText(request.deptCode()) || !StringUtils.hasText(request.deptName())) {
      throw new IllegalArgumentException("部门编码和名称不能为空");
    }
    Long exists =
        departmentMapper.selectCount(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptCode, request.deptCode().trim())
                .ne(Department::getId, id));
    if (exists != null && exists > 0) {
      throw new IllegalArgumentException("部门编码已存在");
    }
    department.setDeptCode(request.deptCode().trim());
    department.setDeptName(request.deptName().trim());
    department.setDescription(trimToNull(request.description()));
    department.setStatus(request.status() == null ? department.getStatus() : request.status());
    departmentMapper.updateById(department);
    syncUserDepartmentNames(department);
    return toDto(department);
  }

  @Transactional
  public void delete(Long id) {
    ensureSuperAdmin();
    Department department = departmentMapper.selectById(id);
    if (department == null) {
      return;
    }
    Long members =
        userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDepartmentId, id).eq(User::getStatus, UserStatus.ACTIVE));
    if (members != null && members > 0) {
      throw new IllegalArgumentException("部门下仍有启用用户，无法删除");
    }
    departmentMapper.deleteById(id);
  }

  private DepartmentDto toDto(Department department) {
    List<User> users =
        userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getDepartmentId, department.getId()));
    int leadCount = (int) users.stream().filter(user -> user.getRole() == UserRole.DEPT_LEAD).count();
    int executorCount = (int) users.stream().filter(user -> user.getRole() == UserRole.EXECUTOR).count();
    return new DepartmentDto(
        department.getId(),
        department.getDeptCode(),
        department.getDeptName(),
        department.getDescription(),
        department.getStatus(),
        users.size(),
        leadCount,
        executorCount);
  }

  private void syncUserDepartmentNames(Department department) {
    List<User> users =
        userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getDepartmentId, department.getId()));
    for (User user : users) {
      user.setDepartmentName(department.getDeptName());
      userMapper.updateById(user);
    }
  }

  private void ensureSuperAdmin() {
    if (!accessScopeService.currentScope().superAdmin()) {
      throw new IllegalArgumentException("无权操作部门");
    }
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  public record CreateDepartmentRequest(
      String deptCode, String deptName, String description, DepartmentStatus status) {}
}
