package com.ieltspath.community.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityControllerAuthorizationTest {
    @Test
    void communityFeaturesAllowLearnersAndAdministrators() {
        var access = CommunityController.class.getAnnotation(PreAuthorize.class);
        assertThat(access).isNotNull();
        assertThat(access.value()).isEqualTo("hasAnyRole('CUSTOMER', 'ADMIN')");
    }
}
