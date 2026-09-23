package com.group01.access.application.usecase;

import com.group01.access.application.command.CreateKeyProductCommand;
import com.group01.access.application.result.KeyProductResult;
import com.group01.access.domain.aggregate.KeyProduct;
import com.group01.access.domain.repository.KeyProductRepository;
import com.group01.access.domain.vo.KeyType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateKeyProductUseCase {

    private final KeyProductRepository keyProductRepository;

    public CreateKeyProductUseCase(KeyProductRepository keyProductRepository) {
        this.keyProductRepository = keyProductRepository;
    }

    public KeyProductResult execute(CreateKeyProductCommand command) {
        KeyProduct product;
        if (command.keyType() == KeyType.POINTS) {
            product = KeyProduct.createPointsProduct(command.code(), command.name(), command.pointsAmount());
        } else {
            product = KeyProduct.createPremiumProduct(
                    command.code(),
                    command.name(),
                    command.planId(),
                    command.premiumDays(),
                    command.humanGradingCredits()
            );
        }
        KeyProduct saved = keyProductRepository.save(product);

        return new KeyProductResult(
                saved.getId(),
                saved.getCode(),
                saved.getName(),
                saved.getKeyType(),
                saved.getPointsAmount(),
                saved.getPlanId(),
                saved.getPremiumDays(),
                saved.getHumanGradingCredits(),
                saved.getStatus(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
