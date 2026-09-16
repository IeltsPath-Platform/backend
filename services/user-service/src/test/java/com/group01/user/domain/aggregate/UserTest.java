package com.group01.user.domain.aggregate;

import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.UserStatus;
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
}
