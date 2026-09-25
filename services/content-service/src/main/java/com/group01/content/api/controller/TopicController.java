package com.group01.content.api.controller;

import com.group01.content.domain.vo.BandRange;
import com.group01.content.api.dto.request.CreateTopicRequest;
import com.group01.content.api.dto.request.UpdateTopicRequest;
import com.group01.content.api.dto.response.TopicResponse;
import com.group01.content.api.dto.response.TopicTreeResponse;
import com.group01.content.application.command.CreateTopicCommand;
import com.group01.content.application.command.UpdateTopicCommand;
import com.group01.content.application.result.TopicResult;
import com.group01.content.application.usecase.CreateTopicUseCase;
import com.group01.content.application.usecase.GetTopicTreeUseCase;
import com.group01.content.application.usecase.UpdateTopicUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/content/topics", "/api/content/admin/topics"})
@RequiredArgsConstructor
public class TopicController {

    private final GetTopicTreeUseCase getTopicTreeUseCase;
    private final CreateTopicUseCase createTopicUseCase;
    private final UpdateTopicUseCase updateTopicUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR', 'CUSTOMER', 'EXAMINER')")
    public List<TopicTreeResponse> getTopicTree() {
        return getTopicTreeUseCase.execute().stream()
                .map(TopicTreeResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public TopicResponse createTopic(@Valid @RequestBody CreateTopicRequest request) {
        TopicResult result = createTopicUseCase.execute(new CreateTopicCommand(
                request.parentTopicId(),
                request.code(),
                request.name(),
                request.sortOrder(),
                BandRange.of(request.bandMin(), request.bandMax())
        ));
        return TopicResponse.from(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_AUTHOR')")
    public TopicResponse updateTopic(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateTopicRequest request
    ) {
        TopicResult result = updateTopicUseCase.execute(new UpdateTopicCommand(
                id,
                request.parentTopicId(),
                request.name(),
                request.sortOrder(),
                request.status(),
                BandRange.of(request.bandMin(), request.bandMax())
        ));
        return TopicResponse.from(result);
    }
}
