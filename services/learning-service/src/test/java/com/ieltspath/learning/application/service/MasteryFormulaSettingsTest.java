package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The formula in force can be switched by a stored row; anything unreadable falls back to the configuration. */
class MasteryFormulaSettingsTest {
    private final LearningRuleConfigRepository configs = mock(LearningRuleConfigRepository.class);

    private MasteryFormulaSettings settings(String policy) {
        return new MasteryFormulaSettings(configs, new ObjectMapper(), policy, 8, 3, 30, 3, 2, 1.0, 0.03, 7,
                1.0, 1.0, 1.0, 0.6, 0.5, 0.3, 0.75);
    }

    @Test
    void aStoredFormulaSwitchesTheFormulaAndUnreadableValuesFallBack() {
        MasteryFormulaSettings settings = settings("DEEPTUTOR");
        when(configs.findAll()).thenReturn(Map.of(MasteryFormulaSettings.POLICY_KEY, "\"mp1\""));
        assertThat(settings.formula()).isEqualTo(MasteryFormulaSettings.Formula.MP1);
        assertThat(settings.usesMp1()).isTrue();
        for (String unreadable : List.of("42", "\"OTHER\"", "not json")) {
            when(configs.findAll()).thenReturn(Map.of(MasteryFormulaSettings.POLICY_KEY, unreadable));
            assertThat(settings.formula()).as(unreadable).isEqualTo(MasteryFormulaSettings.Formula.DEEPTUTOR);
        }
    }

    @Test
    void anInvalidStoredErrorTrapWeightKeepsTheConfiguredOne() {
        MasteryFormulaSettings settings = settings("MP1");
        for (String invalid : List.of("-0.2", "\"abc\"", "NaN")) {
            when(configs.findAll()).thenReturn(Map.of(MasteryFormulaSettings.ERROR_TRAP_WEIGHT_KEY, invalid));
            assertThat(settings.params().weights().of(EvidenceSource.ERROR_TRAP)).as(invalid).isEqualTo(0.3);
        }
    }
}
