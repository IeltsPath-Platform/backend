package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.domain.vo.NoteSourceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateNoteRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 20000) String body,
        NoteSourceType sourceType,
        UUID sourceReferenceId
) {
    @AssertTrue(message = "sourceType and sourceReferenceId must both be present or absent")
    public boolean isSourcePairValid() {
        return (sourceType == null) == (sourceReferenceId == null);
    }
}
