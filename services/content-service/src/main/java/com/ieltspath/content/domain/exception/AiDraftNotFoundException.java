package com.ieltspath.content.domain.exception;

import java.util.UUID;

/** Also used when the draft belongs to another author, so one author cannot tell whose drafts exist. */
public class AiDraftNotFoundException extends ContentDomainException {
    public AiDraftNotFoundException(UUID id) {
        super("AI draft not found: " + id);
    }
}
