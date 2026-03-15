package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.fleet.UavDevice;
import com.example.uavbackend.fleet.UavDeviceMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(40)
@Slf4j
public class MissionQueueBootstrap implements ApplicationRunner {
  private final MissionMapper missionMapper;
  private final MissionRoutePointMapper routePointMapper;
  private final MissionUavAssignmentMapper assignmentMapper;
  private final UavDeviceMapper uavDeviceMapper;
  private final MissionQueueService missionQueueService;

  @Override
  public void run(ApplicationArguments args) {
    List<Mission> waitingMissions =
        missionMapper.selectList(
            new LambdaQueryWrapper<Mission>()
                .in(Mission::getStatus, List.of(MissionStatus.QUEUE.name(), MissionStatus.PREEMPTED.name())));
    if (waitingMissions.isEmpty()) {
      return;
    }

    Map<Long, UavDevice> uavById =
        uavDeviceMapper.selectList(null).stream().collect(Collectors.toMap(UavDevice::getId, item -> item));

    for (Mission mission : waitingMissions) {
      List<List<Double>> route =
          routePointMapper.selectList(
                  new LambdaQueryWrapper<MissionRoutePoint>()
                      .eq(MissionRoutePoint::getMissionId, mission.getId())
                      .orderByAsc(MissionRoutePoint::getSeq))
              .stream()
              .map(point -> List.of(point.getLat().doubleValue(), point.getLng().doubleValue()))
              .toList();

      List<String> candidateUavCodes =
          assignmentMapper.selectList(
                  new LambdaQueryWrapper<MissionUavAssignment>()
                      .eq(MissionUavAssignment::getMissionId, mission.getId())
                      .isNull(MissionUavAssignment::getReleasedAt))
              .stream()
              .map(MissionUavAssignment::getUavId)
              .map(uavById::get)
              .filter(java.util.Objects::nonNull)
              .map(UavDevice::getUavCode)
              .toList();

      missionQueueService.restoreQueuedMission(mission, route, candidateUavCodes, "startup_restore");
    }
    log.info("Restored {} queued missions into scheduling queue", waitingMissions.size());
  }
}
