package com.example.uavbackend.alert;

public interface AlertNotificationSender {
  NotificationDispatchResult send(AlertNotificationRequest request);
}
