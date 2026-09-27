package com.group01.content.domain.exception;

import java.util.UUID;

/**
 * No readable passage for the section. The message never says why, so a learner cannot probe which sections belong to
 * a mock test, a placement test or an unpublished package.
 */
public class ReadingPassageNotFoundException extends ContentDomainException {
    public ReadingPassageNotFoundException(UUID sectionId) {
        super("Reading passage not found");
    }
}
