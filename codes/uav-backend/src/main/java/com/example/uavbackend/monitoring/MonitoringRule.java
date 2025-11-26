package com.example.uavbackend.monitoring;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("monitoring_rules")
public class MonitoringRule extends BaseEntity {
  private Long taskId;
  private String name;
  private String metric;
  private String threshold;
  private String level;
}
