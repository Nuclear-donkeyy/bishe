package com.example.uavbackend.alert;

import com.example.uavbackend.mission.Mission;
import com.example.uavbackend.mission.MissionService;
import com.example.uavbackend.mission.MissionStatus;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertLinkageService {
  private final MissionService missionService;
  private final AlertRecordMapper recordMapper;
  private final AlertNotificationSender notificationSender;

  public void execute(AlertRule rule, AlertRecord record, Mission mission, AlertRuleCondition matchedCondition) {
    boolean interruptConfigured = Boolean.TRUE.equals(rule.getAutoInterrupt());
    boolean notifyConfigured = Boolean.TRUE.equals(rule.getNotifyEnabled());
    boolean interruptSuccess = false;
    boolean notifySuccess = false;
    boolean anyAction = interruptConfigured || notifyConfigured;
    String notificationStatus = notifyConfigured ? "PENDING" : "SKIPPED";
    List<String> summaries = new ArrayList<>();

    if (interruptConfigured) {
      if (mission == null) {
        summaries.add("自动中断跳过：任务不存在");
      } else if (!isInterruptible(mission)) {
        summaries.add("自动中断跳过：任务状态为 " + mission.getStatus());
      } else {
        try {
          missionService.interrupt(mission.getMissionCode());
          interruptSuccess = true;
          summaries.add("任务已自动中断");
        } catch (Exception ex) {
          log.warn("Auto interrupt failed for mission {}", mission.getMissionCode(), ex);
          summaries.add("自动中断失败：" + safeMessage(ex));
        }
      }
    }

    if (notifyConfigured) {
      try {
        NotificationDispatchResult result =
            notificationSender.send(new AlertNotificationRequest(rule, record, mission, matchedCondition, record.getUavCode()));
        notifySuccess = result.success();
        notificationStatus = result.status();
        summaries.add(result.summary());
      } catch (Exception ex) {
        log.warn("Alert notification failed for record {}", record.getId(), ex);
        notificationStatus = "FAILED";
        summaries.add("通知发送失败：" + safeMessage(ex));
      }
    }

    if (!anyAction) {
      record.setLinkageStatus("SKIPPED");
      record.setNotificationStatus("SKIPPED");
      record.setLinkageSummary("未配置联动动作");
      recordMapper.updateById(record);
      return;
    }

    record.setNotificationStatus(notificationStatus);
    record.setLinkageStatus(resolveLinkageStatus(interruptConfigured, interruptSuccess, notifyConfigured, notifySuccess));
    record.setLinkageSummary(String.join("；", summaries));
    recordMapper.updateById(record);
  }

  private boolean isInterruptible(Mission mission) {
    if (mission.getStatus() == null) {
      return false;
    }
    return mission.getStatus().equals(MissionStatus.QUEUE.name())
        || mission.getStatus().equals(MissionStatus.RUNNING.name())
        || mission.getStatus().equals(MissionStatus.PREEMPTED.name());
  }

  private String resolveLinkageStatus(
      boolean interruptConfigured, boolean interruptSuccess, boolean notifyConfigured, boolean notifySuccess) {
    boolean interruptOk = !interruptConfigured || interruptSuccess;
    boolean notifyOk = !notifyConfigured || notifySuccess;
    if (interruptOk && notifyOk) {
      return "SUCCESS";
    }
    if (interruptSuccess || notifySuccess) {
      return "PARTIAL";
    }
    return "FAILED";
  }

  private String safeMessage(Exception ex) {
    return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
  }
}
