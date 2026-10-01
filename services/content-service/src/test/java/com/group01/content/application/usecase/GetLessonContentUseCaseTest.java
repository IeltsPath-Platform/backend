package com.group01.content.application.usecase;

import com.group01.content.application.port.LearningContentReader;
import com.group01.content.application.result.LessonContentResult;
import com.group01.content.domain.exception.InvalidMediaReferenceException;
import com.group01.content.domain.vo.AssetType;
import com.group01.content.domain.vo.BlockType;
import com.group01.content.domain.vo.LessonBlockKind;
import com.group01.content.domain.vo.MediaReferencePolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetLessonContentUseCaseTest {
    private final LearningContentReader reader = mock(LearningContentReader.class);
    private final UUID lessonId = UUID.randomUUID();
    private final MediaReferencePolicy policy = new MediaReferencePolicy("https://media.example.test/ieltspath");

    private LessonContentResult.Question question(String specType, List<LessonContentResult.QuestionAsset> assets) {
        LessonBlockKind.QuestionSpec spec = specType.equals("ESSAY")
                ? new LessonBlockKind.QuestionSpec("ESSAY", "TASK_1", "6", "Madrid: black roof 82.",
                assets.stream().map(LessonContentResult.QuestionAsset::assetType).toList())
                : new LessonBlockKind.QuestionSpec(specType, null, null, null, List.of());
        return new LessonContentResult.Question(UUID.randomUUID(), 1, "Stem", null, "{}", "Model", null, List.of(), spec,
                assets);
    }

    private LessonContentResult lessonWith(LessonContentResult.Question... questions) {
        return new LessonContentResult(lessonId, UUID.randomUUID(), "L3", "Title", null, 3, List.of(), List.of(
                new LessonContentResult.Block(UUID.randomUUID(), BlockType.TEXT, null, 1, "Text", null, null, null),
                new LessonContentResult.Block(UUID.randomUUID(), BlockType.EXERCISE, null, 2, null, null, null,
                        List.of(questions))));
    }

    private static LessonContentResult.QuestionAsset image(String reference) {
        return new LessonContentResult.QuestionAsset(UUID.randomUUID(), AssetType.IMAGE, reference, null, "Chart", 1);
    }

    @Test
    void essayBlockKeepsItsCheckedImagesAndExerciseQuestionsDropTheirs() {
        String svg = "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=";
        when(reader.publishedLesson(lessonId)).thenReturn(Optional.of(lessonWith(question("ESSAY", List.of(image(svg))))));

        LessonContentResult.Block essay = new GetLessonContentUseCase(reader, policy).execute(lessonId).blocks().get(1);

        assertThat(essay.blockKind()).isEqualTo(LessonBlockKind.ESSAY);
        assertThat(essay.questions().get(0).assets()).singleElement()
                .satisfies(asset -> assertThat(asset.mediaUrl()).isEqualTo(svg));

        when(reader.publishedLesson(lessonId)).thenReturn(Optional.of(lessonWith(question("CHOICE", List.of()))));
        LessonContentResult.Block exercise = new GetLessonContentUseCase(reader, policy).execute(lessonId).blocks().get(1);
        assertThat(exercise.blockKind()).isEqualTo(LessonBlockKind.EXERCISE);
        assertThat(exercise.questions().get(0).assets()).isNull();
    }

    @Test
    void essayImageWithAnUnsafeReferenceFailsTheLesson() {
        when(reader.publishedLesson(lessonId))
                .thenReturn(Optional.of(lessonWith(question("ESSAY", List.of(image("javascript:alert(1)"))))));

        assertThatThrownBy(() -> new GetLessonContentUseCase(reader, policy).execute(lessonId))
                .isInstanceOf(InvalidMediaReferenceException.class);
    }
}
