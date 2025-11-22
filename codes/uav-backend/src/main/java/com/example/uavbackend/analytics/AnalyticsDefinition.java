package com.example.uavbackend.analytics;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "analytics_definitions")
public class AnalyticsDefinition extends BaseEntity {
  @Column(name = "mission_type", nullable = false, length = 64)
  private String missionType;

  @Column(nullable = false, length = 128)
  private String title;

  @Column(length = 255)
  private String description;

  @Column(name = "series_config", columnDefinition = "json")
  private String seriesConfig;

  @Column(name = "display_order")
  private Integer displayOrder;
}
