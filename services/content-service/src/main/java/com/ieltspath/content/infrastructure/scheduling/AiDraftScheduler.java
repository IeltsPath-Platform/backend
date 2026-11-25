package com.ieltspath.content.infrastructure.scheduling;

import com.ieltspath.content.application.usecase.RunAiDraftJobUseCase;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import com.ieltspath.content.domain.repository.AiDraftRepository;
import java.time.Clock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the generation job on a fixed delay. Several instances may run it: the claim keeps them from overlapping. */
@Component
public class AiDraftScheduler {

    private final RunAiDraftJobUseCase job;
    private final ContentAiProperties.Ai settings;
    private final ContentAiProperties.SectionQuestions sectionSettings;
    private final AiDraftRepository drafts;
    private final Clock clock;

    public AiDraftScheduler(RunAiDraftJobUseCase job, ContentAiProperties properties, AiDraftRepository drafts, Clock clock) {
        this.job = job;
        this.settings = properties.getAi();
        this.sectionSettings = properties.getAi().getSectionQuestions();
        this.drafts = drafts;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${content.ai.poll-interval:PT30S}")
    public void run() {
        job.runBatch(settings.getBatchSize(), settings.getStaleAfter(), settings.getMaxAttempts());
        drafts.expireReadySectionDrafts(clock.instant().minus(sectionSettings.getReadyTtl()));
    }
}
