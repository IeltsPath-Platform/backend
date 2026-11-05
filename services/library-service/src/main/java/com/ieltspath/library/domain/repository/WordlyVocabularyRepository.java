package com.ieltspath.library.domain.repository;
import java.util.List;
import java.util.UUID;
public interface WordlyVocabularyRepository {
    List<Word> random(int count);
    List<Word> page(String after,int limit);
    /** Null level or topic means any. A word with several meanings is returned with a meaning that matches. */
    List<Word> page(String after,int limit,String cefrLevel,UUID topicId);
    List<Topic> topics();
    /** Usable words per level and topic. A null level or topic on a row means the total across all of them. */
    List<Availability> availability();
    record Word(UUID sourceWordId, String answer, String meaning) {}
    record Topic(UUID id, String slug, String name, String nameVi) {}
    record Availability(String cefrLevel, UUID topicId, int words) {}
}
