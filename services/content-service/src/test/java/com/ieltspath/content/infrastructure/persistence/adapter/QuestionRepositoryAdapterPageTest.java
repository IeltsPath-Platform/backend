package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.mapper.QuestionPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The question catalog page on the real schema: filters, ordering and totals are computed by the database. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QuestionRepositoryAdapter.class, QuestionPersistenceMapper.class, JacksonAutoConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
class QuestionRepositoryAdapterPageTest {

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
    @Autowired QuestionRepository questions;

    private UUID question(String skill, String purpose, String status, Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO questions(id,question_type,skill,purpose,status,created_at) "
                        + "VALUES(?,'MULTIPLE_CHOICE',?,?,?,?)",
                id, skill, purpose, status, Timestamp.from(createdAt));
        return id;
    }

    private long count(String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM questions WHERE " + where, Long.class);
    }

    @Test
    void withoutFiltersTheTotalCoversEveryQuestionAndThePageIsNewestFirst() {
        Instant base = Instant.parse("2100-01-01T00:00:00Z");
        UUID oldest = question("READING", "LEARNING", "DRAFT", base);
        UUID middle = question("READING", "LEARNING", "DRAFT", base.plusSeconds(60));
        UUID newest = question("READING", "LEARNING", "DRAFT", base.plusSeconds(120));

        QuestionRepository.Page page = questions.findPage(null, null, null, 0, 2);

        assertThat(page.totalElements()).isEqualTo(count("TRUE"));
        assertThat(page.items()).extracting(Question::getId).containsExactly(newest, middle);
        assertThat(questions.findPage(null, null, null, 1, 2).items().get(0).getId()).isEqualTo(oldest);
    }

    @Test
    void eachFilterNarrowsTheTotalAndTheItems() {
        Instant at = Instant.parse("2101-01-01T00:00:00Z");
        UUID match = question("WRITING", "EXAM", "PUBLISHED", at);
        question("WRITING", "EXAM", "DRAFT", at.plusSeconds(1));
        question("WRITING", "LEARNING", "PUBLISHED", at.plusSeconds(2));
        question("READING", "EXAM", "PUBLISHED", at.plusSeconds(3));

        QuestionRepository.Page page = questions.findPage(Skill.WRITING, QuestionPurpose.EXAM,
                PublicationStatus.PUBLISHED, 0, 10);

        assertThat(page.totalElements()).isEqualTo(count("skill = 'WRITING' AND purpose = 'EXAM' AND status = 'PUBLISHED'"));
        assertThat(page.items()).extracting(Question::getId).contains(match);
        assertThat(page.items()).allSatisfy(question -> {
            assertThat(question.getSkill()).isEqualTo(Skill.WRITING);
            assertThat(question.getPurpose()).isEqualTo(QuestionPurpose.EXAM);
            assertThat(question.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        });
        assertThat(questions.findPage(null, null, PublicationStatus.DRAFT, 0, 1).totalElements())
                .isEqualTo(count("status = 'DRAFT'"));
    }

    @Test
    void pageBeyondTheEndIsEmptyButKeepsTheTotal() {
        long total = count("TRUE");

        QuestionRepository.Page page = questions.findPage(null, null, null, (int) total + 5, 10);

        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(total);
    }

    @Test
    void pagesDoNotOverlapAndTogetherCoverTheFilteredSet() {
        Instant at = Instant.parse("2102-01-01T00:00:00Z");
        for (int i = 0; i < 5; i++) {
            question("SPEAKING", "PLACEMENT", "DRAFT", at.plusSeconds(i));
        }

        long total = count("skill = 'SPEAKING' AND purpose = 'PLACEMENT'");
        List<UUID> collected = new ArrayList<>();
        int pages = (int) ((total + 1) / 2);
        for (int index = 0; index < pages; index++) {
            QuestionRepository.Page page = questions.findPage(Skill.SPEAKING, QuestionPurpose.PLACEMENT, null, index, 2);
            assertThat(page.totalElements()).isEqualTo(total);
            assertThat(page.items()).hasSize(index < pages - 1 || total % 2 == 0 ? 2 : 1);
            collected.addAll(ids(page));
        }

        assertThat(collected).hasSize((int) total).doesNotHaveDuplicates();
        assertThat(questions.findPage(Skill.SPEAKING, QuestionPurpose.PLACEMENT, null, pages, 2).items()).isEmpty();
    }

    private static List<UUID> ids(QuestionRepository.Page page) {
        return page.items().stream().map(Question::getId).toList();
    }
}
