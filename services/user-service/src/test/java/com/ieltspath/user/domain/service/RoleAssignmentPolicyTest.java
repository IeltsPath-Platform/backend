package com.ieltspath.user.domain.service;

import com.ieltspath.user.domain.exception.UserManagementRuleViolationException;
import com.ieltspath.user.domain.vo.RoleName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleAssignmentPolicyTest {
    @Test
    void allowsEveryRoleWithoutAdmin() {
        for (RoleName role : RoleName.values()) {
            assertThatCode(() -> RoleAssignmentPolicy.validate(Set.of(role))).doesNotThrowAnyException();
        }
        assertThatCode(() -> RoleAssignmentPolicy.validate(EnumSet.of(RoleName.CUSTOMER, RoleName.CONTENT_AUTHOR,
                RoleName.EXAMINER, RoleName.SALES_STAFF))).doesNotThrowAnyException();
    }

    @Test
    void allowsAdminWithCustomerAndSalesStaff() {
        assertThatCode(() -> RoleAssignmentPolicy.validate(Set.of(RoleName.ADMIN, RoleName.CUSTOMER)))
                .doesNotThrowAnyException();
        assertThatCode(() -> RoleAssignmentPolicy.validate(Set.of(RoleName.ADMIN, RoleName.SALES_STAFF)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAdminWithContentAuthorOrExaminer() {
        for (RoleName restricted : Set.of(RoleName.CONTENT_AUTHOR, RoleName.EXAMINER)) {
            assertThatThrownBy(() -> RoleAssignmentPolicy.validate(Set.of(RoleName.ADMIN, restricted)))
                    .isInstanceOf(UserManagementRuleViolationException.class)
                    .hasMessageContaining("ADMIN")
                    .hasMessageContaining(restricted.name())
                    .extracting(exception -> ((UserManagementRuleViolationException) exception).getCode())
                    .isEqualTo("ROLE_CONFLICT");
        }
    }
}
