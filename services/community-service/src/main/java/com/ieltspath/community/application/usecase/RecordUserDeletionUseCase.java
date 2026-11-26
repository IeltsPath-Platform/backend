package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.port.DeletedUserStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RecordUserDeletionUseCase {
    private final DeletedUserStore deletedUsers;

    public RecordUserDeletionUseCase(DeletedUserStore deletedUsers) {
        this.deletedUsers = deletedUsers;
    }

    @Transactional
    public void execute(UUID eventId, UUID userId, Instant occurredAt) {
        deletedUsers.recordDeletion(eventId, userId, occurredAt);
    }
}
