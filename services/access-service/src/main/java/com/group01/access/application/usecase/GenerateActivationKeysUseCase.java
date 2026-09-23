package com.group01.access.application.usecase;

import com.group01.access.application.command.GenerateActivationKeysCommand;
import com.group01.access.application.result.GeneratedKeyItem;
import com.group01.access.domain.aggregate.ActivationKey;
import com.group01.access.domain.aggregate.KeyProduct;
import com.group01.access.domain.exception.KeyProductNotFoundException;
import com.group01.access.domain.repository.ActivationKeyRepository;
import com.group01.access.domain.repository.KeyProductRepository;
import com.group01.access.domain.vo.ActivationKeyCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class GenerateActivationKeysUseCase {

    private static final String CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // No 0, 1, I, O to avoid confusion
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeyProductRepository keyProductRepository;
    private final ActivationKeyRepository activationKeyRepository;

    public GenerateActivationKeysUseCase(KeyProductRepository keyProductRepository, ActivationKeyRepository activationKeyRepository) {
        this.keyProductRepository = keyProductRepository;
        this.activationKeyRepository = activationKeyRepository;
    }

    public List<GeneratedKeyItem> execute(GenerateActivationKeysCommand command) {
        if (command.count() <= 0 || command.count() > 1000) {
            throw new IllegalArgumentException("Count must be between 1 and 1000");
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
        StringBuilder sb = new StringBuilder("IP");
        for (int group = 0; group < 3; group++) {
            sb.append("-");
            for (int i = 0; i < 4; i++) {
                sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
            }
        }
        return sb.toString();
    }
}
