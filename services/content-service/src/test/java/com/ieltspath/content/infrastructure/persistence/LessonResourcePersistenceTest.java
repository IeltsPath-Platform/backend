package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.exception.DuplicateLessonResourceOrderException;
import com.ieltspath.content.domain.vo.LessonResourceType;
import com.ieltspath.content.infrastructure.persistence.adapter.LessonResourceRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.LessonResourcePersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.LessonResourceJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LessonResourcePersistenceTest {
    private final LessonResourcePersistenceMapper mapper = Mappers.getMapper(LessonResourcePersistenceMapper.class);
    private final LessonResource resource = LessonResource.create(UUID.randomUUID(), LessonResourceType.SLIDE,
            "Slide", "https://docs.google.com/presentation/d/x", 2);

    @Test
    void aConcurrentOrderClashBecomesAResourceConflict() {
        var jpa = mock(LessonResourceJpaRepository.class);
        when(jpa.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate order",
                new ConstraintViolationException("Duplicate", new SQLException("Duplicate", "23505"),
                        "uq_lesson_resources_order")));

        assertThatThrownBy(() -> adapter(jpa).save(resource))
                .isInstanceOf(DuplicateLessonResourceOrderException.class)
                .hasMessageContaining("SLIDE").hasMessageContaining("2");
    }

    @Test
    void unrelatedIntegrityFailuresKeepTheirOriginalCause() {
        var jpa = mock(LessonResourceJpaRepository.class);
        var failure = new DataIntegrityViolationException("Missing lesson",
                new ConstraintViolationException("Missing", new SQLException("Missing", "23503"),
                        "lesson_resources_lesson_id_fkey"));
        when(jpa.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> adapter(jpa).save(resource)).isSameAs(failure);
    }

    @Test
    void resourceFieldsRoundTripThroughTheJpaMapper() {
        assertThat(mapper.toDomain(mapper.toEntity(resource))).usingRecursiveComparison().isEqualTo(resource);
    }

    private LessonResourceRepositoryAdapter adapter(LessonResourceJpaRepository jpa) {
        return new LessonResourceRepositoryAdapter(jpa, mapper, mock(NamedParameterJdbcTemplate.class));
    }
}
