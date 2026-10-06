package com.group01.content.infrastructure.persistence;

import com.group01.content.domain.aggregate.Course;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.infrastructure.persistence.adapter.CourseRepositoryAdapter;
import com.group01.content.infrastructure.persistence.mapper.CoursePersistenceMapper;
import com.group01.content.infrastructure.persistence.mapper.TopicPersistenceMapper;
import com.group01.content.infrastructure.persistence.repository.CourseJpaRepository;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import com.group01.content.domain.exception.DuplicateCourseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoursePersistenceTest {
    private final CoursePersistenceMapper mapper = Mappers.getMapper(CoursePersistenceMapper.class);

    @Test
    void concurrentCodeOrBandConflictsBecomeCourseConflicts() {
        Course course = Course.create("IELTS", "IELTS", new BigDecimal("5.5"));
        for (String constraint : List.of("courses_code_key", "courses_band_level_key")) {
            var jpa = mock(CourseJpaRepository.class);
            when(jpa.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate course",
                    new ConstraintViolationException("Duplicate", new SQLException("Duplicate", "23505"), constraint)));
            assertThatThrownBy(() -> new CourseRepositoryAdapter(jpa, mapper).save(course))
                    .isInstanceOf(DuplicateCourseException.class)
                    .hasMessageContaining(constraint.equals("courses_code_key") ? "code 'IELTS'" : "bandLevel '5.5'");
            verify(jpa).saveAndFlush(any());
        }
    }

    @Test
    void unrelatedIntegrityFailuresKeepTheirOriginalCause() {
        var jpa = mock(CourseJpaRepository.class);
        var failure = new DataIntegrityViolationException("Invalid status",
                new ConstraintViolationException("Invalid status", new SQLException("Invalid status", "23514"), "courses_status_check"));
        when(jpa.saveAndFlush(any())).thenThrow(failure);
        assertThatThrownBy(() -> new CourseRepositoryAdapter(jpa, mapper).save(
                Course.create("IELTS", "IELTS", new BigDecimal("5.5")))).isSameAs(failure);
    }

    @Test
    void courseFieldsRoundTripThroughTheJpaMapper() {
        Course course = Course.create("IELTS", "IELTS", new BigDecimal("5.5"));
        Course restored = mapper.toDomain(mapper.toEntity(course));
        assertThat(restored).usingRecursiveComparison().isEqualTo(course);
    }

    @Test
    void courseListingUsesTheDatabaseBandOrderQuery() {
        var jpa = mock(CourseJpaRepository.class);
        Course low = Course.create("LOW", "Low", new BigDecimal("5.5"));
        Course high = Course.create("HIGH", "High", new BigDecimal("6.5"));
        when(jpa.findAllByOrderByBandLevelAsc()).thenReturn(List.of(mapper.toEntity(low), mapper.toEntity(high)));
        assertThat(new CourseRepositoryAdapter(jpa, mapper).findAllOrderByBandLevel())
                .extracting(Course::getBandLevel).containsExactly(new BigDecimal("5.5"), new BigDecimal("6.5"));
        verify(jpa).findAllByOrderByBandLevelAsc();
        verify(jpa, never()).findAll();
    }

    @Test
    void topicCourseMembershipRoundTripsWithoutChangingBandMetadata() {
        Topic topic = Topic.create(null, "TOPIC", "Topic", 1);
        topic.assignCourse(UUID.randomUUID());
        var topicMapper = new TopicPersistenceMapper();
        assertThat(topicMapper.toDomain(topicMapper.toEntity(topic))).usingRecursiveComparison().isEqualTo(topic);
    }
}
