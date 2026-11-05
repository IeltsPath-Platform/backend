package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.port.FlashcardImageStorage;
import com.ieltspath.library.application.query.CatalogBrowseQuery;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.domain.aggregate.VocabularyItem;
import com.ieltspath.library.infrastructure.adapter.VocabularyAudioUrlResolver;
import com.ieltspath.library.infrastructure.adapter.VocabularyCatalogJdbcAdapter;
import com.ieltspath.library.infrastructure.persistence.adapter.VocabularyRepositoryAdapter;
import com.ieltspath.library.infrastructure.persistence.mapper.VocabularyPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
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

/** The word searches on the real schema: what they match, the order they return and the cap on the result. */
@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VocabularyRepositoryAdapter.class, VocabularyPersistenceMapper.class, VocabularyCatalogJdbcAdapter.class,
        VocabularyAudioUrlResolver.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class VocabularySearchPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean FlashcardImageStorage imageStorage;

    @Autowired VocabularyRepositoryAdapter vocabulary;
    @Autowired VocabularyCatalogJdbcAdapter catalog;
    @Autowired JdbcTemplate jdbc;

    /** A stem no seeded word contains, so each test only sees its own words. */
    private final String stem = "qz" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

    @BeforeEach
    void words() {
        word("x" + stem + "y", "chứa ở giữa");
        word(stem + "ing", "bắt đầu bằng gốc");
        word(stem, "từ gốc " + stem);
        word("Mixed" + stem.toUpperCase(), "chữ hoa");
    }

    private void word(String lemma, String meaning) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vocabulary_items(id, lemma, normalized_lemma, status) VALUES (?, ?, ?, 'ACTIVE')",
                id, lemma, lemma.trim().toLowerCase(java.util.Locale.ROOT));
        jdbc.update("INSERT INTO vocabulary_senses(vocabulary_item_id, part_of_speech, vietnamese_meaning, example_sentence) "
                + "VALUES (?, 'NOUN', ?, 'Example.')", id, meaning);
    }

    @Test
    void searchPutsTheExactWordFirstThenWordsStartingWithItThenTheRest() {
        assertThat(vocabulary.searchByLemma(" " + stem.toUpperCase() + " ")).extracting(VocabularyItem::getNormalizedLemma)
                .containsExactly(stem, stem + "ing", "mixed" + stem, "x" + stem + "y");
    }

    @Test
    void anEmptySearchReturnsNothingRatherThanTheWholeCatalog() {
        assertThat(vocabulary.searchByLemma("")).isEmpty();
        assertThat(vocabulary.searchByLemma("   ")).isEmpty();
    }

    @Test
    void likeWildcardsInTheSearchTextAreMatchedLiterally() {
        assertThat(vocabulary.searchByLemma(stem + "%")).isEmpty();
        assertThat(vocabulary.searchByLemma(stem.charAt(0) + "_" + stem.substring(2))).isEmpty();
    }

    @Test
    void searchReturnsTheWordsWithTheirSenses() {
        assertThat(vocabulary.searchByLemma(stem + "ing")).singleElement()
                .satisfies(item -> assertThat(item.getSenses()).hasSize(1));
    }

    @Test
    void catalogSuggestionsMatchTheStartOfTheWordWhateverItsCase() {
        assertThat(catalog.suggestions(stem.toUpperCase())).extracting("lemma")
                .containsExactly(stem, stem + "ing");
    }

    @Test
    void catalogBrowseMatchesEnglishOnTheWordAndVietnameseOnTheMeaning() {
        var english = catalog.browse(new CatalogBrowseQuery(stem, false, null, null, new PageQuery(0, 20)));
        assertThat(english.total()).isEqualTo(4);

        var vietnamese = catalog.browse(new CatalogBrowseQuery("BẮT ĐẦU", true, null, null, new PageQuery(0, 20)));
        assertThat(vietnamese.items()).extracting("normalizedLemma").containsExactly(stem + "ing");
    }

    @Test
    void vietnameseSearchIgnoresAccentsIncludingTheLetterD() {
        // "bắt đầu bằng gốc" is found typed with no accent at all, đ written as d.
        var plain = catalog.browse(new CatalogBrowseQuery("bat dau bang", true, null, null, new PageQuery(0, 20)));
        assertThat(plain.items()).extracting("normalizedLemma").containsExactly(stem + "ing");

        var partlyAccented = catalog.browse(new CatalogBrowseQuery("chứa o giua", true, null, null, new PageQuery(0, 20)));
        assertThat(partlyAccented.items()).extracting("normalizedLemma").containsExactly("x" + stem + "y");
    }
}
