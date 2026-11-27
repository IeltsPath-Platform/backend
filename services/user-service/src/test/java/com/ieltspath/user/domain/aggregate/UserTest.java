package com.ieltspath.user.domain.aggregate;

import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void roleSetIsDefensiveAndMutationTouchesTimestamp() {
        User user = User.builder()
                .email(new Email("user@example.com"))
                .fullName("User")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>())
                .build();

        user.assignRoles(Set.of());

        assertThat(user.getUpdatedAt()).isNotNull();
        assertThatThrownBy(() -> user.getRoles().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aDeletedAccountCannotBeReopenedEditedOrSetToDeletedByHand() {
        User active = User.builder().id(java.util.UUID.randomUUID()).email(new Email("a@example.com")).fullName("A")
                .status(UserStatus.ACTIVE).roles(new HashSet<>()).build();
        assertThatThrownBy(() -> active.changeStatus(UserStatus.DELETED))
                .isInstanceOf(com.ieltspath.user.domain.exception.UserManagementRuleViolationException.class)
                .extracting("code").isEqualTo("DELETED_STATUS_RESERVED");

        active.markDeleted(java.time.LocalDateTime.now());
        for (Runnable change : java.util.List.<Runnable>of(
                () -> active.changeStatus(UserStatus.ACTIVE),
                active::deactivate,
                () -> active.updateProfile("Back again", null),
                () -> active.assignRoles(Set.of()))) {
            assertThatThrownBy(change::run)
                    .isInstanceOf(com.ieltspath.user.domain.exception.UserManagementRuleViolationException.class)
                    .hasMessage("Tài khoản này đã bị xóa nên không thể thay đổi.")
                    .extracting("code").isEqualTo("ACCOUNT_DELETED");
        }
        assertThat(active.getStatus()).isEqualTo(UserStatus.DELETED);
    }
}