package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.exception.GlobalExceptionHandler;
import com.ieltspath.content.application.result.LessonContentResult;
import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.application.result.TopicSequenceResult;
import com.ieltspath.content.application.result.LessonPracticeSetResult;
import com.ieltspath.content.application.result.LessonPracticeSetsResult;
import com.ieltspath.content.application.command.CountPracticeSetsCommand;
import com.ieltspath.content.application.usecase.CountAvailablePracticeSetsUseCase;
import com.ieltspath.content.application.usecase.GetLessonContentUseCase;
import com.ieltspath.content.application.usecase.GetLessonPracticeSetsUseCase;
import com.ieltspath.content.application.usecase.GetTopicPracticeSetsUseCase;
import com.ieltspath.content.application.usecase.GetPackageVersionContentUseCase;
import com.ieltspath.content.application.usecase.GetTopicLessonsUseCase;
import com.ieltspath.content.application.usecase.GetTopicSequenceUseCase;
import com.ieltspath.content.application.usecase.GetTopicTestPackagesUseCase;
import com.ieltspath.content.application.usecase.SearchPracticeSetsUseCase;
import com.ieltspath.content.domain.exception.LessonNotFoundException;
import com.ieltspath.content.domain.exception.TopicNotFoundException;
import com.ieltspath.content.domain.vo.AssetType;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.LessonBlockKind;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalLearningContentControllerTest {
    @Test
    void practiceSkillFilterIsForwardedAndSetSkillsAreSerialized() throws Exception {
        UUID lessonId = UUID.randomUUID();
        var filter = java.util.Optional.of(Skill.READING);
        var set = new LessonPracticeSetResult(lessonId, UUID.randomUUID(), UUID.randomUUID(), "PURE_R", "Reading",
                3, List.of(), null, List.of(Skill.READING));
        when(lessonPracticeSets.execute(lessonId, filter)).thenReturn(List.of(set));
        mockMvc.perform(get("/internal/learning-content/lessons/{id}/practice-sets", lessonId).param("skill", "READING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skills[0]").value("READING"));
        verify(lessonPracticeSets).execute(lessonId, filter);
    }

    @Test
    void invalidPracticeSkillFilterIsRejected() throws Exception {
        mockMvc.perform(get("/internal/learning-content/lessons/{id}/practice-sets", UUID.randomUUID())
                        .param("skill", "FOO"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/internal/learning-content/lessons/{id}/practice-sets", UUID.randomUUID())
                        .param("skill", "ALL"))
                .andExpect(status().isBadRequest());
    }

    private final com.ieltspath.content.application.usecase.GetCourseTestPackagesUseCase courseTests =
            mock(com.ieltspath.content.application.usecase.GetCourseTestPackagesUseCase.class);

    @Test
    void sequenceCarriesCourseMetadataAndCourseTestPackagesUseTheExistingShape() throws Exception {
        UUID course = UUID.randomUUID();
        UUID pkg = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        when(topicSequence.execute()).thenReturn(List.of(new TopicSequenceResult(UUID.randomUUID(), "TOPIC", "Topic",
                1, null, List.of(), Skill.READING, false,
                new TopicSequenceResult.CourseEntry(course, "IELTS_5_5", "IELTS 5.5", new java.math.BigDecimal("5.5"), true))));
        when(courseTests.execute(course)).thenReturn(List.of(
                new com.ieltspath.content.application.result.TopicTestPackageResult(pkg, version, "FINAL")));
        mockMvc.perform(get("/internal/learning-content/topic-sequence"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].course.courseId").value(course.toString()))
                .andExpect(jsonPath("$[0].course.bandLevel").value(5.5))
                .andExpect(jsonPath("$[0].course.hasCourseTest").value(true));
        mockMvc.perform(get("/internal/learning-content/courses/{id}/test-packages", course))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].packageId").value(pkg.toString()))
                .andExpect(jsonPath("$[0].packageVersionId").value(version.toString()))
                .andExpect(jsonPath("$[0].code").value("FINAL"));
    }
    private final GetTopicSequenceUseCase topicSequence = mock(GetTopicSequenceUseCase.class);
    private final GetTopicLessonsUseCase topicLessons = mock(GetTopicLessonsUseCase.class);
    private final GetLessonContentUseCase lessonContent = mock(GetLessonContentUseCase.class);
    private final GetTopicTestPackagesUseCase testPackages = mock(GetTopicTestPackagesUseCase.class);
    private final SearchPracticeSetsUseCase search = mock(SearchPracticeSetsUseCase.class);
    private final GetPackageVersionContentUseCase packageVersion = mock(GetPackageVersionContentUseCase.class);
    private final GetLessonPracticeSetsUseCase lessonPracticeSets = mock(GetLessonPracticeSetsUseCase.class);
    private final GetTopicPracticeSetsUseCase topicPracticeSets = mock(GetTopicPracticeSetsUseCase.class);
    private final CountAvailablePracticeSetsUseCase availability = mock(CountAvailablePracticeSetsUseCase.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalLearningContentController(topicSequence, topicLessons,
                        lessonContent, testPackages, search, packageVersion, lessonPracticeSets, topicPracticeSets,
                        availability, courseTests))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void topicSequenceCarriesEachTopicsSkillAndWhetherItHasAFinalTest() throws Exception {
        UUID writing = UUID.randomUUID();
        when(topicSequence.execute()).thenReturn(List.of(new TopicSequenceResult(writing, "DEMO_WRITING",
                "Writing cơ bản", 950, null, List.of(), Skill.WRITING, false)));

        mockMvc.perform(get("/internal/learning-content/topic-sequence"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topicId").value(writing.toString()))
                .andExpect(jsonPath("$[0].code").value("DEMO_WRITING"))
                .andExpect(jsonPath("$[0].skill").value("WRITING"))
                .andExpect(jsonPath("$[0].hasTopicTest").value(false))
                .andExpect(jsonPath("$[0].knowledgePoints").isEmpty());
    }

    @Test
    void lessonAndTopicPracticeSetsCarryNoQuestionsOrAnswers() throws Exception {
        UUID lessonId = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID kp = UUID.randomUUID();
        LessonPracticeSetResult set = new LessonPracticeSetResult(lessonId, UUID.randomUUID(), UUID.randomUUID(),
                "PS-TF-A", "Luyện thêm", 3, List.of(kp), null);
        when(lessonPracticeSets.execute(lessonId)).thenReturn(List.of(set));
        when(topicPracticeSets.execute(topicId)).thenReturn(List.of(new LessonPracticeSetsResult(lessonId, List.of(set)),
                new LessonPracticeSetsResult(UUID.randomUUID(), List.of())));

        mockMvc.perform(get("/internal/learning-content/lessons/{id}/practice-sets", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PS-TF-A"))
                .andExpect(jsonPath("$[0].questionCount").value(3))
                .andExpect(jsonPath("$[0].knowledgePointIds[0]").value(kp.toString()))
                .andExpect(jsonPath("$[0].requiredFeatureKey").value((Object) null))
                .andExpect(jsonPath("$[0].answerSpec").doesNotExist())
                .andExpect(jsonPath("$[0].questions").doesNotExist());
        mockMvc.perform(get("/internal/learning-content/topics/{id}/practice-sets", topicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons[0].lessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.lessons[0].practiceSets[0].code").value("PS-TF-A"))
                .andExpect(jsonPath("$.lessons[1].practiceSets").isEmpty());
    }

    @Test
    void availabilityCountsPerKnowledgePointAndRejectsTooManyIds() throws Exception {
        UUID kp = UUID.randomUUID();
        when(availability.execute(any(CountPracticeSetsCommand.class))).thenReturn(Map.of(kp, 2));

        mockMvc.perform(post("/internal/learning-content/practice-sets/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"knowledgePointIds\":[\"" + kp + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts['" + kp + "']").value(2));

        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 51; i++) many.append(i == 0 ? "" : ",").append('"').append(UUID.randomUUID()).append('"');
        mockMvc.perform(post("/internal/learning-content/practice-sets/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"knowledgePointIds\":[" + many + "]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/learning-content/practice-sets/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"knowledgePointIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/learning-content/practice-sets/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"knowledgePointIds\":[null]}"))
                .andExpect(status().isBadRequest());
        verify(availability, times(1)).execute(any());
    }

    @Test
    void lessonBlocksCarryOnlyTheirOwnFieldsAndAnswerSpecsAsJson() throws Exception {
        UUID lessonId = UUID.randomUUID();
        UUID kp = UUID.randomUUID();
        when(lessonContent.execute(lessonId)).thenReturn(new LessonContentResult(lessonId, UUID.randomUUID(), "L1",
                "Câu chủ đề nằm ở đâu", null, 1, List.of(), List.of(
                new LessonContentResult.Block(UUID.randomUUID(), BlockType.TEXT, null, 1, "Mẹo", null, null, null,
                        List.of(kp)),
                new LessonContentResult.Block(UUID.randomUUID(), BlockType.ASSET, null, 2, null,
                        new LessonContentResult.Asset(UUID.randomUUID(), AssetType.PASSAGE, "A. Text", null, null, null),
                        null, null, List.of()),
                new LessonContentResult.Block(UUID.randomUUID(), BlockType.EXERCISE, LessonBlockKind.EXERCISE, 3, null, null, null, List.of(
                        new LessonContentResult.Question(UUID.randomUUID(), 1, "Complete ______.", null,
                                "{\"type\":\"FILL\",\"accepted\":[\"critics\"]}", "Đoạn D.", "Đọc câu thứ hai của đoạn D.", List.of(),
                                new LessonBlockKind.QuestionSpec("FILL", null, null, null, List.of()), null)), List.of(kp))),
                Skill.READING));

        mockMvc.perform(get("/internal/learning-content/lessons/{id}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value((Object) null))
                .andExpect(jsonPath("$.skill").value("READING"))
                .andExpect(jsonPath("$.blocks[0].textContent").value("Mẹo"))
                .andExpect(jsonPath("$.blocks[0].knowledgePointIds[0]").value(kp.toString()))
                .andExpect(jsonPath("$.blocks[1].knowledgePointIds").isEmpty())
                .andExpect(jsonPath("$.blocks[2].knowledgePointIds[0]").value(kp.toString()))
                .andExpect(jsonPath("$.blocks[0].asset").doesNotExist())
                .andExpect(jsonPath("$.blocks[0].questions").doesNotExist())
                .andExpect(jsonPath("$.blocks[0].blockKind").doesNotExist())
                .andExpect(jsonPath("$.blocks[2].blockKind").value("EXERCISE"))
                .andExpect(jsonPath("$.blocks[2].questions[0].spec").doesNotExist())
                .andExpect(jsonPath("$.blocks[2].questions[0].assets").doesNotExist())
                .andExpect(jsonPath("$.blocks[1].asset.assetType").value("PASSAGE"))
                .andExpect(jsonPath("$.blocks[1].asset.mediaReference").value((Object) null))
                .andExpect(jsonPath("$.blocks[2].textContent").doesNotExist())
                .andExpect(jsonPath("$.blocks[2].questions[0].options").value((Object) null))
                .andExpect(jsonPath("$.blocks[2].questions[0].answerSpec.type").value("FILL"))
                .andExpect(jsonPath("$.blocks[2].questions[0].answerSpec.accepted[0]").value("critics"))
                .andExpect(jsonPath("$.blocks[2].questions[0].hint").value("Đọc câu thứ hai của đoạn D."));
    }

    @Test
    void packageVersionRulesAndOptionsAreJsonObjects() throws Exception {
        UUID versionId = UUID.randomUUID();
        when(packageVersion.execute(versionId)).thenReturn(new PackageVersionContentResult(versionId,
                UUID.randomUUID(), PackageType.TOPIC_TEST, UUID.randomUUID(), "{}", List.of(
                new PackageVersionContentResult.Section(UUID.randomUUID(), "Street trees", Skill.READING, null, 1,
                        "A. City trees", null, List.of(new PackageVersionContentResult.Item(UUID.randomUUID(), 1,
                        "What is the passage mainly about?",
                        "[{\"optionKey\":\"A\",\"content\":\"x\",\"sortOrder\":1}]",
                        "{\"type\":\"CHOICE\",\"correct\":\"A\"}", "x", "Đọc câu đầu đoạn A.", 1.0,
                        List.of(new PackageVersionContentResult.KnowledgePointMapping(UUID.randomUUID(), 1.0))))))));

        mockMvc.perform(get("/internal/learning-content/package-versions/{id}", versionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageType").value("TOPIC_TEST"))
                .andExpect(jsonPath("$.rules").isMap())
                .andExpect(jsonPath("$.sections[0].items[0].options[0].optionKey").value("A"))
                .andExpect(jsonPath("$.sections[0].items[0].answerSpec.correct").value("A"))
                .andExpect(jsonPath("$.sections[0].items[0].knowledgePointMappings[0].weight").value(1.0));
    }

    @Test
    void unknownIdsAreNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(lessonContent.execute(id)).thenThrow(new LessonNotFoundException(id));
        when(topicLessons.execute(id)).thenThrow(new TopicNotFoundException(id));

        mockMvc.perform(get("/internal/learning-content/lessons/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/internal/learning-content/topics/{id}/lessons", id)).andExpect(status().isNotFound());
    }

    @Test
    void practiceSetSearchValidatesItsInput() throws Exception {
        mockMvc.perform(post("/internal/learning-content/practice-sets/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"knowledgePointId\":\"%s\",\"limit\":11}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/internal/learning-content/practice-sets/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verify(search, never()).execute(any());
    }
}
