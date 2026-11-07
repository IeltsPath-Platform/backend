package com.ieltspath.library.application.service;

import com.ieltspath.library.application.port.ExercisePreview;
import com.ieltspath.library.application.result.StudyContentResult.Preview;
import com.ieltspath.library.domain.study.StudyWordSnapshot;
import com.ieltspath.library.domain.study.ExerciseGenerator;
import com.ieltspath.library.domain.study.StudyStepSnapshot;
import org.springframework.stereotype.Component;
import java.util.List;

/** Publication and attempts use the same generator, with a fixed publication-preview seed. */
@Component
public class StudyExercisePreview implements ExercisePreview {
    public Preview preview(List<StudyWordSnapshot> words, List<StudyWordSnapshot> pool) {
        var steps=new ExerciseGenerator().generate(words,pool,new java.util.UUID(0,0));
        return new Preview(words.size(),(int)steps.stream().filter(StudyStepSnapshot::graded).count(),
            (int)steps.stream().filter(s->s.type().equals("FILL")).count(),steps.stream().filter(StudyStepSnapshot::graded).map(StudyStepSnapshot::type).distinct().toList());
    }
}
