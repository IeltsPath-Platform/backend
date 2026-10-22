package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardDeckItemJpaEntity;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardDeckJpaEntity;
import com.ieltspath.library.infrastructure.persistence.entity.FlashcardJpaEntity;
import com.ieltspath.library.infrastructure.persistence.repository.DeckCardCountProjection;
import com.ieltspath.library.infrastructure.persistence.repository.FlashcardDeckItemJpaRepository;
import com.ieltspath.library.infrastructure.persistence.repository.FlashcardDeckJpaRepository;
import com.ieltspath.library.infrastructure.persistence.repository.FlashcardJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class FlashcardImageAndDeckCountIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private FlashcardJpaRepository cardRepository;
    @Autowired
    private FlashcardDeckJpaRepository deckRepository;
    @Autowired
    private FlashcardDeckItemJpaRepository itemRepository;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void savesAndReadsFlashcardImageKeyAndExampleSentence() {
        UUID cardId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        FlashcardJpaEntity entity = new FlashcardJpaEntity();
        entity.setId(cardId);
        entity.setUserId(userId);
        entity.setSourceType(FlashcardSourceType.MANUAL);
        entity.setFront("benevolent");
        entity.setBack("well meaning and kindly");
        entity.setStatus(LibraryStatus.ACTIVE);
        entity.setLeitnerBox((short) 1);
        entity.setReviewCount(0);
        entity.setImageKey("images/" + userId + "/benevolent.png");
        entity.setExampleSentence("A benevolent smile greeted the visitors.");

        cardRepository.saveAndFlush(entity);

        FlashcardJpaEntity loaded = cardRepository.findById(cardId).orElseThrow();
        assertEquals("images/" + userId + "/benevolent.png", loaded.getImageKey());
        assertEquals("A benevolent smile greeted the visitors.", loaded.getExampleSentence());
    }

    @Test
    void savesAndReadsDeckCoverImageKey() {
        UUID deckId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        FlashcardDeckJpaEntity entity = new FlashcardDeckJpaEntity();
        entity.setId(deckId);
        entity.setUserId(userId);
        entity.setName("IELTS Academic");
        entity.setDescription("Vocabulary for Academic test");
        entity.setStatus(LibraryStatus.ACTIVE);
        entity.setCoverImageKey("images/" + userId + "/academic-cover.webp");

        deckRepository.saveAndFlush(entity);

        FlashcardDeckJpaEntity loaded = deckRepository.findById(deckId).orElseThrow();
        assertEquals("images/" + userId + "/academic-cover.webp", loaded.getCoverImageKey());
    }

    @Test
    void countsCardsAndDueCardsForMultipleDecksInSingleQuery() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        // Deck 1
        UUID deck1Id = UUID.randomUUID();
        FlashcardDeckJpaEntity deck1 = new FlashcardDeckJpaEntity(deck1Id, userId, "Deck 1", "Desc 1",
                LibraryStatus.ACTIVE, now, now, null);
        deckRepository.save(deck1);

        // Deck 2
        UUID deck2Id = UUID.randomUUID();
        FlashcardDeckJpaEntity deck2 = new FlashcardDeckJpaEntity(deck2Id, userId, "Deck 2", "Desc 2",
                LibraryStatus.ACTIVE, now, now, null);
        deckRepository.save(deck2);

        // Empty Deck 3
        UUID deck3Id = UUID.randomUUID();
        FlashcardDeckJpaEntity deck3 = new FlashcardDeckJpaEntity(deck3Id, userId, "Deck 3", "Desc 3",
                LibraryStatus.ACTIVE, now, now, null);
        deckRepository.save(deck3);

        // Cards for Deck 1:
        // Card 1: Active, nextReviewAt is null (due)
        UUID c1Id = createCard(userId, LibraryStatus.ACTIVE, null);
        addItem(deck1Id, c1Id, 1);

        // Card 2: Active, nextReviewAt in past (due)
        UUID c2Id = createCard(userId, LibraryStatus.ACTIVE, now.minus(2, ChronoUnit.DAYS));
        addItem(deck1Id, c2Id, 2);

        // Card 3: Active, nextReviewAt in future (not due)
        UUID c3Id = createCard(userId, LibraryStatus.ACTIVE, now.plus(2, ChronoUnit.DAYS));
        addItem(deck1Id, c3Id, 3);

        // Card 4: Archived (should NOT be counted)
        UUID c4Id = createCard(userId, LibraryStatus.ARCHIVED, null);
        addItem(deck1Id, c4Id, 4);

        // Cards for Deck 2:
        // Card 5: Active, nextReviewAt in future (not due)
        UUID c5Id = createCard(userId, LibraryStatus.ACTIVE, now.plus(5, ChronoUnit.DAYS));
        addItem(deck2Id, c5Id, 1);

        deckRepository.flush();
        cardRepository.flush();
        itemRepository.flush();

        List<DeckCardCountProjection> projections = itemRepository.countCardsAndDueByDeckIds(
                List.of(deck1Id, deck2Id, deck3Id), userId, now);

        Map<UUID, DeckCardCountProjection> byDeck = projections.stream()
                .collect(Collectors.toMap(DeckCardCountProjection::getDeckId, p -> p));

        // Deck 1 has 3 active cards, 2 are due
        assertTrue(byDeck.containsKey(deck1Id));
        assertEquals(3, byDeck.get(deck1Id).getCardCount());
        assertEquals(2, byDeck.get(deck1Id).getDueCount());

        // Deck 2 has 1 active card, 0 due
        assertTrue(byDeck.containsKey(deck2Id));
        assertEquals(1, byDeck.get(deck2Id).getCardCount());
        assertEquals(0, byDeck.get(deck2Id).getDueCount());

        // Deck 3 has 0 cards (not present in group-by result, use case maps default ZERO)
        assertFalse(byDeck.containsKey(deck3Id));
    }

    private UUID createCard(UUID userId, LibraryStatus status, Instant nextReviewAt) {
        UUID id = UUID.randomUUID();
        FlashcardJpaEntity card = new FlashcardJpaEntity();
        card.setId(id);
        card.setUserId(userId);
        card.setSourceType(FlashcardSourceType.MANUAL);
        card.setFront("Front " + id);
        card.setBack("Back " + id);
        card.setStatus(status);
        card.setLeitnerBox((short) 1);
        card.setNextReviewAt(nextReviewAt);
        card.setReviewCount(0);
        cardRepository.save(card);
        return id;
    }

    private void addItem(UUID deckId, UUID cardId, int sortOrder) {
        FlashcardDeckItemJpaEntity item = new FlashcardDeckItemJpaEntity();
        item.setDeckId(deckId);
        item.setFlashcardId(cardId);
        item.setSortOrder(sortOrder);
        item.setAddedAt(Instant.now());
        itemRepository.save(item);
    }
}
