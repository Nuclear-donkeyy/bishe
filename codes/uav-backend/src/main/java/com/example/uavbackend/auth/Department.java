package com.example.uavbackend.auth;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("departments")
public class Department extends BaseEntity {
  private String deptCode;
  private String deptName;
  private String description;
  private DepartmentStatus status = DepartmentStatus.ACTIVE;
}
