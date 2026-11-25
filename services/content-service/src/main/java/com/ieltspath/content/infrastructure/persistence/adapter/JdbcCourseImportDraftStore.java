package com.ieltspath.content.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.content.application.port.CourseImportDraftStore;
import com.ieltspath.content.domain.vo.CourseImportModel;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JdbcCourseImportDraftStore implements CourseImportDraftStore {

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcCourseImportDraftStore(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void insert(Draft draft) {
        jdbc.update("""
                INSERT INTO course_import_drafts (id, author_id, filename, status, content, imported_course_id, created_at, updated_at)
                VALUES (:id, :author, :filename, :status, CAST(:content AS jsonb), :course, :created, :updated)
                """, new MapSqlParameterSource()
                .addValue("id", draft.id()).addValue("author", draft.authorId()).addValue("filename", draft.filename())
                .addValue("status", draft.status()).addValue("content", write(draft.model()))
                .addValue("course", draft.importedCourseId()).addValue("created", time(draft.createdAt()))
                .addValue("updated", time(draft.updatedAt())));
    }

    @Override
    public Optional<Draft> find(UUID author, UUID id) {
        return jdbc.query("""
                SELECT id, author_id, filename, status, content::text AS content, imported_course_id, created_at, updated_at
                FROM course_import_drafts WHERE id = :id AND author_id = :author
                """, new MapSqlParameterSource("id", id).addValue("author", author), (rs, i) -> new Draft(
                rs.getObject("id", UUID.class), rs.getObject("author_id", UUID.class), rs.getString("filename"),
                rs.getString("status"), rs.getObject("imported_course_id", UUID.class), read(rs.getString("content")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant())).stream().findFirst();
    }

    @Override
    public void updateContent(UUID author, UUID id, CourseImportModel model, Instant now) {
        jdbc.update("UPDATE course_import_drafts SET content = CAST(:content AS jsonb), updated_at = :now "
                + "WHERE id = :id AND author_id = :author", new MapSqlParameterSource("content", write(model))
                .addValue("now", time(now)).addValue("id", id).addValue("author", author));
    }

    @Override
    public void markImported(UUID author, UUID id, UUID courseId, Instant now) {
        jdbc.update("UPDATE course_import_drafts SET status = 'IMPORTED', imported_course_id = :course, updated_at = :now "
                + "WHERE id = :id AND author_id = :author", new MapSqlParameterSource("course", courseId)
                .addValue("now", time(now)).addValue("id", id).addValue("author", author));
    }

    @Override
    public boolean delete(UUID author, UUID id) {
        return jdbc.update("DELETE FROM course_import_drafts WHERE id = :id AND author_id = :author",
                new MapSqlParameterSource("id", id).addValue("author", author)) > 0;
    }

    @Override
    public List<Summary> list(UUID author) {
        return jdbc.query("""
                SELECT id, filename, status, imported_course_id, updated_at,
                       content -> 'sheets' -> 'Course' -> 0 -> 'cells' ->> 'code' AS course_code,
                       content -> 'sheets' -> 'Course' -> 0 -> 'cells' ->> 'name' AS course_name
                FROM course_import_drafts WHERE author_id = :author ORDER BY updated_at DESC
                """, new MapSqlParameterSource("author", author), (rs, i) -> new Summary(
                rs.getObject("id", UUID.class), rs.getString("filename"), rs.getString("status"),
                rs.getObject("imported_course_id", UUID.class), rs.getString("course_code"), rs.getString("course_name"),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()));
    }

    @Override
    public int countDrafts(UUID author) {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM course_import_drafts WHERE author_id = :author "
                + "AND status = 'DRAFT'", new MapSqlParameterSource("author", author), Integer.class);
        return count == null ? 0 : count;
    }

    private String write(CourseImportModel model) {
        try {
            return json.writeValueAsString(model);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Không ghi được bản nháp nhập khóa học", failure);
        }
    }

    private CourseImportModel read(String content) {
        try {
            return json.readValue(content, CourseImportModel.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Không đọc được bản nháp nhập khóa học", failure);
        }
    }

    private static OffsetDateTime time(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
