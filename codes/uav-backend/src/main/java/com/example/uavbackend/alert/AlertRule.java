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
  private Boolean templateEnabled;
  private Long templateId;
  private String templateCode;
  private String templateCategory;
  private Long departmentId;
  private String departmentName;
  private String createdBy;
  private Boolean autoInterrupt;
  private Boolean notifyEnabled;
  private String notifyChannels;
  private String notifyTargets;
  private String notifyTemplate;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
