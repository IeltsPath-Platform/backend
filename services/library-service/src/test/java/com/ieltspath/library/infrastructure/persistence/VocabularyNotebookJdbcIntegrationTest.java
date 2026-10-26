package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.query.NotebookQuery;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import com.ieltspath.library.infrastructure.adapter.VocabularyNotebookJdbcAdapter;
import com.ieltspath.library.infrastructure.adapter.VocabularyCatalogJdbcAdapter;
import com.ieltspath.library.application.port.FlashcardImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
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
@Import({VocabularyNotebookJdbcAdapter.class, VocabularyCatalogJdbcAdapter.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {"spring.flyway.enabled=true", "spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class VocabularyNotebookJdbcIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired VocabularyNotebookJdbcAdapter adapter;
    @Autowired VocabularyCatalogJdbcAdapter catalog;
    @MockBean FlashcardImageStorage imageStorage;

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

    @Test void catalogBrowseFiltersVietnameseAndCefrAndReturnsEnrichedDetails() {
        UUID item=UUID.randomUUID(), sense=UUID.randomUUID(), topic=UUID.fromString("c1000000-0000-4000-8000-000000000002");
        jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status) values(?,?,?,'ACTIVE')",item,"test lemma","test lemma");
        jdbc.update("insert into vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,english_definition,example_sentence,status,cefr_level,topic_id) values(?,?,'NOUN',?,?,?,'ACTIVE','B2',?)",
                sense,item,"từ tìm kiếm","a test definition","A test example.",topic);
        jdbc.update("insert into vocabulary_collocations(sense_id,phrase,meaning_vi,example) values(?,?,?,?)",sense,"test phrase","cụm thử nghiệm","A test phrase.");
        jdbc.update("insert into vocabulary_relations(sense_id,related_lemma,kind) values(?,?,'SYNONYM')",sense,"sample");
        jdbc.update("insert into vocabulary_common_mistakes(sense_id,wrong_usage,correct_usage) values(?,?,?)",sense,"wrong form","correct form");
        jdbc.update("insert into vocabulary_examples(sense_id,sentence,translation_vi) values(?,?,?)",sense,"A separate example.","Một ví dụ riêng.");

        var filtered=catalog.browse("từ tìm kiếm","VI","B2",topic,0,10);
        assertEquals(1L,filtered.get("total"));
        @SuppressWarnings("unchecked") var items=(List<java.util.Map<String,Object>>)filtered.get("items");
        @SuppressWarnings("unchecked") var senses=(List<java.util.Map<String,Object>>)items.getFirst().get("senses");
        var enriched=senses.getFirst();
        assertTrue(items.getFirst().keySet().containsAll(List.of("id","lemma","normalizedLemma","ipa","pronunciationAudioReference","status","createdAt","updatedAt","senses")));
        assertTrue(enriched.keySet().containsAll(List.of("id","vocabularyItemId","partOfSpeech","englishDefinition","vietnameseMeaning","exampleSentence","imageUrl","sortOrder","status","createdAt","updatedAt")));
        assertEquals(5.5,((com.ieltspath.library.domain.vo.IeltsBandRange)enriched.get("bandRange")).min());
        assertEquals(1,((List<?>)enriched.get("collocations")).size());
        assertEquals(1,((List<?>)enriched.get("synonyms")).size());
        assertEquals(1,((List<?>)enriched.get("commonMistakes")).size());
        assertEquals(1,((List<?>)enriched.get("examples")).size());
        assertEquals(1,((java.util.Map<?,?>)enriched.get("counts")).get("thesaurus"));
        assertEquals(0L,catalog.browse("nothing","VI","B2",topic,0,10).get("total"));
    }

    @Test void mindmapDoesNotExceedEightyNodes() {
        UUID topic=UUID.fromString("c1000000-0000-4000-8000-000000000002");
        for(int i=0;i<45;i++) {
            UUID item=UUID.randomUUID(),sense=UUID.randomUUID();
            jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status) values(?,?,?,'ACTIVE')",item,"mapword"+i,"mapword"+i);
            jdbc.update("insert into vocabulary_senses(id,vocabulary_item_id,part_of_speech,vietnamese_meaning,example_sentence,status,topic_id) values(?,?,'NOUN',?,'Example.','ACTIVE',?)",sense,item,"nghĩa "+i,topic);
            jdbc.update("insert into vocabulary_collocations(sense_id,phrase) values(?,?)",sense,"phrase"+i);
            jdbc.update("insert into vocabulary_relations(sense_id,related_lemma,kind) values(?,?,'SYNONYM')",sense,"synonym"+i);
        }
        var map=catalog.mindmap(topic,null);
        @SuppressWarnings("unchecked") var nodes=(List<java.util.Map<String,Object>>)map.get("nodes");
        @SuppressWarnings("unchecked") var edges=(List<java.util.Map<String,Object>>)map.get("edges");
        assertEquals(80,nodes.size());
        assertTrue(edges.size()<=80);
    }

    @Test void listsTopicsAndPagesBandAndTopicSets() {
        assertEquals(6,catalog.topics().size());
        assertFalse(catalog.suggestions("re").isEmpty());
        var bands=catalog.sets("BAND",0,10);
        assertEquals(6L,bands.get("total"));
        UUID bandId=UUID.fromString("c2000000-0000-4000-8000-000000000004");
        assertEquals("BAND",catalog.set(bandId).get("kind"));
        var bandWords=catalog.setWords(bandId,null,0,10);
        assertTrue((Long)bandWords.get("total")>0);
        UUID topicSet=UUID.fromString("c2000000-0000-4000-8000-000000000106");
        assertEquals("TOPIC",catalog.set(topicSet).get("kind"));
        var topicWords=catalog.setWords(topicSet,null,0,10);
        assertTrue((Long)topicWords.get("total")>0);
        UUID proficiencySet=UUID.fromString("c2000000-0000-4000-8000-000000000006");
        assertTrue((Long)catalog.setWords(proficiencySet,null,0,10).get("total")>0);
        assertTrue(((List<?>)catalog.browse("","EN",null,null,Integer.MAX_VALUE,10).get("items")).isEmpty());
        assertTrue(((List<?>)catalog.sets("BAND",Integer.MAX_VALUE,10).get("items")).isEmpty());
    }
}
