package com.ieltspath.user.domain.service;

import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.vo.RoleName;

import java.util.Set;

public final class RoleAssignmentPolicy {
    private RoleAssignmentPolicy() {
    }

    public static void validate(Set<RoleName> roles) {
        if (!roles.contains(RoleName.ADMIN)) {
            return;
        }
        for (RoleName restrictedRole : Set.of(RoleName.CONTENT_AUTHOR, RoleName.EXAMINER)) {
            if (roles.contains(restrictedRole)) {
                throw new UserManagementRuleViolationException(
                        "ROLE_CONFLICT",
                        "ADMIN cannot be combined with " + restrictedRole.name()
                );
            }
        }
    }
}
