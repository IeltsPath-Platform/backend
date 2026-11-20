package com.ieltspath.content.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.content.api.dto.request.CodeSettingsRequest;
import com.ieltspath.content.application.usecase.CodeSettingsUseCases;
import com.ieltspath.content.application.result.UpdateCodeSettings;
import com.ieltspath.content.api.dto.response.CodeSettingResponse;
import com.ieltspath.content.api.dto.response.CodeSuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import com.ieltspath.content.domain.vo.Skill;

@RestController
@PreAuthorize("hasRole('CONTENT_AUTHOR')")
public class CodeSettingsController {
    private final CodeSettingsUseCases codeSettings;
    private final CurrentUserProvider currentUser;

    public CodeSettingsController(CodeSettingsUseCases codeSettings, CurrentUserProvider currentUser) {
        this.codeSettings = codeSettings;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/content/admin/code-settings")
    public List<CodeSettingResponse> list() {
        return codeSettings.listConnected().stream().map(CodeSettingResponse::from).toList();
    }

    @PutMapping("/api/content/admin/code-settings/{kind}")
    public CodeSettingResponse update(@PathVariable String kind, @Valid @RequestBody CodeSettingsRequest.Update request) {
        return CodeSettingResponse.from(codeSettings.update(kind, new UpdateCodeSettings(request.applied(), request.pattern(),
                request.startNumber(), request.editable()), currentUser.requireUserId()));
    }

    @GetMapping("/api/content/admin/code-suggestion")
    public CodeSuggestionResponse suggest(@RequestParam(defaultValue = "COURSE") String kind,
                              @RequestParam(required = false) String band,
                              @RequestParam(required = false) UUID courseId,
                              @RequestParam(required = false) Skill skill) {
        return CodeSuggestionResponse.from(codeSettings.suggest(kind, band, courseId, skill));
    }
}
