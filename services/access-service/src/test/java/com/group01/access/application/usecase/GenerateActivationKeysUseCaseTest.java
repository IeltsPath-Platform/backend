package com.group01.access.application.usecase;

import com.group01.access.application.command.GenerateActivationKeysCommand;
import com.group01.access.application.result.GeneratedKeyItem;
import com.group01.access.domain.aggregate.KeyProduct;
import com.group01.access.domain.repository.ActivationKeyRepository;
import com.group01.access.domain.repository.KeyProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateActivationKeysUseCaseTest {

    @Mock
    private KeyProductRepository keyProductRepository;
    @Mock
    private ActivationKeyRepository activationKeyRepository;

    @InjectMocks
    private GenerateActivationKeysUseCase generateActivationKeysUseCase;

    @Test
    @DisplayName("Should generate specified count of raw keys with hashes")
    void shouldGenerateKeysSuccessfully() {
        UUID productId = UUID.randomUUID();
        KeyProduct product = KeyProduct.createPointsProduct("POINT_50", "50 Points", 50);

        when(keyProductRepository.findById(productId)).thenReturn(Optional.of(product));

        List<GeneratedKeyItem> keys = generateActivationKeysUseCase.execute(new GenerateActivationKeysCommand(
                productId, 5, null, UUID.randomUUID()
        ));

        assertThat(keys).hasSize(5);
        for (GeneratedKeyItem k : keys) {
            assertThat(k.rawKey()).startsWith("IP-");
            assertThat(k.codeHint()).hasSize(4);
            assertThat(k.id()).isNotNull();
        }

        verify(activationKeyRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when count is out of bounds")
    void shouldThrowWhenCountInvalid() {
        UUID productId = UUID.randomUUID();

        assertThatThrownBy(() -> generateActivationKeysUseCase.execute(new GenerateActivationKeysCommand(
                productId, 0, null, null
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> generateActivationKeysUseCase.execute(new GenerateActivationKeysCommand(
                productId, 1001, null, null
        ))).isInstanceOf(IllegalArgumentException.class);
    }
}
