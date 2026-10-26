package com.ieltspath.library.application.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface VocabularyCatalogStore {
    Map<String, Object> detail(UUID itemId);
    Map<String, Object> browse(String q, String lang, String cefr, UUID topicId, int page, int size);
    List<Map<String, Object>> suggestions(String q);
    List<Map<String, Object>> topics();
    Map<String, Object> sets(String kind, int page, int size);
    Map<String, Object> set(UUID id);
    Map<String, Object> setWords(UUID id, UUID topicId, int page, int size);
    Map<String, Object> mindmap(UUID topicId, UUID itemId);
    Map<String, Object> updateItem(UUID id, Map<String, Object> values);
    Map<String, Object> updateSense(UUID id, Map<String, Object> values);
    List<Map<String, Object>> replaceChildren(String table, UUID senseId, List<Map<String, Object>> values);
    List<Map<String, Object>> topicsAdmin();
    Map<String, Object> saveTopic(UUID id, Map<String, Object> values);
    void deleteTopic(UUID id);
    List<Map<String, Object>> setsAdmin();
    Map<String, Object> saveSet(UUID id, Map<String, Object> values);
    void deleteSet(UUID id);
    void replaceSetItems(UUID id, List<UUID> senseIds);
}
