package com.ieltspath.content.infrastructure.persistence.adapter;

import com.ieltspath.content.domain.aggregate.Rubric;
import com.ieltspath.content.domain.exception.RubricNotEditableException;
import com.ieltspath.content.domain.exception.RubricNotFoundException;
import com.ieltspath.content.domain.repository.RubricRepository;
import com.ieltspath.content.domain.vo.RubricVersionRef;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcRubricRepository implements RubricRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRubricRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Rubric create(Rubric rubric) {
        jdbc.update("INSERT INTO rubrics (code, task, rubric_version, status, source_note) "
                        + "VALUES (:code, :task, :version, 'DRAFT', :sourceNote)", params(rubric));
        saveChildren(rubric);
        return findByCodeAndVersion(rubric.code(), rubric.version()).orElseThrow();
    }

    @Override
    public Rubric updateDraft(String code, int version, Rubric rubric) {
        var key = new MapSqlParameterSource().addValue("code", code).addValue("version", version);
        String status;
        try {
            status = jdbc.queryForObject("SELECT status FROM rubrics WHERE code=:code AND rubric_version=:version FOR UPDATE",
                    key, String.class);
        } catch (EmptyResultDataAccessException ex) {
            throw new RubricNotFoundException("Rubric not found: " + code + "@" + version);
        }
        if (!"DRAFT".equals(status)) throw new RubricNotEditableException("Only draft rubrics can be edited");
        jdbc.update("UPDATE rubrics SET task=:task, source_note=:sourceNote, updated_at=CURRENT_TIMESTAMP "
                + "WHERE code=:code AND rubric_version=:version", params(rubric));
        UUID id = jdbc.queryForObject("SELECT id FROM rubrics WHERE code=:code AND rubric_version=:version", key, UUID.class);
        jdbc.update("DELETE FROM rubric_criteria WHERE rubric_id=:id", new MapSqlParameterSource("id", id));
        jdbc.update("DELETE FROM rubric_examples WHERE rubric_id=:id", new MapSqlParameterSource("id", id));
        saveChildren(id, rubric);
        return findByCodeAndVersion(code, version).orElseThrow();
    }

    @Override
    public Rubric activate(String code, int version) {
        var key = new MapSqlParameterSource().addValue("code", code).addValue("version", version);
        UUID rubricId;
        String task;
        try {
            var row = jdbc.queryForMap("SELECT id, task FROM rubrics WHERE code=:code AND rubric_version=:version FOR UPDATE", key);
            rubricId = (UUID) row.get("id");
            task = (String) row.get("task");
        } catch (EmptyResultDataAccessException ex) {
            throw new RubricNotFoundException("Rubric not found: " + code + "@" + version);
        }
        validateForActivation(load(rubricId));
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(:task))", new MapSqlParameterSource("task", task), rs -> null);
        jdbc.update("UPDATE rubrics SET status='RETIRED', updated_at=CURRENT_TIMESTAMP WHERE task=:task AND status='ACTIVE'",
                new MapSqlParameterSource("task", task));
        jdbc.update("UPDATE rubrics SET status='ACTIVE', updated_at=CURRENT_TIMESTAMP WHERE code=:code AND rubric_version=:version", key);
        return findByCodeAndVersion(code, version).orElseThrow();
    }

    @Override
    public List<RubricVersionRef> findVersions(String code) {
        return jdbc.query("SELECT code, task, rubric_version, status FROM rubrics WHERE code = :code ORDER BY rubric_version",
                new MapSqlParameterSource("code", code),
                (row, n) -> new RubricVersionRef(row.getString("code"), row.getString("task"),
                        row.getInt("rubric_version"), row.getString("status")));
    }

    @Override
    public List<Rubric> findActive(String task) { return findByStatus(task, "ACTIVE"); }

    @Override
    public List<Rubric> findDrafts(String task) { return findByStatus(task, "DRAFT"); }

    @Override
    public Optional<Rubric> findByCodeAndVersion(String code, int version) {
        return find("SELECT id FROM rubrics WHERE code=:code AND rubric_version=:version",
                new MapSqlParameterSource().addValue("code", code).addValue("version", version));
    }

    private List<Rubric> findByStatus(String task, String status) {
        List<UUID> ids = jdbc.queryForList("SELECT id FROM rubrics WHERE task=:task AND status=:status ORDER BY rubric_version DESC",
                new MapSqlParameterSource().addValue("task", task).addValue("status", status), UUID.class);
        return ids.stream().map(this::load).toList();
    }

    private Optional<Rubric> find(String sql, MapSqlParameterSource params) {
        return jdbc.queryForList(sql, params, UUID.class).stream().findFirst().map(this::load);
    }

    private Rubric load(UUID id) {
        var p = new MapSqlParameterSource("id", id);
        var parent = jdbc.queryForMap("SELECT code, task, rubric_version, source_note, status FROM rubrics WHERE id=:id", p);
        List<CriterionRow> criterionRows = jdbc.query("""
                        SELECT c.id, c.code, c.name, c.description, c.sort_order, m.knowledge_point_id
                        FROM rubric_criteria c
                        JOIN rubrics r ON r.id = c.rubric_id
                        LEFT JOIN writing_criterion_knowledge_points m ON m.task = r.task AND m.criterion_code = c.code
                        WHERE c.rubric_id = :id ORDER BY c.sort_order
                        """, p,
                (rs, row) -> new CriterionRow(rs.getObject("id", UUID.class), rs.getString("code"),
                        rs.getString("name"), rs.getString("description"), rs.getInt("sort_order"),
                        rs.getObject("knowledge_point_id", UUID.class)));
        String task = (String) parent.get("task");
        Map<String, List<Rubric.SubKnowledgePoint>> subPoints = jdbc.query("""
                        SELECT s.criterion_code, kp.id, kp.code, kp.name, kp.description
                        FROM writing_criterion_sub_knowledge_points s
                        JOIN knowledge_points kp ON kp.id = s.knowledge_point_id
                        JOIN topics t ON t.id = kp.topic_id
                        JOIN courses c ON c.id = t.course_id
                        WHERE s.task = :task AND kp.status = 'ACTIVE' AND kp.skill = 'WRITING'
                          AND t.status = 'ACTIVE' AND c.status = 'ACTIVE'
                        ORDER BY s.criterion_code, s.sort_order
                        """, new MapSqlParameterSource("task", task),
                (rs, row) -> new SubRow(rs.getString("criterion_code"), new Rubric.SubKnowledgePoint(
                        rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                        rs.getString("description"))))
                .stream().collect(Collectors.groupingBy(SubRow::criterionCode, java.util.LinkedHashMap::new,
                        Collectors.mapping(SubRow::point, Collectors.toList())));
        List<Rubric.Criterion> criteria = criterionRows.stream().map(criterion -> {
            List<Rubric.BandDescriptor> descriptors = jdbc.query("SELECT band, descriptor FROM rubric_band_descriptors "
                            + "WHERE criterion_id=:criterionId ORDER BY band",
                    new MapSqlParameterSource("criterionId", criterion.id()),
                    (d, i) -> new Rubric.BandDescriptor(d.getBigDecimal("band").doubleValue(), d.getString("descriptor")));
            return new Rubric.Criterion(criterion.code(), criterion.name(), criterion.description(),
                    criterion.sortOrder(), descriptors, criterion.knowledgePointId(),
                    subPoints.getOrDefault(criterion.code(), List.of()));
        }).toList();
        List<Rubric.Example> examples = jdbc.query("SELECT band, prompt, essay, commentary FROM rubric_examples "
                        + "WHERE rubric_id=:id ORDER BY band, id", p,
                (rs, row) -> new Rubric.Example(rs.getBigDecimal("band").doubleValue(), rs.getString("prompt"),
                        rs.getString("essay"), rs.getString("commentary")));
        return new Rubric((String) parent.get("code"), (String) parent.get("task"),
                ((Number) parent.get("rubric_version")).intValue(), (String) parent.get("source_note"),
                (String) parent.get("status"), criteria, examples);
    }

    private static void validateForActivation(Rubric rubric) {
        Set<String> expectedCriteria = "TASK_1".equals(rubric.task())
                ? Set.of("TA", "CC", "LR", "GRA") : Set.of("TR", "CC", "LR", "GRA");
        Set<String> actualCriteria = rubric.criteria().stream().map(Rubric.Criterion::code)
                .collect(java.util.stream.Collectors.toSet());
        if (!expectedCriteria.equals(actualCriteria)) {
            throw new IllegalArgumentException("Rubric " + rubric.task() + " requires criteria " + expectedCriteria);
        }
        List<Double> expectedBands = java.util.stream.IntStream.rangeClosed(0, 10)
                .mapToDouble(step -> 4.0 + step * 0.5).boxed().toList();
        for (Rubric.Criterion criterion : rubric.criteria()) {
            List<Double> actualBands = criterion.bandDescriptors().stream().map(Rubric.BandDescriptor::band)
                    .sorted().toList();
            if (!expectedBands.equals(actualBands)) {
                throw new IllegalArgumentException("Rubric criterion " + criterion.code()
                        + " must have one descriptor for every band from 4.0 to 9.0 in 0.5 steps");
            }
        }
    }

    private void saveChildren(Rubric rubric) {
        UUID id = jdbc.queryForObject("SELECT id FROM rubrics WHERE code=:code AND rubric_version=:version",
                new MapSqlParameterSource().addValue("code", rubric.code()).addValue("version", rubric.version()), UUID.class);
        saveChildren(id, rubric);
    }

    private void saveChildren(UUID id, Rubric rubric) {
        for (Rubric.Criterion criterion : rubric.criteria()) {
            UUID criterionId = UUID.randomUUID();
            jdbc.update("INSERT INTO rubric_criteria (id, rubric_id, code, name, description, sort_order) "
                            + "VALUES (:id, :rubricId, :code, :name, :description, :sortOrder)",
                    new MapSqlParameterSource().addValue("id", criterionId).addValue("rubricId", id)
                            .addValue("code", criterion.code()).addValue("name", criterion.name())
                            .addValue("description", criterion.description()).addValue("sortOrder", criterion.sortOrder()));
            for (Rubric.BandDescriptor descriptor : criterion.bandDescriptors()) {
                jdbc.update("INSERT INTO rubric_band_descriptors (criterion_id, band, descriptor) VALUES (:criterionId, :band, :descriptor)",
                        new MapSqlParameterSource().addValue("criterionId", criterionId).addValue("band", descriptor.band())
                                .addValue("descriptor", descriptor.descriptor()));
            }
        }
        for (Rubric.Example example : rubric.examples()) {
            jdbc.update("INSERT INTO rubric_examples (rubric_id, band, prompt, essay, commentary) "
                            + "VALUES (:rubricId, :band, :prompt, :essay, :commentary)",
                    new MapSqlParameterSource().addValue("rubricId", id).addValue("band", example.band())
                            .addValue("prompt", example.prompt()).addValue("essay", example.essay())
                            .addValue("commentary", example.commentary()));
        }
    }

    private static MapSqlParameterSource params(Rubric rubric) {
        return new MapSqlParameterSource().addValue("code", rubric.code()).addValue("task", rubric.task())
                .addValue("version", rubric.version()).addValue("sourceNote", rubric.sourceNote());
    }

    private record CriterionRow(UUID id, String code, String name, String description, int sortOrder,
                                UUID knowledgePointId) {}
    private record SubRow(String criterionCode, Rubric.SubKnowledgePoint point) {}
}
