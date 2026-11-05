package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.VocabularySuggestionsStore;
import com.ieltspath.library.application.query.VocabularySuggestionsQuery;
import com.ieltspath.library.application.result.VocabularySuggestionsResult;
import com.ieltspath.library.application.result.VocabularySuggestionsResult.Sense;
import com.ieltspath.library.application.result.VocabularySuggestionsResult.Word;
import com.ieltspath.library.domain.vo.PartOfSpeech;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class VocabularySuggestionsJdbcAdapter implements VocabularySuggestionsStore {
    private final NamedParameterJdbcTemplate jdbc;

    public VocabularySuggestionsJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public VocabularySuggestionsResult search(VocabularySuggestionsQuery query) {
        String escaped = query.text().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        var params = new MapSqlParameterSource().addValue("query", query.text())
                .addValue("prefix", escaped + "%").addValue("contains", "%" + escaped + "%")
                .addValue("limit", query.size() + 1).addValue("offset", query.offset());
        String match = switch (query.length()) {
            case 1 -> "i.normalized_lemma = :query";
            case 2 -> "i.normalized_lemma LIKE :prefix ESCAPE '\\'";
            default -> "i.normalized_lemma LIKE :contains ESCAPE '\\'";
        };
        // Limit words before joining senses: a word with many meanings must occupy just one page slot.
        List<Word> candidates = jdbc.query("""
                SELECT i.id, i.lemma, i.ipa FROM vocabulary_items i
                WHERE i.status = 'ACTIVE' AND EXISTS (
                  SELECT 1 FROM vocabulary_senses s WHERE s.vocabulary_item_id = i.id AND s.status = 'ACTIVE'
                ) AND %s
                 ORDER BY CASE WHEN i.normalized_lemma = :query THEN 0
                   WHEN i.normalized_lemma LIKE :prefix ESCAPE '\\' THEN 1 ELSE 2 END,
                   i.normalized_lemma, i.id LIMIT :limit OFFSET :offset
                """.formatted(match), params, (rs, row) -> new Word(rs.getObject("id", UUID.class), rs.getString("lemma"),
                rs.getString("ipa"), List.of()));
        boolean hasMore = candidates.size() > query.size();
        List<Word> words = candidates.subList(0, Math.min(query.size(), candidates.size()));
        if (words.isEmpty()) return new VocabularySuggestionsResult(List.of(), query.page(), query.size(), false);

        Map<UUID, List<Sense>> senses = new HashMap<>();
        jdbc.query("""
                SELECT id, vocabulary_item_id, part_of_speech, english_definition, vietnamese_meaning
                FROM vocabulary_senses WHERE vocabulary_item_id IN (:ids) AND status = 'ACTIVE'
                ORDER BY vocabulary_item_id, sort_order, id
                """, Map.of("ids", words.stream().map(Word::id).toList()), rs -> {
            UUID itemId = rs.getObject("vocabulary_item_id", UUID.class);
            senses.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(new Sense(
                    rs.getObject("id", UUID.class), PartOfSpeech.valueOf(rs.getString("part_of_speech")),
                    rs.getString("english_definition"), rs.getString("vietnamese_meaning")));
        });
        return new VocabularySuggestionsResult(words.stream().map(word -> new Word(word.id(), word.lemma(), word.ipa(),
                List.copyOf(senses.getOrDefault(word.id(), List.of())))).toList(), query.page(), query.size(), hasMore);
    }
}
