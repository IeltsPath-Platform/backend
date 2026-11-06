package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.api.dto.request.StudyUnitRequest;
import com.ieltspath.library.application.result.StudyContentResult.*;
import com.ieltspath.library.application.usecase.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/content/admin/vocabulary")
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class StudyUnitAuthoringController {
    private final CurrentUserProvider currentUser;
    private final CreateStudyUnitUseCase create;
    private final UpdateStudyUnitUseCase update;
    private final ReorderStudyUnitsUseCase reorder;
    private final ValidateStudyUnitUseCase validate;
    private final PublishStudyUnitUseCase publish;
    private final ArchiveStudyUnitUseCase archive;
    private final ListAuthoringStudyUnitsUseCase list;
    public record OrderRequest(@NotNull @Size(max=1000) List<@NotNull UUID> unitIds) {}
    @GetMapping("/topics/{topicId}/study-units") public List<AuthorUnit> list(@PathVariable UUID topicId) { return list.execute(topicId); }
    @PostMapping("/topics/{topicId}/study-units") @ResponseStatus(HttpStatus.CREATED)
    public AuthorUnit create(@PathVariable UUID topicId,@Valid @RequestBody StudyUnitRequest body) { return create.execute(topicId,currentUser.requireUserId(),body.draft()); }
    @PutMapping("/study-units/{unitId}") public AuthorUnit update(@PathVariable UUID unitId,@Valid @RequestBody StudyUnitRequest body) { return update.execute(unitId,currentUser.requireUserId(),body.draft()); }
    @PutMapping("/topics/{topicId}/study-units/order") public List<AuthorUnit> order(@PathVariable UUID topicId,@Valid @RequestBody OrderRequest body) { return reorder.execute(topicId,body.unitIds()); }
    @GetMapping("/study-units/{unitId}/validation") public Validation validation(@PathVariable UUID unitId) { return validate.execute(unitId); }
    @PostMapping("/study-units/{unitId}/publish") public AuthorUnit publish(@PathVariable UUID unitId) { return publish.execute(unitId); }
    @PostMapping("/study-units/{unitId}/archive") public AuthorUnit archive(@PathVariable UUID unitId) { return archive.execute(unitId); }
}
