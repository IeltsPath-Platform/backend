package com.ieltspath.library.application.result;

import java.util.UUID;

public record FlashcardVocabularyMetadata(UUID senseId, String ipa, String pronunciationAudioReference,
                                          String exampleSentence) {
}
