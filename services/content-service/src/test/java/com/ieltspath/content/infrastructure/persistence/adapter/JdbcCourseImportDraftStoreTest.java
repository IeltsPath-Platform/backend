package com.ieltspath.content.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.port.CourseImportDraftStore.Draft;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Row;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The kept workbooks on the real schema: the rows survive a round trip and every read stays with its author. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JdbcCourseImportDraftStore.class, JdbcCourseImportDraftStoreTest.Beans.class})
@Testcontainers(disabledWithoutDocker = true)
class JdbcCourseImportDraftStoreTest {

    @TestConfiguration
    static class Beans {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired JdbcCourseImportDraftStore store;

    private final UUID author = UUID.randomUUID();
    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private Draft draft(UUID owner, String code) {
        var model = new CourseImportModel("1", Map.of("Course", List.of(new Row(3, Map.of("code", code, "name", "Khóa thử", "bandLevel", "4.5")))));
        return new Draft(UUID.randomUUID(), owner, "course.xlsx", "DRAFT", null, model, now, now);
    }

    @Test
    void theRowsSurviveARoundTripAndOtherAuthorsCannotSeeThem() {
        Draft saved = draft(author, "C1");
        store.insert(saved);

        var found = store.find(author, saved.id()).orElseThrow();
        assertThat(found.model().rows("Course").get(0).get("name")).isEqualTo("Khóa thử");
        assertThat(found.model().rows("Course").get(0).number()).isEqualTo(3);
        assertThat(store.find(UUID.randomUUID(), saved.id())).isEmpty();
        assertThat(store.delete(UUID.randomUUID(), saved.id())).isFalse();
    }

    @Test
    void theListNamesTheCourseAndCountsOnlyOpenDrafts() {
        Draft open = draft(author, "C2");
        Draft done = draft(author, "C3");
        store.insert(open);
        store.insert(done);
        store.markImported(author, done.id(), UUID.randomUUID(), now);

        var list = store.list(author);

        assertThat(list).extracting(s -> s.courseCode()).containsExactlyInAnyOrder("C2", "C3");
        assertThat(list).filteredOn(s -> s.id().equals(done.id())).extracting(s -> s.status()).containsExactly("IMPORTED");
        assertThat(store.countDrafts(author)).isEqualTo(1);
    }

    @Test
    void savingReplacesTheRowsAndDeletingRemovesTheDraft() {
        Draft saved = draft(author, "C4");
        store.insert(saved);

        store.updateContent(author, saved.id(), draft(author, "C5").model(), now);
        assertThat(store.find(author, saved.id()).orElseThrow().model().rows("Course").get(0).get("code")).isEqualTo("C5");

        assertThat(store.delete(author, saved.id())).isTrue();
        assertThat(store.find(author, saved.id())).isEmpty();
    }
}
