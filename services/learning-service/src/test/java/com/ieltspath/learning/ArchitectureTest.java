package com.ieltspath.learning;

import java.util.List;
import java.util.function.Predicate;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

@AnalyzeClasses(packagesOf = ArchitectureTest.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static void respectArchitecture(JavaClasses importedClasses) {
        rulesFor("com.ieltspath.learning").forEach(rule -> freeze(rule).check(importedClasses));
    }

    // Bytecode cannot prove HTTP calls run outside transactions/locks, nor identify a generic HTTP client's URL.
    // These invariants still require review. A dependency on ResilientCall alone does not prove every call uses it.
    static List<ArchRule> rulesFor(String root) {
        return List.of(
                dependencyRule(root, "domain", "domain depends only on domain and technical libraries", target ->
                        inPackage(target, "org.springframework") || inPackage(target, "jakarta.persistence")
                                || inPackage(target, "org.hibernate") || projectLayer(target, "api")
                                || projectLayer(target, "application") || projectLayer(target, "infrastructure")),
                dependencyRule(root, "application", "application does not depend on API or infrastructure", target ->
                        projectLayer(target, "api") || projectLayer(target, "infrastructure")),
                dependencyRule(root, "api", "API does not access persistence or Spring Data repositories", target ->
                        projectPersistence(target) || springDataRepository(target)
                                || target.isAssignableTo("org.springframework.data.repository.Repository")),
                classes().that().resideInAPackage(root + "..")
                        .should(onlyUseInInfrastructure(root, ArchitectureTest::messagingClient, "AMQP templates"))
                        .as("AMQP templates are used only in infrastructure").allowEmptyShould(true),
                classes().that().resideInAPackage(root + "..")
                        .should(httpClientsUseResilience(root))
                        .as("HTTP clients stay in infrastructure and client adapters use resilience").allowEmptyShould(true),
                dependencyRule(root, "", "Supabase SDK and storage provider internals stay in common-storage", target ->
                        inPackage(target, "io.github.jan.supabase") || inPackage(target, "com.supabase")
                                || inPackage(target, "com.ieltspath.commonstorage.supabase")));
    }

    private static ArchRule dependencyRule(String root, String layer, String description,
                                          Predicate<JavaClass> forbidden) {
        String scope = root + (layer.isEmpty() ? "" : "." + layer) + "..";
        return classes().that().resideInAPackage(scope).should(new ArchCondition<JavaClass>(description) {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                source.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> forbidden.test(dependency.getTargetClass()))
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(source, dependency.getDescription())));
            }
        }).as(description).allowEmptyShould(true);
    }

    private static ArchCondition<JavaClass> onlyUseInInfrastructure(String root, Predicate<JavaClass> client,
                                                                   String label) {
        return new ArchCondition<>("use " + label + " only in infrastructure") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                if (!inPackage(source, root + ".infrastructure")) {
                    source.getDirectDependenciesFromSelf().stream()
                            .filter(dependency -> client.test(dependency.getTargetClass()))
                            .forEach(dependency -> events.add(SimpleConditionEvent.violated(source, dependency.getDescription())));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> httpClientsUseResilience(String root) {
        return new ArchCondition<>("keep HTTP in infrastructure and use ResilientCall or common-llm in client adapters") {
            @Override
            public void check(JavaClass source, ConditionEvents events) {
                onlyUseInInfrastructure(root, ArchitectureTest::httpClient, "HTTP clients").check(source, events);
                boolean usesHttp = source.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> httpClient(dependency.getTargetClass()));
                boolean usesResilience = source.getDirectDependenciesFromSelf().stream().anyMatch(dependency ->
                        dependency.getTargetClass().getName().equals("com.ieltspath.commonresilience.ResilientCall")
                                || inPackage(dependency.getTargetClass(), "com.ieltspath.commonllm"));
                if (usesHttp && inPackage(source, root + ".infrastructure.client") && !usesResilience) {
                    events.add(SimpleConditionEvent.violated(source,
                            source.getName() + " uses HTTP without a dependency on ResilientCall or common-llm"));
                }
            }
        };
    }

    private static boolean httpClient(JavaClass target) {
        return typeOrNested(target, "org.springframework.web.client.RestClient")
                || typeOrNested(target, "org.springframework.web.client.RestTemplate")
                || typeOrNested(target, "org.springframework.web.reactive.function.client.WebClient");
    }

    private static boolean messagingClient(JavaClass target) {
        return typeOrNested(target, "org.springframework.amqp.rabbit.core.RabbitTemplate")
                || typeOrNested(target, "org.springframework.amqp.core.AmqpTemplate");
    }

    private static boolean typeOrNested(JavaClass target, String name) {
        return target.getName().equals(name) || target.getName().startsWith(name + "$");
    }

    private static boolean inPackage(JavaClass target, String name) {
        return target.getPackageName().equals(name) || target.getPackageName().startsWith(name + ".");
    }

    private static boolean projectLayer(JavaClass target, String layer) {
        return target.getPackageName().matches("com\\.ieltspath\\.[^.]+\\." + layer + "(\\..*)?");
    }

    private static boolean projectPersistence(JavaClass target) {
        return target.getPackageName().matches("com\\.ieltspath\\.[^.]+\\.infrastructure\\.persistence(\\..*)?");
    }

    private static boolean springDataRepository(JavaClass target) {
        return target.getPackageName().matches("org\\.springframework\\.data(\\.[^.]+)*\\.repository(\\..*)?");
    }
}
