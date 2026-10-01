package com.group01.learningsupport.domain.aggregate;

import com.group01.learningsupport.domain.exception.InvalidDataException;

import com.group01.learningsupport.domain.DomainChecks;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.domain.vo.NoteSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Note {
    private final UUID id;
    private final UUID userId;
    private String title;
    private String body;
    private LibraryStatus status;
    private final NoteSourceType sourceType;
    private final UUID sourceReferenceId;
    private final Instant createdAt;
    private Instant updatedAt;

    public static Note create(UUID userId, String title, String body) {
        return create(userId, title, body, null, null);
    }

    public static Note create(UUID userId, String title, String body,
                              NoteSourceType sourceType, UUID sourceReferenceId) {
        if ((sourceType == null) != (sourceReferenceId == null)) {
            throw new InvalidDataException("Note source must include both type and reference ID");
        }
        return new Note(
                UUID.randomUUID(),
                DomainChecks.userId(userId),
                DomainChecks.required(title, 255, "title"),
                DomainChecks.required(body, DomainChecks.BODY_MAX, "body"),
                LibraryStatus.ACTIVE,
                sourceType,
                sourceReferenceId,
                null,
                null
        );
    }

    public boolean update(String title, String body, LibraryStatus status) {
        if (status == null) {
            throw new InvalidDataException("status không hợp lệ");
        }
        this.title = DomainChecks.required(title, 255, "title");
        this.body = DomainChecks.required(body, DomainChecks.BODY_MAX, "body");
        boolean dropLinks = this.status == LibraryStatus.ACTIVE && status != LibraryStatus.ACTIVE;
        this.status = status;
        return dropLinks;
    }
}
