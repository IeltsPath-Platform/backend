package com.ieltspath.content.infrastructure.scheduling;

import com.ieltspath.content.application.usecase.RunAiDraftJobUseCase;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the generation job on a fixed delay. Several instances may run it: the claim keeps them from overlapping. */
@Component
public class AiDraftScheduler {

    private final RunAiDraftJobUseCase job;
    private final ContentAiProperties.Ai settings;

    public AiDraftScheduler(RunAiDraftJobUseCase job, ContentAiProperties properties) {
        this.job = job;
        this.settings = properties.getAi();
    }

    @Scheduled(fixedDelayString = "${content.ai.poll-interval:PT30S}")
    public void run() {
        job.runBatch(settings.getBatchSize(), settings.getStaleAfter());
    }
}
