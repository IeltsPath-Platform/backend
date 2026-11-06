package com.ieltspath.library.application.service;

import com.ieltspath.library.application.port.ExercisePreview;
import com.ieltspath.library.application.result.StudyContentResult.Preview;
import com.ieltspath.library.domain.study.StudyWordSnapshot;
import org.springframework.stereotype.Component;
import java.util.List;

/** Unit publication can precede installation of the exercise generator. */
@Component
public class StudyExercisePreview implements ExercisePreview {
    public Preview preview(List<StudyWordSnapshot> words, List<StudyWordSnapshot> pool) { return null; }
}
