package com.group01.access.application.usecase;

import com.group01.access.application.result.PlanFeatureResult;
import com.group01.access.application.result.PlanResult;
import com.group01.access.domain.aggregate.Plan;
import com.group01.access.domain.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListPlansUseCase {

    private final PlanRepository planRepository;

    public ListPlansUseCase(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<PlanResult> execute() {
        return planRepository.findAll().stream()
                .map(this::toResult)
                .toList();
    }

    private PlanResult toResult(Plan p) {
        List<PlanFeatureResult> features = p.getFeatures().stream()
                .map(f -> new PlanFeatureResult(f.getId(), f.getFeatureKey(), f.isEnabled(), f.getLimitValue(), f.getConfig()))
                .toList();

        return new PlanResult(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getStatus(),
                features,
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
