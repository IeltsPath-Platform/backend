package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.command.CreateVocabularyItemCommand;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.usecase.CreateVocabularyItemUseCase;
import com.ieltspath.library.domain.catalog.VocabularyItemDetails;
import com.ieltspath.library.domain.exception.DuplicateCodeException;
import com.ieltspath.library.infrastructure.adapter.VocabularyAudioUrlResolver;
import com.ieltspath.library.infrastructure.adapter.VocabularyCatalogJdbcAdapter;
import com.ieltspath.library.infrastructure.persistence.adapter.VocabularyRepositoryAdapter;
import com.ieltspath.library.infrastructure.persistence.mapper.VocabularyPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Creating a word with its US/UK pronunciation: the dictionary columns are written by a second statement in the same
 * transaction, so they must end up on the row the entity insert made.
 */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VocabularyRepositoryAdapter.class, VocabularyPersistenceMapper.class, CreateVocabularyItemUseCase.class,
        VocabularyCatalogJdbcAdapter.class, VocabularyAudioUrlResolver.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class CreateVocabularyItemDictionaryPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean FlashcardImageStorage imageStorage;

    @Autowired CreateVocabularyItemUseCase create;
    @Autowired JdbcTemplate jdbc;

    private final String lemma = "zz" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    @Test
    void theUsAndUkPronunciationAreStoredWithTheNewWord() {
        var dictionary = new VocabularyItemDetails("/ɪpʌk/", "/ipus/", "vocabulary/pronunciation/uk/x.mp3",
                "https://cdn.test/us.mp3", null, false, null);

        VocabularyItemResult created = create.execute(new CreateVocabularyItemCommand(lemma, "/ipus/", null, dictionary));

        var row = jdbc.queryForMap("SELECT ipa, ipa_us, ipa_uk, audio_us_reference, audio_uk_reference "
                + "FROM vocabulary_items WHERE id = ?", created.id());
        assertThat(row).containsEntry("ipa", "/ipus/")
                .containsEntry("ipa_us", "/ipus/")
                .containsEntry("ipa_uk", "/ɪpʌk/")
                .containsEntry("audio_us_reference", "https://cdn.test/us.mp3")
                .containsEntry("audio_uk_reference", "vocabulary/pronunciation/uk/x.mp3");
    }

    @Test
    void aWordWithoutPronunciationIsStillCreated() {
        var empty = new VocabularyItemDetails(null, null, null, null, null, false, null);

        VocabularyItemResult created = create.execute(new CreateVocabularyItemCommand(lemma, null, null, empty));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM vocabulary_items WHERE id = ?", Integer.class, created.id()))
                .isEqualTo(1);
    }

    @Test
    void creatingAWordKeptInactiveSwitchesItOnAndKeepsTheAudioItAlreadyHas() {
        // A dictation pronunciation row: inactive, with a US recording and an IPA.
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vocabulary_items(id, lemma, normalized_lemma, ipa, status, audio_us_reference) "
                + "VALUES (?, ?, ?, '/old/', 'INACTIVE', 'vocabulary/pronunciation/us/kept.mp3')", id, lemma, lemma);
        var given = new VocabularyItemDetails(null, "/new us/", "https://cdn.test/uk.mp3", null, null, false, null);

        VocabularyItemResult created = create.execute(new CreateVocabularyItemCommand(lemma, "/new us/", null, given));

        assertThat(created.id()).isEqualTo(id);
        var row = jdbc.queryForMap("SELECT status, ipa, ipa_us, audio_us_reference, audio_uk_reference "
                + "FROM vocabulary_items WHERE id = ?", id);
        assertThat(row).containsEntry("status", "ACTIVE")
                .containsEntry("ipa", "/new us/")
                .containsEntry("ipa_us", "/new us/")
                .containsEntry("audio_us_reference", "vocabulary/pronunciation/us/kept.mp3")
                .containsEntry("audio_uk_reference", "https://cdn.test/uk.mp3");
    }

    @Test
    void aSecondWordWithTheSameLemmaIsRefused() {
        var empty = new VocabularyItemDetails(null, null, null, null, null, false, null);
        create.execute(new CreateVocabularyItemCommand(lemma, null, null, empty));

        assertThatThrownBy(() -> create.execute(new CreateVocabularyItemCommand(lemma, null, null, empty)))
                .isInstanceOf(DuplicateCodeException.class);
    }
}
