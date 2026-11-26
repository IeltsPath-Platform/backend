package com.ieltspath.access.api.controller;

import com.ieltspath.commonsecurity.authorization.LearnerOnly;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerOnlyControllerCoverageTest {
    @Test
    void learnerAccessIsRestrictedToLearners() {
        assertThat(LearnerAccessController.class.isAnnotationPresent(LearnerOnly.class)).isTrue();
    }

    /** The sales screens read the key products to issue keys, so the catalogue stays open to every signed-in user. */
    @Test
    void theCatalogueStaysOpenToStaff() {
        assertThat(AccessCatalogController.class.isAnnotationPresent(LearnerOnly.class)).isFalse();
    }
}
