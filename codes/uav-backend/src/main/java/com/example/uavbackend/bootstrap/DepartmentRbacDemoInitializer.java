package com.example.uavbackend.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.uavbackend.alert.AlertRule;
import com.example.uavbackend.alert.AlertRuleCondition;
import com.example.uavbackend.alert.AlertRuleConditionMapper;
import com.example.uavbackend.alert.AlertRuleMapper;
import com.example.uavbackend.auth.Department;
import com.example.uavbackend.auth.DepartmentMapper;
import com.example.uavbackend.auth.User;
import com.example.uavbackend.auth.UserMapper;
import com.example.uavbackend.auth.UserRole;
import com.example.uavbackend.analytics.MissionDataRecord;
import com.example.uavbackend.analytics.MissionDataRecordMapper;
import com.example.uavbackend.analytics.TaskExecution;
import com.example.uavbackend.analytics.TaskExecutionMapper;
import com.example.uavbackend.fleet.UavDevice;
import com.example.uavbackend.fleet.UavDeviceMapper;
import com.example.uavbackend.mission.Mission;
import com.example.uavbackend.mission.MissionMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(30)
@Slf4j
public class DepartmentRbacDemoInitializer implements ApplicationRunner {
  private final DepartmentMapper departmentMapper;
  private final UserMapper userMapper;
  private final UavDeviceMapper uavDeviceMapper;
  private final MissionMapper missionMapper;
  private final MissionDataRecordMapper missionDataRecordMapper;
  private final TaskExecutionMapper taskExecutionMapper;
  private final AlertRuleMapper alertRuleMapper;
  private final AlertRuleConditionMapper alertRuleConditionMapper;

  @Override
  public void run(ApplicationArguments args) {
    Map<String, Department> departmentByCode =
        departmentMapper.selectList(new LambdaQueryWrapper<>()).stream()
            .collect(Collectors.toMap(Department::getDeptCode, item -> item, (left, right) -> left));
    if (departmentByCode.isEmpty()) {
      return;
    }
    List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
    Map<String, User> userByName =
        users.stream().collect(Collectors.toMap(User::getName, item -> item, (left, right) -> left));
    Map<String, User> userByUsername =
        users.stream().collect(Collectors.toMap(User::getUsername, item -> item, (left, right) -> left));

    backfillDevices(userByName);
    ensureSharedDevice(
        "FOREST-UAV-02", "Matrice 350 RTK", userByUsername.get("forest.lead"), userByUsername.get("forest.exec2"));
    ensureSharedDevice(
        "AIR-UAV-02", "Mavic 3E", userByUsername.get("air.lead"), userByUsername.get("air.exec2"));
    ensureSharedDevice(
        "GRID-UAV-02", "M300 Grid", userByUsername.get("grid.lead"), userByUsername.get("grid.exec2"));

    backfillMissions(userByName);
    backfillMissionRecords(userByName);
    backfillTaskExecutions(userByName);

    ensureDepartmentAlertAssets(
        userByUsername.get("forest.lead"),
        "森林高温模板",
        "FOREST_TEMP_TEMPLATE",
        "森林火情",
        List.of(
            condition("SURFACE_TEMP", "GTE", 72d),
            condition("SMOKE_INDEX", "GTE", 68d)),
        "森林火情升级规则");
    ensureDepartmentAlertAssets(
        userByUsername.get("air.lead"),
        "空气污染模板",
        "AIR_POLLUTION_TEMPLATE",
        "空气质量",
        List.of(
            condition("PM25", "GTE", 80d),
            condition("CO2", "GTE", 900d)),
        "空气污染处置规则");
    ensureDepartmentAlertAssets(
        userByUsername.get("grid.lead"),
        "电网过热模板",
        "GRID_HEAT_TEMPLATE",
        "电网巡检",
        List.of(
            condition("LINE_TEMP", "GTE", 65d),
            condition("CORONA_INTENSITY", "GTE", 40d)),
        "电网线路过热规则");

    log.info("Ensured department RBAC demo data");
  }

  private void backfillDevices(Map<String, User> userByName) {
    List<UavDevice> devices = uavDeviceMapper.selectList(new LambdaQueryWrapper<>());
    for (UavDevice device : devices) {
      User owner = userByName.get(device.getPilotName());
      if (owner == null) {
        continue;
      }
      applyDepartmentFields(
          owner,
          deptId -> device.setDepartmentId(deptId),
          deptName -> device.setDepartmentName(deptName));
      if (device.getOwnerUsername() == null) {
        device.setOwnerUsername(owner.getUsername());
      }
      uavDeviceMapper.updateById(device);
    }
  }

  private void ensureSharedDevice(
      String uavCode, String model, User owner, User pilotUser) {
    if (owner == null || pilotUser == null) {
      return;
    }
    UavDevice device =
        uavDeviceMapper.selectOne(
            new LambdaQueryWrapper<UavDevice>().eq(UavDevice::getUavCode, uavCode).last("limit 1"));
    if (device == null) {
      device = new UavDevice();
      device.setUavCode(uavCode);
      device.setModel(model);
      device.setPilotName(pilotUser.getName());
      device.setOwnerUsername(owner.getUsername());
      device.setDepartmentId(owner.getDepartmentId());
      device.setDepartmentName(owner.getDepartmentName());
      uavDeviceMapper.insert(device);
      return;
    }
    device.setModel(model);
    device.setPilotName(pilotUser.getName());
    device.setOwnerUsername(owner.getUsername());
    device.setDepartmentId(owner.getDepartmentId());
    device.setDepartmentName(owner.getDepartmentName());
    uavDeviceMapper.updateById(device);
  }

