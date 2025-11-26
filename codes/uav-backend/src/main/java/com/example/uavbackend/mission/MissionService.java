package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.mission.dto.MissionCreateRequest;
import com.example.uavbackend.mission.dto.MissionDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MissionService {
  private final MissionMapper missionMapper;
  private final MissionRoutePointMapper routePointMapper;

  public List<MissionDto> list(List<String> statuses) {
    LambdaQueryWrapper<Mission> wrapper = new LambdaQueryWrapper<>();
    if (statuses != null && !statuses.isEmpty()) {
      wrapper.in(Mission::getStatus, statuses);
    }
    List<Mission> missions = missionMapper.selectList(wrapper);
    return missions.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Optional<MissionDto> findByCode(String code) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, code));
    return Optional.ofNullable(mission).map(this::toDtoWithRoute);
  }

  @Transactional
  public MissionDto create(MissionCreateRequest request) {
    Mission mission = new Mission();
    mission.setMissionCode(
        request.missionCode() != null ? request.missionCode() : generateMissionCode());
    mission.setName(request.name());
    mission.setMissionType(request.missionType());
    mission.setPilotName(request.pilotName());
    mission.setStatus(request.status());
    mission.setPriority(request.priority());
    mission.setProgress(request.progress() == null ? 0 : request.progress());
    mission.setColorHex(request.status().equals("执行中") ? "#f97316" : "#22c55e");
    mission.setMetrics(JsonUtils.toJson(request.metrics()));
    mission.setMilestones(JsonUtils.toJson(request.milestones()));
    missionMapper.insert(mission);
    saveRoutePoints(mission.getId(), request.route());
    return toDtoWithRoute(mission);
  }

  @Transactional
  public MissionDto updateProgress(String missionCode, Integer progress) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission == null) {
      throw new IllegalArgumentException("任务不存在");
    }
    mission.setProgress(progress);
    missionMapper.updateById(mission);
    return toDto(mission);
  }

  @Transactional
  public void interrupt(String missionCode) {
    Mission mission =
        missionMapper.selectOne(
            new LambdaQueryWrapper<Mission>().eq(Mission::getMissionCode, missionCode));
    if (mission != null) {
      mission.setStatus("异常中止");
      missionMapper.updateById(mission);
    }
  }

  private void saveRoutePoints(Long missionId, List<List<Double>> points) {
    routePointMapper.delete(
        new LambdaQueryWrapper<MissionRoutePoint>().eq(MissionRoutePoint::getMissionId, missionId));
    for (int i = 0; i < points.size(); i++) {
      List<Double> p = points.get(i);
      MissionRoutePoint point = new MissionRoutePoint();
      point.setMissionId(missionId);
      point.setSeq(i + 1);
      point.setLat(BigDecimal.valueOf(p.get(0)));
      point.setLng(BigDecimal.valueOf(p.get(1)));
      routePointMapper.insert(point);
    }
  }

  private MissionDto toDto(Mission entity) {
    List<MissionRoutePoint> route =
        routePointMapper.selectList(
            new LambdaQueryWrapper<MissionRoutePoint>()
                .eq(MissionRoutePoint::getMissionId, entity.getId())
                .orderByAsc(MissionRoutePoint::getSeq));
    return new MissionDto(
        entity.getId(),
        entity.getMissionCode(),
        entity.getName(),
        entity.getMissionType(),
        entity.getPilotName(),
        entity.getStatus(),
        entity.getPriority(),
        entity.getProgress(),
        entity.getColorHex(),
        route.stream()
            .map(p -> List.of(p.getLat().doubleValue(), p.getLng().doubleValue()))
            .collect(Collectors.toList()),
        JsonUtils.fromJsonArray(entity.getMilestones()),
        JsonUtils.fromJsonArray(entity.getMetrics()),
        List.of());
  }

  private MissionDto toDtoWithRoute(Mission mission) {
    return toDto(mission);
  }

  private String generateMissionCode() {
    return "M-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
