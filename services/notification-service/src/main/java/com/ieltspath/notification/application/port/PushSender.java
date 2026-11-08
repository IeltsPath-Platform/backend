package com.ieltspath.notification.application.port;

import com.ieltspath.notification.domain.vo.PushSendSnapshot;

public interface PushSender {
    boolean isReady();
    String send(PushSendSnapshot snapshot);
}
