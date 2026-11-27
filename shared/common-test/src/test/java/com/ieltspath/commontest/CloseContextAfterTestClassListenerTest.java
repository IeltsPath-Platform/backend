package com.ieltspath.commontest;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;

class CloseContextAfterTestClassListenerTest {

    /** The manager picks up listeners only through META-INF/spring.factories, so this also proves the registration. */
    @Test
    void contextOfATestClassIsClosedWhenTheClassEnds() throws Exception {
        TestContextManager manager = new TestContextManager(Probe.class);
        manager.beforeTestClass();
        var context = (ConfigurableApplicationContext) manager.getTestContext().getApplicationContext();
        assertThat(context.isActive()).isTrue();

        manager.afterTestClass();

        assertThat(context.isActive()).isFalse();
    }

    @Configuration
    static class EmptyConfiguration {
    }

    @ContextConfiguration(classes = EmptyConfiguration.class)
    static class Probe {
    }
}
