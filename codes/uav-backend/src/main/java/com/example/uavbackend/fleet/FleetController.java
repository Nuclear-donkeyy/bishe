package com.example.uavbackend.fleet;

import com.example.uavbackend.fleet.dto.FleetSummaryDto;
import com.example.uavbackend.fleet.dto.UavDeviceDto;
import com.example.uavbackend.fleet.dto.UavRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class FleetController {
  private final FleetService fleetService;

  @GetMapping("/summary")
  public FleetSummaryDto summary() {
    return fleetService.summary();
  }

  @GetMapping
  public Page<UavDeviceDto> list(
      @RequestParam(name = "status", required = false) List<UavStatus> statuses,
      @RequestParam(name = "page", defaultValue = "1") int page,
      @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
    return fleetService.list(statuses, page, pageSize);
  }

  @GetMapping("/available")
  public List<UavDeviceDto> available(
      @RequestParam(name = "excludeMissionIds", required = false) List<String> excludeMissionIds) {
    return fleetService.available(excludeMissionIds);
  }

  @PostMapping
  public ResponseEntity<UavDeviceDto> register(@Valid @RequestBody UavRequest request) {
    return ResponseEntity.status(201).body(fleetService.register(request));
  }
}
