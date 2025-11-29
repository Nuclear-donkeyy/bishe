package com.example.uavbackend.mission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.mission.dto.MissionCreateRequest;
import com.example.uavbackend.mission.dto.MissionDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MissionController {
  private final MissionService missionService;
  private final MissionTypeMapper missionTypeMapper;

  @GetMapping("/mission-types")
  public List<MissionTypeDefinition> missionTypes() {
    return missionTypeMapper.selectList(new LambdaQueryWrapper<>());
  }

  @GetMapping("/missions")
  public List<MissionDto> missions(
      @RequestParam(value = "status", required = false) List<String> statuses) {
    return missionService.list(statuses);
  }

  @GetMapping("/missions/{missionCode}")
  public ResponseEntity<MissionDto> detail(@PathVariable("missionCode") String missionCode) {
    return missionService
        .findByCode(missionCode)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/missions")
  public ResponseEntity<MissionDto> create(@Valid @RequestBody MissionCreateRequest request) {
    MissionDto dto = missionService.create(request);
    return ResponseEntity.status(201).body(dto);
  }

  @PatchMapping("/missions/{missionCode}")
  public ResponseEntity<MissionDto> updateProgress(
      @PathVariable("missionCode") String missionCode, @RequestBody ProgressPayload request) {
    if (request == null || request.progress() == null) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(missionService.updateProgress(missionCode, request.progress()));
  }

  @PostMapping("/missions/{missionCode}/interrupt")
  public ResponseEntity<Void> interrupt(@PathVariable("missionCode") String missionCode) {
    missionService.interrupt(missionCode);
    return ResponseEntity.ok().build();
  }

  public record ProgressPayload(Integer progress) {}
}
