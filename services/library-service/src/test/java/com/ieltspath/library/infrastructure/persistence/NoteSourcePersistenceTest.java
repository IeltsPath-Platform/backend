package com.ieltspath.library.infrastructure.persistence;

import com.ieltspath.library.domain.aggregate.Note;
import com.ieltspath.library.domain.repository.NoteRepository;
import com.ieltspath.library.domain.vo.LibraryStatus;
import com.ieltspath.library.domain.vo.NoteSourceType;
import com.ieltspath.library.infrastructure.adapter.NoteRepositoryAdapter;
import com.ieltspath.library.infrastructure.persistence.mapper.NoteMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NoteRepositoryAdapter.class, NoteMapperImpl.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class NoteSourcePersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private NoteRepository notes;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void sourceRoundTripsAndFilteringIsScopedToOwner() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        Note legacy = notes.save(Note.create(owner, "Manual", "Body"));
        Note matching = notes.save(Note.create(owner, "Lesson", "Body",
                NoteSourceType.KNOWLEDGE_POINT, sourceId));
        notes.save(Note.create(other, "Other learner", "Body",
                NoteSourceType.KNOWLEDGE_POINT, sourceId));
        notes.save(Note.create(owner, "Other source", "Body",
                NoteSourceType.TUTOR_SESSION, sourceId));

        Note restored = notes.findByIdAndUserId(matching.getId(), owner).orElseThrow();
        assertEquals(NoteSourceType.KNOWLEDGE_POINT, restored.getSourceType());
        assertEquals(sourceId, restored.getSourceReferenceId());
        Note restoredLegacy = notes.findByIdAndUserId(legacy.getId(), owner).orElseThrow();
        assertNull(restoredLegacy.getSourceType());
        assertNull(restoredLegacy.getSourceReferenceId());

        var byType = notes.findByUserIdAndStatusAndSourceType(
                owner, LibraryStatus.ACTIVE, NoteSourceType.KNOWLEDGE_POINT, 0, 20);
        assertEquals(1, byType.total());
        assertEquals(matching.getId(), byType.items().getFirst().getId());

        var byPair = notes.findByUserIdAndStatusAndSourceTypeAndSourceReferenceId(
                owner, LibraryStatus.ACTIVE, NoteSourceType.KNOWLEDGE_POINT, sourceId, 0, 20);
        assertEquals(1, byPair.total());
        assertEquals(matching.getId(), byPair.items().getFirst().getId());
    }
}
