package com.ieltspath.learning.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Fills profiles for evidence that predates them, in the background after startup. Idempotent. */
@Component
public class MasteryProfileBackfillRunner implements ApplicationRunner {
    private final MasteryProfileMaintenance maintenance;
    private final boolean enabled;

    public MasteryProfileBackfillRunner(MasteryProfileMaintenance maintenance,
                                        @Value("${learning.mastery.backfill-on-startup:true}") boolean enabled) {
        this.maintenance = maintenance;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (enabled) maintenance.startBackfill();
    }
}
