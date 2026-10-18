package com.ieltspath.user.infrastructure.specification;

import com.ieltspath.user.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<UserJpaEntity> emailContains(String email) {
        return (root, query, criteriaBuilder) -> email == null || email.isBlank()
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + escapeLike(email.toLowerCase()) + "%", '\\');
    }

    private static String escapeLike(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
