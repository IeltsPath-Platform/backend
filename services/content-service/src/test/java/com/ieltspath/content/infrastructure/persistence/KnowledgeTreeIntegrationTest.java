package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.command.UpdateKnowledgePointCommand;
import com.ieltspath.content.application.usecase.GetActiveKnowledgePrerequisitesUseCase;
import com.ieltspath.content.application.usecase.GetKnowledgeTreeHealthUseCase;
import com.ieltspath.content.application.usecase.GetKnowledgeTreeUseCase;
import com.ieltspath.content.application.usecase.ReplaceKnowledgePointPrerequisitesUseCase;
import com.ieltspath.content.application.usecase.UpdateKnowledgePointUseCase;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.aggregate.Topic;
import com.ieltspath.content.domain.exception.KnowledgePointInUseException;
import com.ieltspath.content.domain.exception.PrerequisiteCycleException;
import com.ieltspath.content.domain.exception.PrerequisiteLevelException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.persistence.adapter.KnowledgePointRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcKnowledgePrerequisiteRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.JdbcLearningContentReader;
import com.ieltspath.content.infrastructure.persistence.mapper.KnowledgePointPersistenceMapper;
import com.ieltspath.content.infrastructure.persistence.repository.KnowledgePointJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class KnowledgeTreeIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private static PGSimpleDataSource dataSource;
    private static EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private JdbcTemplate jdbc;
    private NamedParameterJdbcTemplate namedJdbc;
    private JdbcLearningContentReader reader;
    private JdbcKnowledgePrerequisiteRepository prerequisiteRepository;
    private JdbcTestKnowledgePointRepository knowledgePointRepository;
    private KnowledgePointRepositoryAdapter productionKnowledgePointRepository;
    private JdbcTestTopicRepository topicRepository;
    private GetKnowledgeTreeUseCase getKnowledgeTree;
    private GetActiveKnowledgePrerequisitesUseCase getActivePrerequisites;
    private ReplaceKnowledgePointPrerequisitesUseCase replacePrerequisites;
    private UpdateKnowledgePointUseCase updateKnowledgePoint;
    private TransactionTemplate transactions;
    private final List<UUID> courseIds = new ArrayList<>();
    private final List<UUID> topicIds = new ArrayList<>();
    private final List<UUID> knowledgePointIds = new ArrayList<>();
    private final List<UUID> questionIds = new ArrayList<>();
    private final List<UUID> lessonIds = new ArrayList<>();

    @BeforeAll
    static void migrateToPhaseOneSchema() {
        dataSource = new PGSimpleDataSource();
        dataSource.setURL(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.ieltspath.content.infrastructure.persistence.entity");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        factory.setJpaProperties(properties);
        factory.afterPropertiesSet();
        entityManagerFactory = factory.getObject();
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        namedJdbc = new NamedParameterJdbcTemplate(dataSource);
        reader = new JdbcLearningContentReader(namedJdbc);
        prerequisiteRepository = new JdbcKnowledgePrerequisiteRepository(namedJdbc);
        knowledgePointRepository = new JdbcTestKnowledgePointRepository(jdbc, namedJdbc);
        topicRepository = new JdbcTestTopicRepository(jdbc);
        entityManager = entityManagerFactory.createEntityManager();
        var jpaKnowledgePointRepository = new JpaRepositoryFactory(entityManager)
                .getRepository(KnowledgePointJpaRepository.class);
        productionKnowledgePointRepository = new KnowledgePointRepositoryAdapter(
                jpaKnowledgePointRepository, new KnowledgePointPersistenceMapper());
        getKnowledgeTree = new GetKnowledgeTreeUseCase(reader, prerequisiteRepository);
        getActivePrerequisites = new GetActiveKnowledgePrerequisitesUseCase(reader);
        replacePrerequisites = new ReplaceKnowledgePointPrerequisitesUseCase(
                knowledgePointRepository, prerequisiteRepository);
        updateKnowledgePoint = new UpdateKnowledgePointUseCase(productionKnowledgePointRepository, topicRepository,
                prerequisiteRepository);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @AfterEach
    void cleanFixtures() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        for (UUID id : knowledgePointIds) {
            jdbc.update("DELETE FROM lesson_knowledge_points WHERE knowledge_point_id = ?", id);
            jdbc.update("DELETE FROM question_knowledge_points WHERE knowledge_point_id = ?", id);
            jdbc.update("DELETE FROM knowledge_point_prerequisites WHERE knowledge_point_id = ? OR prerequisite_id = ?", id, id);
        }
        for (UUID id : lessonIds) {
            jdbc.update("DELETE FROM lessons WHERE id = ?", id);
        }
        for (UUID id : questionIds) {
            jdbc.update("DELETE FROM questions WHERE id = ?", id);
        }
        for (UUID id : knowledgePointIds) {
            jdbc.update("DELETE FROM knowledge_points WHERE id = ?", id);
        }
        for (UUID id : topicIds) {
            jdbc.update("DELETE FROM topics WHERE id = ?", id);
        }
        for (UUID id : courseIds) {
            jdbc.update("DELETE FROM courses WHERE id = ?", id);
        }
    }

    @Test
    void readsPrerequisiteEdgesAndRejectsAnIntroducedCycle() {
        UUID course = course();
        UUID topic = topic(course, Skill.READING);
        UUID a = knowledgePoint(topic, "A");
        UUID b = knowledgePoint(topic, "B");
        UUID c = knowledgePoint(topic, "C");

        replace(b, List.of(a));
        replace(c, List.of(b));

        var tree = getKnowledgeTree.execute(course, Skill.READING, false);
        assertThat(tree.nodes()).extracting(node -> node.id()).containsExactlyInAnyOrder(a, b, c);
        assertThat(tree.edges().stream().filter(edge -> knowledgePointIds.contains(edge.to())).toList())
                .containsExactlyInAnyOrder(
                new com.ieltspath.content.application.result.KnowledgeTreeResult.Edge(a, b),
                new com.ieltspath.content.application.result.KnowledgeTreeResult.Edge(b, c));

        assertThatThrownBy(() -> replace(a, List.of(c)))
                .isInstanceOf(PrerequisiteCycleException.class);
        assertThat(prerequisiteRepository.findAll().stream()
                .filter(edge -> knowledgePointIds.contains(edge.knowledgePointId())).toList()).hasSize(2);
    }

    @Test
    void knowledgePointMetadataPersistsAndAppearsInTheInternalTreeProjection() {
        UUID course = course();
        UUID topic = topic(course, Skill.READING);
        UUID id = knowledgePoint(topic, "METADATA");
        jdbc.update("""
                UPDATE knowledge_points
                SET cefr_level='B1', can_do='Find the main idea.', scope_in='Short passages.', scope_out='Long inference chains.'
                WHERE id=?
                """, id);

        KnowledgePoint persisted = productionKnowledgePointRepository.findById(id).orElseThrow();
        assertThat(persisted.getCefrLevel()).isEqualTo(CefrLevel.B1);
        assertThat(persisted.getCanDo()).isEqualTo("Find the main idea.");
        assertThat(persisted.getScopeIn()).isEqualTo("Short passages.");
        assertThat(persisted.getScopeOut()).isEqualTo("Long inference chains.");

        var node = reader.knowledgeTreeNodes(course, Skill.READING, false).stream()
                .filter(candidate -> candidate.id().equals(id)).findFirst().orElseThrow();
        assertThat(node.cefrLevel()).isEqualTo(CefrLevel.B1);
        assertThat(node.canDo()).isEqualTo("Find the main idea.");
        assertThat(node.scopeIn()).isEqualTo("Short passages.");
        assertThat(node.scopeOut()).isEqualTo("Long inference chains.");
    }

    @Test
    void concurrentOpposingPrerequisiteUpdatesAllowOnlyOneEdge() throws Exception {
        UUID course = course();
        UUID topic = topic(course, Skill.READING);
        UUID a = knowledgePoint(topic, "A");
        UUID b = knowledgePoint(topic, "B");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> aDependsOnB = executor.submit(() -> concurrentReplace(a, b, ready, start));
            Future<Boolean> bDependsOnA = executor.submit(() -> concurrentReplace(b, a, ready, start));
            ready.await();
            start.countDown();

            assertThat(List.of(aDependsOnB.get(), bDependsOnA.get())).containsExactlyInAnyOrder(true, false);
            assertThat(prerequisiteRepository.findAll().stream()
                    .filter(edge -> knowledgePointIds.contains(edge.knowledgePointId())).toList()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void inactiveKnowledgePointIsHiddenFromDefaultTreeAndInternalPrerequisitesUntilReactivated() {
        UUID course = course();
        UUID topic = topic(course, Skill.READING);
        UUID a = knowledgePoint(topic, "A");
        UUID b = knowledgePoint(topic, "B");
        UUID c = knowledgePoint(topic, "C");
        replace(b, List.of(a));
        replace(c, List.of(b));

        deactivate(b);

        var defaultTree = getKnowledgeTree.execute(course, Skill.READING, false);
        assertThat(defaultTree.nodes()).extracting(node -> node.id()).containsExactlyInAnyOrder(a, c);
        assertThat(defaultTree.edges()).isEmpty();
        assertThat(getActivePrerequisites.execute(List.of(a, b, c)))
                .containsOnlyKeys(a, c)
                .containsEntry(a, List.of())
                .containsEntry(c, List.of());

        var inclusiveTree = getKnowledgeTree.execute(course, Skill.READING, true);
        assertThat(inclusiveTree.nodes()).extracting(node -> node.id()).containsExactlyInAnyOrder(a, b, c);
        assertThat(inclusiveTree.edges()).hasSize(2);

        activate(b);

        var reactivatedTree = getKnowledgeTree.execute(course, Skill.READING, false);
        assertThat(reactivatedTree.nodes()).extracting(node -> node.id()).containsExactlyInAnyOrder(a, b, c);
        assertThat(reactivatedTree.edges()).hasSize(2);
        assertThat(getActivePrerequisites.execute(List.of(a, b, c)))
                .containsEntry(b, List.of(a))
                .containsEntry(c, List.of(b));
    }

    @Test
    void changingTopicIsRejectedWhenKnowledgePointHasPublishedQuestionOrLessonUsage() {
        UUID course = course();
        UUID originalTopic = topic(course, Skill.READING);
        UUID targetTopic = topic(course, Skill.READING);
        UUID questionMappedPoint = knowledgePoint(originalTopic, "QUESTION_USAGE");
        UUID lessonMappedPoint = knowledgePoint(originalTopic, "LESSON_USAGE");
        publishedQuestionUsage(questionMappedPoint);
        lessonUsage(targetTopic, lessonMappedPoint);

        assertThatThrownBy(() -> updateTopic(questionMappedPoint, targetTopic))
                .isInstanceOf(KnowledgePointInUseException.class);
        assertThatThrownBy(() -> updateTopic(lessonMappedPoint, targetTopic))
                .isInstanceOf(KnowledgePointInUseException.class);
    }

    @Test
    void replacingPrerequisitesRejectsAPrerequisiteWithAHigherCefrLevel() {
        UUID topic = topic(course(), Skill.READING);
        UUID dependent = knowledgePoint(topic, "DEPENDENT");
        UUID sameLevel = knowledgePoint(topic, "SAME");
        UUID higher = knowledgePoint(topic, "HIGHER");
        setCefrLevel(dependent, "B1");
        setCefrLevel(sameLevel, "B1");
        setCefrLevel(higher, "C1");

        assertThatThrownBy(() -> replace(dependent, List.of(sameLevel, higher)))
                .isInstanceOfSatisfying(PrerequisiteLevelException.class,
                        exception -> assertThat(exception.getInvalidIds()).containsExactly(higher));
        assertThat(prerequisiteRepository.findPrerequisiteIds(dependent)).isEmpty();

        replace(dependent, List.of(sameLevel));
        assertThat(prerequisiteRepository.findPrerequisiteIds(dependent)).containsExactly(sameLevel);
    }

    @Test
    void changingACefrLevelIsRejectedWhenItInvertsAnExistingEdge() {
        UUID topic = topic(course(), Skill.READING);
        UUID prerequisite = knowledgePoint(topic, "PRE");
        UUID dependent = knowledgePoint(topic, "DEP");
        replace(dependent, List.of(prerequisite));

        assertThatThrownBy(() -> updateCefrLevel(prerequisite, CefrLevel.C1))
                .isInstanceOfSatisfying(PrerequisiteLevelException.class,
                        exception -> assertThat(exception.getInvalidIds()).containsExactly(dependent));
        assertThatThrownBy(() -> updateCefrLevel(dependent, CefrLevel.B1))
                .isInstanceOfSatisfying(PrerequisiteLevelException.class,
                        exception -> assertThat(exception.getInvalidIds()).containsExactly(prerequisite));

    }

    @Test
    void healthReportsIsolatedPointsMissingMetadataAndInvertedEdges() {
        UUID topic = topic(course(), Skill.READING);
        UUID alone = knowledgePoint(topic, "ALONE");
        UUID noMetadata = knowledgePoint(topic, "NOMETA");
        UUID low = knowledgePoint(topic, "LOW");
        UUID high = knowledgePoint(topic, "HIGH");
        jdbc.update("UPDATE knowledge_points SET cefr_level = NULL, can_do = NULL WHERE id = ?", noMetadata);
        setCefrLevel(low, "B1");
        setCefrLevel(high, "C1");
        // The API refuses an inverted edge, so the fixture writes it directly.
        jdbc.update("INSERT INTO knowledge_point_prerequisites(knowledge_point_id, prerequisite_id) VALUES (?, ?)",
                low, high);

        var health = new GetKnowledgeTreeHealthUseCase(reader, prerequisiteRepository).execute();

        assertThat(health.isolatedIds()).contains(alone, noMetadata).doesNotContain(low, high);
        assertThat(health.missingMetadataIds()).contains(noMetadata).doesNotContain(alone, low, high);
        assertThat(health.levelViolations())
                .contains(new com.ieltspath.content.application.result.KnowledgeTreeHealthResult.LevelViolation(high, low));
    }

    private void replace(UUID id, List<UUID> prerequisites) {
        transactions.executeWithoutResult(status -> replacePrerequisites.execute(id, prerequisites));
    }

    private boolean concurrentReplace(UUID id, UUID prerequisiteId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            transactions.executeWithoutResult(status -> replacePrerequisites.execute(id, List.of(prerequisiteId)));
            return true;
        } catch (PrerequisiteCycleException expected) {
            return false;
        }
    }

    private void updateTopic(UUID id, UUID topicId) {
        KnowledgePoint point = knowledgePointRepository.findById(id).orElseThrow();
        updateKnowledgePoint.execute(id, new UpdateKnowledgePointCommand(topicId, point.getCode(), point.getName(),
                point.getKind(), point.getLearningType(), point.getSkill(), point.getDescription(),
                point.getCefrLevel(), point.getCanDo(), point.getScopeIn(), point.getScopeOut()));
    }

    private void updateCefrLevel(UUID id, CefrLevel level) {
        KnowledgePoint point = knowledgePointRepository.findById(id).orElseThrow();
        transactions.executeWithoutResult(status -> updateKnowledgePoint.execute(id, new UpdateKnowledgePointCommand(
                point.getTopicId(), point.getCode(), point.getName(), point.getKind(), point.getLearningType(),
                point.getSkill(), point.getDescription(), level, point.getCanDo(), point.getScopeIn(),
                point.getScopeOut())));
    }

    private void setCefrLevel(UUID id, String level) {
        jdbc.update("UPDATE knowledge_points SET cefr_level = ? WHERE id = ?", level, id);
    }

    private void deactivate(UUID id) {
        KnowledgePoint point = knowledgePointRepository.findById(id).orElseThrow();
        point.deactivate();
        knowledgePointRepository.save(point);
    }

    private void activate(UUID id) {
        KnowledgePoint point = knowledgePointRepository.findById(id).orElseThrow();
        point.activate();
        knowledgePointRepository.save(point);
    }

    private UUID course() {
        UUID id = UUID.randomUUID();
        courseIds.add(id);
        BigDecimal band = null;
        for (int halfBands = 1; halfBands <= 18; halfBands++) {
            BigDecimal candidate = BigDecimal.valueOf(halfBands).divide(BigDecimal.valueOf(2));
            Integer existing = jdbc.queryForObject("SELECT count(*) FROM courses WHERE band_level = ?", Integer.class,
                    candidate);
            if (existing != null && existing == 0) {
                band = candidate;
                break;
            }
        }
        if (band == null) {
            throw new IllegalStateException("No unused course band level is available for the integration fixture");
        }
        jdbc.update("INSERT INTO courses(id, code, name, band_level, status) VALUES (?, ?, ?, ?, 'ACTIVE')",
                id, "TREE_TEST_COURSE_" + id.toString().replace("-", ""), "Tree test course", band);
        return id;
    }

    private UUID topic(UUID courseId, Skill skill) {
        UUID id = UUID.randomUUID();
        topicIds.add(id);
        jdbc.update("INSERT INTO topics(id, code, name, sort_order, status, skill, course_id) VALUES (?, ?, ?, 1, 'ACTIVE', ?, ?)",
                id, "TREE_TEST_TOPIC_" + id.toString().replace("-", ""), "Tree test topic", skill.name(), courseId);
        return id;
    }

    private UUID knowledgePoint(UUID topicId, String suffix) {
        UUID id = UUID.randomUUID();
        knowledgePointIds.add(id);
        jdbc.update("""
                INSERT INTO knowledge_points(id, topic_id, code, name, kind, learning_type, skill, status,
                                             cefr_level, can_do)
                VALUES (?, ?, ?, ?, 'GRAMMAR', 'CONCEPT', 'READING', 'ACTIVE', 'B2', 'Fixture can-do.')
                """, id, topicId, "R55_TREE_" + suffix + "_" + id.toString().replace("-", ""), suffix);
        return id;
    }

    private void publishedQuestionUsage(UUID knowledgePointId) {
        UUID questionId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        questionIds.add(questionId);
        jdbc.update("INSERT INTO questions(id, question_type, skill, status) VALUES (?, 'MULTIPLE_CHOICE', 'READING', 'PUBLISHED')",
                questionId);
        jdbc.update("INSERT INTO question_versions(id, question_id, version_number, stem, status) VALUES (?, ?, 1, 'Published question', 'PUBLISHED')",
                versionId, questionId);
        jdbc.update("INSERT INTO question_knowledge_points(question_version_id, knowledge_point_id, weight) VALUES (?, ?, 1)",
                versionId, knowledgePointId);
    }

    private void lessonUsage(UUID topicId, UUID knowledgePointId) {
        UUID lessonId = UUID.randomUUID();
        lessonIds.add(lessonId);
        jdbc.update("INSERT INTO lessons(id, topic_id, code, title, sort_order, status) VALUES (?, ?, ?, 'Published lesson', 1, 'PUBLISHED')",
                lessonId, topicId, "TREE_TEST_LESSON_" + lessonId.toString().replace("-", ""));
        jdbc.update("INSERT INTO lesson_knowledge_points(lesson_id, knowledge_point_id) VALUES (?, ?)",
                lessonId, knowledgePointId);
    }

    private static KnowledgePoint knowledgePoint(ResultSet rs, int row) throws SQLException {
        return new KnowledgePoint(rs.getObject("id", UUID.class), rs.getObject("topic_id", UUID.class),
                rs.getString("code"), rs.getString("name"), KnowledgePointKind.valueOf(rs.getString("kind")),
                LearningType.valueOf(rs.getString("learning_type")),
                rs.getString("skill") == null ? null : Skill.valueOf(rs.getString("skill")),
                rs.getString("description"),
                rs.getString("cefr_level") == null ? null : CefrLevel.valueOf(rs.getString("cefr_level")),
                rs.getString("can_do"), rs.getString("scope_in"), rs.getString("scope_out"),
                ContentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private static Topic topic(ResultSet rs, int row) throws SQLException {
        return new Topic(rs.getObject("id", UUID.class), null, rs.getString("code"), rs.getString("name"),
                rs.getInt("sort_order"), ContentStatus.valueOf(rs.getString("status")), null,
                Skill.valueOf(rs.getString("skill")), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(), rs.getObject("course_id", UUID.class));
    }

    private static class JdbcTestKnowledgePointRepository implements KnowledgePointRepository {
        private static final String COLUMNS = "id, topic_id, code, name, kind, learning_type, skill, description, cefr_level, can_do, scope_in, scope_out, "
                + "status, created_at, updated_at";
        private final JdbcTemplate jdbc;
        private final NamedParameterJdbcTemplate namedJdbc;

        private JdbcTestKnowledgePointRepository(JdbcTemplate jdbc, NamedParameterJdbcTemplate namedJdbc) {
            this.jdbc = jdbc;
            this.namedJdbc = namedJdbc;
        }

        @Override
        public KnowledgePoint save(KnowledgePoint point) {
            jdbc.update("""
                    UPDATE knowledge_points SET topic_id = ?, code = ?, name = ?, kind = ?, learning_type = ?,
                        skill = ?, description = ?, cefr_level = ?, can_do = ?, scope_in = ?, scope_out = ?,
                        status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                    """, point.getTopicId(), point.getCode(), point.getName(), point.getKind().name(),
                    point.getLearningType().name(), point.getSkill() == null ? null : point.getSkill().name(),
                    point.getDescription(), point.getCefrLevel() == null ? null : point.getCefrLevel().name(),
                    point.getCanDo(), point.getScopeIn(), point.getScopeOut(), point.getStatus().name(),
                    point.getId());
            return findById(point.getId()).orElseThrow();
        }

        @Override
        public Optional<KnowledgePoint> findById(UUID id) {
            return jdbc.query("SELECT " + COLUMNS + " FROM knowledge_points WHERE id = ?", KnowledgeTreeIntegrationTest::knowledgePoint, id)
                    .stream().findFirst();
        }

        @Override
        public List<KnowledgePoint> findByIds(Collection<UUID> ids) {
            if (ids.isEmpty()) return List.of();
            return namedJdbc.query("SELECT " + COLUMNS + " FROM knowledge_points WHERE id IN (:ids)", Map.of("ids", ids),
                    KnowledgeTreeIntegrationTest::knowledgePoint);
        }

        @Override
        public Optional<KnowledgePoint> findByCode(String code) {
            return jdbc.query("SELECT " + COLUMNS + " FROM knowledge_points WHERE code = ?", KnowledgeTreeIntegrationTest::knowledgePoint, code)
                    .stream().findFirst();
        }

        @Override public List<KnowledgePoint> findByTopicId(UUID topicId) { throw new UnsupportedOperationException(); }
        @Override public List<KnowledgePoint> findAll() { throw new UnsupportedOperationException(); }
        @Override public boolean existsByCode(String code) { throw new UnsupportedOperationException(); }

        @Override
        public boolean isInUse(UUID id) {
            return Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM question_knowledge_points qkp
                        JOIN question_versions qv ON qv.id = qkp.question_version_id
                        WHERE qkp.knowledge_point_id = ? AND qv.status = 'PUBLISHED'
                    ) OR EXISTS (
                        SELECT 1 FROM lesson_knowledge_points lkp WHERE lkp.knowledge_point_id = ?
                    )
                    """, Boolean.class, id, id));
        }
    }

    private static class JdbcTestTopicRepository implements TopicRepository {
        private final JdbcTemplate jdbc;

        private JdbcTestTopicRepository(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @Override
        public Optional<Topic> findById(UUID id) {
            return jdbc.query("SELECT id, code, name, sort_order, status, skill, created_at, updated_at, course_id FROM topics WHERE id = ?",
                    KnowledgeTreeIntegrationTest::topic, id).stream().findFirst();
        }

        @Override public Topic save(Topic topic) { throw new UnsupportedOperationException(); }
        @Override public Optional<Topic> findByCode(String code) { throw new UnsupportedOperationException(); }
        @Override public List<Topic> findAll() { throw new UnsupportedOperationException(); }
        @Override public List<Topic> findAllByIds(Collection<UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public List<Topic> findRootTopics() { throw new UnsupportedOperationException(); }
        @Override public List<Topic> findByParentTopicId(UUID parentTopicId) { throw new UnsupportedOperationException(); }
        @Override public boolean existsByCode(String code) { throw new UnsupportedOperationException(); }
        @Override public boolean hasPublishedLessons(UUID topicId) { throw new UnsupportedOperationException(); }
    }
}
