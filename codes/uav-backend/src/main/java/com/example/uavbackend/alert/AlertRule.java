package com.example.uavbackend.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("alert_rule")
public class AlertRule {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String name;
  private String description;
  /** AND / OR */
  private String logicOperator;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

