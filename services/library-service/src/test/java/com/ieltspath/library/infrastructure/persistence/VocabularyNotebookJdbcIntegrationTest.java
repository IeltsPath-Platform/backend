package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.query.NotebookQuery;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import com.ieltspath.library.infrastructure.adapter.VocabularyNotebookJdbcAdapter;
import com.ieltspath.library.infrastructure.adapter.VocabularyCatalogJdbcAdapter;
import com.ieltspath.library.application.query.CatalogBrowseQuery;
import com.ieltspath.library.application.query.PageQuery;
import com.ieltspath.library.domain.vo.CefrLevel;
import com.ieltspath.library.domain.vo.VocabularySetKind;
import com.ieltspath.library.infrastructure.adapter.VocabularyProgressJdbcAdapter;
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
@Import({VocabularyNotebookJdbcAdapter.class, VocabularyCatalogJdbcAdapter.class, VocabularyProgressJdbcAdapter.class,
        com.ieltspath.library.infrastructure.adapter.VocabularyAudioUrlResolver.class,
        com.ieltspath.library.application.usecase.VocabularyCatalogUseCase.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {"spring.flyway.enabled=true", "spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class VocabularyNotebookJdbcIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    @Autowired JdbcTemplate jdbc;
    @Autowired VocabularyNotebookJdbcAdapter adapter;
    @Autowired com.ieltspath.library.application.usecase.VocabularyCatalogUseCase catalog;
    @Autowired VocabularyProgressJdbcAdapter progress;
    @Autowired VocabularyCatalogJdbcAdapter catalogStore;
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
        assertEquals(1, summary.learnedCount());
        assertTrue(summary.catalogTotal() > 0);
        assertEquals(List.of(sense), adapter.findSavedSenseIds(user, List.of(sense)));
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () -> jdbc.update(
                "insert into flashcards(id,user_id,source_type,vocabulary_sense_id,front,back,status) values(?,?,'VOCABULARY_SENSE',?,?,?,'ACTIVE')",
                UUID.randomUUID(), user, sense, "apple", "meaning"));
    }

    @Test void pronunciationAudioIsReturnedOnlyForWordsThatHaveAny() {
        jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status,audio_uk_reference) values(?,?,?,'ACTIVE',?)",
                UUID.randomUUID(), "audio probe", "audio probe", "https://cdn.example.com/probe.mp3");
        jdbc.update("insert into vocabulary_items(id,lemma,normalized_lemma,status) values(?,?,?,'ACTIVE')",
                UUID.randomUUID(), "silent probe", "silent probe");

        var audio = catalogStore.pronunciationAudio(List.of("audio probe", "silent probe", "unknown probe"));

        assertEquals(List.of("audio probe"), List.copyOf(audio.keySet()));
        assertEquals("https://cdn.example.com/probe.mp3", audio.get("audio probe").audioUk());
        assertNull(audio.get("audio probe").audioUs());
        assertTrue(catalogStore.pronunciationAudio(List.of()).isEmpty());
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

        var filtered=catalog.browse(browse("từ tìm kiếm",true,CefrLevel.B2,topic,0));
        assertEquals(1L,filtered.total());
        var found=filtered.items().getFirst();
        assertEquals(item,found.id());
        assertEquals("test lemma",found.lemma());
        var enriched=found.senses().getFirst();
        assertEquals(sense,enriched.id());
        assertEquals(CefrLevel.B2,enriched.cefrLevel());
        assertEquals(5.5,enriched.bandRange().min());
        assertEquals(1,enriched.collocations().size());
        assertEquals(1,enriched.synonyms().size());
        assertEquals(1,enriched.commonMistakes().size());
        assertEquals(1,enriched.examples().size());
        assertEquals(1,enriched.counts().thesaurus());
        assertEquals(1,found.counts().definitions());
        assertEquals(0L,catalog.browse(browse("nothing",true,CefrLevel.B2,topic,0)).total());
    }

    @Test void searchTextIsMatchedLiterallyNotAsLikeWildcards() {
        long all=catalog.browse(browse("",false,null,null,0)).total();
        assertTrue(all>10);
        for (String wildcard : new String[] {"%","_","\\","a%","%_%"}) {
            assertEquals(0L,catalog.browse(browse(wildcard,false,null,null,0)).total(),wildcard);
            assertEquals(0L,catalog.browse(browse(wildcard,true,null,null,0)).total(),wildcard);
        }
        assertTrue(catalog.suggestions("%").isEmpty());
        assertTrue(catalog.suggestions("_").isEmpty());
        assertFalse(catalog.suggestions("pro").isEmpty());
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
        assertEquals(80,map.nodes().size());
        assertTrue(map.edges().size()<=80);
    }

    @Test void listsTopicsAndPagesBandAndTopicSets() {
        assertEquals(7,catalog.topics().size());
        assertFalse(catalog.suggestions("re").isEmpty());
        var bands=catalog.sets(VocabularySetKind.BAND,new PageQuery(0,10));
        assertEquals(6L,bands.total());
        UUID bandId=UUID.fromString("c2000000-0000-4000-8000-000000000004");
        assertEquals(VocabularySetKind.BAND,catalog.set(bandId).kind());
        var bandWords=catalog.setWords(bandId,null,new PageQuery(0,10));
        assertTrue(bandWords.total()>0);
        UUID topicSet=UUID.fromString("c2000000-0000-4000-8000-000000000106");
        assertEquals(VocabularySetKind.TOPIC,catalog.set(topicSet).kind());
        var topicWords=catalog.setWords(topicSet,null,new PageQuery(0,10));
        assertTrue(topicWords.total()>0);
        UUID proficiencySet=UUID.fromString("c2000000-0000-4000-8000-000000000006");
        assertTrue(catalog.setWords(proficiencySet,null,new PageQuery(0,10)).total()>0);
        assertTrue(catalog.browse(browse("",false,null,null,1_000_000)).items().isEmpty());
        assertTrue(catalog.sets(VocabularySetKind.BAND,new PageQuery(1_000_000,10)).items().isEmpty());
    }

    private static CatalogBrowseQuery browse(String text,boolean vietnamese,CefrLevel cefr,UUID topic,int page) {
        return new CatalogBrowseQuery(text,vietnamese,cefr,topic,new PageQuery(page,10));
    }

    @Test void progressAggregatesAreScopedToTheCurrentUserAndRespectCatalogFilters() {
        UUID userA=UUID.randomUUID(),userB=UUID.randomUUID();
        UUID research= sense("research"), algorithm=sense("algorithm");
        Instant reviewedAt=Instant.parse("2026-10-27T07:00:00Z");
        UUID cardA=insertCard(userA,research,2,1,reviewedAt);
        UUID cardB=insertCard(userB,algorithm,4,1,reviewedAt);
        UUID session=UUID.randomUUID();
        jdbc.update("insert into flashcard_review_events(user_id,flashcard_id,vocabulary_sense_id,session_id,correct,reviewed_at) values(?,?,?,?,?,?)",
                userA,cardA,research,session,true,java.sql.Timestamp.from(reviewedAt));
        jdbc.update("insert into flashcard_review_events(user_id,flashcard_id,vocabulary_sense_id,session_id,correct,reviewed_at) values(?,?,?,?,?,?)",
                userB,cardB,algorithm,UUID.randomUUID(),false,java.sql.Timestamp.from(reviewedAt));

        assertEquals(120,progress.weeklyTarget(userA,120));
        Instant weekStart=Instant.parse("2026-10-25T17:00:00Z"),weekEnd=Instant.parse("2026-11-01T17:00:00Z");
        assertEquals(1,progress.weeklyProgress(userA,weekStart,weekEnd));
        assertEquals(List.of(java.time.LocalDate.of(2026,10,27)),progress.reviewDays(userA,
                java.time.LocalDate.of(2026,10,26),java.time.LocalDate.of(2026,11,2),java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        progress.saveWeeklyTarget(userA,80);
        assertEquals(80,progress.weeklyTarget(userA,120));

        var aMap=progress.vocabularyMap(userA,reviewedAt).stream().collect(java.util.stream.Collectors.toMap(
                com.ieltspath.library.application.port.VocabularyProgressReader.BandCounts::cefr,x->x));
        var bMap=progress.vocabularyMap(userB,reviewedAt).stream().collect(java.util.stream.Collectors.toMap(
                com.ieltspath.library.application.port.VocabularyProgressReader.BandCounts::cefr,x->x));
        assertEquals(1,aMap.get("B2").learned());
        assertEquals(0,bMap.get("B2").learned());
        assertEquals(1,bMap.get("C1").learned());

        var filtered=adapter.findEntries(userA,new NotebookQuery(null,null,null,"RECENT",0,20,
                UUID.fromString("c1000000-0000-4000-8000-000000000001"),UUID.fromString("c2000000-0000-4000-8000-000000000004")),reviewedAt);
        assertEquals(1,filtered.total());
        assertEquals("B2",filtered.items().getFirst().cefrLevel());
        assertEquals("Education",filtered.items().getFirst().topicName());

        UUID educationSet=UUID.fromString("c2000000-0000-4000-8000-000000000101");
        var aTopics=progress.topicsProgress(userA,educationSet,reviewedAt);
        var bTopics=progress.topicsProgress(userB,educationSet,reviewedAt);
        assertEquals(1,aTopics.stream().filter(t->t.name().equals("Education")).findFirst().orElseThrow().learned());
        assertTrue(progress.nextWords(userA,aTopics.getFirst().topicId(),educationSet).size()<=5);
        assertEquals(0,bTopics.stream().filter(t->t.name().equals("Education")).findFirst().orElseThrow().learned());
        assertTrue(progress.setProgress(userA,UUID.fromString("c2000000-0000-4000-8000-000000000004"),reviewedAt).orElseThrow().learned()>0);
        assertEquals(session,progress.recentResult(userA).orElseThrow().sessionId());
        assertTrue(progress.recentResult(userB).isPresent());
        assertEquals(0,progress.achievementCounts(userA,reviewedAt).masteredWords());
    }

    private UUID sense(String lemma) {
        return jdbc.queryForObject("select s.id from vocabulary_senses s join vocabulary_items i on i.id=s.vocabulary_item_id where i.normalized_lemma=? and s.status='ACTIVE' order by s.sort_order limit 1",UUID.class,lemma);
    }

    private UUID insertCard(UUID user,UUID sense,int box,int reviewCount,Instant reviewedAt) {
        UUID card=UUID.randomUUID();
        jdbc.update("insert into flashcards(id,user_id,source_type,vocabulary_sense_id,source_reference_id,front,back,status,leitner_box,created_at,next_review_at,last_reviewed_at,review_count) values(?,?,'VOCABULARY_SENSE',?,?,?,?,'ACTIVE',?,?,?,?,?)",
                card,user,sense,sense,"front","back",box,java.sql.Timestamp.from(reviewedAt),java.sql.Timestamp.from(reviewedAt),java.sql.Timestamp.from(reviewedAt),reviewCount);
        return card;
    }
}
