package com.ieltspath.access.infrastructure.scheduler;

import com.ieltspath.access.application.port.WalletReconciliationStore;
import com.ieltspath.access.application.usecase.ReconcileWalletsUseCase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WalletReconciliationSchedulerConditionTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class, WalletReconciliationScheduler.class);

    @Test
    void schedulerIsAbsentWhenDisabledByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(WalletReconciliationScheduler.class));
    }

    @Test
    void enabledSchedulerRunsTheUseCase() {
        contextRunner.withPropertyValues("access.points.reconciliation.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(WalletReconciliationScheduler.class);
                    context.getBean(WalletReconciliationScheduler.class).reconcile();
                    verify(context.getBean(WalletReconciliationStore.class)).findMismatches(100);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {
        @Bean WalletReconciliationStore store() { return mock(WalletReconciliationStore.class); }
        @Bean SimpleMeterRegistry meterRegistry() { return new SimpleMeterRegistry(); }
        @Bean ReconcileWalletsUseCase useCase(WalletReconciliationStore store, SimpleMeterRegistry registry) {
            return new ReconcileWalletsUseCase(store, registry, 100);
        }
    }
}
