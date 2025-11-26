package com.example.uavbackend.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseEntity {
  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField(value = "created_at", fill = FieldFill.INSERT)
  private Instant createdAt;

  @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
  private Instant updatedAt;
}
