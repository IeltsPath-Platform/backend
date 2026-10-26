package com.ieltspath.library.application.usecase;

import com.ieltspath.library.application.port.VocabularyCatalogStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VocabularyCatalogUseCase {
    private final VocabularyCatalogStore store;

    @Transactional(readOnly = true) public Map<String,Object> detail(UUID id) { return store.detail(id); }
    @Transactional(readOnly = true) public Map<String,Object> browse(String q,String lang,String cefr,UUID topicId,int page,int size) { return store.browse(q,lang,cefr,topicId,page,size); }
    @Transactional(readOnly = true) public List<Map<String,Object>> suggestions(String q) { return store.suggestions(q); }
    @Transactional(readOnly = true) public List<Map<String,Object>> topics() { return store.topics(); }
    @Transactional(readOnly = true) public Map<String,Object> sets(String kind,int page,int size) { return store.sets(kind,page,size); }
    @Transactional(readOnly = true) public Map<String,Object> set(UUID id) { return store.set(id); }
    @Transactional(readOnly = true) public Map<String,Object> setWords(UUID id,UUID topicId,int page,int size) { return store.setWords(id,topicId,page,size); }
    @Transactional(readOnly = true) public Map<String,Object> mindmap(UUID topicId,UUID itemId) { return store.mindmap(topicId,itemId); }
    @Transactional public Map<String,Object> updateItem(UUID id,Map<String,Object> values) { return store.updateItem(id,values); }
    @Transactional public Map<String,Object> updateSense(UUID id,Map<String,Object> values) { return store.updateSense(id,values); }
    @Transactional public List<Map<String,Object>> replaceChildren(String table,UUID id,List<Map<String,Object>> values) { return store.replaceChildren(table,id,values); }
    @Transactional(readOnly = true) public List<Map<String,Object>> topicsAdmin() { return store.topicsAdmin(); }
    @Transactional public Map<String,Object> saveTopic(UUID id,Map<String,Object> values) { return store.saveTopic(id,values); }
    @Transactional public void deleteTopic(UUID id) { store.deleteTopic(id); }
    @Transactional(readOnly = true) public List<Map<String,Object>> setsAdmin() { return store.setsAdmin(); }
    @Transactional public Map<String,Object> saveSet(UUID id,Map<String,Object> values) { return store.saveSet(id,values); }
    @Transactional public void deleteSet(UUID id) { store.deleteSet(id); }
    @Transactional public void replaceSetItems(UUID id,List<UUID> ids) { store.replaceSetItems(id,ids); }
}
