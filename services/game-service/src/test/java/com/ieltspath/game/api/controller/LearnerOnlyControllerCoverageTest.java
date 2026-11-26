package com.ieltspath.game.api.controller;

import com.ieltspath.commonsecurity.authorization.LearnerOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerOnlyControllerCoverageTest {
    @Test
    void everyGameControllerIsRestrictedToLearners() {
        assertThat(List.of(GameCatalogController.class, GameMatchController.class, GameRoomController.class,
                GameSessionController.class, RocketGameController.class, RoomSettingsController.class,
                SoloGameplayController.class, WordFiltersController.class, WordlyActiveGameController.class,
                WordlyCurrentWordController.class, WordlyHintController.class, WordlyRankingController.class,
                WordlyRoomController.class))
                .allSatisfy(controller -> assertThat(controller.isAnnotationPresent(LearnerOnly.class))
                        .as("%s should require CUSTOMER", controller.getSimpleName()).isTrue());
    }
}
