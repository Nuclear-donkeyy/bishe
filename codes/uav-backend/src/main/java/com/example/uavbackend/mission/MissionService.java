package com.example.uavbackend.mission;

import com.example.uavbackend.mission.dto.MissionCreateRequest;
import com.example.uavbackend.mission.dto.MissionDto;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MissionService {
  private final MissionRepository missionRepository;
  private final MissionRoutePointRepository routePointRepository;

  public List<MissionDto> list(List<String> statuses) {
    List<Mission> missions =
        statuses == null || statuses.isEmpty()
            ? missionRepository.findAll()
            : missionRepository.findByStatusIn(statuses);
    return missions.stream().map(this::toDto).collect(Collectors.toList());
  }

  public Optional<MissionDto> findByCode(String code) {
    return missionRepository.findByMissionCode(code).map(this::toDtoWithRoute);
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
    Mission saved = missionRepository.save(mission);
    saveRoutePoints(saved.getId(), request.route());
    return toDtoWithRoute(saved);
  }

  @Transactional
  public MissionDto updateProgress(String missionCode, Integer progress) {
    Mission mission =
        missionRepository
            .findByMissionCode(missionCode)
            .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    mission.setProgress(progress);
    Mission saved = missionRepository.save(mission);
    return toDto(saved);
  }

  @Transactional
  public void interrupt(String missionCode) {
    missionRepository
        .findByMissionCode(missionCode)
        .ifPresent(
            mission -> {
              mission.setStatus("异常中止");
              missionRepository.save(mission);
            });
  }

  private void saveRoutePoints(Long missionId, List<List<Double>> points) {
    routePointRepository.deleteAll(routePointRepository.findByMissionIdOrderBySeqAsc(missionId));
    for (int i = 0; i < points.size(); i++) {
      List<Double> p = points.get(i);
      MissionRoutePoint point = new MissionRoutePoint();
      point.setMissionId(missionId);
      point.setSeq(i + 1);
      point.setLat(BigDecimal.valueOf(p.get(0)));
      point.setLng(BigDecimal.valueOf(p.get(1)));
      routePointRepository.save(point);
    }
  }

  private MissionDto toDto(Mission entity) {
    List<MissionRoutePoint> route = routePointRepository.findByMissionIdOrderBySeqAsc(entity.getId());
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
