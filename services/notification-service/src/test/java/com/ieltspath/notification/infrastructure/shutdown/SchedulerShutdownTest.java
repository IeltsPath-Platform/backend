package com.ieltspath.notification.infrastructure.shutdown;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerShutdownTest {
    @Test
    void waitsForRunningScheduledWorkDuringShutdown() throws IOException, InterruptedException {
        PropertySource<?> application = new YamlPropertySourceLoader()
                .load("application", new FileSystemResource(Path.of("../../infra/config-server/config-repo/application.yaml")))
                .getFirst();
        assertThat(application.getProperty("spring.task.scheduling.shutdown.await-termination")).isEqualTo(true);
        assertThat(application.getProperty("spring.task.scheduling.shutdown.await-termination-period")).isEqualTo("20s");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);
        scheduler.initialize();
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean finished = new AtomicBoolean();
        scheduler.schedule(() -> {
            started.countDown();
            try {
                Thread.sleep(300);
                finished.set(true);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }, Instant.now());

        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        scheduler.destroy();

        assertThat(finished).isTrue();
    }
}
