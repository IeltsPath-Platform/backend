package com.ieltspath.commonstorage.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CommonStorageAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonStorageAutoConfiguration.class));

    @Test
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(ObjectStorage.class);
            assertThat(context).doesNotHaveBean(SupabaseStorageProperties.class);
        });
    }

    @Test
    void enabledWithoutConfigRegistersUnavailableStorage() {
        runner.withPropertyValues("app.storage.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(ObjectStorage.class)).isInstanceOf(UnavailableObjectStorage.class);
            assertThatThrownBy(() -> context.getBean(ObjectStorage.class).exists("avatars", "a/b"))
                    .isInstanceOf(StorageException.class)
                    .satisfies(e -> assertThat(((StorageException) e).reason())
                            .isEqualTo(StorageException.Reason.UNAVAILABLE));
        });
    }

    @Test
    void partialConfigFailsFastAndNamesPropertyOnly() {
        runner.withPropertyValues("app.storage.enabled=true",
                        "app.storage.supabase.url=https://abc.supabase.co")
                .run(context -> {
                    assertThat(context).hasFailed();
                    Throwable root = context.getStartupFailure();
                    while (root.getCause() != null) {
                        root = root.getCause();
                    }
                    assertThat(root.getMessage())
                            .isEqualTo("app.storage.supabase.url and app.storage.supabase.secret-key must be configured together");
                    assertThat(context.getStartupFailure().getMessage()).doesNotContain("abc.supabase.co");
                });
    }

    @Test
    void serviceOwnBeanWins() {
        ObjectStorage custom = new UnavailableObjectStorage();
        runner.withPropertyValues("app.storage.enabled=true")
                .withBean("custom", ObjectStorage.class, () -> custom)
                .run(context -> assertThat(context.getBean(ObjectStorage.class)).isSameAs(custom));
    }

    @Test
    void autoConfigurationIsListedForDiscovery() throws Exception {
        try (var in = getClass().getClassLoader()
                .getResourceAsStream("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
            assertThat(in).isNotNull();
            assertThat(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .contains(CommonStorageAutoConfiguration.class.getName());
        }
    }
}
