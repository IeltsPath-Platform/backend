package com.ieltspath.library.infrastructure.persistence;
import com.ieltspath.library.infrastructure.persistence.adapter.WordlyVocabularyAdapter;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;
/** The seeded catalogue must let a game be played on every level and topic, at the longest usual match of 20 words. */
@Testcontainers(disabledWithoutDocker=true)
class GameWordSeedPostgresTest {
    @Container static PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:15-alpine");
    @Test void everyLevelAndTopicHasEnoughWords() {
        var ds=new DriverManagerDataSource(db.getJdbcUrl(),db.getUsername(),db.getPassword());
        Flyway.configure().dataSource(ds).load().migrate();
        var sql=new JdbcTemplate(ds);
        var adapter=new WordlyVocabularyAdapter(sql);
        var topics=adapter.topics();
        assertEquals(7,topics.size());
        var availability=adapter.availability();
        var cells=availability.stream().filter(a->a.cefrLevel()!=null&&a.topicId()!=null).toList();
        assertEquals(42,cells.size());
        assertTrue(cells.stream().allMatch(a->a.words()>=20),()->cells.stream().filter(a->a.words()<20).toList().toString());
        // Totals are separate rows: per level, per topic and overall, and each one covers its cells.
        var total=availability.stream().filter(a->a.cefrLevel()==null&&a.topicId()==null).toList();
        assertEquals(1,total.size());
        assertTrue(total.get(0).words()>=840);
        assertEquals(6,availability.stream().filter(a->a.cefrLevel()!=null&&a.topicId()==null).count());
        assertEquals(7,availability.stream().filter(a->a.cefrLevel()==null&&a.topicId()!=null).count());
        // The count has to agree with what a game actually receives.
        var cell=cells.get(0);
        var words=adapter.page("",500,cell.cefrLevel(),cell.topicId());
        assertEquals(cell.words(),words.size());
        assertTrue(words.stream().allMatch(w->w.answer().matches("[A-Z]+")&&!w.meaning().isBlank()));
        assertEquals(0,sql.queryForObject("""
            SELECT count(*) FROM vocabulary_senses s LEFT JOIN vocabulary_items i ON i.id=s.vocabulary_item_id
            WHERE s.cefr_level IS NOT NULL AND (i.id IS NULL OR btrim(s.example_sentence)='')
            """,Integer.class));
    }
}
