package com.ieltspath.library.application.result;

/** Playable URLs of the recorded pronunciation of a word, one per accent; either can be null. */
public record WordAudioResult(String audioUk, String audioUs) {
}
