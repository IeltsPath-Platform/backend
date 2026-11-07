package com.ieltspath.library.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker=true)
class StudyUnitSeedUpgradeTest {
    @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:15-alpine");
    JdbcTemplate jdbc;
    Flyway migrations(String target) {return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()).locations("classpath:db/migration").target(target).cleanDisabled(false).load();}
    @BeforeEach void beforeSeed() {
        migrations("41").clean();migrations("41").migrate();
        jdbc=new JdbcTemplate(new DriverManagerDataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword()));
    }
    @Test void catalogAuthorDeactivationDoesNotBreakUpgradeOrCreatePartialCurriculum() {
        jdbc.update("update vocabulary_items set status='INACTIVE' where normalized_lemma='school'");
        assertDoesNotThrow(()->migrations("42").migrate());
        assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_units u join vocabulary_topics t on t.id=u.topic_id where t.slug='education'",Integer.class));
        assertEquals(4,jdbc.queryForObject("select count(*) from vocabulary_study_units u join vocabulary_topics t on t.id=u.topic_id where t.slug='environment'",Integer.class));
        assertEquals("INACTIVE",jdbc.queryForObject("select status from vocabulary_items where normalized_lemma='school'",String.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_units u where (select count(*) from vocabulary_study_unit_words w where w.unit_id=u.id)<>8",Integer.class));
    }
    @Test void incompleteExamplesOrInactiveTopicsArePreservedAndSkipped() {
        jdbc.update("update vocabulary_senses set example_sentence='The example was edited.' where vocabulary_item_id in (select id from vocabulary_items where normalized_lemma='school')");
        jdbc.update("update vocabulary_topics set status='INACTIVE' where slug='environment'");
        assertDoesNotThrow(()->migrations("42").migrate());
        assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_units",Integer.class));
        assertEquals("INACTIVE",jdbc.queryForObject("select status from vocabulary_topics where slug='environment'",String.class));
    }
    @Test void duplicateEditedMeaningsDoNotPublishInvalidUnits() {
        jdbc.update("update vocabulary_senses set vietnamese_meaning='same edited meaning' where topic_id=(select id from vocabulary_topics where slug='education') and vocabulary_item_id in (select id from vocabulary_items where normalized_lemma in ('school','classroom'))");
        migrations("42").migrate();
        assertEquals(0,jdbc.queryForObject("select count(*) from vocabulary_study_units u join vocabulary_topics t on t.id=u.topic_id where t.slug='education'",Integer.class));
    }
    @Test void seedAppendsAfterAuthoredUnitsAndBumpsExistingCurriculumVersionOnce() {
        UUID topic=jdbc.queryForObject("select id from vocabulary_topics where slug='education'",UUID.class);
        jdbc.update("insert into vocabulary_study_units(id,topic_id,sort_order,kind,draft_revision) values(?,?,7,'LEARN',1)",UUID.randomUUID(),topic);
        jdbc.update("insert into vocabulary_study_topic_versions(topic_id,curriculum_version) values(?,5)",topic);
        migrations("42").migrate();
        assertEquals(6,jdbc.queryForObject("select curriculum_version from vocabulary_study_topic_versions where topic_id=?",Integer.class,topic));
        assertEquals(List.of(7,8,9,10,11),jdbc.queryForList("select sort_order from vocabulary_study_units where topic_id=? order by sort_order",Integer.class,topic));
        assertEquals(1,jdbc.queryForObject("select curriculum_version from vocabulary_study_topic_versions v join vocabulary_topics t on t.id=v.topic_id where t.slug='environment'",Integer.class));
    }
}
