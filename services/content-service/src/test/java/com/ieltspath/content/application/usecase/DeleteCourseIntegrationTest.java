package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.exception.CourseNotEmptyException;
import com.ieltspath.content.domain.repository.CourseRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.CoursePersistenceMapperImpl;
import com.ieltspath.content.application.command.CreateCourseCommand;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Deleting a course on the real schema: an empty one goes with its skill prices, one holding anything stays. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DeleteCourseUseCase.class, CreateCourseUseCase.class, CourseRepositoryAdapter.class,
        CoursePersistenceMapperImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class DeleteCourseIntegrationTest {

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
    @Autowired CreateCourseUseCase create;
    @Autowired DeleteCourseUseCase delete;
    @Autowired CourseRepository courses;

    private final String tag = "DL" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

    private UUID newCourse(String suffix, String band) {
        return create.execute(new CreateCourseCommand(tag + suffix, "Fixture " + suffix, new BigDecimal(band))).id();
    }

    @Test
    void anEmptyCourseIsDeletedTogetherWithItsSkillPrices() {
        UUID id = create.execute(new CreateCourseCommand(tag + "_E", "Empty", new BigDecimal("8.5"), null, null,
                Map.of(com.ieltspath.content.domain.vo.Skill.READING, 120), null, false, null, null)).id();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM course_skill_prices WHERE course_id = ?", Integer.class, id))
                .isPositive();

        delete.execute(id);

        assertThat(courses.findById(id)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM course_skill_prices WHERE course_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void aCourseWithATopicIsKept() {
        UUID id = newCourse("_T", "9.0");
        jdbc.update("INSERT INTO topics(id,code,name,sort_order,course_id) VALUES(?,?,?,?,?)",
                UUID.randomUUID(), tag + "_TOPIC", "Fixture", 1, id);

        assertThatThrownBy(() -> delete.execute(id)).isInstanceOf(CourseNotEmptyException.class);
        assertThat(courses.findById(id)).isPresent();
    }

    @Test
    void aCourseWithAFinalTestIsKept() {
        UUID id = newCourse("_P", "0.5");
        jdbc.update("INSERT INTO content_packages(id,code,title,package_type,course_id,status) VALUES(?,?,?,?,?,?)",
                UUID.randomUUID(), tag + "_FINAL", "Fixture final", "COURSE_TEST", id, "DRAFT");

        assertThatThrownBy(() -> delete.execute(id)).isInstanceOf(CourseNotEmptyException.class);
        assertThat(courses.findById(id)).isPresent();
    }
}
