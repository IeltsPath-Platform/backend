package com.ieltspath.library.infrastructure.adapter;

public final class VocabularyAudioSql {
    public static final String PREFERRED_REFERENCE = "COALESCE(NULLIF(trim(v.audio_us_reference), ''), "
            + "NULLIF(trim(v.audio_uk_reference), ''), NULLIF(trim(v.pronunciation_audio_reference), ''))";

    private VocabularyAudioSql() {
    }
}
