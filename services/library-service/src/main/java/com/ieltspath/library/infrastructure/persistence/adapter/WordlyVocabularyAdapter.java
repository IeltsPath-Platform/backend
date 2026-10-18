package com.ieltspath.library.infrastructure.persistence.adapter;
import com.ieltspath.library.domain.repository.WordlyVocabularyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
@Repository
public class WordlyVocabularyAdapter implements WordlyVocabularyRepository {
    private final JdbcTemplate jdbc;
    public WordlyVocabularyAdapter(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Word> page(String after,int limit) {
        return jdbc.query("""
            SELECT source_id,answer,meaning FROM (
                SELECT DISTINCT ON (upper(btrim(v.lemma))) s.id AS source_id,upper(btrim(v.lemma)) AS answer,s.vietnamese_meaning AS meaning
                FROM vocabulary_items v JOIN vocabulary_senses s ON s.vocabulary_item_id=v.id
                WHERE v.status='ACTIVE' AND s.status='ACTIVE' AND btrim(v.lemma) ~ '^[A-Za-z]{1,100}$'
                  AND length(btrim(s.vietnamese_meaning))>0
                ORDER BY upper(btrim(v.lemma)),s.sort_order,s.id
            ) words WHERE answer>? ORDER BY answer LIMIT ?
            """,(rs,n)->new Word(rs.getObject("source_id",UUID.class),rs.getString("answer"),rs.getString("meaning")),after,limit);
    }
    public List<Word> random(int count) {
        return jdbc.query("""
            SELECT source_id, answer, meaning FROM (
                SELECT DISTINCT ON (upper(btrim(v.lemma))) s.id AS source_id,
                    upper(btrim(v.lemma)) AS answer, s.vietnamese_meaning AS meaning
                FROM vocabulary_items v JOIN vocabulary_senses s ON s.vocabulary_item_id=v.id
                WHERE v.status='ACTIVE' AND s.status='ACTIVE'
                    AND btrim(v.lemma) ~ '^[A-Za-z]{1,100}$'
                    AND length(btrim(s.vietnamese_meaning))>0
                ORDER BY upper(btrim(v.lemma)), s.sort_order, s.id
            ) words ORDER BY random() LIMIT ?
            """, (rs,n)->new Word(rs.getObject("source_id",UUID.class),rs.getString("answer"),rs.getString("meaning")),count);
    }
}
