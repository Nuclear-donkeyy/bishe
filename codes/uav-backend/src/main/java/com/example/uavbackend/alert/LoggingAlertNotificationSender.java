package com.example.uavbackend.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Primary
@Slf4j
public class LoggingAlertNotificationSender implements AlertNotificationSender {
  @Override
  public NotificationDispatchResult send(AlertNotificationRequest request) {
    String targets =
        StringUtils.hasText(request.rule().getNotifyTargets())
            ? request.rule().getNotifyTargets()
            : "未配置接收人";
    String template =
        StringUtils.hasText(request.rule().getNotifyTemplate())
            ? request.rule().getNotifyTemplate()
            : "报警通知：任务=%s，无人机=%s，指标=%s，值=%s"
                .formatted(
                    request.record().getMissionCode(),
                    request.uavCode(),
                    request.record().getMetricCode(),
                    request.record().getMetricValue());
    log.info(
        "Alert notification placeholder rule={} mission={} uav={} channels={} targets={} content={}",
        request.rule().getId(),
        request.record().getMissionCode(),
        request.uavCode(),
        request.rule().getNotifyChannels(),
        targets,
        template);
    return new NotificationDispatchResult(true, "PLACEHOLDER", "通知占位发送已记录，待接入短信 SDK");
  }
}
