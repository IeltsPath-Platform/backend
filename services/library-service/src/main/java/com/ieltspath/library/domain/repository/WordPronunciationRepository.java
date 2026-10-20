package com.ieltspath.library.domain.repository;

import java.util.Collection;
import java.util.Map;

public interface WordPronunciationRepository {
    /**
     * The pronunciation (IPA, with the slashes) of each given word, looked up in one query. {@code words} are
     * normalized as {@link com.ieltspath.library.domain.vo.SentenceWords} does; a word without one is absent.
     */
    Map<String, String> findIpaByWords(Collection<String> words);
}
