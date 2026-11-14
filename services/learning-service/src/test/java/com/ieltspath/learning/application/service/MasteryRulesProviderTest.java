package com.ieltspath.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import com.ieltspath.learning.domain.vo.MasteryRules;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The rules in force: the service configuration, with Admin overrides on top when they are valid together. */
class MasteryRulesProviderTest {
    private final LearningRuleConfigRepository configs = mock(LearningRuleConfigRepository.class);
    private final MasteryRulesProvider provider =
            new MasteryRulesProvider(configs, new ObjectMapper(), 0.6, 0.8, List.of(7, 14, 30, 45, 60), 3);

    @Test
    void withoutOverridesTheServiceConfigurationIsUsed() {
        when(configs.findAll()).thenReturn(Map.of());
        assertThat(provider.current()).isEqualTo(new MasteryRules(0.6, 0.8, List.of(7, 14, 30, 45, 60), 3));
        assertThat(provider.overriddenKeys()).isEmpty();
    }

    @Test
    void oneOverrideReplacesOnlyItsValue() {
        when(configs.findAll()).thenReturn(Map.of(MasteryRulesProvider.PRIORITY_BELOW, "0.7", "unrelated.key", "1"));
        MasteryRules current = provider.current();
        assertThat(current.priorityBelow()).isEqualTo(0.7);
        assertThat(current.maintainFrom()).isEqualTo(0.8);
        assertThat(provider.overriddenKeys()).containsExactly(MasteryRulesProvider.PRIORITY_BELOW);
    }

    @Test
    void anInvalidStoredSetIsIgnoredWhole() {
        MasteryRules configured = provider.configured();
        for (Map<String, String> stored : List.of(
                Map.of(MasteryRulesProvider.PRIORITY_BELOW, "0.9", MasteryRulesProvider.MAINTAIN_FROM, "0.8"),
                Map.of(MasteryRulesProvider.PRIORITY_BELOW, "\"abc\""),
                Map.of(MasteryRulesProvider.REVIEW_INTERVALS_DAYS, "[7, 1.5]"),
                Map.of(MasteryRulesProvider.CONSOLIDATE_DAYS, "not json"))) {
            when(configs.findAll()).thenReturn(stored);
            assertThat(provider.current()).as("%s", stored).isEqualTo(configured);
        }
    }
}
