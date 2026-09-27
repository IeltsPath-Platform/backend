package com.group01.learningsupport.infrastructure.persistence;

import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckItemJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardDeckJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.repository.FlashcardDeckItemJpaRepository;
import com.group01.learningsupport.infrastructure.persistence.repository.FlashcardDeckJpaRepository;
import com.group01.learningsupport.infrastructure.persistence.repository.FlashcardJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class FlashcardDeckRestorationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private FlashcardJpaRepository cards;
    @Autowired private FlashcardDeckJpaRepository decks;
    @Autowired private FlashcardDeckItemJpaRepository items;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void oldMembershipReappearsOnlyWhenCardAndDeckAreActive() {
        UUID userId = UUID.randomUUID();
        FlashcardJpaEntity card = new FlashcardJpaEntity();
        card.setId(UUID.randomUUID());
        card.setUserId(userId);
        card.setSourceType(FlashcardSourceType.MANUAL);
        card.setFront("front");
        card.setBack("back");
        card.setStatus(LibraryStatus.ACTIVE);
        card = cards.saveAndFlush(card);

        FlashcardDeckJpaEntity deck = new FlashcardDeckJpaEntity();
        deck.setId(UUID.randomUUID());
        deck.setUserId(userId);
        deck.setName("Deck");
        deck.setStatus(LibraryStatus.ACTIVE);
        decks.saveAndFlush(deck);

        FlashcardDeckItemJpaEntity item = new FlashcardDeckItemJpaEntity();
        item.setDeckId(deck.getId());
        item.setFlashcardId(card.getId());
        item.setSortOrder(1);
        items.saveAndFlush(item);
        assertVisibleCount(deck.getId(), userId, 1);

        card.setStatus(LibraryStatus.ARCHIVED);
        card = cards.saveAndFlush(card);
        assertVisibleCount(deck.getId(), userId, 0);
        card.setStatus(LibraryStatus.ACTIVE);
        card = cards.saveAndFlush(card);
        assertVisibleCount(deck.getId(), userId, 1);

        card.setStatus(LibraryStatus.DELETED);
        card = cards.saveAndFlush(card);
        assertVisibleCount(deck.getId(), userId, 0);

        card.setStatus(LibraryStatus.ACTIVE);
        card = cards.saveAndFlush(card);
        assertVisibleCount(deck.getId(), userId, 1);

        deck.setStatus(LibraryStatus.ARCHIVED);
        decks.saveAndFlush(deck);
        assertVisibleCount(deck.getId(), userId, 0);
        deck.setStatus(LibraryStatus.ACTIVE);
        decks.saveAndFlush(deck);
        assertVisibleCount(deck.getId(), userId, 1);

        deck.setStatus(LibraryStatus.DELETED);
        decks.saveAndFlush(deck);
        assertVisibleCount(deck.getId(), userId, 0);

        deck.setStatus(LibraryStatus.ACTIVE);
        decks.saveAndFlush(deck);
        assertVisibleCount(deck.getId(), userId, 1);
        assertEquals(1, items.count());
    }

    private void assertVisibleCount(UUID deckId, UUID userId, long expected) {
        assertEquals(expected, items.findActive(deckId, userId, PageRequest.of(0, 20)).getTotalElements());
    }
}
