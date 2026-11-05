package com.ieltspath.library.application.result;

import java.util.UUID;

/** {@code lemma} is the normalized lemma; {@code created} is false when the library already had a usable sense. */
public record ResolvedVocabularyResult(String lemma, UUID senseId, boolean created) {
}
