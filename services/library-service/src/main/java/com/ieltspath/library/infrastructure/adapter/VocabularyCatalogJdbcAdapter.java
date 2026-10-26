package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.port.VocabularyCatalogStore;
import com.ieltspath.library.domain.exception.ResourceNotFoundException;
import com.ieltspath.library.domain.vo.CefrLevel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class VocabularyCatalogJdbcAdapter implements VocabularyCatalogStore {
    private static final List<String> CHILD_TABLES = List.of("collocations", "relations", "common-mistakes", "examples");
    private static final Map<String,String> TABLES = Map.of(
            "collocations", "vocabulary_collocations", "relations", "vocabulary_relations",
            "common-mistakes", "vocabulary_common_mistakes", "examples", "vocabulary_examples");
    private final NamedParameterJdbcTemplate jdbc;
    private final FlashcardImageStorage storage;

    public VocabularyCatalogJdbcAdapter(NamedParameterJdbcTemplate jdbc, FlashcardImageStorage storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @Override public Map<String,Object> detail(UUID itemId) {
        List<Map<String,Object>> rows = loadItems(List.of(itemId));
        if (rows.isEmpty() || !"ACTIVE".equals(rows.getFirst().get("status"))) throw new ResourceNotFoundException();
        Map<String,Object> item = rows.getFirst();
        @SuppressWarnings("unchecked") List<Map<String,Object>> senses = (List<Map<String,Object>>) item.get("senses");
        senses.removeIf(s -> !"ACTIVE".equals(s.get("status")));
        return item;
    }

    @Override public Map<String,Object> browse(String q, String lang, String cefr, UUID topicId, int page, int size) {
        long offset = (long) page * size;
        String searchColumn = "VI".equalsIgnoreCase(lang) ? "s.vietnamese_meaning" : "i.lemma";
        String senseFilters="AND (:query='' OR lower(" + searchColumn + ") LIKE :pattern)";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("query", q.toLowerCase(Locale.ROOT))
                .addValue("pattern", "%" + q.toLowerCase(Locale.ROOT) + "%").addValue("limit", size).addValue("offset", offset);
        if(cefr!=null) { senseFilters += " AND s.cefr_level=:cefr"; p.addValue("cefr",cefr); }
        if(topicId!=null) { senseFilters += " AND s.topic_id=:topicId"; p.addValue("topicId",topicId); }
        String where = "i.status='ACTIVE' AND EXISTS (SELECT 1 FROM vocabulary_senses s WHERE s.vocabulary_item_id=i.id AND s.status='ACTIVE' " + senseFilters + ")";
        Long total = jdbc.queryForObject("SELECT count(*) FROM vocabulary_items i WHERE " + where, p, Long.class);
        List<UUID> ids = jdbc.query("SELECT i.id FROM vocabulary_items i WHERE " + where
                        + " ORDER BY i.normalized_lemma, i.id LIMIT :limit OFFSET :offset", p,
                (rs,n) -> rs.getObject(1, UUID.class));
        return page(loadItems(ids), page, size, total == null ? 0L : total);
    }

    @Override public List<Map<String,Object>> suggestions(String q) {
        if (q == null || q.isBlank()) return List.of();
        return jdbc.queryForList("SELECT id, lemma FROM vocabulary_items WHERE status='ACTIVE' AND lower(lemma) LIKE :q ORDER BY lower(lemma),id LIMIT 8",
                Map.of("q", q.toLowerCase(Locale.ROOT) + "%"));
    }

    @Override public List<Map<String,Object>> topics() {
        List<Map<String,Object>> result=jdbc.queryForList("SELECT t.id,t.slug,t.name,t.name_vi AS \"nameVi\",t.cover_key AS \"coverKey\",t.sort_order AS \"sortOrder\",count(DISTINCT CASE WHEN i.status='ACTIVE' THEN s.id END) AS \"wordCount\" "
                + "FROM vocabulary_topics t LEFT JOIN vocabulary_senses s ON s.topic_id=t.id AND s.status='ACTIVE' LEFT JOIN vocabulary_items i ON i.id=s.vocabulary_item_id "
                + "WHERE t.status='ACTIVE' GROUP BY t.id ORDER BY t.sort_order,t.name", Map.of());
        result.forEach(topic->topic.put("coverUrl",storage.publicUrl((String)topic.get("coverKey"))));
        return result;
    }

    @Override public Map<String,Object> sets(String kind, int page, int size) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit",size).addValue("offset",(long)page*size);
        String filter = "";
        if(kind!=null) { filter=" AND s.kind=:kind"; p.addValue("kind",kind); }
        long total = Optional.ofNullable(jdbc.queryForObject("SELECT count(*) FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id WHERE s.status='ACTIVE' AND (s.topic_id IS NULL OR t.status='ACTIVE')"+filter,p,Long.class)).orElse(0L);
        List<Map<String,Object>> items = jdbc.queryForList("SELECT s.id,s.title,s.description,s.kind,s.cefr_level AS \"cefrLevel\",s.topic_id AS \"topicId\",t.name AS \"topicName\",s.cover_key AS \"coverKey\",s.sort_order AS \"sortOrder\",count(DISTINCT st.topic_id) AS \"topicCount\",count(DISTINCT CASE WHEN st.status='ACTIVE' THEN si.sense_id END) AS \"wordCount\" "
                + "FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id LEFT JOIN vocabulary_set_items si ON si.set_id=s.id LEFT JOIN vocabulary_senses st ON st.id=si.sense_id AND st.topic_id IS NOT NULL AND st.status='ACTIVE' "
                + "WHERE s.status='ACTIVE' AND (s.topic_id IS NULL OR t.status='ACTIVE')"+filter+" GROUP BY s.id,t.name ORDER BY s.sort_order,s.title LIMIT :limit OFFSET :offset",p);
        items.forEach(it -> { it.put("coverUrl", storage.publicUrl((String)it.get("coverKey"))); it.put("bandRange", band(it.get("cefrLevel"))); });
        return page(items,page,size,total);
    }

    @Override public Map<String,Object> set(UUID id) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT s.id,s.title,s.description,s.kind,s.cefr_level AS \"cefrLevel\",s.topic_id AS \"topicId\",s.cover_key AS \"coverKey\",s.sort_order AS \"sortOrder\",s.status,t.name AS \"topicName\",t.name_vi AS \"topicNameVi\" FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id WHERE s.id=:id AND s.status='ACTIVE' AND (s.topic_id IS NULL OR t.status='ACTIVE')",Map.of("id",id));
        if(rows.isEmpty()) throw new ResourceNotFoundException();
        Map<String,Object> result=rows.getFirst();
        result.put("coverUrl",storage.publicUrl((String)result.get("coverKey")));
        result.put("bandRange",band(result.get("cefrLevel")));
        result.put("wordCount",jdbc.queryForObject("SELECT count(*) FROM vocabulary_set_items si JOIN vocabulary_senses s ON s.id=si.sense_id AND s.status='ACTIVE' JOIN vocabulary_items i ON i.id=s.vocabulary_item_id AND i.status='ACTIVE' WHERE si.set_id=:id",Map.of("id",id),Long.class));
        List<Map<String,Object>> topics=jdbc.queryForList("SELECT t.id,t.slug,t.name,t.name_vi AS \"nameVi\",t.cover_key AS \"coverKey\",count(si.sense_id) AS \"wordCount\" FROM vocabulary_set_items si JOIN vocabulary_senses s ON s.id=si.sense_id AND s.status='ACTIVE' JOIN vocabulary_items i ON i.id=s.vocabulary_item_id AND i.status='ACTIVE' JOIN vocabulary_topics t ON t.id=s.topic_id WHERE si.set_id=:id GROUP BY t.id ORDER BY t.sort_order,t.name",Map.of("id",id));
        topics.forEach(t->t.put("coverUrl",storage.publicUrl((String)t.get("coverKey")))); result.put("topics",topics);
        return result;
    }

    @Override public Map<String,Object> setWords(UUID id, UUID topicId, int page, int size) {
        if(Optional.ofNullable(jdbc.queryForObject("SELECT count(*) FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id WHERE s.id=:id AND s.status='ACTIVE' AND (s.topic_id IS NULL OR t.status='ACTIVE')",Map.of("id",id),Long.class)).orElse(0L)==0L)throw new ResourceNotFoundException();
        MapSqlParameterSource p=new MapSqlParameterSource().addValue("id",id).addValue("limit",size).addValue("offset",(long)page*size);
        String topicFilter=""; if(topicId!=null) { topicFilter=" AND s.topic_id=:topicId"; p.addValue("topicId",topicId); }
        Long total=jdbc.queryForObject("SELECT count(*) FROM vocabulary_set_items si JOIN vocabulary_senses s ON s.id=si.sense_id AND s.status='ACTIVE' JOIN vocabulary_items i ON i.id=s.vocabulary_item_id AND i.status='ACTIVE' WHERE si.set_id=:id"+topicFilter,p,Long.class);
        List<UUID> senseIds=jdbc.query("SELECT si.sense_id FROM vocabulary_set_items si JOIN vocabulary_senses s ON s.id=si.sense_id AND s.status='ACTIVE' JOIN vocabulary_items i ON i.id=s.vocabulary_item_id AND i.status='ACTIVE' WHERE si.set_id=:id"+topicFilter+" ORDER BY si.sort_order,si.sense_id LIMIT :limit OFFSET :offset",p,(rs,n)->rs.getObject(1,UUID.class));
        Set<UUID> selected=new HashSet<>(senseIds);
        List<UUID> itemIds=jdbc.query("SELECT i.id FROM vocabulary_set_items si JOIN vocabulary_senses s ON s.id=si.sense_id JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE si.sense_id IN (:ids) GROUP BY i.id ORDER BY min(si.sort_order),i.id",new MapSqlParameterSource("ids",senseIds.isEmpty()?List.of(UUID.randomUUID()):senseIds),(rs,n)->rs.getObject(1,UUID.class));
        List<Map<String,Object>> items=loadItems(itemIds);
        items.forEach(item->{ @SuppressWarnings("unchecked") List<Map<String,Object>> senses=(List<Map<String,Object>>)item.get("senses"); senses.removeIf(s->!selected.contains(s.get("id"))); });
        return page(items,page,size,total==null?0:total);
    }

    @Override public Map<String,Object> mindmap(UUID topicId, UUID itemId) {
        if ((topicId == null) == (itemId == null)) throw new IllegalArgumentException("provide exactly one of topicId or itemId");
        List<Map<String,Object>> roots;
        if(topicId!=null) roots=jdbc.queryForList("SELECT s.id AS \"senseId\",i.id AS \"itemId\",i.lemma,s.english_definition AS \"label\" FROM vocabulary_senses s JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE s.topic_id=:id AND s.status='ACTIVE' AND i.status='ACTIVE' ORDER BY i.normalized_lemma,s.sort_order LIMIT 79",Map.of("id",topicId));
        else roots=jdbc.queryForList("SELECT s.id AS \"senseId\",i.id AS \"itemId\",i.lemma,s.english_definition AS \"label\" FROM vocabulary_senses s JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE i.id=:id AND s.status='ACTIVE' AND i.status='ACTIVE' ORDER BY s.sort_order LIMIT 80",Map.of("id",itemId));
        if(roots.isEmpty()) throw new ResourceNotFoundException();
        List<Map<String,Object>> nodes=new ArrayList<>(), edges=new ArrayList<>();
        if(topicId!=null) {
            Map<String,Object> topic=jdbc.queryForMap("SELECT id,name FROM vocabulary_topics WHERE id=:id AND status='ACTIVE'",Map.of("id",topicId));
            nodes.add(Map.of("id",topicId,"type","TOPIC","label",topic.get("name")));
        }
        for(Map<String,Object> root:roots) { nodes.add(Map.of("id",root.get("senseId"),"type","WORD","label",root.get("lemma"),"itemId",root.get("itemId"))); }
        List<UUID> senseIds=roots.stream().map(r->(UUID)r.get("senseId")).toList();
        List<Map<String,Object>> collocations=children("vocabulary_collocations",senseIds,"phrase");
        List<Map<String,Object>> relations=children("vocabulary_relations",senseIds,"related_lemma");
        Map<UUID,Map<String,Object>> bySense=roots.stream().collect(Collectors.toMap(r->(UUID)r.get("senseId"),r->r));
        if(topicId!=null) roots.forEach(root->edges.add(Map.of("from",topicId,"to",root.get("senseId"),"kind","TOPIC_WORD")));
        for(Map<String,Object> child:collocations) addMindmapNode(nodes,edges,child,bySense,"COLLOCATION","phrase");
        for(Map<String,Object> child:relations) if("SYNONYM".equals(child.get("kind"))) addMindmapNode(nodes,edges,child,bySense,"SYNONYM","relatedLemma");
        return Map.of("nodes",nodes,"edges",edges);
    }

    private void addMindmapNode(List<Map<String,Object>> nodes,List<Map<String,Object>> edges,Map<String,Object> child,Map<UUID,Map<String,Object>> roots,String type,String labelKey) {
        if(nodes.size()>=80)return; UUID sense=(UUID)child.get("senseId"); Map<String,Object> root=roots.get(sense); if(root==null)return;
        Object id=child.get("id"); nodes.add(Map.of("id",id,"type",type,"label",child.get(labelKey)));
        edges.add(Map.of("from",sense,"to",id,"kind",type));
    }

    private List<Map<String,Object>> loadItems(List<UUID> ids) {
        if(ids.isEmpty()) return new ArrayList<>();
        MapSqlParameterSource p=new MapSqlParameterSource("ids",ids);
        List<Map<String,Object>> items=jdbc.queryForList("SELECT id,lemma,normalized_lemma AS \"normalizedLemma\",ipa,pronunciation_audio_reference AS \"pronunciationAudioReference\",ipa_uk AS \"ipaUk\",ipa_us AS \"ipaUs\",audio_uk_reference AS \"audioUkReference\",audio_us_reference AS \"audioUsReference\",frequency_rank AS \"frequencyRank\",academic_word_list AS \"academicWordList\",word_family AS \"wordFamily\",status,created_at AS \"createdAt\",updated_at AS \"updatedAt\" FROM vocabulary_items WHERE id IN (:ids)",p);
        List<Map<String,Object>> senses=jdbc.queryForList("SELECT s.id,s.vocabulary_item_id AS \"vocabularyItemId\",s.part_of_speech AS \"partOfSpeech\",s.english_definition AS \"englishDefinition\",s.vietnamese_meaning AS \"vietnameseMeaning\",s.example_sentence AS \"exampleSentence\",s.image_url AS \"imageUrl\",s.sort_order AS \"sortOrder\",s.status,s.created_at AS \"createdAt\",s.updated_at AS \"updatedAt\",s.cefr_level AS \"cefrLevel\",s.topic_id AS \"topicId\",s.register,s.grammar_note AS \"grammarNote\",s.usage_note AS \"usageNote\",s.example_translation AS \"exampleTranslation\" FROM vocabulary_senses s WHERE s.vocabulary_item_id IN (:ids) AND s.status='ACTIVE' ORDER BY s.vocabulary_item_id,s.sort_order,s.id",p);
        List<UUID> senseIds=senses.stream().map(s->(UUID)s.get("id")).toList();
        Map<UUID,Map<String,Object>> topicMap=new HashMap<>();
        List<Map<String,Object>> topics=jdbc.queryForList("SELECT id,name,name_vi AS \"nameVi\",slug,cover_key AS \"coverKey\" FROM vocabulary_topics WHERE id IN (SELECT topic_id FROM vocabulary_senses WHERE id IN (:ids))",new MapSqlParameterSource("ids",senseIds.isEmpty()?List.of(UUID.randomUUID()):senseIds));
        topics.forEach(t->t.put("coverUrl",storage.publicUrl((String)t.get("coverKey"))));
        topics.forEach(t->topicMap.put((UUID)t.get("id"),t));
        Map<UUID,List<Map<String,Object>>> collocations=group(children("vocabulary_collocations",senseIds,"phrase"));
        Map<UUID,List<Map<String,Object>>> relations=group(children("vocabulary_relations",senseIds,"related_lemma"));
        Map<UUID,List<Map<String,Object>>> mistakes=group(children("vocabulary_common_mistakes",senseIds,"wrong_usage"));
        Map<UUID,List<Map<String,Object>>> examples=group(children("vocabulary_examples",senseIds,"sentence"));
        for(Map<String,Object>s:senses) {
            UUID tid=(UUID)s.get("topicId"); s.put("topic",tid==null?null:topicMap.get(tid)); s.put("bandRange",band(s.get("cefrLevel")));
            UUID sid=(UUID)s.get("id"); s.put("collocations",collocations.getOrDefault(sid,List.of()));
            List<Map<String,Object>> rel=relations.getOrDefault(sid,List.of()); s.put("synonyms",rel.stream().filter(x->"SYNONYM".equals(x.get("kind"))).toList()); s.put("antonyms",rel.stream().filter(x->"ANTONYM".equals(x.get("kind"))).toList());
            s.put("commonMistakes",mistakes.getOrDefault(sid,List.of())); s.put("examples",examples.getOrDefault(sid,List.of()));
            s.put("counts",Map.of("definitions",1,"collocations",((List<?>)s.get("collocations")).size(),"thesaurus",rel.size(),"commonMistakes",((List<?>)s.get("commonMistakes")).size(),"examples",((List<?>)s.get("examples")).size()));
        }
        Map<UUID,List<Map<String,Object>>> byItem=senses.stream().collect(Collectors.groupingBy(s->(UUID)s.get("vocabularyItemId")));
        items.forEach(i->{ UUID iid=(UUID)i.get("id"); i.put("ipaUk",i.get("ipaUk")!=null?i.get("ipaUk"):i.get("ipa")); i.put("audioUkReference",i.get("audioUkReference")!=null?i.get("audioUkReference"):i.get("pronunciationAudioReference"));
            List<Map<String,Object>> itemSenses=byItem.getOrDefault(iid,new ArrayList<>()); i.put("senses",itemSenses);
            i.put("counts",Map.of("definitions",itemSenses.size(),"collocations",itemSenses.stream().mapToInt(x->((List<?>)x.get("collocations")).size()).sum(),
                    "thesaurus",itemSenses.stream().mapToInt(x->((List<?>)x.get("synonyms")).size()+((List<?>)x.get("antonyms")).size()).sum(),
                    "commonMistakes",itemSenses.stream().mapToInt(x->((List<?>)x.get("commonMistakes")).size()).sum(),"examples",itemSenses.stream().mapToInt(x->((List<?>)x.get("examples")).size()).sum())); });
        Map<UUID,Map<String,Object>> ordered=new HashMap<>(); items.forEach(i->ordered.put((UUID)i.get("id"),i));
        return ids.stream().map(ordered::get).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Map<String,Object>> children(String table,List<UUID> ids,String ignored) {
        if(ids.isEmpty()) return List.of();
        String cols=switch(table) {
            case "vocabulary_collocations" -> "id,sense_id AS \"senseId\",phrase,meaning_vi AS \"meaningVi\",example,sort_order AS \"sortOrder\"";
            case "vocabulary_relations" -> "id,sense_id AS \"senseId\",related_lemma AS \"relatedLemma\",related_item_id AS \"relatedItemId\",kind,sort_order AS \"sortOrder\"";
            case "vocabulary_common_mistakes" -> "id,sense_id AS \"senseId\",wrong_usage AS \"wrongUsage\",correct_usage AS \"correctUsage\",explanation,sort_order AS \"sortOrder\"";
            case "vocabulary_examples" -> "id,sense_id AS \"senseId\",sentence,translation_vi AS \"translationVi\",sort_order AS \"sortOrder\"";
            default -> throw new IllegalArgumentException("unsupported child table");
        };
        return jdbc.queryForList("SELECT "+cols+" FROM "+table+" WHERE sense_id IN (:ids) ORDER BY sense_id,sort_order,id",new MapSqlParameterSource("ids",ids));
    }
    private Map<UUID,List<Map<String,Object>>> group(List<Map<String,Object>> rows) { return rows.stream().collect(Collectors.groupingBy(r->(UUID)r.get("senseId"))); }

    private static Map<String,Object> page(List<?> items,int page,int size,long total) { Map<String,Object> out=new LinkedHashMap<>(); out.put("items",items);out.put("page",page);out.put("size",size);out.put("total",total);return out; }
    private static Object band(Object cefr) { if(cefr==null)return null; return CefrLevel.valueOf(cefr.toString()).bandRange(); }

    @Override public Map<String,Object> updateItem(UUID id,Map<String,Object> values) {
        update("vocabulary_items",id,values,Map.of("ipaUk","ipa_uk","ipaUs","ipa_us","audioUkReference","audio_uk_reference","audioUsReference","audio_us_reference","frequencyRank","frequency_rank","academicWordList","academic_word_list","wordFamily","word_family")); return detailEvenInactive(id);
    }
    @Override public Map<String,Object> updateSense(UUID id,Map<String,Object> values) {
        Map<String,Object> converted=new HashMap<>(values); if(converted.containsKey("cefrLevel"))converted.put("cefr_level",converted.remove("cefrLevel")); if(converted.containsKey("topicId"))converted.put("topic_id",converted.remove("topicId"));
        update("vocabulary_senses",id,converted,Map.of("cefr_level","cefr_level","topic_id","topic_id","register","register","grammarNote","grammar_note","usageNote","usage_note","exampleTranslation","example_translation"));
        return jdbc.queryForMap("SELECT id,vocabulary_item_id AS \"vocabularyItemId\",cefr_level AS \"cefrLevel\",topic_id AS \"topicId\",register,grammar_note AS \"grammarNote\",usage_note AS \"usageNote\",example_translation AS \"exampleTranslation\",status FROM vocabulary_senses WHERE id=:id",Map.of("id",id));
    }
    private void update(String table,UUID id,Map<String,Object> values,Map<String,String> allowed) {
        if(values.isEmpty()) return; List<String> sets=new ArrayList<>(); MapSqlParameterSource p=new MapSqlParameterSource("id",id);
        values.forEach((key,value)->{String col=allowed.get(key);if(col==null) throw new IllegalArgumentException("unsupported field: "+key); sets.add(col+"= :"+key); p.addValue(key,value);});
        sets.add("updated_at=CURRENT_TIMESTAMP"); int n=jdbc.update("UPDATE "+table+" SET "+String.join(",",sets)+" WHERE id=:id",p); if(n==0)throw new ResourceNotFoundException();
    }
    private Map<String,Object> detailEvenInactive(UUID id) { List<Map<String,Object>> r=loadItems(List.of(id));if(r.isEmpty())throw new ResourceNotFoundException();return r.getFirst(); }

    @Override public List<Map<String,Object>> replaceChildren(String kind,UUID senseId,List<Map<String,Object>> values) {
        String table=TABLES.get(kind);if(table==null)throw new IllegalArgumentException("unsupported child collection");
        if(jdbc.query("SELECT id FROM vocabulary_senses WHERE id=:id FOR UPDATE",Map.of("id",senseId),(rs,n)->rs.getObject(1,UUID.class)).isEmpty())throw new ResourceNotFoundException();
        jdbc.update("DELETE FROM "+table+" WHERE sense_id=:id",Map.of("id",senseId));
        List<MapSqlParameterSource> rows=new ArrayList<>(); int sort=0;
        for(Map<String,Object> v:values) { MapSqlParameterSource row=new MapSqlParameterSource().addValue("id",senseId).addValue("sort",sort++);
            switch(kind) {
                case "collocations" -> row.addValue("phrase",v.get("phrase")).addValue("meaning",v.get("meaningVi")).addValue("example",v.get("example"));
                case "relations" -> row.addValue("lemma",v.get("relatedLemma")).addValue("item",v.get("relatedItemId")).addValue("kind",v.get("kind"));
                case "common-mistakes" -> row.addValue("wrong",v.get("wrongUsage")).addValue("correct",v.get("correctUsage")).addValue("explanation",v.get("explanation"));
                case "examples" -> row.addValue("sentence",v.get("sentence")).addValue("translation",v.get("translationVi"));
            }
            rows.add(row);
        }
        if(!rows.isEmpty()) {
            String insert=switch(kind) {
                case "collocations" -> "INSERT INTO "+table+"(sense_id,phrase,meaning_vi,example,sort_order) VALUES(:id,:phrase,:meaning,:example,:sort)";
                case "relations" -> "INSERT INTO "+table+"(sense_id,related_lemma,related_item_id,kind,sort_order) VALUES(:id,:lemma,:item,:kind,:sort)";
                case "common-mistakes" -> "INSERT INTO "+table+"(sense_id,wrong_usage,correct_usage,explanation,sort_order) VALUES(:id,:wrong,:correct,:explanation,:sort)";
                case "examples" -> "INSERT INTO "+table+"(sense_id,sentence,translation_vi,sort_order) VALUES(:id,:sentence,:translation,:sort)";
                default -> throw new IllegalArgumentException("unsupported child collection");
            };
            jdbc.batchUpdate(insert,rows.toArray(MapSqlParameterSource[]::new));
        }
        return children(table,List.of(senseId),"");
    }

    @Override public List<Map<String,Object>> topicsAdmin() { return jdbc.queryForList("SELECT id,slug,name,name_vi AS \"nameVi\",cover_key AS \"coverKey\",sort_order AS \"sortOrder\",status,created_at AS \"createdAt\",updated_at AS \"updatedAt\" FROM vocabulary_topics ORDER BY sort_order,name",Map.of()); }
    @Override public Map<String,Object> saveTopic(UUID id,Map<String,Object> v) {
        if(id==null) { id=UUID.randomUUID(); jdbc.update("INSERT INTO vocabulary_topics(id,slug,name,name_vi,cover_key,sort_order,status) VALUES(:id,:slug,:name,:nameVi,:coverKey,:sortOrder,:status)",params(id,v)); }
        else { int n=jdbc.update("UPDATE vocabulary_topics SET slug=:slug,name=:name,name_vi=:nameVi,cover_key=:coverKey,sort_order=:sortOrder,status=:status,updated_at=CURRENT_TIMESTAMP WHERE id=:id",params(id,v));if(n==0)throw new ResourceNotFoundException(); }
        return jdbc.queryForMap("SELECT id,slug,name,name_vi AS \"nameVi\",cover_key AS \"coverKey\",sort_order AS \"sortOrder\",status,created_at AS \"createdAt\",updated_at AS \"updatedAt\" FROM vocabulary_topics WHERE id=:id",Map.of("id",id));
    }
    private MapSqlParameterSource params(UUID id,Map<String,Object> v) { return new MapSqlParameterSource().addValue("id",id).addValue("slug",v.get("slug")).addValue("name",v.get("name")).addValue("nameVi",v.get("nameVi")).addValue("coverKey",v.get("coverKey")).addValue("sortOrder",v.getOrDefault("sortOrder",0)).addValue("status",v.getOrDefault("status","ACTIVE")); }
    @Override public void deleteTopic(UUID id) { if(jdbc.update("UPDATE vocabulary_topics SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP WHERE id=:id",Map.of("id",id))==0)throw new ResourceNotFoundException(); }
    @Override public List<Map<String,Object>> setsAdmin() { return jdbc.queryForList("SELECT id,title,description,kind,cefr_level AS \"cefrLevel\",topic_id AS \"topicId\",cover_key AS \"coverKey\",sort_order AS \"sortOrder\",status,created_at AS \"createdAt\",updated_at AS \"updatedAt\" FROM vocabulary_sets ORDER BY sort_order,title",Map.of()); }
    @Override public Map<String,Object> saveSet(UUID id,Map<String,Object> v) {
        if(id==null)id=UUID.randomUUID(); MapSqlParameterSource p=new MapSqlParameterSource().addValue("id",id).addValue("title",v.get("title")).addValue("description",v.get("description")).addValue("kind",v.get("kind")).addValue("cefr",v.get("cefrLevel")).addValue("topic",v.get("topicId")).addValue("cover",v.get("coverKey")).addValue("order",v.getOrDefault("sortOrder",0)).addValue("status",v.getOrDefault("status","ACTIVE"));
        int n=jdbc.update("INSERT INTO vocabulary_sets(id,title,description,kind,cefr_level,topic_id,cover_key,sort_order,status) VALUES(:id,:title,:description,:kind,:cefr,:topic,:cover,:order,:status) ON CONFLICT(id) DO UPDATE SET title=:title,description=:description,kind=:kind,cefr_level=:cefr,topic_id=:topic,cover_key=:cover,sort_order=:order,status=:status,updated_at=CURRENT_TIMESTAMP",p);
        if(n==0)throw new ResourceNotFoundException();return jdbc.queryForMap("SELECT id,title,description,kind,cefr_level AS \"cefrLevel\",topic_id AS \"topicId\",cover_key AS \"coverKey\",sort_order AS \"sortOrder\",status,created_at AS \"createdAt\",updated_at AS \"updatedAt\" FROM vocabulary_sets WHERE id=:id",Map.of("id",id));
    }
    @Override public void deleteSet(UUID id) { if(jdbc.update("UPDATE vocabulary_sets SET status='INACTIVE',updated_at=CURRENT_TIMESTAMP WHERE id=:id",Map.of("id",id))==0)throw new ResourceNotFoundException(); }
    @Override public void replaceSetItems(UUID id,List<UUID> senseIds) {
        if(jdbc.query("SELECT id FROM vocabulary_sets WHERE id=:id FOR UPDATE",Map.of("id",id),(rs,n)->rs.getObject(1,UUID.class)).isEmpty())throw new ResourceNotFoundException();
        if(!senseIds.isEmpty() && jdbc.queryForObject("SELECT count(DISTINCT id) FROM vocabulary_senses WHERE id IN (:ids)",Map.of("ids",senseIds),Long.class)!=senseIds.size())throw new ResourceNotFoundException();
        jdbc.update("DELETE FROM vocabulary_set_items WHERE set_id=:id",Map.of("id",id));
        MapSqlParameterSource[] rows=new MapSqlParameterSource[senseIds.size()];
        for(int i=0;i<senseIds.size();i++)rows[i]=new MapSqlParameterSource().addValue("id",id).addValue("sense",senseIds.get(i)).addValue("order",i);
        if(rows.length>0)jdbc.batchUpdate("INSERT INTO vocabulary_set_items(set_id,sense_id,sort_order) VALUES(:id,:sense,:order)",rows);
    }
}
