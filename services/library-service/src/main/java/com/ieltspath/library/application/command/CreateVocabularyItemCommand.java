package com.ieltspath.library.application.command;

public record CreateVocabularyItemCommand(
        String lemma,
        String ipa,
        String pronunciationAudioReference
) {}
