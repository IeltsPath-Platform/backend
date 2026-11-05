package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.KnowledgePointCatalogRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ErrorTrapEvidenceTest {
    private final UUID child = UUID.randomUUID();
    private final ErrorTrapEvidence service = new ErrorTrapEvidence(
            new ErrorTrapCatalog(mock(KnowledgePointCatalogRepository.class)), mock(ErrorTrapReviews.class),
            mock(ErrorTypeTexts.class));

    @Test
    void anAppendedTrapIsExcludedFromMilestoneXp() {
        List<KnowledgeEvidence> evidence = new ArrayList<>();
        Set<UUID> withoutMilestone = new HashSet<>();

        service.append(evidence, withoutMilestone,
                Map.of("distractors", Map.of("B", "TFNG_NG_AS_FALSE")), false, "TFNG_NG_AS_FALSE",
                UUID.randomUUID(), ignored -> UUID.randomUUID(), catalog());

        assertEquals(1, evidence.size());
        assertEquals(EvidenceSource.ERROR_TRAP, evidence.getFirst().source());
        assertEquals(child, evidence.getFirst().knowledgePointId());
        assertEquals(Set.of(child), withoutMilestone);
    }

    @Test
    void missingTrapMappingSkipsEvidenceWithoutFailingTheSubmission() {
        List<KnowledgeEvidence> evidence = new ArrayList<>();
        Set<UUID> withoutMilestone = new HashSet<>();

        assertDoesNotThrow(() -> service.append(evidence, withoutMilestone,
                Map.of("distractors", Map.of("B", "UNKNOWN_CODE")), false, "UNKNOWN_CODE", UUID.randomUUID(),
                ignored -> UUID.randomUUID(), catalog()));

        assertTrue(evidence.isEmpty());
        assertTrue(withoutMilestone.isEmpty());
    }

    private ErrorTrapCatalog.ErrorTraps catalog() {
        return new ErrorTrapCatalog.ErrorTraps(Map.of("TFNG_NG_AS_FALSE", child), Map.of(), Map.of());
    }
}
