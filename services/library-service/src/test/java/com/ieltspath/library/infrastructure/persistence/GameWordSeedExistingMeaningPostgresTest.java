package com.ieltspath.library.infrastructure.persistence;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;
/** A database can already hold seeded words with a meaning of their own but no level or topic. */
@Testcontainers(disabledWithoutDocker=true)
class GameWordSeedExistingMeaningPostgresTest {
    @Container static PostgreSQLContainer<?> db=new PostgreSQLContainer<>("postgres:15-alpine");
    @Test void existingMeaningKeepsItsTextAndGainsLevelAndTopic() {
        var ds=new DriverManagerDataSource(db.getJdbcUrl(),db.getUsername(),db.getPassword());
        Flyway.configure().dataSource(ds).target(MigrationVersion.fromVersion("23")).load().migrate();
        var sql=new JdbcTemplate(ds);
        sql.update("INSERT INTO vocabulary_items(lemma,normalized_lemma) SELECT 'tree','tree' WHERE NOT EXISTS (SELECT 1 FROM vocabulary_items WHERE normalized_lemma='tree')");
        var id=sql.queryForObject("SELECT id FROM vocabulary_items WHERE normalized_lemma='tree'",java.util.UUID.class);
        sql.update("INSERT INTO vocabulary_senses(vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence) VALUES (?,'NOUN','cây của admin','x')",id);
        Flyway.configure().dataSource(ds).load().migrate();
        var row=sql.queryForMap("SELECT s.vietnamese_meaning,s.cefr_level,t.slug FROM vocabulary_senses s JOIN vocabulary_topics t ON t.id=s.topic_id WHERE s.vocabulary_item_id=?",id);
        assertEquals("cây của admin",row.get("vietnamese_meaning"));
        assertEquals("A1",row.get("cefr_level"));
        assertEquals("environment",row.get("slug"));
    }
}
