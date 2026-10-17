package com.ieltspath.library.domain.repository;
import java.util.List;
import java.util.UUID;
public interface WordlyVocabularyRepository {
    List<Word> random(int count);
    List<Word> page(String after,int limit);
    record Word(UUID sourceWordId, String answer, String meaning) {}
}
