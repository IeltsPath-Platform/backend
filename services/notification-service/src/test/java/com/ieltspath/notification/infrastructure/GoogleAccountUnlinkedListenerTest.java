package com.ieltspath.notification.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.events.messaging.MessageRetry;
import com.ieltspath.events.user.GoogleAccountUnlinkedV1;
import com.ieltspath.notification.application.SendAccountNoticeMailUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GoogleAccountUnlinkedListenerTest {
    @Test
    void parsesTheEventAndSendsTheAccountNotice() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        SendAccountNoticeMailUseCase send = mock(SendAccountNoticeMailUseCase.class);
        MessageRetry retry = mock(MessageRetry.class);
        var listener = new GoogleAccountUnlinkedListener(mapper, send, retry);
        var event = new GoogleAccountUnlinkedV1(UUID.randomUUID(), GoogleAccountUnlinkedV1.EVENT_TYPE,
                Instant.now(), UUID.randomUUID(), "learner@example.test", "Learner");
        var message = MessageBuilder.withBody(mapper.writeValueAsBytes(event)).build();

        listener.onGoogleAccountUnlinked(message);

        verify(send).googleAccountUnlinked(event);
    }
}
