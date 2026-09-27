package com.group01.learningsupport.infrastructure.persistence;

import com.group01.learningsupport.domain.vo.FlashcardSourceType;
import com.group01.learningsupport.domain.vo.LibraryStatus;
import com.group01.learningsupport.infrastructure.persistence.entity.FlashcardJpaEntity;
import com.group01.learningsupport.infrastructure.persistence.repository.FlashcardJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class FlashcardOptimisticLockTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private FlashcardJpaRepository repository;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void secondWriteFromTheSameVersionIsRejected() {
        FlashcardJpaEntity card = new FlashcardJpaEntity();
        card.setId(UUID.randomUUID());
        card.setUserId(UUID.randomUUID());
        card.setSourceType(FlashcardSourceType.MANUAL);
        card.setFront("front");
        card.setBack("back");
        card.setStatus(LibraryStatus.ACTIVE);
        repository.saveAndFlush(card);
        entityManager.clear();

        FlashcardJpaEntity first = repository.findById(card.getId()).orElseThrow();
        entityManager.detach(first);
        FlashcardJpaEntity stale = repository.findById(card.getId()).orElseThrow();
        entityManager.detach(stale);

        first.setBack("first edit");
        repository.saveAndFlush(first);
        entityManager.clear();

        stale.setBack("stale edit");
        assertThrows(OptimisticLockingFailureException.class, () -> repository.saveAndFlush(stale));
    }
}
