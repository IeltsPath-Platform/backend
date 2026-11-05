package com.ieltspath.user.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleNameTest {

    @Test
    void fromIgnoresCaseAndSurroundingSpaces() {
        assertThat(RoleName.from(" customer ")).isEqualTo(RoleName.CUSTOMER);
    }

    @Test
    void fromRejectsBlankWithTheExistingMessage() {
        assertThatThrownBy(() -> RoleName.from(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tên vai trò không được để trống");
    }

    @Test
    void fromNamesTheUnknownRoleWithoutLeakingTheJavaClass() {
        assertThatThrownBy(() -> RoleName.from(" abc "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vai trò không tồn tại: abc")
                .hasMessageNotContaining("RoleName");
    }
}
