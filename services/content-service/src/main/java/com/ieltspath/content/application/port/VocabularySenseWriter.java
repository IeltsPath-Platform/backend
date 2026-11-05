package com.ieltspath.content.application.port;

import com.ieltspath.content.domain.vo.CourseDraftStructure.NewWord;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Gives reviewed words a vocabulary sense in the library. Needs the signed-in author's token. */
public interface VocabularySenseWriter {

    /**
     * A lemma the library already has keeps its own first active sense and the word's text is ignored; any other is
     * stored as written. Nothing is stored when the call fails.
     *
     * @return the sense id of each word, by {@link NewWord#normalizedLemma()}
     */
    Map<String, UUID> resolveOrCreate(List<NewWord> words);
}
