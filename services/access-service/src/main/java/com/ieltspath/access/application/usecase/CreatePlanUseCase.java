package com.ieltspath.access.application.usecase;

import com.ieltspath.access.application.command.CreatePlanCommand;
import com.ieltspath.access.application.result.PlanResult;
import com.ieltspath.access.domain.aggregate.Plan;
import com.ieltspath.access.domain.repository.PlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Transactional
public class CreatePlanUseCase {

    private final PlanRepository planRepository;

    public CreatePlanUseCase(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public PlanResult execute(CreatePlanCommand command) {
        Plan plan = Plan.create(command.code(), command.name());
        Plan saved = planRepository.save(plan);

        return new PlanResult(
                saved.getId(),
                saved.getCode(),
                saved.getName(),
                saved.getStatus(),
                Collections.emptyList(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
