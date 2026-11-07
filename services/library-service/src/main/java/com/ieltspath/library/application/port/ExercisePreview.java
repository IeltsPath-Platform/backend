package com.ieltspath.library.application.port;

import com.ieltspath.library.application.result.StudyContentResult.Preview;
import com.ieltspath.library.domain.study.StudyWordSnapshot;
import java.util.List;

public interface ExercisePreview {
    Preview preview(List<StudyWordSnapshot> words, List<StudyWordSnapshot> pool);
}
