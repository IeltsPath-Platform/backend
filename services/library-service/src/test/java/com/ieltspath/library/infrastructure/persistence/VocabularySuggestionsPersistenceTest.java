package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.query.VocabularySuggestionsQuery;
import com.ieltspath.library.application.result.VocabularySuggestionsResult;
import com.ieltspath.library.application.result.VocabularySuggestionsResult.Word;
import com.ieltspath.library.application.usecase.SuggestVocabularyUseCase;
import com.ieltspath.library.infrastructure.adapter.VocabularySuggestionsJdbcAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VocabularySuggestionsJdbcAdapter.class, SuggestVocabularyUseCase.class})
@TestPropertySource(properties = {"spring.flyway.enabled=true", "spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class VocabularySuggestionsPersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired SuggestVocabularyUseCase suggest;

    UUID word(String lemma, String status, String senseStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vocabulary_items(id,lemma,normalized_lemma,status) VALUES (?,?,?,?)", id, lemma, lemma.toLowerCase(java.util.Locale.ROOT), status);
        sense(id, senseStatus, 0);
        return id;
    }
    void sense(UUID item, String status, int order) {
        jdbc.update("INSERT INTO vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence,status,sort_order) VALUES (?,?,'NOUN','nghĩa','Example.',?,?)",
                UUID.randomUUID(), item, status, order);
    }
    VocabularySuggestionsResult search(String text, int page, int size) {
        return suggest.execute(new VocabularySuggestionsQuery(text, page, size));
    }

    @Test void ranksExactThenPrefixThenSubstringAndOnlyReturnsActiveSenses() {
        UUID exact = word("zzpoll", "ACTIVE", "ACTIVE");
        sense(exact, "INACTIVE", 2);
        word("zzpollution", "ACTIVE", "ACTIVE");
        word("azzpoll", "ACTIVE", "ACTIVE");
        word("zzpoll-hidden", "INACTIVE", "ACTIVE");
        word("zzpoll-no-meaning", "ACTIVE", "INACTIVE");
        var result = search(" ZZPOLL ", 0, 20);
        assertThat(result.content()).extracting(Word::lemma).containsExactly("zzpoll", "zzpollution", "azzpoll");
        assertThat(result.content().getFirst().senses()).hasSize(1);
    }

    @Test void shortQueriesUseExactOrPrefixAndTreatWildcardsLiterally() {
        word("x", "ACTIVE", "ACTIVE");
        word("xx-prefix", "ACTIVE", "ACTIVE");
        word("axx-middle", "ACTIVE", "ACTIVE");
        assertThat(search("x", 0, 50).content()).extracting(Word::lemma).containsExactly("x");
        assertThat(search("xx", 0, 50).content()).extracting(Word::lemma).containsExactly("xx-prefix");
        for (String literal : List.of("zz%", "zz_", "zz\\")) {
            word(literal, "ACTIVE", "ACTIVE");
            assertThat(search(literal, 0, 50).content()).extracting(Word::lemma).containsExactly(literal);
        }
    }

    @Test void paginatesWordsBeforeLoadingAllOfTheirSenses() {
        for (int n = 0; n < 5; n++) {
            UUID id = word("zzpage" + n, "ACTIVE", "ACTIVE");
            sense(id, "ACTIVE", 1);
        }
        assertThat(search("zzpage", 0, 2).content()).extracting(Word::lemma).containsExactly("zzpage0", "zzpage1");
        assertThat(search("zzpage", 0, 2).hasMore()).isTrue();
        assertThat(search("zzpage", 1, 2).content()).extracting(Word::lemma).containsExactly("zzpage2", "zzpage3");
        assertThat(search("zzpage", 1, 2).content()).allSatisfy(word -> assertThat(word.senses()).hasSize(2));
        assertThat(search("zzpage", 2, 2).hasMore()).isFalse();
        assertThat(search("zzpage", 2, 2).content()).extracting(Word::lemma).containsExactly("zzpage4");
        assertThat(search("zzpage", 3, 2).content()).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void indexesAndBoundedQueriesWorkWithTwentyThousandWords() throws Exception {
        jdbc.update("""
                INSERT INTO vocabulary_items(id,lemma,normalized_lemma)
                SELECT md5('suggest-load-word-'||n)::uuid, 'zzload'||lpad(n::text,5,'0'), 'zzload'||lpad(n::text,5,'0')
                FROM generate_series(1,20000) n
                """);
        try {
            jdbc.update("""
                    INSERT INTO vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence)
                    SELECT md5('suggest-load-sense-'||n)::uuid, md5('suggest-load-word-'||n)::uuid, 'NOUN', 'nghĩa', 'Example.'
                    FROM generate_series(1,20000) n
                    """);
            jdbc.execute("ANALYZE vocabulary_items");
            jdbc.execute("ANALYZE vocabulary_senses");
            String indexes = String.join("\n", jdbc.queryForList("SELECT indexdef FROM pg_indexes WHERE tablename='vocabulary_items'", String.class));
            assertThat(indexes).contains("varchar_pattern_ops", "gin_trgm_ops", "ACTIVE");
            String plan = String.join("\n", jdbc.queryForList("EXPLAIN (ANALYZE, BUFFERS) SELECT id FROM vocabulary_items WHERE status='ACTIVE' AND normalized_lemma LIKE '%load199%'", String.class));
            assertThat(plan).contains("idx_vocabulary_items_active_lemma_trgm");
            System.out.println("Vocabulary substring plan on 20,000 words:\n" + plan);
            var tasks = new ArrayList<Callable<Long>>();
            for (int n = 0; n < 24; n++) tasks.add(() -> {
                long start = System.nanoTime();
                var result = search("zzload19", 0, 20);
                assertThat(result.content()).hasSize(20);
                assertThat(result.hasMore()).isTrue();
                return (System.nanoTime() - start) / 1_000_000;
            });
            try (var executor = Executors.newFixedThreadPool(4)) {
                List<Long> durations = new ArrayList<>();
                for (var future : executor.invokeAll(tasks)) durations.add(future.get());
                Collections.sort(durations);
                System.out.println("Vocabulary suggestions, 24 reads / 4 workers, p95=" + durations.get(22) + "ms (isolated test database)");
            }
        } finally {
            jdbc.update("DELETE FROM vocabulary_items WHERE id IN (SELECT md5('suggest-load-word-'||n)::uuid FROM generate_series(1,20000) n)");
        }
    }
}
