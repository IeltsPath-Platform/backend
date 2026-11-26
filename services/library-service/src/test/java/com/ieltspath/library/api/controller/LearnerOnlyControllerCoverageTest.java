package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.authorization.LearnerOnly;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearnerOnlyControllerCoverageTest {
    @Test
    void everyPlannedLearnerControllerIsRestricted() {
        assertThat(List.of(FlashcardController.class, FlashcardDeckController.class, NoteController.class,
                SavedVideoSegmentController.class, VideoDictationController.class, VideoProgressController.class,
                VocabularyNotebookController.class, VocabularyProgressController.class, VocabularyStudyController.class,
                VocabularyStudyMeController.class))
                .allSatisfy(controller -> assertThat(controller.isAnnotationPresent(LearnerOnly.class))
                        .as("%s should require CUSTOMER", controller.getSimpleName()).isTrue());
    }
}
