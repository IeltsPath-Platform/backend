package com.group01.library.application.command;

public record CreateVocabularyItemCommand(
        String lemma,
        String ipa,
        String pronunciationAudioReference
) {}
