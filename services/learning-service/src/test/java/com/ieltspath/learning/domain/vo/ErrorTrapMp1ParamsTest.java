package com.ieltspath.learning.domain.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.service.MasteryFormulaSettings;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorTrapMp1ParamsTest {
    @Test
    void errorTrapEvidenceHasHalfPracticeWeightAndDoesNotUsePracticeCap() {
        var params = Mp1Params.defaults();

        assertEquals(0.3, params.weights().of(EvidenceSource.ERROR_TRAP));
        assertFalse(Mp1Params.isPractice(EvidenceSource.ERROR_TRAP));
    }

    @Test
    void adminRuleConfigurationOverridesTheErrorTrapWeight() {
        LearningRuleConfigRepository configs = mock(LearningRuleConfigRepository.class);
        when(configs.findAll()).thenReturn(java.util.Map.of(MasteryFormulaSettings.ERROR_TRAP_WEIGHT_KEY, "0.45"));
        MasteryFormulaSettings settings = new MasteryFormulaSettings(configs, new ObjectMapper(), "MP1", 8, 3,
                30, 3, 2, 1.0, 0.03, 7, 1.0, 1.0, 1.0, 0.6, 0.5, 0.3);

        assertEquals(0.45, settings.params().weights().of(EvidenceSource.ERROR_TRAP));
    }
}
