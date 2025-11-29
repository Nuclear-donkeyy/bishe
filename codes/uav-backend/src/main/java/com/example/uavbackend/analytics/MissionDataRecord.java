package com.example.uavbackend.analytics;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("mission_data_record")
public class MissionDataRecord {
  @TableId(type = IdType.AUTO)
  private Long id;

  private Long missionId;
  private String missionCode;
  private String missionType;
  private String pilotName;
  private String uavCode;
  private String operatorName;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String dataMax; // JSON string
  private String dataMin; // JSON string
  private String dataAvg; // JSON string
}

