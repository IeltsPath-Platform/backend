package com.ieltspath.notification.domain.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super("Notification not found");
    }
}
