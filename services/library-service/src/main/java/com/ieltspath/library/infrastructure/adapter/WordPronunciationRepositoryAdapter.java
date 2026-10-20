package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.domain.repository.WordPronunciationRepository;
import com.ieltspath.library.infrastructure.persistence.repository.VocabularyItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class WordPronunciationRepositoryAdapter implements WordPronunciationRepository {
    private final VocabularyItemJpaRepository vocabularyItems;

    @Override
    public Map<String, String> findIpaByWords(Collection<String> words) {
        Map<String, String> ipaByWord = new HashMap<>();
        if (!words.isEmpty()) {
            vocabularyItems.findIpaByNormalizedLemmaIn(words)
                    .forEach(row -> ipaByWord.putIfAbsent(row.getWord(), row.getIpa()));
        }
        return ipaByWord;
    }
}
