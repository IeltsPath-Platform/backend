package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.command.ResolveVocabularyCommand;
import com.ieltspath.library.application.command.ResolveVocabularyCommand.Entry;
import com.ieltspath.library.application.result.ResolvedVocabularyResult;
import com.ieltspath.library.application.result.VocabularyItemResult;
import com.ieltspath.library.application.usecase.FindVocabularyByLemmasUseCase;
import com.ieltspath.library.application.usecase.ResolveVocabularyUseCase;
import com.ieltspath.library.domain.exception.InvalidContentStateException;
import com.ieltspath.library.domain.vo.PartOfSpeech;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Looking words up by lemma and giving reviewed words a sense, on the real schema. */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VocabularyRepositoryAdapter.class, VocabularyPersistenceMapper.class, ResolveVocabularyUseCase.class,
        FindVocabularyByLemmasUseCase.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class VocabularyLemmaPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired ResolveVocabularyUseCase resolve;
    @Autowired FindVocabularyByLemmasUseCase find;
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private final String lemma = "zz" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    private Entry entry(String text) {
        return new Entry(text, "/ipa/", PartOfSpeech.VERB, "to vary", "dao động", "Prices fluctuate.");
    }

    @Test
    void aNewWordIsStoredWithItsSenseAndALaterCallFindsTheSameSense() {
        List<ResolvedVocabularyResult> first = resolve.execute(new ResolveVocabularyCommand(List.of(entry(lemma))));

        assertThat(first).singleElement().satisfies(result -> {
            assertThat(result.created()).isTrue();
            assertThat(result.lemma()).isEqualTo(lemma);
        });
        entityManager.flush(); // the JDBC reads below must see what JPA has queued
        UUID sense = first.get(0).senseId();
        assertThat(jdbc.queryForObject("SELECT vietnamese_meaning FROM vocabulary_senses WHERE id = ?", String.class, sense))
                .isEqualTo("dao động");
        assertThat(jdbc.queryForObject("SELECT status FROM vocabulary_senses WHERE id = ?", String.class, sense)).isEqualTo("ACTIVE");

        List<ResolvedVocabularyResult> again = resolve.execute(new ResolveVocabularyCommand(
                List.of(new Entry(lemma.toUpperCase(), null, PartOfSpeech.NOUN, null, "khác", "Other."))));

        assertThat(again).singleElement().satisfies(result -> {
            assertThat(result.created()).isFalse();
            assertThat(result.senseId()).isEqualTo(sense);
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vocabulary_items WHERE normalized_lemma = ?", Integer.class, lemma))
                .isEqualTo(1);
    }

    @Test
    void wordsAreFoundByExactLemmaIgnoringCaseWithTheirActiveSensesOnly() {
        resolve.execute(new ResolveVocabularyCommand(List.of(entry(lemma))));
        entityManager.flush();
        UUID item = jdbc.queryForObject("SELECT id FROM vocabulary_items WHERE normalized_lemma = ?", UUID.class, lemma);
        jdbc.update("INSERT INTO vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence,status,sort_order) "
                + "VALUES (gen_random_uuid(),?,'NOUN','đã tắt','Off.','INACTIVE',9)", item);
        entityManager.clear(); // read the rows again, with the sense added behind JPA's back

        List<VocabularyItemResult> found = find.execute(List.of(lemma.toUpperCase(), lemma + "x"));

        assertThat(found).singleElement().satisfies(word -> {
            assertThat(word.id()).isEqualTo(item);
            assertThat(word.senses()).extracting(sense -> sense.vietnameseMeaning()).containsExactly("dao động");
        });
    }

    @Test
    void aWordThatIsSwitchedOffIsNotFoundAndCannotBeResolved() {
        resolve.execute(new ResolveVocabularyCommand(List.of(entry(lemma))));
        entityManager.flush();
        jdbc.update("UPDATE vocabulary_items SET status = 'INACTIVE' WHERE normalized_lemma = ?", lemma);
        entityManager.clear();

        assertThat(find.execute(List.of(lemma))).isEmpty();
        assertThatThrownBy(() -> resolve.execute(new ResolveVocabularyCommand(List.of(entry(lemma)))))
                .isInstanceOf(InvalidContentStateException.class);
    }
}
