package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.port.VocabularyCatalogStore;
import com.ieltspath.library.application.query.CatalogBrowseQuery;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.application.result.CatalogCountsResult;
import com.ieltspath.library.application.result.CatalogItemResult;
import com.ieltspath.library.application.result.CatalogSenseResult;
import com.ieltspath.library.application.result.CollocationResult;
import com.ieltspath.library.application.result.CommonMistakeResult;
import com.ieltspath.library.application.result.PageResult;
import com.ieltspath.library.application.result.UsageExampleResult;
import com.ieltspath.library.application.result.VocabularyMindmapResult;
import com.ieltspath.library.application.result.VocabularySetResult;
import com.ieltspath.library.application.result.VocabularySuggestionResult;
import com.ieltspath.library.application.result.VocabularyTopicResult;
import com.ieltspath.library.application.result.WordRelationResult;
import com.ieltspath.library.domain.catalog.Collocation;
import com.ieltspath.library.domain.catalog.CommonMistake;
import com.ieltspath.library.domain.catalog.UsageExample;
import com.ieltspath.library.domain.catalog.VocabularyItemDetails;
import com.ieltspath.library.domain.catalog.VocabularySenseDetails;
import com.ieltspath.library.domain.catalog.VocabularySetDraft;
import com.ieltspath.library.domain.catalog.VocabularyTopicDraft;
import com.ieltspath.library.domain.catalog.WordRelation;
import com.ieltspath.library.domain.exception.ConflictException;
import com.ieltspath.library.domain.vo.CefrLevel;
import com.ieltspath.library.domain.vo.ContentStatus;
import com.ieltspath.library.domain.vo.PartOfSpeech;
import com.ieltspath.library.domain.vo.RelationKind;
import com.ieltspath.library.domain.vo.VocabularySetKind;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class VocabularyCatalogJdbcAdapter implements VocabularyCatalogStore {
    private static final String ACTIVE_SET = "s.status='ACTIVE' AND (s.topic_id IS NULL OR t.status='ACTIVE')";
    private static final String ACTIVE_SET_MEMBER = "FROM vocabulary_set_items si "
            + "JOIN vocabulary_senses s ON s.id=si.sense_id AND s.status='ACTIVE' "
            + "JOIN vocabulary_items i ON i.id=s.vocabulary_item_id AND i.status='ACTIVE' WHERE si.set_id=:id";
    private static final String TOPIC_COLUMNS = "t.id,t.slug,t.name,t.name_vi,t.cover_key,t.sort_order,t.status,t.created_at,t.updated_at";
    private static final String SET_COLUMNS = "s.id,s.title,s.description,s.kind,s.cefr_level,s.topic_id,s.cover_key,"
            + "s.sort_order,s.status,s.created_at,s.updated_at";

    private final NamedParameterJdbcTemplate jdbc;
    private final FlashcardImageStorage storage;

    public VocabularyCatalogJdbcAdapter(NamedParameterJdbcTemplate jdbc, FlashcardImageStorage storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    /** Makes user text match literally: {@code %}, {@code _} and the escape character itself lose their LIKE meaning. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public Optional<CatalogItemResult> findActiveItem(UUID itemId) {
        return loadItems(List.of(itemId), true).stream().findFirst();
    }

    @Override
    public PageResult<CatalogItemResult> browse(CatalogBrowseQuery query) {
        PageQuery page = query.page();
        // The column is chosen from a closed pair, never from input text.
        String searchColumn = query.vietnamese() ? "s.vietnamese_meaning" : "i.lemma";
        String text = query.text().toLowerCase(Locale.ROOT);
        String senseFilters = "AND (:query='' OR lower(" + searchColumn + ") LIKE :pattern ESCAPE '\\')";
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("query", text)
                .addValue("pattern", "%" + escapeLike(text) + "%").addValue("limit", page.size()).addValue("offset", offset(page));
        if (query.cefrLevel() != null) {
            senseFilters += " AND s.cefr_level=:cefr";
            p.addValue("cefr", query.cefrLevel().name());
        }
        if (query.topicId() != null) {
            senseFilters += " AND s.topic_id=:topicId";
            p.addValue("topicId", query.topicId());
        }
        String where = "i.status='ACTIVE' AND EXISTS (SELECT 1 FROM vocabulary_senses s WHERE s.vocabulary_item_id=i.id "
                + "AND s.status='ACTIVE' " + senseFilters + ")";
        long total = count("SELECT count(*) FROM vocabulary_items i WHERE " + where, p);
        List<UUID> ids = jdbc.query("SELECT i.id FROM vocabulary_items i WHERE " + where
                + " ORDER BY i.normalized_lemma, i.id LIMIT :limit OFFSET :offset", p, (rs, n) -> uuid(rs, "id"));
        return new PageResult<>(loadItems(ids, true), page.page(), page.size(), total);
    }

    @Override
    public List<VocabularySuggestionResult> suggestions(String prefix) {
        return jdbc.query("SELECT id, lemma FROM vocabulary_items WHERE status='ACTIVE' AND lower(lemma) LIKE :q ESCAPE '\\' "
                        + "ORDER BY lower(lemma), id LIMIT 8",
                Map.of("q", escapeLike(prefix.toLowerCase(Locale.ROOT)) + "%"),
                (rs, n) -> new VocabularySuggestionResult(uuid(rs, "id"), rs.getString("lemma")));
    }

    @Override
    public List<VocabularyTopicResult> activeTopics() {
        return jdbc.query("SELECT " + TOPIC_COLUMNS + ", count(DISTINCT CASE WHEN i.status='ACTIVE' THEN s.id END) AS word_count "
                        + "FROM vocabulary_topics t LEFT JOIN vocabulary_senses s ON s.topic_id=t.id AND s.status='ACTIVE' "
                        + "LEFT JOIN vocabulary_items i ON i.id=s.vocabulary_item_id "
                        + "WHERE t.status='ACTIVE' GROUP BY t.id ORDER BY t.sort_order, t.name",
                Map.of(), (rs, n) -> topic(rs, rs.getLong("word_count"), false));
    }

    @Override
    public PageResult<VocabularySetResult> activeSets(VocabularySetKind kind, PageQuery page) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("limit", page.size()).addValue("offset", offset(page));
        String filter = "";
        if (kind != null) {
            filter = " AND s.kind=:kind";
            p.addValue("kind", kind.name());
        }
        long total = count("SELECT count(*) FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id WHERE "
                + ACTIVE_SET + filter, p);
        List<VocabularySetResult> items = jdbc.query("SELECT " + SET_COLUMNS + ", t.name AS topic_name, t.name_vi AS topic_name_vi, "
                        + "count(DISTINCT st.topic_id) AS topic_count, "
                        + "count(DISTINCT CASE WHEN st.status='ACTIVE' THEN si.sense_id END) AS word_count "
                        + "FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id "
                        + "LEFT JOIN vocabulary_set_items si ON si.set_id=s.id "
                        + "LEFT JOIN vocabulary_senses st ON st.id=si.sense_id AND st.topic_id IS NOT NULL AND st.status='ACTIVE' "
                        + "WHERE " + ACTIVE_SET + filter
                        + " GROUP BY s.id, t.name, t.name_vi ORDER BY s.sort_order, s.title LIMIT :limit OFFSET :offset",
                p, (rs, n) -> set(rs, rs.getLong("topic_count"), rs.getLong("word_count"), null, false));
        return new PageResult<>(items, page.page(), page.size(), total);
    }

    @Override
    public Optional<VocabularySetResult> findActiveSet(UUID setId) {
        MapSqlParameterSource id = new MapSqlParameterSource("id", setId);
        List<VocabularySetResult> rows = jdbc.query("SELECT " + SET_COLUMNS + ", t.name AS topic_name, t.name_vi AS topic_name_vi "
                        + "FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id WHERE s.id=:id AND " + ACTIVE_SET,
                id, (rs, n) -> set(rs, null, null, null, false));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        long wordCount = count("SELECT count(*) " + ACTIVE_SET_MEMBER, id);
        List<VocabularyTopicResult> topics = jdbc.query("SELECT " + TOPIC_COLUMNS + ", count(si.sense_id) AS word_count "
                        + "FROM vocabulary_set_items si "
                        + "JOIN vocabulary_senses s ON s.id=si.sense_id AND s.status='ACTIVE' "
                        + "JOIN vocabulary_items i ON i.id=s.vocabulary_item_id AND i.status='ACTIVE' "
                        + "JOIN vocabulary_topics t ON t.id=s.topic_id "
                        + "WHERE si.set_id=:id GROUP BY t.id ORDER BY t.sort_order, t.name",
                id, (rs, n) -> topic(rs, rs.getLong("word_count"), false));
        VocabularySetResult found = rows.getFirst();
        return Optional.of(new VocabularySetResult(found.id(), found.title(), found.description(), found.kind(),
                found.cefrLevel(), found.topicId(), found.topicName(), found.topicNameVi(), found.coverKey(),
                found.coverUrl(), found.sortOrder(), found.status(), found.bandRange(), (long) topics.size(),
                wordCount, topics, null, null));
    }

    @Override
    public PageResult<CatalogItemResult> activeSetWords(UUID setId, UUID topicId, PageQuery page) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("id", setId)
                .addValue("limit", page.size()).addValue("offset", offset(page));
        String topicFilter = "";
        if (topicId != null) {
            topicFilter = " AND s.topic_id=:topicId";
            p.addValue("topicId", topicId);
        }
        long total = count("SELECT count(*) " + ACTIVE_SET_MEMBER + topicFilter, p);
        List<UUID> senseIds = jdbc.query("SELECT si.sense_id " + ACTIVE_SET_MEMBER + topicFilter
                + " ORDER BY si.sort_order, si.sense_id LIMIT :limit OFFSET :offset", p, (rs, n) -> uuid(rs, "sense_id"));
        if (senseIds.isEmpty()) {
            return new PageResult<>(List.of(), page.page(), page.size(), total);
        }
        List<UUID> itemIds = jdbc.query("SELECT s.vocabulary_item_id FROM vocabulary_set_items si "
                        + "JOIN vocabulary_senses s ON s.id=si.sense_id WHERE si.set_id=:id AND si.sense_id IN (:ids) "
                        + "GROUP BY s.vocabulary_item_id ORDER BY min(si.sort_order), s.vocabulary_item_id",
                new MapSqlParameterSource("ids", senseIds).addValue("id", setId), (rs, n) -> uuid(rs, "vocabulary_item_id"));
        // A word shows only the meanings that belong to this set.
        List<CatalogItemResult> items = loadItems(itemIds, true).stream()
                .map(item -> withSenses(item, item.senses().stream().filter(s -> senseIds.contains(s.id())).toList()))
                .toList();
        return new PageResult<>(items, page.page(), page.size(), total);
    }

    @Override
    public VocabularyMindmapResult topicMindmap(UUID topicId, int maxNodes) {
        List<VocabularyTopicResult> topic = jdbc.query("SELECT " + TOPIC_COLUMNS + " FROM vocabulary_topics t WHERE t.id=:id AND t.status='ACTIVE'",
                Map.of("id", topicId), (rs, n) -> topic(rs, null, false));
        if (topic.isEmpty()) {
            return new VocabularyMindmapResult(List.of(), List.of());
        }
        List<MindmapRoot> roots = jdbc.query("SELECT s.id AS sense_id, i.id AS item_id, i.lemma FROM vocabulary_senses s "
                        + "JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE s.topic_id=:id AND s.status='ACTIVE' "
                        + "AND i.status='ACTIVE' ORDER BY i.normalized_lemma, s.sort_order LIMIT :limit",
                new MapSqlParameterSource("id", topicId).addValue("limit", maxNodes - 1), this::mindmapRoot);
        List<VocabularyMindmapResult.Node> nodes = new ArrayList<>();
        List<VocabularyMindmapResult.Edge> edges = new ArrayList<>();
        nodes.add(new VocabularyMindmapResult.Node(topicId, "TOPIC", topic.getFirst().name(), null));
        roots.forEach(root -> edges.add(new VocabularyMindmapResult.Edge(topicId, root.senseId(), "TOPIC_WORD")));
        return mindmap(roots, nodes, edges, maxNodes);
    }

    @Override
    public VocabularyMindmapResult itemMindmap(UUID itemId, int maxNodes) {
        List<MindmapRoot> roots = jdbc.query("SELECT s.id AS sense_id, i.id AS item_id, i.lemma FROM vocabulary_senses s "
                        + "JOIN vocabulary_items i ON i.id=s.vocabulary_item_id WHERE i.id=:id AND s.status='ACTIVE' "
                        + "AND i.status='ACTIVE' ORDER BY s.sort_order LIMIT :limit",
                new MapSqlParameterSource("id", itemId).addValue("limit", maxNodes), this::mindmapRoot);
        return mindmap(roots, new ArrayList<>(), new ArrayList<>(), maxNodes);
    }

    private VocabularyMindmapResult mindmap(List<MindmapRoot> roots, List<VocabularyMindmapResult.Node> nodes,
                                            List<VocabularyMindmapResult.Edge> edges, int maxNodes) {
        if (roots.isEmpty()) {
            return new VocabularyMindmapResult(List.of(), List.of());
        }
        roots.forEach(root -> nodes.add(new VocabularyMindmapResult.Node(root.senseId(), "WORD", root.lemma(), root.itemId())));
        List<UUID> senseIds = roots.stream().map(MindmapRoot::senseId).toList();
        for (CollocationResult child : collocations(senseIds)) {
            addMindmapChild(nodes, edges, child.id(), child.senseId(), "COLLOCATION", child.phrase(), maxNodes);
        }
        for (WordRelationResult child : relations(senseIds)) {
            if (child.kind() == RelationKind.SYNONYM) {
                addMindmapChild(nodes, edges, child.id(), child.senseId(), "SYNONYM", child.relatedLemma(), maxNodes);
            }
        }
        return new VocabularyMindmapResult(List.copyOf(nodes), List.copyOf(edges));
    }

    private static void addMindmapChild(List<VocabularyMindmapResult.Node> nodes, List<VocabularyMindmapResult.Edge> edges,
                                        UUID id, UUID senseId, String type, String label, int maxNodes) {
        if (nodes.size() >= maxNodes) {
            return;
        }
        nodes.add(new VocabularyMindmapResult.Node(id, type, label, null));
        edges.add(new VocabularyMindmapResult.Edge(senseId, id, type));
    }

    private MindmapRoot mindmapRoot(ResultSet rs, int n) throws SQLException {
        return new MindmapRoot(uuid(rs, "sense_id"), uuid(rs, "item_id"), rs.getString("lemma"));
    }

    private record MindmapRoot(UUID senseId, UUID itemId, String lemma) {
    }

    @Override
    public boolean topicExists(UUID topicId) {
        return count("SELECT count(*) FROM vocabulary_topics WHERE id=:id", new MapSqlParameterSource("id", topicId)) > 0;
    }

    @Override
    public Optional<CatalogItemResult> updateItem(UUID itemId, VocabularyItemDetails details) {
        int updated = jdbc.update("UPDATE vocabulary_items SET ipa_uk=:ipaUk, ipa_us=:ipaUs, audio_uk_reference=:audioUk, "
                        + "audio_us_reference=:audioUs, frequency_rank=:rank, academic_word_list=:awl, word_family=:family, "
                        + "updated_at=CURRENT_TIMESTAMP WHERE id=:id",
                new MapSqlParameterSource("id", itemId).addValue("ipaUk", details.ipaUk()).addValue("ipaUs", details.ipaUs())
                        .addValue("audioUk", details.audioUkReference()).addValue("audioUs", details.audioUsReference())
                        .addValue("rank", details.frequencyRank()).addValue("awl", details.academicWordList())
                        .addValue("family", details.wordFamily()));
        return updated == 0 ? Optional.empty() : loadItems(List.of(itemId), false).stream().findFirst();
    }

    @Override
    public Optional<CatalogItemResult> updateSense(UUID senseId, VocabularySenseDetails details) {
        List<UUID> itemIds = jdbc.query("UPDATE vocabulary_senses SET cefr_level=:cefr, topic_id=:topicId, register=:register, "
                        + "grammar_note=:grammar, usage_note=:usage, example_translation=:translation, updated_at=CURRENT_TIMESTAMP "
                        + "WHERE id=:id RETURNING vocabulary_item_id",
                new MapSqlParameterSource("id", senseId)
                        .addValue("cefr", details.cefrLevel() == null ? null : details.cefrLevel().name())
                        .addValue("topicId", details.topicId()).addValue("register", details.register())
                        .addValue("grammar", details.grammarNote()).addValue("usage", details.usageNote())
                        .addValue("translation", details.exampleTranslation()),
                (rs, n) -> uuid(rs, "vocabulary_item_id"));
        return itemIds.isEmpty() ? Optional.empty() : loadItems(itemIds, false).stream().findFirst();
    }

    @Override
    public boolean lockSense(UUID senseId) {
        return !jdbc.query("SELECT id FROM vocabulary_senses WHERE id=:id FOR UPDATE", Map.of("id", senseId),
                (rs, n) -> uuid(rs, "id")).isEmpty();
    }

    @Override
    public List<CollocationResult> replaceCollocations(UUID senseId, List<Collocation> values) {
        replaceRows("vocabulary_collocations", senseId, values,
                "INSERT INTO vocabulary_collocations(sense_id, phrase, meaning_vi, example, sort_order) VALUES(:id, :phrase, :meaning, :example, :sort)",
                (row, v) -> row.addValue("phrase", v.phrase()).addValue("meaning", v.meaningVi()).addValue("example", v.example()));
        return collocations(List.of(senseId));
    }

    @Override
    public List<WordRelationResult> replaceRelations(UUID senseId, List<WordRelation> values) {
        replaceRows("vocabulary_relations", senseId, values,
                "INSERT INTO vocabulary_relations(sense_id, related_lemma, related_item_id, kind, sort_order) VALUES(:id, :lemma, :item, :kind, :sort)",
                (row, v) -> row.addValue("lemma", v.relatedLemma()).addValue("item", v.relatedItemId()).addValue("kind", v.kind().name()));
        return relations(List.of(senseId));
    }

    @Override
    public List<CommonMistakeResult> replaceCommonMistakes(UUID senseId, List<CommonMistake> values) {
        replaceRows("vocabulary_common_mistakes", senseId, values,
                "INSERT INTO vocabulary_common_mistakes(sense_id, wrong_usage, correct_usage, explanation, sort_order) VALUES(:id, :wrong, :correct, :explanation, :sort)",
                (row, v) -> row.addValue("wrong", v.wrongUsage()).addValue("correct", v.correctUsage()).addValue("explanation", v.explanation()));
        return commonMistakes(List.of(senseId));
    }

    @Override
    public List<UsageExampleResult> replaceExamples(UUID senseId, List<UsageExample> values) {
        replaceRows("vocabulary_examples", senseId, values,
                "INSERT INTO vocabulary_examples(sense_id, sentence, translation_vi, sort_order) VALUES(:id, :sentence, :translation, :sort)",
                (row, v) -> row.addValue("sentence", v.sentence()).addValue("translation", v.translationVi()));
        return examples(List.of(senseId));
    }

    /** The table name is always one of the four constants passed by this class. */
    private <T> void replaceRows(String table, UUID senseId, List<T> values, String insert,
                                 java.util.function.BiFunction<MapSqlParameterSource, T, MapSqlParameterSource> bind) {
        jdbc.update("DELETE FROM " + table + " WHERE sense_id=:id", Map.of("id", senseId));
        if (values.isEmpty()) {
            return;
        }
        MapSqlParameterSource[] rows = new MapSqlParameterSource[values.size()];
        for (int i = 0; i < values.size(); i++) {
            rows[i] = bind.apply(new MapSqlParameterSource("id", senseId).addValue("sort", i), values.get(i));
        }
        jdbc.batchUpdate(insert, rows);
    }

    @Override
    public List<VocabularyTopicResult> allTopics() {
        return jdbc.query("SELECT " + TOPIC_COLUMNS + " FROM vocabulary_topics t ORDER BY t.sort_order, t.name",
                Map.of(), (rs, n) -> topic(rs, null, true));
    }

    @Override
    public VocabularyTopicResult createTopic(VocabularyTopicDraft draft) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO vocabulary_topics(id, slug, name, name_vi, cover_key, sort_order, status) "
                    + "VALUES(:id, :slug, :name, :nameVi, :coverKey, :sortOrder, :status)", topicParams(id, draft));
        } catch (DuplicateKeyException duplicateSlug) {
            throw new ConflictException();
        }
        return adminTopic(id);
    }

    @Override
    public Optional<VocabularyTopicResult> updateTopic(UUID topicId, VocabularyTopicDraft draft) {
        int updated;
        try {
            updated = jdbc.update("UPDATE vocabulary_topics SET slug=:slug, name=:name, name_vi=:nameVi, cover_key=:coverKey, "
                    + "sort_order=:sortOrder, status=:status, updated_at=CURRENT_TIMESTAMP WHERE id=:id", topicParams(topicId, draft));
        } catch (DuplicateKeyException duplicateSlug) {
            throw new ConflictException();
        }
        return updated == 0 ? Optional.empty() : Optional.of(adminTopic(topicId));
    }

    private VocabularyTopicResult adminTopic(UUID id) {
        return jdbc.queryForObject("SELECT " + TOPIC_COLUMNS + " FROM vocabulary_topics t WHERE t.id=:id",
                Map.of("id", id), (rs, n) -> topic(rs, null, true));
    }

    private static MapSqlParameterSource topicParams(UUID id, VocabularyTopicDraft draft) {
        return new MapSqlParameterSource("id", id).addValue("slug", draft.slug()).addValue("name", draft.name())
                .addValue("nameVi", draft.nameVi()).addValue("coverKey", draft.coverKey())
                .addValue("sortOrder", draft.sortOrder()).addValue("status", draft.status().name());
    }

    @Override
    public boolean deactivateTopic(UUID topicId) {
        return jdbc.update("UPDATE vocabulary_topics SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP WHERE id=:id",
                Map.of("id", topicId)) > 0;
    }

    @Override
    public List<VocabularySetResult> allSets() {
        return jdbc.query("SELECT " + SET_COLUMNS + ", t.name AS topic_name, t.name_vi AS topic_name_vi "
                        + "FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id ORDER BY s.sort_order, s.title",
                Map.of(), (rs, n) -> set(rs, null, null, null, true));
    }

    @Override
    public VocabularySetResult createSet(VocabularySetDraft draft) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vocabulary_sets(id, title, description, kind, cefr_level, topic_id, cover_key, sort_order, status) "
                + "VALUES(:id, :title, :description, :kind, :cefr, :topic, :cover, :order, :status)", setParams(id, draft));
        return adminSet(id);
    }

    @Override
    public Optional<VocabularySetResult> updateSet(UUID setId, VocabularySetDraft draft) {
        int updated = jdbc.update("UPDATE vocabulary_sets SET title=:title, description=:description, kind=:kind, cefr_level=:cefr, "
                + "topic_id=:topic, cover_key=:cover, sort_order=:order, status=:status, updated_at=CURRENT_TIMESTAMP "
                + "WHERE id=:id", setParams(setId, draft));
        return updated == 0 ? Optional.empty() : Optional.of(adminSet(setId));
    }

    private VocabularySetResult adminSet(UUID id) {
        return jdbc.queryForObject("SELECT " + SET_COLUMNS + ", t.name AS topic_name, t.name_vi AS topic_name_vi "
                        + "FROM vocabulary_sets s LEFT JOIN vocabulary_topics t ON t.id=s.topic_id WHERE s.id=:id",
                Map.of("id", id), (rs, n) -> set(rs, null, null, null, true));
    }

    private static MapSqlParameterSource setParams(UUID id, VocabularySetDraft draft) {
        return new MapSqlParameterSource("id", id).addValue("title", draft.title()).addValue("description", draft.description())
                .addValue("kind", draft.kind().name())
                .addValue("cefr", draft.cefrLevel() == null ? null : draft.cefrLevel().name())
                .addValue("topic", draft.topicId()).addValue("cover", draft.coverKey())
                .addValue("order", draft.sortOrder()).addValue("status", draft.status().name());
    }

    @Override
    public boolean deactivateSet(UUID setId) {
        return jdbc.update("UPDATE vocabulary_sets SET status='INACTIVE', updated_at=CURRENT_TIMESTAMP WHERE id=:id",
                Map.of("id", setId)) > 0;
    }

    @Override
    public boolean lockSet(UUID setId) {
        return !jdbc.query("SELECT id FROM vocabulary_sets WHERE id=:id FOR UPDATE", Map.of("id", setId),
                (rs, n) -> uuid(rs, "id")).isEmpty();
    }

    @Override
    public int countExistingSenses(List<UUID> senseIds) {
        return (int) count("SELECT count(DISTINCT id) FROM vocabulary_senses WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", senseIds));
    }

    @Override
    public void replaceSetItems(UUID setId, List<UUID> senseIds) {
        jdbc.update("DELETE FROM vocabulary_set_items WHERE set_id=:id", Map.of("id", setId));
        if (senseIds.isEmpty()) {
            return;
        }
        MapSqlParameterSource[] rows = new MapSqlParameterSource[senseIds.size()];
        for (int i = 0; i < senseIds.size(); i++) {
            rows[i] = new MapSqlParameterSource("id", setId).addValue("sense", senseIds.get(i)).addValue("order", i);
        }
        jdbc.batchUpdate("INSERT INTO vocabulary_set_items(set_id, sense_id, sort_order) VALUES(:id, :sense, :order)", rows);
    }

    /**
     * Loads words with their meanings and every child list in one query per table, keeping the order of
     * {@code ids}. Only active meanings are returned; {@code activeOnly} also drops inactive words.
     */
    private List<CatalogItemResult> loadItems(List<UUID> ids, boolean activeOnly) {
        if (ids.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource p = new MapSqlParameterSource("ids", ids);
        List<ItemRow> items = jdbc.query("SELECT id, lemma, normalized_lemma, ipa, pronunciation_audio_reference, ipa_uk, ipa_us, "
                + "audio_uk_reference, audio_us_reference, frequency_rank, academic_word_list, word_family, status, created_at, "
                + "updated_at FROM vocabulary_items WHERE id IN (:ids)" + (activeOnly ? " AND status='ACTIVE'" : ""), p, this::itemRow);
        List<SenseRow> senses = jdbc.query("SELECT id, vocabulary_item_id, part_of_speech, english_definition, vietnamese_meaning, "
                + "example_sentence, image_url, sort_order, status, created_at, updated_at, cefr_level, topic_id, register, "
                + "grammar_note, usage_note, example_translation FROM vocabulary_senses "
                + "WHERE vocabulary_item_id IN (:ids) AND status='ACTIVE' ORDER BY vocabulary_item_id, sort_order, id", p, this::senseRow);
        List<UUID> senseIds = senses.stream().map(SenseRow::id).toList();
        Map<UUID, VocabularyTopicResult> topics = topicsById(senses.stream().map(SenseRow::topicId).filter(Objects::nonNull).distinct().toList());
        Map<UUID, List<CollocationResult>> collocations = bySense(collocations(senseIds), CollocationResult::senseId);
        Map<UUID, List<WordRelationResult>> relations = bySense(relations(senseIds), WordRelationResult::senseId);
        Map<UUID, List<CommonMistakeResult>> mistakes = bySense(commonMistakes(senseIds), CommonMistakeResult::senseId);
        Map<UUID, List<UsageExampleResult>> examples = bySense(examples(senseIds), UsageExampleResult::senseId);

        Map<UUID, List<CatalogSenseResult>> sensesByItem = senses.stream().map(s -> {
            List<WordRelationResult> related = relations.getOrDefault(s.id(), List.of());
            List<CollocationResult> senseCollocations = collocations.getOrDefault(s.id(), List.of());
            List<CommonMistakeResult> senseMistakes = mistakes.getOrDefault(s.id(), List.of());
            List<UsageExampleResult> senseExamples = examples.getOrDefault(s.id(), List.of());
            // The sense tab counts every related word, including RELATED entries that are not listed separately.
            CatalogCountsResult counts = new CatalogCountsResult(1, senseCollocations.size(), related.size(),
                    senseMistakes.size(), senseExamples.size());
            return new CatalogSenseResult(s.id(), s.itemId(), s.partOfSpeech(), s.englishDefinition(), s.vietnameseMeaning(),
                    s.exampleSentence(), s.imageUrl(), s.sortOrder(), s.status(), s.createdAt(), s.updatedAt(), s.cefrLevel(),
                    s.topicId(), s.register(), s.grammarNote(), s.usageNote(), s.exampleTranslation(),
                    s.topicId() == null ? null : topics.get(s.topicId()),
                    s.cefrLevel() == null ? null : s.cefrLevel().bandRange(),
                    senseCollocations, ofKind(related, RelationKind.SYNONYM), ofKind(related, RelationKind.ANTONYM),
                    senseMistakes, senseExamples, counts);
        }).collect(Collectors.groupingBy(CatalogSenseResult::vocabularyItemId));

        Map<UUID, CatalogItemResult> byId = new HashMap<>();
        for (ItemRow item : items) {
            byId.put(item.id(), toItem(item, sensesByItem.getOrDefault(item.id(), List.of())));
        }
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private static CatalogItemResult toItem(ItemRow item, List<CatalogSenseResult> senses) {
        return new CatalogItemResult(item.id(), item.lemma(), item.normalizedLemma(), item.ipa(),
                item.pronunciationAudioReference(),
                item.ipaUk() != null ? item.ipaUk() : item.ipa(), item.ipaUs(),
                item.audioUkReference() != null ? item.audioUkReference() : item.pronunciationAudioReference(),
                item.audioUsReference(), item.frequencyRank(), item.academicWordList(), item.wordFamily(), item.status(),
                item.createdAt(), item.updatedAt(), senses, itemCounts(senses));
    }

    private static CatalogItemResult withSenses(CatalogItemResult item, List<CatalogSenseResult> senses) {
        return new CatalogItemResult(item.id(), item.lemma(), item.normalizedLemma(), item.ipa(),
                item.pronunciationAudioReference(), item.ipaUk(), item.ipaUs(), item.audioUkReference(),
                item.audioUsReference(), item.frequencyRank(), item.academicWordList(), item.wordFamily(), item.status(),
                item.createdAt(), item.updatedAt(), senses, itemCounts(senses));
    }

    /** The word tabs count synonyms and antonyms only, as the thesaurus tab lists them. */
    private static CatalogCountsResult itemCounts(List<CatalogSenseResult> senses) {
        return new CatalogCountsResult(senses.size(),
                senses.stream().mapToInt(s -> s.collocations().size()).sum(),
                senses.stream().mapToInt(s -> s.synonyms().size() + s.antonyms().size()).sum(),
                senses.stream().mapToInt(s -> s.commonMistakes().size()).sum(),
                senses.stream().mapToInt(s -> s.examples().size()).sum());
    }

    private Map<UUID, VocabularyTopicResult> topicsById(List<UUID> topicIds) {
        if (topicIds.isEmpty()) {
            return Map.of();
        }
        return jdbc.query("SELECT " + TOPIC_COLUMNS + " FROM vocabulary_topics t WHERE t.id IN (:ids)",
                        new MapSqlParameterSource("ids", topicIds), (rs, n) -> topic(rs, null, false))
                .stream().collect(Collectors.toMap(VocabularyTopicResult::id, Function.identity()));
    }

    private List<CollocationResult> collocations(Collection<UUID> senseIds) {
        return children("SELECT id, sense_id, phrase, meaning_vi, example, sort_order FROM vocabulary_collocations", senseIds,
                (rs, n) -> new CollocationResult(uuid(rs, "id"), uuid(rs, "sense_id"), rs.getString("phrase"),
                        rs.getString("meaning_vi"), rs.getString("example"), rs.getInt("sort_order")));
    }

    private List<WordRelationResult> relations(Collection<UUID> senseIds) {
        return children("SELECT id, sense_id, related_lemma, related_item_id, kind, sort_order FROM vocabulary_relations", senseIds,
                (rs, n) -> new WordRelationResult(uuid(rs, "id"), uuid(rs, "sense_id"), rs.getString("related_lemma"),
                        uuid(rs, "related_item_id"), RelationKind.valueOf(rs.getString("kind")), rs.getInt("sort_order")));
    }

    private List<CommonMistakeResult> commonMistakes(Collection<UUID> senseIds) {
        return children("SELECT id, sense_id, wrong_usage, correct_usage, explanation, sort_order FROM vocabulary_common_mistakes", senseIds,
                (rs, n) -> new CommonMistakeResult(uuid(rs, "id"), uuid(rs, "sense_id"), rs.getString("wrong_usage"),
                        rs.getString("correct_usage"), rs.getString("explanation"), rs.getInt("sort_order")));
    }

    private List<UsageExampleResult> examples(Collection<UUID> senseIds) {
        return children("SELECT id, sense_id, sentence, translation_vi, sort_order FROM vocabulary_examples", senseIds,
                (rs, n) -> new UsageExampleResult(uuid(rs, "id"), uuid(rs, "sense_id"), rs.getString("sentence"),
                        rs.getString("translation_vi"), rs.getInt("sort_order")));
    }

    private <T> List<T> children(String select, Collection<UUID> senseIds, RowMapper<T> mapper) {
        if (senseIds.isEmpty()) {
            return List.of();
        }
        return jdbc.query(select + " WHERE sense_id IN (:ids) ORDER BY sense_id, sort_order, id",
                new MapSqlParameterSource("ids", senseIds), mapper);
    }

    private static <T> Map<UUID, List<T>> bySense(List<T> rows, Function<T, UUID> senseId) {
        return rows.stream().collect(Collectors.groupingBy(senseId));
    }

    private static List<WordRelationResult> ofKind(List<WordRelationResult> relations, RelationKind kind) {
        return relations.stream().filter(r -> r.kind() == kind).toList();
    }

    private VocabularyTopicResult topic(ResultSet rs, Long wordCount, boolean admin) throws SQLException {
        String coverKey = rs.getString("cover_key");
        return new VocabularyTopicResult(uuid(rs, "id"), rs.getString("slug"), rs.getString("name"), rs.getString("name_vi"),
                coverKey, storage.publicUrl(coverKey), rs.getInt("sort_order"), wordCount,
                admin ? ContentStatus.valueOf(rs.getString("status")) : null,
                admin ? instant(rs, "created_at") : null, admin ? instant(rs, "updated_at") : null);
    }

    private VocabularySetResult set(ResultSet rs, Long topicCount, Long wordCount, List<VocabularyTopicResult> topics,
                                    boolean admin) throws SQLException {
        String coverKey = rs.getString("cover_key");
        String cefr = rs.getString("cefr_level");
        CefrLevel level = cefr == null ? null : CefrLevel.valueOf(cefr);
        return new VocabularySetResult(uuid(rs, "id"), rs.getString("title"), rs.getString("description"),
                VocabularySetKind.valueOf(rs.getString("kind")), level, uuid(rs, "topic_id"), rs.getString("topic_name"),
                rs.getString("topic_name_vi"), coverKey, storage.publicUrl(coverKey), rs.getInt("sort_order"),
                admin ? ContentStatus.valueOf(rs.getString("status")) : null,
                level == null ? null : level.bandRange(), topicCount, wordCount, topics,
                admin ? instant(rs, "created_at") : null, admin ? instant(rs, "updated_at") : null);
    }

    private ItemRow itemRow(ResultSet rs, int n) throws SQLException {
        return new ItemRow(uuid(rs, "id"), rs.getString("lemma"), rs.getString("normalized_lemma"), rs.getString("ipa"),
                rs.getString("pronunciation_audio_reference"), rs.getString("ipa_uk"), rs.getString("ipa_us"),
                rs.getString("audio_uk_reference"), rs.getString("audio_us_reference"),
                rs.getObject("frequency_rank", Integer.class),
                rs.getBoolean("academic_word_list"), rs.getString("word_family"), ContentStatus.valueOf(rs.getString("status")),
                instant(rs, "created_at"), instant(rs, "updated_at"));
    }

    private SenseRow senseRow(ResultSet rs, int n) throws SQLException {
        String cefr = rs.getString("cefr_level");
        return new SenseRow(uuid(rs, "id"), uuid(rs, "vocabulary_item_id"), PartOfSpeech.valueOf(rs.getString("part_of_speech")),
                rs.getString("english_definition"), rs.getString("vietnamese_meaning"), rs.getString("example_sentence"),
                rs.getString("image_url"), rs.getInt("sort_order"), ContentStatus.valueOf(rs.getString("status")),
                instant(rs, "created_at"), instant(rs, "updated_at"), cefr == null ? null : CefrLevel.valueOf(cefr),
                uuid(rs, "topic_id"), rs.getString("register"), rs.getString("grammar_note"), rs.getString("usage_note"),
                rs.getString("example_translation"));
    }

    private record ItemRow(UUID id, String lemma, String normalizedLemma, String ipa, String pronunciationAudioReference,
                           String ipaUk, String ipaUs, String audioUkReference, String audioUsReference,
                           Integer frequencyRank, boolean academicWordList, String wordFamily, ContentStatus status,
                           Instant createdAt, Instant updatedAt) {
    }

    private record SenseRow(UUID id, UUID itemId, PartOfSpeech partOfSpeech, String englishDefinition,
                            String vietnameseMeaning, String exampleSentence, String imageUrl, int sortOrder,
                            ContentStatus status, Instant createdAt, Instant updatedAt, CefrLevel cefrLevel, UUID topicId,
                            String register, String grammarNote, String usageNote, String exampleTranslation) {
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    private static long offset(PageQuery page) {
        return (long) page.page() * page.size();
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
