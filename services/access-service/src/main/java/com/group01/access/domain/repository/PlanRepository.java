package com.group01.access.domain.repository;

import com.group01.access.domain.aggregate.Plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    Optional<Plan> findById(UUID id);

    Optional<Plan> findByCode(String code);

    List<Plan> findAll();

    Plan save(Plan plan);
}
