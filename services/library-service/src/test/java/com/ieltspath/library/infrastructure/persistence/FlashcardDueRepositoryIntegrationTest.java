package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.port.FlashcardVocabularyMetadataReader;
import com.ieltspath.library.infrastructure.adapter.FlashcardVocabularyMetadataAdapter;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardJpaEntity;
import com.ieltspath.library.infrastructure.persistence.repository.FlashcardJpaRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(FlashcardVocabularyMetadataAdapter.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class FlashcardDueRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private FlashcardJpaRepository cards;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private FlashcardVocabularyMetadataReader vocabularyMetadataReader;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void dueCardsAreOwnerScopedOrderedAndPaginatedInTheDatabase() {
        UUID user = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-17T00:00:00Z");
        UUID notReviewed = insertCard(user, "ACTIVE", null);
        UUID overdue = insertCard(user, "ACTIVE", now.minusSeconds(3600));
        UUID dueNow = insertCard(user, "ACTIVE", now);
        insertCard(user, "ACTIVE", now.plusSeconds(60));
        insertCard(user, "ARCHIVED", null);
        insertCard(otherUser, "ACTIVE", null);

        var first = cards.findDueCards(user, null, now, LibraryStatus.ACTIVE, PageRequest.of(0, 2));
        var second = cards.findDueCards(user, null, now, LibraryStatus.ACTIVE, PageRequest.of(1, 2));

        assertEquals(List.of(notReviewed, overdue), first.getContent().stream().map(FlashcardJpaEntity::getId).toList());
        assertEquals(List.of(dueNow), second.getContent().stream().map(FlashcardJpaEntity::getId).toList());
        assertEquals(3, first.getTotalElements());
    }

    @Test
    void dueCardsCanBeLimitedToAnOwnedActiveDeck() {
        UUID user = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID deck = UUID.randomUUID();
        UUID otherDeck = UUID.randomUUID();
        UUID included = insertCard(user, "ACTIVE", null);
        UUID notInDeck = insertCard(user, "ACTIVE", null);
        UUID foreignCard = insertCard(otherUser, "ACTIVE", null);
        insertDeck(deck, user, "ACTIVE");
        insertDeck(otherDeck, otherUser, "ACTIVE");
        addToDeck(deck, included);
        addToDeck(deck, foreignCard);
        addToDeck(otherDeck, notInDeck);

        var page = cards.findDueCards(user, deck, Instant.now(), LibraryStatus.ACTIVE, PageRequest.of(0, 10));
        var foreignDeckPage = cards.findDueCards(user, otherDeck, Instant.now(), LibraryStatus.ACTIVE,
                PageRequest.of(0, 10));

        assertEquals(List.of(included), page.getContent().stream().map(FlashcardJpaEntity::getId).toList());
        assertEquals(0, foreignDeckPage.getTotalElements());
    }

    @Test
    void vocabularyMetadataIsReadForManySenseIdsInOneBatch() {
        UUID firstItem = insertVocabularyItem("first", "fəːst", "audio/first.mp3");
        UUID secondItem = insertVocabularyItem("second", "ˈsekənd", "audio/second.mp3");
        UUID firstSense = insertVocabularySense(firstItem, "first example");
        UUID secondSense = insertVocabularySense(secondItem, "second example");

        var metadata = vocabularyMetadataReader.findBySenseIds(List.of(firstSense, secondSense));

        assertEquals(2, metadata.size());
        assertEquals("fəːst", metadata.get(firstSense).ipa());
        assertEquals("audio/first.mp3", metadata.get(firstSense).pronunciationAudioReference());
        assertEquals("second example", metadata.get(secondSense).exampleSentence());
    }

    private UUID insertCard(UUID user, String status, Instant nextReviewAt) {
        UUID id = UUID.randomUUID();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO "
                    + "flashcards(id,user_id,source_type,front,back,status,next_review_at) VALUES (?,?,?,?,?,?,?)");
            statement.setObject(1, id);
            statement.setObject(2, user);
            statement.setString(3, "MANUAL");
            statement.setString(4, "Front");
            statement.setString(5, "Back");
            statement.setString(6, status);
            if (nextReviewAt == null) {
                statement.setNull(7, Types.TIMESTAMP_WITH_TIMEZONE);
            } else {
                statement.setObject(7, nextReviewAt.atOffset(ZoneOffset.UTC));
            }
            return statement;
        });
        return id;
    }

    private void insertDeck(UUID id, UUID user, String status) {
        jdbc.update("INSERT INTO flashcard_decks(id,user_id,name,status) VALUES (?,?,?,?)",
                id, user, "Deck " + id, status);
    }

    private void addToDeck(UUID deckId, UUID cardId) {
        jdbc.update("INSERT INTO flashcard_deck_items(deck_id,flashcard_id,sort_order) VALUES (?,?,1)", deckId, cardId);
    }

    private UUID insertVocabularyItem(String lemma, String ipa, String audio) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vocabulary_items(id,lemma,normalized_lemma,ipa,pronunciation_audio_reference) "
                + "VALUES (?,?,?,?,?)", id, lemma, lemma, ipa, audio);
        return id;
    }

    private UUID insertVocabularySense(UUID itemId, String example) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence) "
                + "VALUES (?,?,?,?,?)", id, itemId, "NOUN", "meaning", example);
        return id;
    }
}
