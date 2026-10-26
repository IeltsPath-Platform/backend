package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import com.ieltspath.commonstorage.config.CommonStorageAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibraryStorageConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonStorageAutoConfiguration.class))
            .withUserConfiguration(FlashcardImageStorageAdapter.class);

    @Test
    void whenStorageEnabledWithoutSupabaseConfigServiceInitializesWithUnavailableStorage() {
        runner.withPropertyValues("app.storage.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ObjectStorage.class);
                    assertThat(context.getBean(ObjectStorage.class)).isInstanceOf(UnavailableObjectStorage.class);

                    FlashcardImageStorageAdapter adapter = context.getBean(FlashcardImageStorageAdapter.class);
                    assertThat(adapter).isNotNull();

                    // Core query should not throw and return null URL
                    assertThat(adapter.publicUrl("images/user/some-key.png")).isNull();

                    // Upload operations fail with UNAVAILABLE StorageException
                    assertThatThrownBy(() -> adapter.store("key", new byte[]{1}, "image/png"))
                            .isInstanceOf(StorageException.class)
                            .satisfies(e -> assertThat(((StorageException) e).reason())
                                    .isEqualTo(StorageException.Reason.UNAVAILABLE));
                });
    }

    @Test
    void whenStorageDisabledServiceInitializesWithFallbackUnavailableStorage() {
        runner.withPropertyValues("app.storage.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ObjectStorage.class);

                    FlashcardImageStorageAdapter adapter = context.getBean(FlashcardImageStorageAdapter.class);
                    assertThat(adapter).isNotNull();

                    // Works with fallback UnavailableObjectStorage
                    assertThat(adapter.publicUrl("images/user/some-key.png")).isNull();
                    assertThatThrownBy(() -> adapter.store("key", new byte[]{1}, "image/png"))
                            .isInstanceOf(StorageException.class);
                });
    }
}

