package com.eventflow.service;

import com.eventflow.dto.NotificationResponse;
import com.eventflow.entity.User;
import com.eventflow.entity.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getUserNotifications(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void sendNotification(User user, String title, String message, NotificationType type, String referenceId);
}
