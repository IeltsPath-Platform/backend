package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.LessonResource;
import com.ieltspath.content.domain.exception.DuplicateLessonResourceOrderException;
import com.ieltspath.content.domain.repository.LessonResourceRepository;
import com.ieltspath.content.infrastructure.persistence.mapper.LessonResourcePersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.LessonResourceJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LessonResourceRepositoryAdapter implements LessonResourceRepository {
    private final LessonResourceJpaRepository repository;
    private final LessonResourcePersistenceMapper mapper;
    private final NamedParameterJdbcTemplate jdbc;

    public LessonResourceRepositoryAdapter(LessonResourceJpaRepository repository,
                                           LessonResourcePersistenceMapper mapper, NamedParameterJdbcTemplate jdbc) {
        this.repository = repository;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    @Override
    public LessonResource save(LessonResource resource) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(resource)));
        } catch (DataIntegrityViolationException ex) {
            // Translate only the per-lesson order constraint; other persistence failures retain their cause.
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException constraint
                        && "uq_lesson_resources_order".equals(constraint.getConstraintName())) {
                    throw new DuplicateLessonResourceOrderException(resource.getResourceType(), resource.getSortOrder());
                }
            }
            throw ex;
        }
    }

    @Override
    public Optional<LessonResource> findById(UUID id) { return repository.findById(id).map(mapper::toDomain); }

    @Override
    public List<LessonResource> findByLessonId(UUID lessonId) {
        return repository.findByLessonIdOrderByResourceTypeAscSortOrderAsc(lessonId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public void deferSortOrderCheck() {
        jdbc.getJdbcOperations().execute("SET CONSTRAINTS uq_lesson_resources_order DEFERRED");
    }

    @Override
    public void delete(LessonResource resource) { repository.deleteById(resource.getId()); }

    @Override
    public String findSummaryMarkdown(UUID lessonId) {
        // The column is usually NULL, so the row value cannot go through Optional.
        List<String> rows = jdbc.query("SELECT summary_markdown FROM lessons WHERE id = :id",
                new MapSqlParameterSource("id", lessonId), (row, n) -> row.getString(1));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Override
    public void updateSummaryMarkdown(UUID lessonId, String summaryMarkdown) {
        jdbc.update("UPDATE lessons SET summary_markdown = :summary, updated_at = CURRENT_TIMESTAMP WHERE id = :id",
                new MapSqlParameterSource("id", lessonId).addValue("summary", summaryMarkdown));
    }
}
