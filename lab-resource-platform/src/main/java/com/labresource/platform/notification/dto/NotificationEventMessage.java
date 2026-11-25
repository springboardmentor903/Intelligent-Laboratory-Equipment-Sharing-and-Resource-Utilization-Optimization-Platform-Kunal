
package com.labresource.platform.notification.dto;

import java.io.Serializable;

public class NotificationEventMessage implements Serializable {

    private Long notificationId;
    private Long userId;

    public NotificationEventMessage() {
    }

    public NotificationEventMessage(Long notificationId, Long userId) {
        this.notificationId = notificationId;
        this.userId = userId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
