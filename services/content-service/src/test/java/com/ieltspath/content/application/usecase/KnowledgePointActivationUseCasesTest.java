package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.service.TrapKnowledgePointSync;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.KnowledgePointNotFoundException;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Turning a parent point on or off must reach its error-trap children too. */
class KnowledgePointActivationUseCasesTest {
    private final KnowledgePointRepository points = mock(KnowledgePointRepository.class);
    private final TrapKnowledgePointSync traps = mock(TrapKnowledgePointSync.class);
    private final KnowledgePoint point = KnowledgePoint.create(UUID.randomUUID(), "R55_EVIDENCE", "Evidence",
            KnowledgePointKind.STRATEGY, LearningType.PROCEDURE, Skill.READING, null, CefrLevel.B1, "Find evidence.", null, null);

    KnowledgePointActivationUseCasesTest() {
        when(points.findById(point.getId())).thenReturn(Optional.of(point));
        when(points.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void deactivatingAParentAlsoSyncsItsTrapChildren() {
        new DeactivateKnowledgePointUseCase(points, traps).execute(point.getId());
        assertThat(point.getStatus()).isEqualTo(ContentStatus.INACTIVE);
        var order = inOrder(points, traps);
        order.verify(points).save(point);
        order.verify(traps).syncForKnowledgePoint(point.getId());
    }

    @Test
    void activatingAPointSavesItAndResyncsItsTrapChildren() {
        point.deactivate();
        var result = new ActivateKnowledgePointUseCase(points, traps).execute(point.getId());
        assertThat(result.status()).isEqualTo(ContentStatus.ACTIVE);
        var order = inOrder(points, traps);
        order.verify(points).save(point);
        order.verify(traps).syncForKnowledgePoint(point.getId());
    }

    @Test
    void anUnknownPointIsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(points.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new ActivateKnowledgePointUseCase(points, traps).execute(unknown))
                .isInstanceOf(KnowledgePointNotFoundException.class);
        verify(points, never()).save(any());
        verifyNoInteractions(traps);
    }
}
