package com.example.uavbackend.configcenter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.configcenter.dto.MetricCreateRequest;
import com.example.uavbackend.configcenter.dto.MetricItem;
import com.example.uavbackend.configcenter.dto.MissionTypeCreateRequest;
import com.example.uavbackend.configcenter.dto.MissionTypeItem;
import com.example.uavbackend.configcenter.dto.SensorCreateRequest;
import com.example.uavbackend.configcenter.dto.SensorTypeItem;
import com.example.uavbackend.mission.MissionTypeDefinition;
import com.example.uavbackend.mission.MissionTypeMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogService {
  private final MissionTypeMapper missionTypeMapper;
  private final MissionTypeMetricMapper missionTypeMetricMapper;
  private final MetricDefinitionMapper metricDefinitionMapper;
  private final MetricSensorMapper metricSensorMapper;
  private final SensorTypeMapper sensorTypeMapper;

  // Mission Types
  public List<MissionTypeItem> listMissionTypes() {
    List<MissionTypeDefinition> types = missionTypeMapper.selectList(null);
    Map<Long, List<MissionTypeMetric>> relMap =
        missionTypeMetricMapper.selectList(null).stream()
            .collect(Collectors.groupingBy(MissionTypeMetric::getMissionTypeId));
    return types.stream()
        .map(
            t ->
                new MissionTypeItem(
                    t.getId(),
                    t.getTypeCode(),
                    t.getDisplayName(),
                    t.getDescription(),
                    relMap.getOrDefault(t.getId(), List.of()).stream()
                        .map(MissionTypeMetric::getMetricId)
                        .toList()))
        .toList();
  }

  @Transactional
  public MissionTypeItem createMissionType(MissionTypeCreateRequest req) {
    ensureMissionTypeUnique(req.typeCode(), req.displayName(), null);
    List<Long> metricIds = normalizeIds(req.metricIds());
    MissionTypeDefinition type = new MissionTypeDefinition();
    type.setTypeCode(req.typeCode());
    type.setDisplayName(req.displayName());
    type.setDescription(req.description());
    missionTypeMapper.insert(type);
    saveMissionTypeMetrics(type.getId(), metricIds);
    return new MissionTypeItem(type.getId(), type.getTypeCode(), type.getDisplayName(), type.getDescription(), metricIds);
  }

  @Transactional
  public MissionTypeItem updateMissionType(Long id, MissionTypeCreateRequest req) {
    MissionTypeDefinition type = missionTypeMapper.selectById(id);
    if (type == null) {
      throw new IllegalArgumentException("任务类型不存在");
    }
    List<Long> metricIds = normalizeIds(req.metricIds());
    ensureMissionTypeUnique(type.getTypeCode(), req.displayName(), id);
    type.setDisplayName(req.displayName());
    type.setDescription(req.description());
    missionTypeMapper.updateById(type);
    missionTypeMetricMapper.delete(
        new LambdaQueryWrapper<MissionTypeMetric>().eq(MissionTypeMetric::getMissionTypeId, id));
    saveMissionTypeMetrics(id, metricIds);
    return new MissionTypeItem(id, type.getTypeCode(), type.getDisplayName(), type.getDescription(), metricIds);
  }

  @Transactional
  public void deleteMissionType(Long id) {
    missionTypeMetricMapper.delete(
        new LambdaQueryWrapper<MissionTypeMetric>().eq(MissionTypeMetric::getMissionTypeId, id));
    missionTypeMapper.deleteById(id);
  }

  private void saveMissionTypeMetrics(Long missionTypeId, List<Long> metricIds) {
    if (metricIds == null || metricIds.isEmpty()) return;
    List<MissionTypeMetric> rels = new ArrayList<>();
    for (int i = 0; i < metricIds.size(); i++) {
      MissionTypeMetric rel = new MissionTypeMetric();
      rel.setMissionTypeId(missionTypeId);
      rel.setMetricId(metricIds.get(i));
      rel.setDisplayOrder(i);
      rels.add(rel);
    }
    rels.forEach(missionTypeMetricMapper::insert);
  }

  // Metrics
  public List<MetricItem> listMetrics() {
    List<MetricDefinition> metrics = metricDefinitionMapper.selectList(null);
    Map<Long, List<Long>> metricSensors =
        metricSensorMapper.selectList(null).stream()
            .collect(Collectors.groupingBy(MetricSensor::getMetricId, Collectors.mapping(MetricSensor::getSensorTypeId, Collectors.toList())));
    return metrics.stream()
        .map(
            m ->
                new MetricItem(
                    m.getId(),
                    m.getMetricCode(),
                    m.getName(),
                    m.getUnit(),
                    m.getDescription(),
                    metricSensors.getOrDefault(m.getId(), List.of())))
        .toList();
  }

  @Transactional
  public MetricItem createMetric(MetricCreateRequest req) {
    ensureMetricUnique(req.metricCode(), req.name(), null);
    MetricDefinition metric = new MetricDefinition();
    metric.setMetricCode(req.metricCode());
    metric.setName(req.name());
    metric.setUnit(req.unit());
    metric.setDescription(req.description());
    metricDefinitionMapper.insert(metric);
    saveMetricSensors(metric.getId(), req.sensorTypeIds());
    return new MetricItem(metric.getId(), metric.getMetricCode(), metric.getName(), metric.getUnit(), metric.getDescription(), req.sensorTypeIds());
  }

  @Transactional
  public MetricItem updateMetric(Long id, MetricCreateRequest req) {
    MetricDefinition metric = metricDefinitionMapper.selectById(id);
    if (metric == null) {
      throw new IllegalArgumentException("指标不存在");
    }
    ensureMetricUnique(metric.getMetricCode(), req.name(), id);
    metric.setName(req.name());
    metric.setUnit(req.unit());
    metric.setDescription(req.description());
    metricDefinitionMapper.updateById(metric);
    metricSensorMapper.delete(new LambdaQueryWrapper<MetricSensor>().eq(MetricSensor::getMetricId, id));
    saveMetricSensors(id, req.sensorTypeIds());
    return new MetricItem(id, metric.getMetricCode(), metric.getName(), metric.getUnit(), metric.getDescription(), req.sensorTypeIds());
  }

  @Transactional
  public void deleteMetric(Long id) {
    Long mtCount =
        missionTypeMetricMapper.selectCount(
            new LambdaQueryWrapper<MissionTypeMetric>().eq(MissionTypeMetric::getMetricId, id));
    if (mtCount != null && mtCount > 0) {
      throw new IllegalStateException("指标已被任务类型引用，无法删除");
    }
    metricSensorMapper.delete(new LambdaQueryWrapper<MetricSensor>().eq(MetricSensor::getMetricId, id));
    metricDefinitionMapper.deleteById(id);
  }

  private void saveMetricSensors(Long metricId, List<Long> sensorIds) {
    if (sensorIds == null || sensorIds.isEmpty()) return;
    List<MetricSensor> rels = sensorIds.stream().map(sid -> {
      MetricSensor r = new MetricSensor();
      r.setMetricId(metricId);
      r.setSensorTypeId(sid);
      return r;
    }).toList();
    rels.forEach(metricSensorMapper::insert);
  }

  // Sensors
  public List<SensorTypeItem> listSensors() {
    return sensorTypeMapper.selectList(null).stream()
        .map(s -> new SensorTypeItem(s.getId(), s.getSensorCode(), s.getName(), s.getDescription()))
        .toList();
  }

  @Transactional
  public SensorTypeItem createSensor(SensorCreateRequest req) {
    ensureSensorUnique(req.sensorCode(), req.name(), null);
    SensorType sensor = new SensorType();
    sensor.setSensorCode(req.sensorCode());
    sensor.setName(req.name());
    sensor.setDescription(req.description());
    sensorTypeMapper.insert(sensor);
    return new SensorTypeItem(sensor.getId(), sensor.getSensorCode(), sensor.getName(), sensor.getDescription());
  }

  @Transactional
  public void deleteSensor(Long id) {
    Long refCount =
        metricSensorMapper.selectCount(
            new LambdaQueryWrapper<MetricSensor>().eq(MetricSensor::getSensorTypeId, id));
    if (refCount != null && refCount > 0) {
      throw new IllegalStateException("传感器已被指标引用，无法删除");
    }
    sensorTypeMapper.deleteById(id);
  }

  private void ensureMissionTypeUnique(String typeCode, String displayName, Long excludeId) {
    LambdaQueryWrapper<MissionTypeDefinition> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(MissionTypeDefinition::getTypeCode, typeCode);
    if (excludeId != null) {
      wrapper.ne(MissionTypeDefinition::getId, excludeId);
    }
    if (missionTypeMapper.selectCount(wrapper) > 0) {
      throw new IllegalArgumentException("任务类型编码已存在");
    }
    LambdaQueryWrapper<MissionTypeDefinition> nameWrapper = new LambdaQueryWrapper<>();
    nameWrapper.eq(MissionTypeDefinition::getDisplayName, displayName);
    if (excludeId != null) {
      nameWrapper.ne(MissionTypeDefinition::getId, excludeId);
    }
    if (missionTypeMapper.selectCount(nameWrapper) > 0) {
      throw new IllegalArgumentException("任务类型名称已存在");
    }
  }

  private void ensureMetricUnique(String metricCode, String name, Long excludeId) {
    LambdaQueryWrapper<MetricDefinition> codeWrapper = new LambdaQueryWrapper<>();
    codeWrapper.eq(MetricDefinition::getMetricCode, metricCode);
    if (excludeId != null) {
      codeWrapper.ne(MetricDefinition::getId, excludeId);
    }
    if (metricDefinitionMapper.selectCount(codeWrapper) > 0) {
      throw new IllegalArgumentException("指标编码已存在");
    }
    LambdaQueryWrapper<MetricDefinition> nameWrapper = new LambdaQueryWrapper<>();
    nameWrapper.eq(MetricDefinition::getName, name);
    if (excludeId != null) {
      nameWrapper.ne(MetricDefinition::getId, excludeId);
    }
    if (metricDefinitionMapper.selectCount(nameWrapper) > 0) {
      throw new IllegalArgumentException("指标名称已存在");
    }
  }

  private void ensureSensorUnique(String sensorCode, String name, Long excludeId) {
    LambdaQueryWrapper<SensorType> codeWrapper = new LambdaQueryWrapper<>();
    codeWrapper.eq(SensorType::getSensorCode, sensorCode);
    if (excludeId != null) {
      codeWrapper.ne(SensorType::getId, excludeId);
    }
    if (sensorTypeMapper.selectCount(codeWrapper) > 0) {
      throw new IllegalArgumentException("传感器编码已存在");
    }
    LambdaQueryWrapper<SensorType> nameWrapper = new LambdaQueryWrapper<>();
    nameWrapper.eq(SensorType::getName, name);
    if (excludeId != null) {
      nameWrapper.ne(SensorType::getId, excludeId);
    }
    if (sensorTypeMapper.selectCount(nameWrapper) > 0) {
      throw new IllegalArgumentException("传感器名称已存在");
    }
  }

  private List<Long> normalizeIds(List<Long> ids) {
    if (ids == null) return List.of();
    return ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
  }
}
