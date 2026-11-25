package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.GenerateActivationKeysCommand;
import com.ieltspath.access.application.result.GeneratedKeyItem;
import com.ieltspath.access.domain.aggregate.ActivationKey;
import com.ieltspath.access.domain.aggregate.KeyProduct;
import com.ieltspath.access.domain.exception.KeyProductNotFoundException;
import com.ieltspath.access.domain.repository.ActivationKeyRepository;
import com.ieltspath.access.domain.repository.KeyProductRepository;
import com.ieltspath.access.domain.vo.ActivationKeyCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class GenerateActivationKeysUseCase {

    private final KeyProductRepository keyProductRepository;
    private final ActivationKeyRepository activationKeyRepository;

    public GenerateActivationKeysUseCase(KeyProductRepository keyProductRepository, ActivationKeyRepository activationKeyRepository) {
        this.keyProductRepository = keyProductRepository;
        this.activationKeyRepository = activationKeyRepository;
    }

    public List<GeneratedKeyItem> execute(GenerateActivationKeysCommand command) {
        if (command.count() <= 0 || command.count() > 1000) {
            throw new IllegalArgumentException("Số lượng key phải từ 1 đến 1000");
        }

        KeyProduct product = keyProductRepository.findById(command.productId())
                .orElseThrow(() -> new KeyProductNotFoundException(command.productId()));

        List<ActivationKey> keysToSave = new ArrayList<>();
        List<GeneratedKeyItem> result = new ArrayList<>();

        for (int i = 0; i < command.count(); i++) {
            String rawKey = generateRawKey();
            ActivationKeyCode keyCode = ActivationKeyCode.fromRawKey(rawKey);

            ActivationKey key = ActivationKey.create(
                    product.getId(),
                    keyCode.codeHash(),
                    keyCode.codeHint(),
                    command.expiresAt(),
                    command.createdBy()
            );

            keysToSave.add(key);
            result.add(new GeneratedKeyItem(key.getId(), rawKey, keyCode.codeHint(), command.expiresAt()));
        }

        activationKeyRepository.saveAll(keysToSave);
        return result;
    }

    private String generateRawKey() {
        return ActivationKeyCode.generateRaw();
    }
}
