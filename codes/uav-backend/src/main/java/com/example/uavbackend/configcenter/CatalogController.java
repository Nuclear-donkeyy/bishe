package com.example.uavbackend.configcenter;

import com.example.uavbackend.configcenter.dto.MetricCreateRequest;
import com.example.uavbackend.configcenter.dto.MetricItem;
import com.example.uavbackend.configcenter.dto.MissionTypeCreateRequest;
import com.example.uavbackend.configcenter.dto.MissionTypeItem;
import com.example.uavbackend.configcenter.dto.SensorCreateRequest;
import com.example.uavbackend.configcenter.dto.SensorTypeItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/catalog", "/api/configcenter"})
@RequiredArgsConstructor
public class CatalogController {
  private final CatalogService catalogService;

  // Mission Types
  @GetMapping("/mission-types")
  public List<MissionTypeItem> missionTypes() {
    return catalogService.listMissionTypes();
  }

  @PostMapping("/mission-types")
  public ResponseEntity<MissionTypeItem> createMissionType(@RequestBody MissionTypeCreateRequest req) {
    return ResponseEntity.status(201).body(catalogService.createMissionType(req));
  }

  @PutMapping("/mission-types/{id}")
  public MissionTypeItem updateMissionType(@PathVariable("id") Long id, @RequestBody MissionTypeCreateRequest req) {
    return catalogService.updateMissionType(id, req);
  }

  @DeleteMapping("/mission-types/{id}")
  public ResponseEntity<Void> deleteMissionType(@PathVariable("id") Long id) {
    catalogService.deleteMissionType(id);
    return ResponseEntity.noContent().build();
  }

  // Metrics
  @GetMapping("/metrics")
  public List<MetricItem> metrics() {
    return catalogService.listMetrics();
  }

  @PostMapping("/metrics")
  public ResponseEntity<MetricItem> createMetric(@RequestBody MetricCreateRequest req) {
    return ResponseEntity.status(201).body(catalogService.createMetric(req));
  }

  @PutMapping("/metrics/{id}")
  public MetricItem updateMetric(@PathVariable("id") Long id, @RequestBody MetricCreateRequest req) {
    return catalogService.updateMetric(id, req);
  }

  @DeleteMapping("/metrics/{id}")
  public ResponseEntity<Void> deleteMetric(@PathVariable("id") Long id) {
    catalogService.deleteMetric(id);
    return ResponseEntity.noContent().build();
  }

  // Sensors
  @GetMapping("/sensors")
  public List<SensorTypeItem> sensors() {
    return catalogService.listSensors();
  }

  @PostMapping("/sensors")
  public ResponseEntity<SensorTypeItem> createSensor(@RequestBody SensorCreateRequest req) {
    return ResponseEntity.status(201).body(catalogService.createSensor(req));
  }

  @DeleteMapping("/sensors/{id}")
  public ResponseEntity<Void> deleteSensor(@PathVariable("id") Long id) {
    catalogService.deleteSensor(id);
    return ResponseEntity.noContent().build();
  }
}