  private void backfillMissions(Map<String, User> userByName) {
    List<Mission> missions = missionMapper.selectList(new LambdaQueryWrapper<>());
    for (Mission mission : missions) {
      User pilot = userByName.get(mission.getPilotName());
      if (pilot == null) {
        continue;
      }
      mission.setDepartmentId(pilot.getDepartmentId());
      mission.setDepartmentName(pilot.getDepartmentName());
      mission.setPilotUsername(pilot.getUsername());
      missionMapper.updateById(mission);
    }
  }

  private void backfillMissionRecords(Map<String, User> userByName) {
    List<MissionDataRecord> records = missionDataRecordMapper.selectList(new LambdaQueryWrapper<>());
    for (MissionDataRecord record : records) {
      User owner = userByName.get(record.getPilotName());
      if (owner == null) {
        owner = userByName.get(record.getOperatorName());
      }
      if (owner == null) {
        continue;
      }
      record.setDepartmentId(owner.getDepartmentId());
      record.setDepartmentName(owner.getDepartmentName());
      missionDataRecordMapper.updateById(record);
    }
  }

  private void backfillTaskExecutions(Map<String, User> userByName) {
    List<TaskExecution> executions = taskExecutionMapper.selectList(new LambdaQueryWrapper<>());
    for (TaskExecution execution : executions) {
      User owner = userByName.get(execution.getOwnerName());
      if (owner == null) {
        continue;
      }
      execution.setDepartmentId(owner.getDepartmentId());
      execution.setDepartmentName(owner.getDepartmentName());
      taskExecutionMapper.updateById(execution);
    }
  }

  private void ensureDepartmentAlertAssets(
      User owner,
      String templateName,
      String templateCode,
      String templateCategory,
      List<AlertRuleCondition> conditions,
      String ruleName) {
    if (owner == null || owner.getDepartmentId() == null) {
      return;
    }
    AlertRule template =
        ensureRule(
            owner,
            templateName,
            true,
            null,
            rule -> {
              rule.setTemplateCode(templateCode);
              rule.setTemplateCategory(templateCategory);
              rule.setAutoInterrupt(true);
              rule.setNotifyEnabled(true);
              rule.setNotifyChannels("SMS");
              rule.setNotifyTargets(owner.getName() + ",值班群");
              rule.setNotifyTemplate(templateName + "触发通知");
            },
            conditions);
    ensureRule(
        owner,
        ruleName,
        false,
        template,
        rule -> {
          rule.setAutoInterrupt(true);
          rule.setNotifyEnabled(true);
          rule.setNotifyChannels("SMS");
          rule.setNotifyTargets(owner.getName() + ",应急值班");
          rule.setNotifyTemplate(ruleName + "通知");
        },
        conditions);
  }

  private AlertRule ensureRule(
      User owner,
      String ruleName,
      boolean templateEnabled,
      AlertRule template,
      Consumer<AlertRule> extraSetter,
      List<AlertRuleCondition> conditions) {
    AlertRule rule =
        alertRuleMapper.selectOne(
            new LambdaQueryWrapper<AlertRule>()
                .eq(AlertRule::getName, ruleName)
                .eq(AlertRule::getDepartmentId, owner.getDepartmentId())
                .last("limit 1"));
    boolean isNew = false;
    if (rule == null) {
      isNew = true;
      rule = new AlertRule();
      rule.setCreatedAt(LocalDateTime.now());
    }
    rule.setName(ruleName);
    rule.setDescription(ruleName + "（部门共享资源）");
    rule.setLogicOperator("OR");
    rule.setTemplateEnabled(templateEnabled);
    rule.setTemplateId(template == null ? null : template.getId());
    rule.setTemplateCode(templateEnabled ? rule.getTemplateCode() : null);
    rule.setTemplateCategory(templateEnabled ? rule.getTemplateCategory() : null);
    rule.setDepartmentId(owner.getDepartmentId());
    rule.setDepartmentName(owner.getDepartmentName());
    rule.setCreatedBy(owner.getUsername());
    rule.setUpdatedAt(LocalDateTime.now());
    extraSetter.accept(rule);
    if (isNew) {
      alertRuleMapper.insert(rule);
    } else {
      alertRuleMapper.updateById(rule);
      alertRuleConditionMapper.delete(
          new LambdaQueryWrapper<AlertRuleCondition>().eq(AlertRuleCondition::getRuleId, rule.getId()));
    }
    for (AlertRuleCondition source : conditions) {
      AlertRuleCondition condition = new AlertRuleCondition();
      condition.setRuleId(rule.getId());
      condition.setMetricCode(source.getMetricCode());
      condition.setComparator(source.getComparator());
      condition.setThreshold(source.getThreshold());
      alertRuleConditionMapper.insert(condition);
    }
    return rule;
  }

  private AlertRuleCondition condition(String metricCode, String comparator, double threshold) {
    AlertRuleCondition condition = new AlertRuleCondition();
    condition.setMetricCode(metricCode);
    condition.setComparator(comparator);
    condition.setThreshold(threshold);
    return condition;
  }

  private void applyDepartmentFields(
      User owner, Consumer<Long> deptIdSetter, Consumer<String> deptNameSetter) {
    deptIdSetter.accept(owner.getDepartmentId());
    deptNameSetter.accept(owner.getDepartmentName());
  }
}
