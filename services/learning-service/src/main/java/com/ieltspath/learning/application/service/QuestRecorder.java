package com.ieltspath.learning.application.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Advances a learner's daily quests with the answers their submission wrote. It runs before the submission commits, so
 * progress, the XP reward and the queued points commit together with the answers.
 */
@Component
public class QuestRecorder {
    private final QuestService quests;

    public QuestRecorder(QuestService quests) {
        this.quests = quests;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onAnswers(QuestionEventsAppended appended) {
        quests.record(appended.userId(), appended.events());
    }
}
