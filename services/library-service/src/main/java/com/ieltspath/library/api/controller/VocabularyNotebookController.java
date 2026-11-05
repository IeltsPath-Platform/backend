package com.ieltspath.library.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.library.api.dto.request.AddNotebookEntryRequest;
import com.ieltspath.library.api.dto.request.SetUnderstandingLevelRequest;
import com.ieltspath.library.api.dto.response.NotebookEntryResponse;
import com.ieltspath.library.api.dto.response.NotebookSummaryResponse;
import com.ieltspath.library.api.dto.response.PageResponse;
import com.ieltspath.library.application.port.VocabularyNotebookReader;
import com.ieltspath.library.application.query.NotebookQuery;
import com.ieltspath.library.application.usecase.*;
import com.ieltspath.library.domain.exception.InvalidDataException;
import com.ieltspath.library.domain.vo.UnderstandingLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/learning-support/flashcards/notebook")
@RequiredArgsConstructor
public class VocabularyNotebookController {
    private final CurrentUserProvider currentUser;
    private final AddNotebookEntryUseCase add;
    private final ListNotebookEntriesUseCase list;
    private final GetNotebookSummaryUseCase summary;
    private final SetUnderstandingLevelUseCase setLevel;
    private final VocabularyNotebookReader reader;

    @PostMapping("/entries")
    public ResponseEntity<NotebookEntryResponse> add(@Valid @RequestBody AddNotebookEntryRequest request) {
        var saved = add.execute(currentUser.requireUserId(), request.vocabularySenseId());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(NotebookEntryResponse.from(saved.entry()));
    }

    @GetMapping("/entries")
    public PageResponse<NotebookEntryResponse> list(@RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) List<UnderstandingLevel> level,
            @RequestParam(required = false) Boolean due,
            @RequestParam(defaultValue = "RECENT") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @RequestParam(required = false) UUID topicId,
            @RequestParam(required = false) UUID setId) {
        if (!List.of("RECENT", "DUE", "ALPHABET", "WEAKEST").contains(sort)) throw new InvalidDataException("sort is invalid");
        return PageResponse.from(list.execute(currentUser.requireUserId(), new NotebookQuery(q, level, due, sort, page, size, topicId, setId)),
                NotebookEntryResponse::from);
    }

    @GetMapping("/summary")
    public NotebookSummaryResponse summary(@RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String timezone) {
        try {
            if (!ZoneId.getAvailableZoneIds().contains(timezone)) throw new InvalidDataException("timezone must be an IANA timezone");
            return NotebookSummaryResponse.from(summary.execute(currentUser.requireUserId(), ZoneId.of(timezone)));
        }
        catch (java.time.DateTimeException exception) { throw new InvalidDataException("timezone must be an IANA timezone"); }
    }

    @PutMapping("/entries/{flashcardId}/level")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setLevel(@PathVariable UUID flashcardId, @Valid @RequestBody SetUnderstandingLevelRequest request) {
        setLevel.execute(currentUser.requireUserId(), flashcardId, request.level(), request.requestId());
    }

    @GetMapping("/saved")
    public List<String> saved(@RequestParam(required = false) String senseIds) {
        List<UUID> ids = parseIds(senseIds);
        return reader.findSavedSenseIds(currentUser.requireUserId(), ids).stream().map(UUID::toString).toList();
    }

    private List<UUID> parseIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        String[] parts = value.split(",", -1);
        if (parts.length > 100) throw new InvalidDataException("senseIds accepts at most 100 UUIDs");
        try { return Arrays.stream(parts).map(UUID::fromString).distinct().toList(); }
        catch (IllegalArgumentException exception) { throw new InvalidDataException("senseIds must contain UUIDs"); }
    }
}
