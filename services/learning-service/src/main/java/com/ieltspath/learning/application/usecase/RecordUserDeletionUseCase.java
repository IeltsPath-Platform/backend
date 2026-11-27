package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.domain.repository.DeletedUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RecordUserDeletionUseCase {
    private final DeletedUserRepository deletedUsers;

    public RecordUserDeletionUseCase(DeletedUserRepository deletedUsers) {
        this.deletedUsers = deletedUsers;
    }

    @Transactional
    public void execute(UUID eventId, UUID userId, Instant occurredAt) {
        deletedUsers.recordDeletion(eventId, userId, occurredAt);
    }
}
