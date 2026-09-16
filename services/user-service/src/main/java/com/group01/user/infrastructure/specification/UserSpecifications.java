package com.group01.user.infrastructure.specification;

import com.group01.user.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<UserJpaEntity> emailContains(String email) {
        return (root, query, criteriaBuilder) -> email == null || email.isBlank()
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }
}
