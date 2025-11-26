package com.example.uavbackend.fleet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("uav_sessions")
public class UavSession extends BaseEntity {
  private Long uavId;
  private String sessionCode;
  private String clientId;
  private Instant connectedAt;
  private Instant disconnectedAt;
  private String connectionIp;
  private String reason;
}
