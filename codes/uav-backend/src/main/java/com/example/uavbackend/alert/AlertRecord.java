package com.example.uavbackend.alert;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("alert_record")
public class AlertRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long ruleId;
  private String missionCode;
  private String uavCode;
  private String metricCode;
  private Double metricValue;
  private LocalDateTime triggeredAt;
  private Boolean processed;
  private LocalDateTime processedAt;
}

