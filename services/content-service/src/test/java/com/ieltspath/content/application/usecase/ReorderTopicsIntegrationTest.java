package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.repository.TopicRepository;
import com.ieltspath.content.infrastructure.persistence.adapter.TopicRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.TopicPersistenceMapper;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Reordering on the real schema: the new order is what is stored, and other groups are untouched. */
@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReorderTopicsUseCase.class, TopicRepositoryAdapter.class, TopicPersistenceMapper.class})
@Testcontainers(disabledWithoutDocker = true)
class ReorderTopicsIntegrationTest {

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
    @Autowired ReorderTopicsUseCase reorder;
    @Autowired TopicRepository topics;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private final String tag = "RO" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

    private UUID topic(UUID parent, String code, int order) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO topics(id,parent_topic_id,code,name,sort_order) VALUES(?,?,?,?,?)", id, parent,
                tag + "_" + code, code, order);
        return id;
    }

    private List<UUID> storedOrder(UUID parent) {
        return jdbc.queryForList("SELECT id FROM topics WHERE parent_topic_id = ? ORDER BY sort_order", UUID.class,
                parent);
    }

    @Test
    void childrenAreStoredInTheNewOrderWithTheirOwnSortOrders() {
        UUID parent = topic(null, "P", 800_000 + (int) (Math.random() * 90_000));
        UUID a = topic(parent, "A", 10);
        UUID b = topic(parent, "B", 20);
        UUID c = topic(parent, "C", 35);
        UUID otherParent = topic(null, "Q", 890_000 + (int) (Math.random() * 90_000));
        UUID unrelated = topic(otherParent, "U", 10);

        reorder.execute(parent, List.of(c, a, b));
        entityManager.flush();

        assertThat(storedOrder(parent)).containsExactly(c, a, b);
        assertThat(jdbc.queryForList("SELECT sort_order FROM topics WHERE parent_topic_id = ? ORDER BY sort_order",
                Integer.class, parent)).containsExactly(10, 20, 35);
        assertThat(jdbc.queryForObject("SELECT sort_order FROM topics WHERE id = ?", Integer.class, unrelated))
                .isEqualTo(10);
    }

    @Test
    void theTopLevelGroupCanBeReversedAndRestored() {
        List<UUID> before = jdbc.queryForList("SELECT id FROM topics WHERE parent_topic_id IS NULL ORDER BY sort_order",
                UUID.class);
        assertThat(before).hasSizeGreaterThan(2);
        List<UUID> reversed = new ArrayList<>(before);
        Collections.reverse(reversed);

        // The seeded top-level topics share some sort orders, so this also clears those ties.
        reorder.execute(null, reversed);
        entityManager.flush();

        assertThat(jdbc.queryForList("SELECT id FROM topics WHERE parent_topic_id IS NULL ORDER BY sort_order",
                UUID.class)).containsExactlyElementsOf(reversed);
        assertThat(jdbc.queryForList("SELECT sort_order FROM topics WHERE parent_topic_id IS NULL",
                Integer.class)).doesNotHaveDuplicates();

        reorder.execute(null, before);
        entityManager.flush();

        assertThat(jdbc.queryForList("SELECT id FROM topics WHERE parent_topic_id IS NULL ORDER BY sort_order",
                UUID.class)).containsExactlyElementsOf(before);
    }

    @Test
    void aTiedGroupGetsDistinctOrders() {
        UUID parent = topic(null, "T", 700_000 + (int) (Math.random() * 90_000));
        UUID a = topic(parent, "A", 5);
        UUID b = topic(parent, "B", 5);

        reorder.execute(parent, List.of(b, a));
        entityManager.flush();

        assertThat(storedOrder(parent)).containsExactly(b, a);
        assertThat(jdbc.queryForList("SELECT sort_order FROM topics WHERE parent_topic_id = ? ORDER BY sort_order",
                Integer.class, parent)).containsExactly(5, 6);
        assertThat(topics.findByParentTopicId(parent)).extracting(topic -> topic.getId()).containsExactly(b, a);
    }
}
