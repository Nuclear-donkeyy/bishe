package com.example.uavbackend.monitoring;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.uavbackend.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("monitoring_tasks")
public class MonitoringTask extends BaseEntity {
  private String taskCode;
  private Long missionId;
  private String missionName;
  private String missionType;
  private String ownerName;
  private String status;
  private String locationDesc;
  private Integer devicesCount;
  private String videoUrl;
  private String streamId;
  private String extraData;
}
