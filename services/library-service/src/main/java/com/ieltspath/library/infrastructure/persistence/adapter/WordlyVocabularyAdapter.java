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
        return page(after,limit,null,null);
    }
    public List<Word> page(String after,int limit,String cefrLevel,UUID topicId) {
        return jdbc.query("""
            SELECT source_id,answer,meaning FROM (
                SELECT DISTINCT ON (upper(btrim(v.lemma))) s.id AS source_id,upper(btrim(v.lemma)) AS answer,s.vietnamese_meaning AS meaning
                FROM vocabulary_items v JOIN vocabulary_senses s ON s.vocabulary_item_id=v.id
                WHERE v.status='ACTIVE' AND s.status='ACTIVE' AND btrim(v.lemma) ~ '^[A-Za-z]{1,100}$'
                  AND length(btrim(s.vietnamese_meaning))>0
                  AND (?::varchar IS NULL OR s.cefr_level=?) AND (?::uuid IS NULL OR s.topic_id=?)
                ORDER BY upper(btrim(v.lemma)),s.sort_order,s.id
            ) words WHERE answer>? ORDER BY answer LIMIT ?
            """,(rs,n)->new Word(rs.getObject("source_id",UUID.class),rs.getString("answer"),rs.getString("meaning")),
            cefrLevel,cefrLevel,topicId,topicId,after,limit);
    }
    public List<Topic> topics() {
        return jdbc.query("SELECT id,slug,name,name_vi FROM vocabulary_topics WHERE status='ACTIVE' ORDER BY sort_order,id",
            (rs,n)->new Topic(rs.getObject("id",UUID.class),rs.getString("slug"),rs.getString("name"),rs.getString("name_vi")));
    }
    public List<Availability> availability() {
        // GROUPING tells a total row apart from words that have no level or topic, which belong to no cell.
        return jdbc.query("""
            SELECT s.cefr_level,s.topic_id,count(DISTINCT upper(btrim(v.lemma))) AS words
            FROM vocabulary_items v JOIN vocabulary_senses s ON s.vocabulary_item_id=v.id
            LEFT JOIN vocabulary_topics t ON t.id=s.topic_id
            WHERE v.status='ACTIVE' AND s.status='ACTIVE' AND btrim(v.lemma) ~ '^[A-Za-z]{1,100}$'
              AND length(btrim(s.vietnamese_meaning))>0 AND (s.topic_id IS NULL OR t.status='ACTIVE')
            GROUP BY GROUPING SETS ((s.cefr_level,s.topic_id),(s.cefr_level),(s.topic_id),())
            HAVING (GROUPING(s.cefr_level)=1 OR s.cefr_level IS NOT NULL)
               AND (GROUPING(s.topic_id)=1 OR s.topic_id IS NOT NULL)
            """,(rs,n)->new Availability(rs.getString("cefr_level"),rs.getObject("topic_id",UUID.class),rs.getInt("words")));
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
