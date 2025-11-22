package com.example.uavbackend.mission;

import com.example.uavbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mission_route_points")
public class MissionRoutePoint extends BaseEntity {
  @Column(name = "mission_id", nullable = false)
  private Long missionId;

  @Column(nullable = false)
  private Integer seq;

  @Column(nullable = false, precision = 9, scale = 6)
  private BigDecimal lat;

  @Column(nullable = false, precision = 9, scale = 6)
  private BigDecimal lng;

  @Column(precision = 8, scale = 2)
  private BigDecimal altitude;
}
