package com.example.uavbackend.analytics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.analytics.dto.MissionDataQuery;
import com.example.uavbackend.analytics.dto.MissionDataRecordDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/data")
@RequiredArgsConstructor
public class AnalyticsController {
  private final MissionDataRecordMapper recordMapper;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @GetMapping
  public List<MissionDataRecordDto> list(
      @RequestParam("missionType") String missionType,
      @RequestParam(value = "uavCode", required = false) String uavCode,
      @RequestParam(value = "operatorName", required = false) String operatorName,
      @RequestParam(value = "missionCode", required = false) String missionCode,
      @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to) {
    LambdaQueryWrapper<MissionDataRecord> wrapper =
        new LambdaQueryWrapper<MissionDataRecord>().eq(MissionDataRecord::getMissionType, missionType);
    if (uavCode != null) wrapper.eq(MissionDataRecord::getUavCode, uavCode);
    if (operatorName != null) wrapper.eq(MissionDataRecord::getOperatorName, operatorName);
    if (missionCode != null) wrapper.eq(MissionDataRecord::getMissionCode, missionCode);
    if (from != null) wrapper.ge(MissionDataRecord::getEndTime, from);
    if (to != null) wrapper.le(MissionDataRecord::getEndTime, to);
    wrapper.orderByDesc(MissionDataRecord::getEndTime);
    return recordMapper.selectList(wrapper).stream().map(this::toDto).toList();
  }

  private MissionDataRecordDto toDto(MissionDataRecord r) {
    Map<String, Object> maxMap = MissionDataAggregator.jsonToMap(objectMapper, r.getDataMax());
    Map<String, Object> minMap = MissionDataAggregator.jsonToMap(objectMapper, r.getDataMin());
    Map<String, Object> avgMap = MissionDataAggregator.jsonToMap(objectMapper, r.getDataAvg());
    return new MissionDataRecordDto(
        r.getId(),
        r.getMissionId(),
        r.getMissionCode(),
        r.getMissionType(),
        r.getPilotName(),
        r.getUavCode(),
        r.getOperatorName(),
        r.getStartTime(),
        r.getEndTime(),
        maxMap,
        minMap,
        avgMap);
  }
}

