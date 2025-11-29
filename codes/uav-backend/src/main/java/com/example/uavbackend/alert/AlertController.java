package com.example.uavbackend.alert;

import com.example.uavbackend.alert.dto.AlertRecordDto;
import com.example.uavbackend.alert.dto.AlertRuleCreateRequest;
import com.example.uavbackend.alert.dto.AlertRuleDto;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {
  private final AlertService alertService;

  @GetMapping("/rules")
  public List<AlertRuleDto> listRules() {
    return alertService.listRules();
  }

  @PostMapping("/rules")
  public ResponseEntity<AlertRuleDto> createRule(@Valid @RequestBody AlertRuleCreateRequest req) {
    return ResponseEntity.status(201).body(alertService.createRule(req));
  }

  @PutMapping("/rules/{id}")
  public ResponseEntity<AlertRuleDto> updateRule(
      @PathVariable("id") Long id, @Valid @RequestBody AlertRuleCreateRequest req) {
    return ResponseEntity.ok(alertService.updateRule(id, req));
  }

  @DeleteMapping("/rules/{id}")
  public ResponseEntity<Void> deleteRule(@PathVariable("id") Long id) {
    alertService.deleteRule(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/records")
  public List<AlertRecordDto> listRecords(@RequestParam(value = "ruleId", required = false) Long ruleId) {
    return alertService.listRecords(ruleId);
  }

  @PutMapping("/records/{id}/process")
  public ResponseEntity<Void> processRecord(@PathVariable("id") Long id) {
    alertService.markRecordProcessed(id);
    return ResponseEntity.ok().build();
  }
}
