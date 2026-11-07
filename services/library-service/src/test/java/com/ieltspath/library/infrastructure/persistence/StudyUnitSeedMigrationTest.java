package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.application.service.*;
import com.ieltspath.library.infrastructure.adapter.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Testcontainers(disabledWithoutDocker=true)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Import({StudyUnitJdbcAdapter.class,VocabularyAudioUrlResolver.class,StudyUnitAuthoringService.class,
    StudyUnitContentService.class,StudyExercisePreview.class,StudyUnitSeedMigrationTest.Config.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties={"spring.flyway.enabled=true","spring.cloud.config.enabled=false","eureka.client.enabled=false"})
class StudyUnitSeedMigrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {r.add("spring.datasource.url",POSTGRES::getJdbcUrl);r.add("spring.datasource.username",POSTGRES::getUsername);r.add("spring.datasource.password",POSTGRES::getPassword);}
    @TestConfiguration static class Config {@Bean Clock clock(){return Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"),ZoneOffset.UTC);}}
    @Autowired StudyUnitAuthoringService author;
    @Autowired StudyUnitContentService content;
    @Autowired JdbcTemplate jdbc;
    static final Map<UUID,List<String>> WORDS=Map.ofEntries(
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000101"),List.of("school","classroom","book","notebook","pen","pencil","dictionary","library")),
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000102"),List.of("lesson","homework","assignment","question","answer","test","exam","grade")),
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000103"),List.of("university","campus","degree","diploma","tuition","scholarship","semester","professor")),
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000104"),List.of("book","notebook","homework","assignment","university","campus","tuition","scholarship")),
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000201"),List.of("weather","climate","temperature","rain","wind","cloud","storm","snow")),
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000202"),List.of("forest","river","ocean","coast","habitat","species","biodiversity","ecosystem")),
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000203"),List.of("pollution","waste","recycle","renewable","solar","conservation","protect","sustainable")),
        Map.entry(UUID.fromString("d9000000-0000-4000-8000-000000000204"),List.of("weather","climate","forest","habitat","pollution","recycle","renewable","sustainable"))
    );
    @Test void seededTopicsHaveCuratedPublishedUnitsAndDeduplicatedCounts() {
        for(String slug:List.of("education","environment")) {
            UUID topic=jdbc.queryForObject("select id from vocabulary_topics where slug=?",UUID.class,slug);
            var list=content.list(topic);
            assertEquals(4,list.totalUnits(),slug);assertEquals(1,list.curriculumVersion());assertEquals(24,list.uniqueWordCount());
            assertEquals(List.of(1,2,3,4),list.items().stream().map(u->u.sortOrder()).toList());
            assertEquals(List.of("LEARN","LEARN","LEARN","REVIEW"),list.items().stream().map(u->u.kind()).toList());
            for(var unit:list.items()) {
                assertTrue(WORDS.containsKey(unit.unitId()));assertEquals(1,unit.revision());assertEquals(8,unit.wordCount());
                var words=content.get(unit.unitId()).words();
                assertEquals(WORDS.get(unit.unitId()),words.stream().map(w->w.lemma()).toList());
                assertEquals(List.of(1,2,3,4,5,6,7,8),words.stream().map(w->w.sortOrder()).toList());
                assertTrue(words.stream().allMatch(w->w.vietnameseMeaning()!=null&&!w.vietnameseMeaning().isBlank()&&w.exampleSentence()!=null&&!w.exampleSentence().isBlank()));
                assertEquals(8,jdbc.queryForObject("select count(*) from vocabulary_study_unit_words w join vocabulary_senses s on s.id=w.sense_id join vocabulary_items i on i.id=s.vocabulary_item_id where w.unit_id=? and s.status='ACTIVE' and i.status='ACTIVE' and s.topic_id=?",Integer.class,unit.unitId(),topic));
            }
        }
    }
    @Test void publicationMetadataMatchesActualGeneratorAndHasNoBlockingValidation() {
        assertEquals(8,WORDS.size());
        for(UUID unit:WORDS.keySet()) {
            var validation=author.validate(unit);
            assertTrue(validation.canPublish(),()->unit+": "+validation.errors());assertTrue(validation.errors().isEmpty());
            assertEquals(18,validation.preview().gradedStepCount());assertEquals(8,validation.preview().fillStepCount());
            assertEquals(List.of("CHOICE","MATCH","FILL"),validation.preview().exerciseTypes());
            assertEquals(18,jdbc.queryForObject("select graded_step_count from vocabulary_study_unit_revisions where unit_id=? and revision=1",Integer.class,unit));
            assertEquals(validation.preview().exerciseTypes(),content.get(unit).unit().exerciseTypes());
            assertNotNull(jdbc.queryForObject("select published_at from vocabulary_study_unit_revisions where unit_id=? and revision=1",Instant.class,unit));
        }
    }
}

