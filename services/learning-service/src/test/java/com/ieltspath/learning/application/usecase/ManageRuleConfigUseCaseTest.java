package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.command.UpdateRuleConfigCommand;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.service.MasteryProfileMaintenance;
import com.ieltspath.learning.application.service.MasteryRulesProvider;
import com.ieltspath.learning.domain.repository.LearningRuleConfigRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** An Admin changes the mastery thresholds; the rules must stay valid as a whole. */
class ManageRuleConfigUseCaseTest {
    private final Map<String, String> stored = new HashMap<>();
    private final LearningRuleConfigRepository configs = mock(LearningRuleConfigRepository.class);
    private final MasteryProfileMaintenance maintenance = mock(MasteryProfileMaintenance.class);
    private final ObjectMapper json = new ObjectMapper();
    private final MasteryRulesProvider rules = new MasteryRulesProvider(configs, json, 0.6, 0.8, List.of(7, 14, 30, 45, 60), 3);
    private final ManageRuleConfigUseCase useCase = new ManageRuleConfigUseCase(rules, configs, maintenance, json);
    private final UUID admin = UUID.randomUUID();

    ManageRuleConfigUseCaseTest() {
        when(configs.findAll()).thenAnswer(call -> Map.copyOf(stored));
        doAnswer(call -> stored.put(call.getArgument(0), call.getArgument(1)))
                .when(configs).upsert(anyString(), anyString(), any());
    }

    private static int status(Throwable error) { return ((LearningRequestException) error).getStatus(); }
    private static String code(Throwable error) { return ((LearningRequestException) error).getCode(); }

    @Test
    void aPartialUpdateStoresOnlyTheGivenValue() {
        var result = useCase.update(admin, new UpdateRuleConfigCommand(0.65, null, null, null));

        assertThat(stored).containsOnlyKeys(MasteryRulesProvider.PRIORITY_BELOW);
        assertThat(result.priorityBelow()).isEqualTo(0.65);
        assertThat(result.maintainFrom()).isEqualTo(0.8);
        assertThat(result.reviewIntervalsDays()).containsExactly(7, 14, 30, 45, 60);
        assertThat(result.consolidateDays()).isEqualTo(3);
        assertThat(result.overridden()).containsExactly(MasteryRulesProvider.PRIORITY_BELOW);
        verify(configs).upsert(MasteryRulesProvider.PRIORITY_BELOW, "0.65", admin);
    }

    @Test
    void aValueThatBreaksTheRulesAsAWholeIsRefusedAndNothingIsStored() {
        assertThatThrownBy(() -> useCase.update(admin, new UpdateRuleConfigCommand(0.85, null, null, null)))
                .satisfies(error -> {
                    assertThat(status(error)).isEqualTo(422);
                    assertThat(code(error)).isEqualTo("INVALID_RULE_CONFIG");
                });
        verify(configs, never()).upsert(anyString(), anyString(), any());
    }

    @Test
    void decreasingIntervalsOrAZeroConsolidationAreRefused() {
        for (UpdateRuleConfigCommand command : List.of(new UpdateRuleConfigCommand(null, null, List.of(14, 7), null),
                new UpdateRuleConfigCommand(null, null, null, 0))) {
            assertThatThrownBy(() -> useCase.update(admin, command))
                    .satisfies(error -> assertThat(code(error)).isEqualTo("INVALID_RULE_CONFIG"));
        }
        verify(configs, never()).upsert(anyString(), anyString(), any());
    }

    @Test
    void recomputeReportsWhenAJobIsAlreadyRunning() {
        when(maintenance.startRecompute()).thenReturn(false);
        assertThat(useCase.recompute()).isFalse();
        verify(maintenance).startRecompute();
    }
}
