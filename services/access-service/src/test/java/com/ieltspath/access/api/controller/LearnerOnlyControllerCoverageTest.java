package com.ieltspath.access.api.controller;

import com.ieltspath.commonsecurity.authorization.LearnerOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerOnlyControllerCoverageTest {
    @Test
    void learnerAccessAndCatalogControllersAreRestricted() {
        assertThat(List.of(AccessCatalogController.class, LearnerAccessController.class))
                .allSatisfy(controller -> assertThat(controller.isAnnotationPresent(LearnerOnly.class))
                        .as("%s should require CUSTOMER", controller.getSimpleName()).isTrue());
    }
}
