package com.ieltspath.learning.domain.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.service.MasteryFormulaSettings;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HumanReviewMp1ParamsTest {
    @Test
    void anExaminersBandWeighsMoreThanAnAiEssayAndLessThanAFormalResult() {
        var weights = Mp1Params.defaults().weights();

        assertEquals(0.75, weights.of(EvidenceSource.HUMAN_REVIEW));
        assertEquals(0.5, weights.of(EvidenceSource.LESSON_WRITING));
        assertEquals(1.0, weights.of(EvidenceSource.ASSESSMENT));
    }

    @Test
    void anExaminersBandDoesNotUseThePracticeCap() {
        assertFalse(Mp1Params.isPractice(EvidenceSource.HUMAN_REVIEW));
    }

    @Test
    void theHumanReviewWeightIsSetByConfigurationAndCarriedThroughTheSettings() {
        LearningRuleConfigRepository configs = mock(LearningRuleConfigRepository.class);
        when(configs.findAll()).thenReturn(Map.of());
        MasteryFormulaSettings settings = new MasteryFormulaSettings(configs, new ObjectMapper(), "MP1", 8, 3,
                30, 3, 2, 1.0, 0.03, 7, 1.0, 1.0, 1.0, 0.6, 0.5, 0.3, 0.9);

        assertEquals(0.9, settings.params().weights().of(EvidenceSource.HUMAN_REVIEW));
    }
}
