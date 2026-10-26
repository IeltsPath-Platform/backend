package com.ieltspath.content.api.controller;

import com.ieltspath.content.api.dto.request.*;
import com.ieltspath.content.application.result.LessonAuthoringResult;
import com.ieltspath.content.application.result.LessonAuthoringSummaryResult;
import com.ieltspath.content.application.usecase.LessonAuthoringUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/content/admin")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class LessonAuthoringAdminController {
    private final LessonAuthoringUseCase authoring;
    public LessonAuthoringAdminController(LessonAuthoringUseCase authoring) { this.authoring = authoring; }

    @GetMapping("/topics/{topicId}/lessons") public java.util.List<LessonAuthoringSummaryResult> list(@PathVariable("topicId") UUID topicId) { return authoring.list(topicId); }
    @PostMapping("/topics/{topicId}/lessons") @ResponseStatus(HttpStatus.CREATED)
    public LessonAuthoringResult create(@PathVariable("topicId") UUID topicId, @Valid @RequestBody CreateLessonRequest r) { return authoring.create(topicId,r.code(),r.title(),r.summary(),r.summaryMarkdown(),r.estimatedMinutes(),r.sortOrder()); }
    @PutMapping("/topics/{topicId}/lessons/order") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable("topicId") UUID topicId,@Valid @RequestBody UpdateLessonOrderRequest r){authoring.reorderLessons(topicId,r.lessonIds());}
    @GetMapping("/lessons/{lessonId}") public LessonAuthoringResult get(@PathVariable("lessonId") UUID id){return authoring.get(id);}
    @PutMapping("/lessons/{lessonId}") public LessonAuthoringResult update(@PathVariable("lessonId") UUID id,@Valid @RequestBody UpdateLessonRequest r){return authoring.update(id,r.title(),r.summary(),r.summaryMarkdown(),r.estimatedMinutes());}
    @PutMapping("/lessons/{lessonId}/knowledge-points") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void points(@PathVariable("lessonId") UUID id,@Valid @RequestBody ReplaceLessonKnowledgePointsRequest r){authoring.knowledgePoints(id,r.knowledgePointIds());}
    @PostMapping("/lessons/{lessonId}/blocks") @ResponseStatus(HttpStatus.CREATED)
    public LessonAuthoringResult addBlock(@PathVariable("lessonId") UUID id,@Valid @RequestBody LessonBlockRequest r){return authoring.addBlock(id,r.blockType(),r.sortOrder(),r.textContent(),r.assetId(),r.vocabularySenseIds(),r.questionVersionIds(),r.knowledgePointIds());}
    @PutMapping("/lessons/{lessonId}/blocks/{blockId}")
    public LessonAuthoringResult updateBlock(@PathVariable("lessonId") UUID lessonId,@PathVariable("blockId") UUID blockId,@Valid @RequestBody LessonBlockRequest request){return authoring.updateBlock(lessonId,blockId,request.blockType(),request.sortOrder(),request.textContent(),request.assetId(),request.vocabularySenseIds(),request.questionVersionIds(),request.knowledgePointIds());}
    @DeleteMapping("/lessons/{lessonId}/blocks/{blockId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBlock(@PathVariable("lessonId") UUID id,@PathVariable("blockId") UUID bid){authoring.deleteBlock(id,bid);}
    @PutMapping("/lessons/{lessonId}/blocks/order") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderBlocks(@PathVariable("lessonId") UUID id,@Valid @RequestBody UpdateBlockOrderRequest r){authoring.reorderBlocks(id,r.blockIds());}
    @PostMapping("/lessons/{lessonId}/publish") public LessonAuthoringResult publish(@PathVariable("lessonId") UUID id){return authoring.publish(id);}
    @PostMapping("/lessons/{lessonId}/archive") public LessonAuthoringResult archive(@PathVariable("lessonId") UUID id){return authoring.archive(id);}
}
