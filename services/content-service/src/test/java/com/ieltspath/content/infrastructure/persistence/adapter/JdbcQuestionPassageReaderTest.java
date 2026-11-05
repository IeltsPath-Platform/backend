package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.application.port.QuestionPassageReader.Passage;
import com.ieltspath.content.application.port.QuestionPassageReader.Source;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcQuestionPassageReader.class)
@Testcontainers(disabledWithoutDocker = true)
class JdbcQuestionPassageReaderTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired JdbcQuestionPassageReader reader;

    @Test
    void findsThePassageAboveTheExerciseBlockThatHoldsTheQuestion() {
        UUID version = questionVersion();
        UUID lesson = lesson("Reading lesson");
        UUID farPassage = passage("A. Far passage");
        UUID nearPassage = passage("A. Near passage");
        block(lesson, 1, "ASSET", farPassage);
        block(lesson, 2, "ASSET", nearPassage);
        UUID exercise = block(lesson, 3, "EXERCISE", null);
        jdbc.update("INSERT INTO lesson_block_questions(block_id,question_version_id,sort_order) VALUES(?,?,1)", exercise, version);
        UUID otherLesson = lesson("Other lesson");
        block(otherLesson, 1, "ASSET", passage("A. Someone else's passage"));

        List<Passage> found = reader.passagesOf(version);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).text()).isEqualTo("A. Near passage");
        assertThat(found.get(0).source()).isEqualTo(Source.LESSON);
        assertThat(found.get(0).sourceName()).isEqualTo("Reading lesson");
    }

    @Test
    void findsAPassageLinkedToTheVersionAndOneLinkedToItsSectionOnceEach() {
        UUID version = questionVersion();
        UUID direct = passage("A. Direct passage");
        jdbc.update("INSERT INTO content_asset_links(id,asset_id,question_version_id,sort_order) VALUES(?,?,?,0)", UUID.randomUUID(), direct, version);
        UUID section = section("Reading section");
        jdbc.update("INSERT INTO section_questions(id,section_id,question_version_id,sort_order) VALUES(?,?,?,1)", UUID.randomUUID(), section, version);
        UUID onSection = passage("A. Section passage");
        jdbc.update("INSERT INTO content_asset_links(id,asset_id,section_id,sort_order) VALUES(?,?,?,0)", UUID.randomUUID(), onSection, section);
        jdbc.update("INSERT INTO content_asset_links(id,asset_id,section_id,sort_order) VALUES(?,?,?,1)", UUID.randomUUID(), direct, section);

        List<Passage> found = reader.passagesOf(version);

        assertThat(found).extracting(Passage::text).containsExactly("A. Direct passage", "A. Section passage");
        assertThat(found).extracting(Passage::source).containsExactly(Source.DIRECT, Source.SECTION);
        assertThat(found.get(1).sourceName()).isEqualTo("Reading section");
    }

    @Test
    void ignoresAssetsThatAreNotPassagesAndAnswersEmptyForAVersionWithNone() {
        UUID version = questionVersion();
        UUID audio = UUID.randomUUID();
        jdbc.update("INSERT INTO content_assets(id,asset_type,media_reference) VALUES(?,'AUDIO','clip.mp3')", audio);
        jdbc.update("INSERT INTO content_asset_links(id,asset_id,question_version_id,sort_order) VALUES(?,?,?,0)", UUID.randomUUID(), audio, version);

        assertThat(reader.passagesOf(version)).isEmpty();
        assertThat(reader.passagesOf(UUID.randomUUID())).isEmpty();
    }

    private UUID questionVersion() {
        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,question_type,skill,purpose,status) VALUES(?,'MULTIPLE_CHOICE','READING','LEARNING','DRAFT')", question);
        UUID version = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,status) VALUES(?,?,1,'Stem','DRAFT')", version, question);
        return version;
    }

    private UUID passage(String text) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO content_assets(id,asset_type,text_content) VALUES(?,'PASSAGE',?)", id, text);
        return id;
    }

    private UUID lesson(String title) {
        UUID topic = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order) VALUES(?,?,?,?)", topic, "PASS_" + topic, "Fixture",
                900_000 + (int) (Math.random() * 90_000));
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons(id,topic_id,code,title,sort_order) VALUES(?,?,?,?,1)", id, topic, "PASS_L_" + id, title);
        return id;
    }

    private UUID block(UUID lesson, int order, String type, UUID asset) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO lesson_blocks(id,lesson_id,sort_order,block_type,asset_id) VALUES(?,?,?,?,?)", id, lesson, order, type, asset);
        return id;
    }

    private UUID section(String title) {
        UUID packageId = UUID.randomUUID();
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type) VALUES(?,?,'Fixture','PRACTICE_SET')", packageId, "PASS_P_" + packageId);
        UUID packageVersion = UUID.randomUUID();
        jdbc.update("INSERT INTO content_package_versions(id,package_id,version_number) VALUES(?,?,1)", packageVersion, packageId);
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO content_sections(id,package_version_id,title,skill,sort_order) VALUES(?,?,?,'READING',1)", id, packageVersion, title);
        return id;
    }
}
