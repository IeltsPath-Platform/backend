package com.ieltspath.content.application.result;

import java.util.List;
import java.util.UUID;

public record ReadingPassageResult(
        UUID sectionId,
        String sectionTitle,
        String instructions,
        UUID packageId,
        String packageTitle,
        List<Paragraph> paragraphs
) {
    /** One passage paragraph with its IELTS-style label (A, B, C...). */
    public record Paragraph(String label, String text) {
    }
}
