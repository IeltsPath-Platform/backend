package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.internal.LessonContentResponse;
import com.ieltspath.content.api.dto.internal.PracticeSetSkillFilter;
import com.ieltspath.content.api.dto.internal.LessonPracticeSetResponse;
import com.ieltspath.content.api.dto.internal.PracticeSetAvailabilityRequest;
import com.ieltspath.content.api.dto.internal.PracticeSetAvailabilityResponse;
import com.ieltspath.content.api.dto.internal.TopicPracticeSetsResponse;
import com.ieltspath.content.api.dto.internal.LessonSummaryResponse;
import com.ieltspath.content.api.dto.internal.PackageVersionContentResponse;
import com.ieltspath.content.api.dto.internal.PracticeSetResponse;
import com.ieltspath.content.api.dto.internal.PracticeSetSearchRequest;
import com.ieltspath.content.api.dto.internal.TopicSequenceResponse;
import com.ieltspath.content.api.dto.internal.TopicTestPackageResponse;
import com.ieltspath.content.application.command.CountPracticeSetsCommand;
import com.ieltspath.content.application.command.SearchPracticeSetsCommand;
import com.ieltspath.content.application.usecase.CountAvailablePracticeSetsUseCase;
import com.ieltspath.content.application.usecase.GetLessonPracticeSetsUseCase;
import com.ieltspath.content.application.usecase.GetTopicPracticeSetsUseCase;
import com.ieltspath.content.application.usecase.GetLessonContentUseCase;
import com.ieltspath.content.application.usecase.GetPackageVersionContentUseCase;
import com.ieltspath.content.application.usecase.GetTopicLessonsUseCase;
import com.ieltspath.content.application.usecase.GetTopicSequenceUseCase;
import com.ieltspath.content.application.usecase.GetTopicTestPackagesUseCase;
import com.ieltspath.content.application.usecase.GetCourseTestPackagesUseCase;
import com.ieltspath.content.application.usecase.GetPlacementTestPackagesUseCase;
import com.ieltspath.content.application.usecase.SearchPracticeSetsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Service-facing curriculum reads for the learning flow (AI Learning and Assessment). Responses include answer specs;
 * the gateway denies {@code /internal/**} to clients.
 */
@RestController
@RequestMapping("/internal/learning-content")
@RequiredArgsConstructor
public class InternalLearningContentController {
    private final GetTopicSequenceUseCase getTopicSequence;
    private final GetTopicLessonsUseCase getTopicLessons;
    private final GetLessonContentUseCase getLessonContent;
    private final GetTopicTestPackagesUseCase getTopicTestPackages;
    private final SearchPracticeSetsUseCase searchPracticeSets;
    private final GetPackageVersionContentUseCase getPackageVersionContent;
    private final GetLessonPracticeSetsUseCase getLessonPracticeSets;
    private final GetTopicPracticeSetsUseCase getTopicPracticeSets;
    private final CountAvailablePracticeSetsUseCase countAvailablePracticeSets;
    private final GetCourseTestPackagesUseCase getCourseTestPackages;
    private final GetPlacementTestPackagesUseCase getPlacementTestPackages;

    @GetMapping("/placement-packages")
    public List<TopicTestPackageResponse> placementPackages() {
        return getPlacementTestPackages.execute().stream().map(TopicTestPackageResponse::from).toList();
    }

    @GetMapping("/courses/{id}/test-packages")
    public List<TopicTestPackageResponse> courseTestPackages(@PathVariable("id") UUID courseId) {
        return getCourseTestPackages.execute(courseId).stream().map(TopicTestPackageResponse::from).toList();
    }

    @GetMapping("/topic-sequence")
    public List<TopicSequenceResponse> topicSequence() {
        return getTopicSequence.execute().stream().map(TopicSequenceResponse::from).toList();
    }

    @GetMapping("/topics/{id}/lessons")
    public List<LessonSummaryResponse> topicLessons(@PathVariable("id") UUID topicId) {
        return getTopicLessons.execute(topicId).stream().map(LessonSummaryResponse::from).toList();
    }

    @GetMapping("/lessons/{id}")
    public LessonContentResponse lesson(@PathVariable("id") UUID lessonId) {
        return LessonContentResponse.from(getLessonContent.execute(lessonId));
    }

    @GetMapping("/topics/{id}/test-packages")
    public List<TopicTestPackageResponse> topicTestPackages(@PathVariable("id") UUID topicId) {
        return getTopicTestPackages.execute(topicId).stream().map(TopicTestPackageResponse::from).toList();
    }

    @PostMapping("/practice-sets/search")
    public List<PracticeSetResponse> searchPracticeSets(@Valid @RequestBody PracticeSetSearchRequest request) {
        return searchPracticeSets.execute(new SearchPracticeSetsCommand(request.knowledgePointId(),
                        request.excludePackageIds(), request.minQuestions(), request.limit(),
                        request.preferredLessonId()))
                .stream().map(PracticeSetResponse::from).toList();
    }

    @GetMapping("/lessons/{id}/practice-sets")
    public List<LessonPracticeSetResponse> lessonPracticeSets(@PathVariable("id") UUID lessonId,
                                                            @RequestParam(value = "skill", required = false) String value) {
        var skill = PracticeSetSkillFilter.parse(value);
        var sets = skill.isEmpty() ? getLessonPracticeSets.execute(lessonId)
                : getLessonPracticeSets.execute(lessonId, skill);
        return sets.stream().map(LessonPracticeSetResponse::from).toList();
    }

    @GetMapping("/topics/{id}/practice-sets")
    public TopicPracticeSetsResponse topicPracticeSets(@PathVariable("id") UUID topicId) {
        return TopicPracticeSetsResponse.from(getTopicPracticeSets.execute(topicId));
    }

    @PostMapping("/practice-sets/availability")
    public PracticeSetAvailabilityResponse practiceSetAvailability(
            @Valid @RequestBody PracticeSetAvailabilityRequest request) {
        return new PracticeSetAvailabilityResponse(countAvailablePracticeSets.execute(new CountPracticeSetsCommand(
                request.knowledgePointIds(), request.excludePackageIds(), request.minQuestions())));
    }

    @GetMapping("/package-versions/{id}")
    public PackageVersionContentResponse packageVersion(@PathVariable("id") UUID packageVersionId) {
        return PackageVersionContentResponse.from(getPackageVersionContent.execute(packageVersionId));
    }
}
