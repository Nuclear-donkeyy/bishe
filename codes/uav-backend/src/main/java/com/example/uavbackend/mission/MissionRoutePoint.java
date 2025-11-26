package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("mission_route_points")
public class MissionRoutePoint extends BaseEntity {
  private Long missionId;
  private Integer seq;
  private BigDecimal lat;
  private BigDecimal lng;
  private BigDecimal altitude;
}
