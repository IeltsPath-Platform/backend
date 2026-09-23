package com.group01.learningsupport.domain.aggregate;

import com.group01.learningsupport.domain.exception.InvalidDataException;

import com.group01.learningsupport.domain.DomainChecks;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class FlashcardDeck {
    private final UUID id;
    private final UUID userId;
    private String name;
    private String description;
    private LibraryStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public static FlashcardDeck create(UUID userId, String name, String description) {
        return new FlashcardDeck(
                UUID.randomUUID(),
                DomainChecks.userId(userId),
                DomainChecks.required(name, 150, "name"),
                DomainChecks.optional(description, DomainChecks.DESCRIPTION_MAX, "description"),
                LibraryStatus.ACTIVE,
                null,
                null
        );
    }

    public boolean update(String name, String description, LibraryStatus status) {
        if (status == null) {
            throw new InvalidDataException("status không hợp lệ");
        }
        this.name = DomainChecks.required(name, 150, "name");
        this.description = DomainChecks.optional(description, DomainChecks.DESCRIPTION_MAX, "description");
        boolean dropLinks = this.status == LibraryStatus.ACTIVE && status != LibraryStatus.ACTIVE;
        this.status = status;
        return dropLinks;
    }
}
