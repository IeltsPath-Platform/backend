package com.ieltspath.learning.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessControllerRoleConventionTest {
    @Test
    void businessEndpointsDoNotGrantSystemAdministratorAccess() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<String> violations = new ArrayList<>();

        for (var candidate : scanner.findCandidateComponents("com.ieltspath.learning.api.controller")) {
            Class<?> controller = Class.forName(candidate.getBeanClassName());
            if (controller == ActivityStatsController.class) {
                continue;
            }
            check(controller, controller, violations);
            for (var method : controller.getDeclaredMethods()) {
                check(method, controller, violations);
            }
        }

        assertTrue(violations.isEmpty(), () -> "Business controllers must not grant ADMIN: " + violations);
    }

    private static void check(AnnotatedElement element, Class<?> controller, List<String> violations) {
        PreAuthorize authorization = AnnotatedElementUtils.findMergedAnnotation(element, PreAuthorize.class);
        if (authorization != null && authorization.value().contains("ADMIN")) {
            violations.add(controller.getSimpleName() + "#" + element);
        }
    }
}
