package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.result.SavedFlashcard;
import com.ieltspath.library.application.usecase.SavePracticeQuestionFlashcardUseCase;
import com.ieltspath.library.domain.aggregate.Flashcard;
import com.ieltspath.library.domain.exception.ConflictException;
import com.ieltspath.library.domain.repository.FlashcardRepository;
import com.ieltspath.library.domain.vo.FlashcardSourceType;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.OwnedPage;
import com.ieltspath.library.infrastructure.adapter.FlashcardRepositoryAdapter;
import com.ieltspath.library.infrastructure.persistence.mapper.FlashcardMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each save commits on its own, as in production, so the partial unique index is really exercised. */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({FlashcardRepositoryAdapter.class, FlashcardMapperImpl.class, SavePracticeQuestionFlashcardUseCase.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class PracticeQuestionFlashcardPersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private FlashcardRepository cards;
    @Autowired
    private SavePracticeQuestionFlashcardUseCase savePracticeQuestion;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static Flashcard practiceCard(UUID userId, UUID questionId) {
        return Flashcard.create(userId, FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "Front", "Back");
    }

    @Test
    void savingTheSameQuestionTwiceKeepsOneCard() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();

        SavedFlashcard first = savePracticeQuestion.execute(userId, questionId, "Front", "Back");
        SavedFlashcard second = savePracticeQuestion.execute(userId, questionId, "Front again", "Back again");

        assertTrue(first.created());
        assertFalse(second.created());
        assertEquals(first.flashcard().id(), second.flashcard().id());
        assertEquals(1, cards.findByUserIdAndStatus(userId, LibraryStatus.ACTIVE, 0, 20).total());
    }

    @Test
    void theIndexRejectsASecondLiveCardButAllowsOneAfterDeletion() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Flashcard first = cards.save(practiceCard(userId, questionId));

        assertThrows(ConflictException.class, () -> cards.save(practiceCard(userId, questionId)));

        first.update(FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "Front", "Back",
                LibraryStatus.DELETED);
        cards.save(first);
        SavedFlashcard again = savePracticeQuestion.execute(userId, questionId, "Front", "Back");
        assertTrue(again.created());
    }

    @Test
    void savingAnArchivedCardAgainBringsItBackToActive() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Flashcard archived = cards.save(practiceCard(userId, questionId));
        archived.update(FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "Front", "Back",
                LibraryStatus.ARCHIVED);
        cards.save(archived);

        SavedFlashcard saved = savePracticeQuestion.execute(userId, questionId, "Front again", "Back again");

        assertFalse(saved.created());
        assertEquals(archived.getId(), saved.flashcard().id());
        assertEquals(LibraryStatus.ACTIVE, saved.flashcard().status());
        assertEquals("Front", saved.flashcard().front());
        var active = cards.findByUserIdAndStatus(userId, LibraryStatus.ACTIVE, 0, 20);
        assertEquals(1, active.total());
        assertEquals(archived.getId(), active.items().getFirst().getId());
    }

    @Test
    void aSaveThatLosesTheRaceReadsTheWinnerBackFromTheDatabase() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Flashcard winner = cards.save(practiceCard(userId, questionId));
        // The first lookup misses the winner, as when both requests look before either has committed.
        AtomicBoolean missedOnce = new AtomicBoolean();
        FlashcardRepository racing = new FlashcardRepository() {
            @Override
            public Flashcard save(Flashcard flashcard) {
                return cards.save(flashcard);
            }

            @Override
            public Optional<Flashcard> findByIdAndUserId(UUID id, UUID owner) {
                return cards.findByIdAndUserId(id, owner);
            }

            @Override
            public OwnedPage<Flashcard> findByUserIdAndStatus(UUID owner, LibraryStatus status, int page, int size) {
                return cards.findByUserIdAndStatus(owner, status, page, size);
            }

            @Override
            public Optional<Flashcard> findLivePracticeQuestionCard(UUID owner, UUID practiceQuestionId) {
                return missedOnce.compareAndSet(false, true)
                        ? Optional.empty()
                        : cards.findLivePracticeQuestionCard(owner, practiceQuestionId);
            }
        };

        SavedFlashcard saved = new SavePracticeQuestionFlashcardUseCase(racing).execute(userId, questionId, "Front", "Back");

        assertFalse(saved.created());
        assertEquals(winner.getId(), saved.flashcard().id());
        assertEquals(1, cards.findByUserIdAndStatus(userId, LibraryStatus.ACTIVE, 0, 20).total());
    }

    @Test
    void updatesThatWouldCreateASecondLiveCardAreConflicts() {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        cards.save(practiceCard(userId, questionId));

        // Turning another card into a copy of the live one.
        Flashcard manual = cards.save(Flashcard.create(
                userId, FlashcardSourceType.MANUAL, null, null, null, "Front", "Back"));
        manual.update(FlashcardSourceType.PRACTICE_QUESTION, null, questionId, null, "Front", "Back",
                LibraryStatus.ACTIVE);
        assertThrows(ConflictException.class, () -> cards.save(manual));

        // Restoring a deleted card while a newer live card exists.
        UUID otherQuestion = UUID.randomUUID();
        Flashcard deleted = cards.save(practiceCard(userId, otherQuestion));
        deleted.update(FlashcardSourceType.PRACTICE_QUESTION, null, otherQuestion, null, "Front", "Back",
                LibraryStatus.DELETED);
        cards.save(deleted);
        cards.save(practiceCard(userId, otherQuestion));
        deleted.update(FlashcardSourceType.PRACTICE_QUESTION, null, otherQuestion, null, "Front", "Back",
                LibraryStatus.ACTIVE);
        assertThrows(ConflictException.class, () -> cards.save(deleted));
    }

    @Test
    void otherLearnersAndOtherSourcesAreIndependent() {
        UUID questionId = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        cards.save(practiceCard(owner, questionId));

        assertTrue(savePracticeQuestion.execute(UUID.randomUUID(), questionId, "Front", "Back").created());
        // Highlights pointing at the same id are not practice question cards, so the index does not limit them.
        cards.save(Flashcard.create(owner, FlashcardSourceType.HIGHLIGHT, null, questionId, "text", "Front", "Back"));
        cards.save(Flashcard.create(owner, FlashcardSourceType.HIGHLIGHT, null, questionId, "text", "Front", "Back"));
        assertEquals(3, cards.findByUserIdAndStatus(owner, LibraryStatus.ACTIVE, 0, 20).total());
    }
}
