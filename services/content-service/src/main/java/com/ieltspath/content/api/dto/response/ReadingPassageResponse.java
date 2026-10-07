package com.ieltspath.content.api.dto.response;

import com.ieltspath.content.application.result.ReadingPassageResult;

import java.util.List;
import java.util.UUID;

public record ReadingPassageResponse(
        UUID sectionId,
        String sectionTitle,
        String instructions,
        UUID packageId,
        String packageTitle,
        List<Paragraph> paragraphs
) {
    public record Paragraph(String label, String text) {
    }

    public static ReadingPassageResponse from(ReadingPassageResult result) {
        return new ReadingPassageResponse(
                result.sectionId(),
                result.sectionTitle(),
                result.instructions(),
                result.packageId(),
                result.packageTitle(),
                result.paragraphs().stream().map(p -> new Paragraph(p.label(), p.text())).toList()
        );
    }
}
