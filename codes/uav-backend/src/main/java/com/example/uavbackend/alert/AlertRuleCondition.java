package com.example.uavbackend.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("alert_rule_condition")
public class AlertRuleCondition {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long ruleId;
  private String metricCode;
  /** GT, GTE, LT, LTE, EQ */
  private String comparator;
  private Double threshold;
}

