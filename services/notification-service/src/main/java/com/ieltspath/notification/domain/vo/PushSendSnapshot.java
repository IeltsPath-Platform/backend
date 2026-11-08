package com.ieltspath.notification.domain.vo;

public record PushSendSnapshot(PushDeliveryJob job, Notification notification, String token, String tokenHash) {
    @Override
    public String toString() {
        return "PushSendSnapshot[deliveryId=" + job.id() + ", notificationId=" + job.notificationId()
                + ", token=[REDACTED]]";
    }
}
