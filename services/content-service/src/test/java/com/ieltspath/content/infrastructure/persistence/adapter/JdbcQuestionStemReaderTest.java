package com.ieltspath.content.infrastructure.persistence.adapter;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JdbcQuestionStemReader.class)
@Testcontainers(disabledWithoutDocker = true)
class JdbcQuestionStemReaderTest {

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
    @Autowired JdbcQuestionStemReader reader;

    @Test
    void returnsTheStemsOfEveryVersionTiedToThePointAndNoOthers() {
        UUID topic = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,code,name,sort_order) VALUES(?,?,?,?)", topic, "STEM_" + topic, "Fixture",
                900_000 + (int) (Math.random() * 90_000));
        UUID point = knowledgePoint(topic, "A");
        UUID other = knowledgePoint(topic, "B");
        UUID question = question();
        version(question, 1, "First wording", point);
        version(question, 2, "Second wording", point);
        version(question(), 1, "Someone else's stem", other);

        assertThat(reader.stemsOf(point)).containsExactlyInAnyOrder("First wording", "Second wording");
        assertThat(reader.stemsOf(UUID.randomUUID())).isEmpty();
    }

    private UUID knowledgePoint(UUID topic, String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO knowledge_points(id,topic_id,code,name,kind,status,learning_type,cefr_level,skill) "
                + "VALUES(?,?,?,?,'GRAMMAR','ACTIVE','CONCEPT','C2','READING')", id, topic, "STEM_KP_" + id, name);
        return id;
    }

    private UUID question() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,question_type,skill,purpose,status) "
                + "VALUES(?,'MULTIPLE_CHOICE','READING','LEARNING','DRAFT')", id);
        return id;
    }

    private void version(UUID question, int number, String stem, UUID point) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO question_versions(id,question_id,version_number,stem,status) VALUES(?,?,?,?,'DRAFT')",
                id, question, number, stem);
        jdbc.update("INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id) VALUES(?,?)", id,
                point);
    }
}
