package com.group01.access.application.usecase;

import com.group01.access.application.result.KeyProductResult;
import com.group01.access.domain.aggregate.KeyProduct;
import com.group01.access.domain.repository.KeyProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListKeyProductsUseCase {

    private final KeyProductRepository keyProductRepository;

    public ListKeyProductsUseCase(KeyProductRepository keyProductRepository) {
        this.keyProductRepository = keyProductRepository;
    }

    public List<KeyProductResult> execute() {
        return keyProductRepository.findAll().stream()
                .map(this::toResult)
                .toList();
    }

    private KeyProductResult toResult(KeyProduct p) {
        return new KeyProductResult(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getKeyType(),
                p.getPointsAmount(),
                p.getPlanId(),
                p.getPremiumDays(),
                p.getHumanGradingCredits(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
