package com.ieltspath.access.application.usecase;

import com.ieltspath.access.domain.repository.UserEmailVerificationRepository;
import com.ieltspath.events.user.UserEmailVerificationStateV1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordUserEmailVerificationUseCase {
    private final UserEmailVerificationRepository repository;

    @Transactional
    public void execute(UserEmailVerificationStateV1 event) {
        if (event == null || event.eventId() == null || event.userId() == null || event.occurredAt() == null
                || !UserEmailVerificationStateV1.EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("Email verification state event is invalid");
        }
        repository.recordState(event.userId(), event.verified(), event.email(), event.displayName(), event.occurredAt());
    }
}
