package com.group01.content.application.command;

public record CreateVocabularyItemCommand(
        String lemma,
        String ipa,
        String pronunciationAudioReference
) {}

