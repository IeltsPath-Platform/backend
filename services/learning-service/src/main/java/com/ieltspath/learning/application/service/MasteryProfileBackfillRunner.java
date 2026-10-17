package com.ieltspath.learning.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Fills profiles for evidence that predates them, in the background after startup (idempotent). After the mastery
 * formula changed, {@code learning.mastery.rebuild-on-startup=true} rebuilds every profile once instead.
 */
@Component
public class MasteryProfileBackfillRunner implements ApplicationRunner {
    private final MasteryProfileMaintenance maintenance;
    private final boolean enabled;
    private final boolean rebuild;

    public MasteryProfileBackfillRunner(MasteryProfileMaintenance maintenance,
                                        @Value("${learning.mastery.backfill-on-startup:true}") boolean enabled,
                                        @Value("${learning.mastery.rebuild-on-startup:false}") boolean rebuild) {
        this.maintenance = maintenance;
        this.enabled = enabled;
        this.rebuild = rebuild;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (rebuild) maintenance.startRebuild();
        else if (enabled) maintenance.startBackfill();
    }
}
