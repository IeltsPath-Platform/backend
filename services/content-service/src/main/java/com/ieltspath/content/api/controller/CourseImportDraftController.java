package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.response.CourseImportResponses;
import com.ieltspath.content.application.usecase.CourseImportDraftUseCases;
import com.ieltspath.content.domain.vo.CourseImportModel;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The author's kept course workbooks: keep one from a file, open it, correct it, import it, delete it. */
@RestController
@RequestMapping("/api/content/admin/course-import/drafts")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class CourseImportDraftController {

    /** The rows by sheet, as the editing screen sends them back. */
    public record UpdateRequest(@NotNull Map<String, List<CourseImportModel.Row>> sheets) {
    }

    private final CourseImportDraftUseCases drafts;
    private final CurrentUserProvider currentUser;

    public CourseImportDraftController(CourseImportDraftUseCases drafts, CurrentUserProvider currentUser) {
        this.drafts = drafts;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseImportResponses.DraftDetail create(@RequestParam("file") MultipartFile file) {
        return CourseImportResponses.DraftDetail.from(drafts.create(currentUser.requireUserId(),
                file.getOriginalFilename(), CourseImportController.bytes(file)));
    }

    @GetMapping
    public List<CourseImportResponses.DraftSummary> list() {
        return drafts.list(currentUser.requireUserId()).stream().map(CourseImportResponses.DraftSummary::from).toList();
    }

    @GetMapping("/{id}")
    public CourseImportResponses.DraftDetail get(@PathVariable UUID id) {
        return CourseImportResponses.DraftDetail.from(drafts.get(currentUser.requireUserId(), id));
    }

    @PutMapping("/{id}")
    public CourseImportResponses.DraftDetail update(@PathVariable UUID id, @RequestBody UpdateRequest request) {
        return CourseImportResponses.DraftDetail.from(drafts.update(currentUser.requireUserId(), id, request.sheets()));
    }

    @PostMapping("/{id}/import")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseImportResponses.Imported importDraft(@PathVariable UUID id) {
        return CourseImportResponses.Imported.from(drafts.importDraft(currentUser.requireUserId(), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        drafts.delete(currentUser.requireUserId(), id);
    }
}
