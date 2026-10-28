package com.ieltspath.user.infrastructure.adapter;

import com.ieltspath.commonstorage.ObjectStorage;
import com.ieltspath.commonstorage.StorageException;
import com.ieltspath.commonstorage.UnavailableObjectStorage;
import com.ieltspath.commonstorage.config.CommonStorageAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** user-service enables storage while SUPABASE_URL and the key default to empty: it must still start. */
class AvatarStorageConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CommonStorageAutoConfiguration.class))
            .withUserConfiguration(AvatarStorageAdapter.class);

    @Test
    void startsWithUnavailableStorageWhenSupabaseIsNotConfigured() {
        runner.withPropertyValues("app.storage.enabled=true", "app.storage.supabase.url=",
                        "app.storage.supabase.secret-key=", "app.user.avatar.bucket=avatars")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ObjectStorage.class)).isInstanceOf(UnavailableObjectStorage.class);

                    AvatarStorageAdapter adapter = context.getBean(AvatarStorageAdapter.class);
                    assertThatThrownBy(() -> adapter.put("images/u/a.png", new byte[]{1}, "image/png"))
                            .isInstanceOfSatisfying(StorageException.class,
                                    e -> assertThat(e.reason()).isEqualTo(StorageException.Reason.UNAVAILABLE));
                });
    }
}
