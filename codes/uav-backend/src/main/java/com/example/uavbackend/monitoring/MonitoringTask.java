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
@Table(name = "monitoring_tasks")
public class MonitoringTask extends BaseEntity {
  @Column(name = "task_code", nullable = false, unique = true, length = 64)
  private String taskCode;

  @Column(name = "mission_id")
  private Long missionId;

  @Column(name = "mission_name", nullable = false, length = 128)
  private String missionName;

  @Column(name = "mission_type", nullable = false, length = 64)
  private String missionType;

  @Column(name = "owner_name", nullable = false, length = 64)
  private String ownerName;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "location_desc", length = 128)
  private String locationDesc;

  @Column(name = "devices_count")
  private Integer devicesCount;

  @Column(name = "video_url", length = 255)
  private String videoUrl;

  @Column(name = "stream_id", length = 64)
  private String streamId;

  @Column(name = "extra_data", columnDefinition = "json")
  private String extraData;
}
