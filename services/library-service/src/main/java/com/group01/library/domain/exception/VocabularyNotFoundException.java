package com.group01.library.domain.exception;

import java.util.UUID;

public class VocabularyNotFoundException extends ContentDomainException {
    public VocabularyNotFoundException(UUID id) {
        super("Vocabulary item not found with id: " + id);
    }

    public VocabularyNotFoundException(String lemma) {
        super("Vocabulary item not found for lemma: " + lemma);
    }
}
