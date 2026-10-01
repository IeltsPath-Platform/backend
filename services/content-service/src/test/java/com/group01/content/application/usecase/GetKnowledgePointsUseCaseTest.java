package com.group01.content.application.usecase;

import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.aggregate.Topic;
import com.group01.content.domain.repository.KnowledgePointRepository;
import com.group01.content.domain.repository.TopicRepository;
import com.group01.content.domain.vo.BandRange;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.LearningType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetKnowledgePointsUseCaseTest {
    @Mock KnowledgePointRepository knowledgePoints;
    @Mock TopicRepository topics;

    private static BandRange range(String min, String max) {
        return BandRange.of(min == null ? null : new BigDecimal(min), max == null ? null : new BigDecimal(max));
    }

    @Test
    void knowledgePointsExposeTheirOwnAndTheirEffectiveBand() {
        Topic basic = Topic.create(null, "BASIC", "Basic grammar", 0, range("4.0", "5.0"));
        Topic open = Topic.create(null, "OPEN", "No band yet", 1, BandRange.UNBOUNDED);
        KnowledgePoint inherits = kp(basic, "KP-INHERIT", BandRange.UNBOUNDED);
        KnowledgePoint overrides = kp(basic, "KP-OVERRIDE", range("6.0", null));
        KnowledgePoint unbanded = kp(open, "KP-OPEN", BandRange.UNBOUNDED);
        when(knowledgePoints.findAll()).thenReturn(List.of(inherits, overrides, unbanded));
        when(topics.findAllByIds(any())).thenReturn(List.of(basic, open));

        List<KnowledgePointResult> results = new GetKnowledgePointsUseCase(knowledgePoints, topics).execute(null);

        assertThat(results.get(0).band()).isEqualTo(BandRange.UNBOUNDED);
        assertThat(results.get(0).effectiveBand()).isEqualTo(range("4.0", "5.0"));
        assertThat(results.get(1).band()).isEqualTo(range("6.0", null));
        assertThat(results.get(1).effectiveBand()).isEqualTo(range("6.0", null));
        assertThat(results.get(2).effectiveBand()).isEqualTo(BandRange.UNBOUNDED);
        // One batched topic lookup, not one per knowledge point.
        verify(topics, times(1)).findAllByIds(Set.of(basic.getId(), open.getId()));
    }

    private static KnowledgePoint kp(Topic topic, String code, BandRange band) {
        return KnowledgePoint.create(topic.getId(), code, code, KnowledgePointKind.GRAMMAR, LearningType.PROCEDURE,
                null, null, band);
    }
}
