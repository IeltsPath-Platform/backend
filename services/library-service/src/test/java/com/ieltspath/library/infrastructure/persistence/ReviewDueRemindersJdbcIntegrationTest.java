package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.port.ReviewDueReminders.DueLearner;
import com.ieltspath.library.infrastructure.adapter.ReviewDueRemindersJdbcAdapter;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ReviewDueRemindersJdbcAdapter.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {"spring.flyway.enabled=true", "spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class ReviewDueRemindersJdbcIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-11-16T01:00:00Z");
    private static final UUID NOBODY = new UUID(0L, 0L);

    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired ReviewDueRemindersJdbcAdapter adapter;

    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test void countsOnlyLearnedWordsThatAreDueAndSkipsLearnersBelowTheMinimum() {
        List<UUID> senses = senses(5);
        UUID due = UUID.randomUUID();
        UUID fewDue = UUID.randomUUID();
        UUID onlyNew = UUID.randomUUID();
        UUID notYetDue = UUID.randomUUID();
        for (UUID sense : senses) {
            card(due, sense, 3, 2, NOW.minusSeconds(60));
            card(onlyNew, sense, 1, 0, null);
            card(notYetDue, sense, 3, 2, NOW.plusSeconds(86400));
        }
        card(fewDue, senses.get(0), 2, 1, NOW.minusSeconds(60));
        card(fewDue, senses.get(1), 2, 1, NOW.minusSeconds(60));

        List<DueLearner> found = adapter.findDueLearners(NOW, 5, NOBODY, 100);

        assertEquals(List.of(new DueLearner(due, 5)), found);
        assertEquals(2, adapter.findDueLearners(NOW, 2, NOBODY, 100).stream()
                .filter(l -> l.userId().equals(due) || l.userId().equals(fewDue)).count());
    }

    @Test void pagesByUserIdWithoutRepeatingOrSkippingLearners() {
        UUID sense = senses(1).get(0);
        List<UUID> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID user = UUID.randomUUID();
            users.add(user);
            card(user, sense, 3, 2, NOW.minusSeconds(60));
        }
        // PostgreSQL orders uuid as unsigned bytes, which is the order of the lowercase hex text; UUID.compareTo is signed.
        List<UUID> sorted = users.stream().sorted(java.util.Comparator.comparing(UUID::toString)).toList();

        List<UUID> seen = new ArrayList<>();
        UUID after = NOBODY;
        for (List<DueLearner> page = adapter.findDueLearners(NOW, 1, after, 2); !page.isEmpty();
             page = adapter.findDueLearners(NOW, 1, after, 2)) {
            page.forEach(l -> seen.add(l.userId()));
            after = page.get(page.size() - 1).userId();
        }

        assertEquals(seen.size(), seen.stream().distinct().count(), "no learner is returned twice");
        assertEquals(sorted, seen.stream().filter(users::contains).toList());
    }

    @Test void queuingTheSameEventTwiceStoresItOnce() {
        UUID eventId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        String payload = "{\"userId\":\"" + user + "\",\"dueCount\":7}";

        assertTrue(adapter.queue(eventId, user, payload, NOW));
        assertFalse(adapter.queue(eventId, user, payload, NOW));

        assertEquals(1, jdbc.queryForObject("select count(*) from outbox_events where id = ?", Integer.class, eventId));
        assertEquals("PENDING", jdbc.queryForObject("select status from outbox_events where id = ?", String.class, eventId));
        assertEquals(7, jdbc.queryForObject("select (payload ->> 'dueCount')::int from outbox_events where id = ?",
                Integer.class, eventId));
    }

    private List<UUID> senses(int count) {
        List<UUID> senses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID item = UUID.randomUUID();
            UUID sense = UUID.randomUUID();
            String lemma = "word" + UUID.randomUUID();
            jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status) values(?,?,?,'ACTIVE')", item, lemma, lemma);
            jdbc.update("insert into vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,english_definition,example_sentence,status) values(?,?,'NOUN',?,?,?,'ACTIVE')",
                    sense, item, "nghĩa", "meaning", "Example.");
            senses.add(sense);
        }
        return senses;
    }

    private void card(UUID user, UUID sense, int box, int reviewCount, Instant nextReviewAt) {
        jdbc.update("insert into flashcards(id,user_id,source_type,vocabulary_sense_id,front,back,status,leitner_box,review_count,next_review_at) values(?,?,'VOCABULARY_SENSE',?,?,?,'ACTIVE',?,?,?)",
                UUID.randomUUID(), user, sense, "front", "back", box, reviewCount,
                nextReviewAt == null ? null : Timestamp.from(nextReviewAt));
    }
}
