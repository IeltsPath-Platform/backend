package com.group01.access.application.usecase;

import com.group01.access.domain.aggregate.ActivationKey;
import com.group01.access.domain.exception.ActivationKeyNotFoundException;
import com.group01.access.domain.repository.ActivationKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RevokeActivationKeyUseCase {

    private final ActivationKeyRepository activationKeyRepository;

    public RevokeActivationKeyUseCase(ActivationKeyRepository activationKeyRepository) {
        this.activationKeyRepository = activationKeyRepository;
    }

    public void execute(UUID keyId) {
        ActivationKey key = activationKeyRepository.findById(keyId)
                .orElseThrow(() -> new ActivationKeyNotFoundException(keyId));

        key.revoke();
        activationKeyRepository.save(key);
    }
}
