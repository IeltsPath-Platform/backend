package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.query.NotebookQuery;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import com.ieltspath.library.infrastructure.adapter.VocabularyNotebookJdbcAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(VocabularyNotebookJdbcAdapter.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {"spring.flyway.enabled=true", "spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class VocabularyNotebookJdbcIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired VocabularyNotebookJdbcAdapter adapter;

    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test void filtersAndPagesInSqlAndAggregatesSummary() {
        UUID user = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        UUID sense = UUID.randomUUID();
        UUID card = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-26T10:00:00Z");
        jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status) values(?,?,?,'ACTIVE')", item, "apple", "apple");
        jdbc.update("insert into vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,english_definition,example_sentence,status) values(?,?,'NOUN',?,?,?,'ACTIVE')",
                sense, item, "quả táo", "a fruit", "An apple.");
        jdbc.update("insert into flashcards(id,user_id,source_type,vocabulary_sense_id,source_reference_id,front,back,status,leitner_box,created_at,next_review_at,last_reviewed_at,review_count) values(?,?,'VOCABULARY_SENSE',?,?,?,?,'ACTIVE',3,?,?,?,2)",
                card, user, sense, sense, "apple", "quả táo", java.sql.Timestamp.from(now.minusSeconds(2 * 86400L)),
                java.sql.Timestamp.from(now.minusSeconds(1)), java.sql.Timestamp.from(now.minusSeconds(3600)));

        var page = adapter.findEntries(user, new NotebookQuery("app", List.of(UnderstandingLevel.LEARNING), true, "WEAKEST", 0, 10), now);
        assertEquals(1, page.total());
        assertEquals("apple", page.items().getFirst().lemma());
        assertEquals(UnderstandingLevel.LEARNING, page.items().getFirst().level());
        var summary = adapter.summarize(user, now, LocalDate.of(2026, 10, 26).atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant(),
                LocalDate.of(2026, 10, 27).atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        assertEquals(1, summary.total());
        assertEquals(1, summary.learning());
        assertEquals(1, summary.dueNow());
        assertEquals(1, summary.reviewedToday());
        assertEquals(List.of(sense), adapter.findSavedSenseIds(user, List.of(sense)));
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> jdbc.update(
                "insert into flashcards(id,user_id,source_type,vocabulary_sense_id,front,back,status) values(?,?,'VOCABULARY_SENSE',?,?,?,'ACTIVE')",
                UUID.randomUUID(), user, sense, "apple", "meaning"));
    }
}
