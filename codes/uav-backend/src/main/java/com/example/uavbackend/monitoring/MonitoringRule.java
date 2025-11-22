package com.example.uavbackend.monitoring;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "monitoring_rules")
public class MonitoringRule extends BaseEntity {
  @Column(name = "task_id", nullable = false)
  private Long taskId;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(nullable = false, length = 64)
  private String metric;

  @Column(nullable = false, length = 64)
  private String threshold;

  @Column(nullable = false, length = 8)
  private String level;
}
