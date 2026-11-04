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

    @Test
    void anUploadedFileRoundTripsThroughTheJpaMapper() {
        var file = LessonResource.createFile(UUID.randomUUID(), "Giáo trình",
                "lesson-document/0b1f7c9e-3a40-4d3a-9a52-1d2c8e0a7f11/9c2e5d3a-77b1-4f0a-8f6e-2b8d1c4a9e10.pdf", 1);

        assertThat(mapper.toDomain(mapper.toEntity(file))).usingRecursiveComparison().isEqualTo(file);
        assertThat(mapper.toEntity(file).getUrl()).isNull();
    }

    private LessonResourceRepositoryAdapter adapter(LessonResourceJpaRepository jpa) {
        return new LessonResourceRepositoryAdapter(jpa, mapper, mock(NamedParameterJdbcTemplate.class));
    }
}
