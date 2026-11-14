package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.AddPackageVersionCommand;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddPackageVersionUseCaseTest {
    private final ContentPackageRepository packages = mock(ContentPackageRepository.class);

    @Test
    void anUnknownPackageIsNotFound() {
        UUID unknown = UUID.randomUUID();
        when(packages.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new AddPackageVersionUseCase(packages).execute(new AddPackageVersionCommand(unknown, 1, "{}")))
                .isInstanceOf(ContentPackageNotFoundException.class);
        verify(packages, never()).save(any());
    }
}
