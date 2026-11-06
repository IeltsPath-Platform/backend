package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.LessonResourceType;
import com.ieltspath.content.domain.vo.LessonSummaryMarkdown;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LessonResourceTest {
    private final UUID lessonId = UUID.randomUUID();

    @Test
    void createTrimsTitleAndLinkAndKeepsTheType() {
        LessonResource resource = LessonResource.create(lessonId, LessonResourceType.SLIDE, "  Slide buổi 1 ",
                " https://docs.google.com/presentation/d/abc/edit ", 0);

        assertThat(resource.getLessonId()).isEqualTo(lessonId);
        assertThat(resource.getResourceType()).isEqualTo(LessonResourceType.SLIDE);
        assertThat(resource.getTitle()).isEqualTo("Slide buổi 1");
        assertThat(resource.getUrl()).isEqualTo("https://docs.google.com/presentation/d/abc/edit");
        assertThat(resource.getCreatedAt()).isEqualTo(resource.getUpdatedAt());
    }

    private static final String KEY = "lesson-document/0b1f7c9e-3a40-4d3a-9a52-1d2c8e0a7f11/9c2e5d3a-77b1-4f0a-8f6e-2b8d1c4a9e10.pdf";

    @Test
    void aDocumentCanBeAnUploadedFileThatHasNoLink() {
        LessonResource file = LessonResource.createFile(lessonId, " Giáo trình ", " " + KEY + " ", 0);

        assertThat(file.getResourceType()).isEqualTo(LessonResourceType.DOCUMENT);
        assertThat(file.getFileKey()).isEqualTo(KEY);
        assertThat(file.getUrl()).isNull();
        assertThat(file.getTitle()).isEqualTo("Giáo trình");
    }

    @Test
    void aFileResourceCanBeRenamedAndReorderedButNotTurnedIntoALink() {
        LessonResource file = LessonResource.createFile(lessonId, "Cũ", KEY, 0);

        file.update("Mới", null, 3);

        assertThat(file.getTitle()).isEqualTo("Mới");
        assertThat(file.getSortOrder()).isEqualTo(3);
        assertThat(file.getFileKey()).isEqualTo(KEY);
        assertThatThrownBy(() -> file.update("Mới", "https://a.com", 3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aResourceIsEitherALinkOrAFileAndOnlyADocumentCanBeAFile() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> new LessonResource(UUID.randomUUID(), lessonId, LessonResourceType.DOCUMENT, "x",
                "https://a.com", KEY, 0, now, now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LessonResource(UUID.randomUUID(), lessonId, LessonResourceType.DOCUMENT, "x",
                null, null, 0, now, now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LessonResource(UUID.randomUUID(), lessonId, LessonResourceType.SLIDE, "x",
                null, KEY, 0, now, now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LessonResource.createFile(lessonId, "x", "content-image/u/f.png", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:text/html,hi", "ftp://example.com/a", "/relative/link",
            "https://", "http://exa mple.com", "  "})
    void onlyAbsoluteHttpLinksAreAccepted(String url) {
        assertThatThrownBy(() -> LessonResource.create(lessonId, LessonResourceType.DOCUMENT, "Doc", url, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void httpAndUpperCaseSchemesAreAccepted() {
        assertThat(LessonResource.create(lessonId, LessonResourceType.DOCUMENT, "Doc", "HTTP://cdn.example.com/a.pdf", 0)
                .getUrl()).isEqualTo("HTTP://cdn.example.com/a.pdf");
    }

    @Test
    void blankOrLongTitlesTooLongLinksAndNegativeOrderAreRejected() {
        assertThatThrownBy(() -> LessonResource.create(lessonId, LessonResourceType.SLIDE, " ", "https://a.com", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LessonResource.create(lessonId, LessonResourceType.SLIDE, "x".repeat(256),
                "https://a.com", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LessonResource.create(lessonId, LessonResourceType.SLIDE, "Slide",
                "https://a.com/" + "x".repeat(LessonResource.MAX_URL_LENGTH), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LessonResource.create(lessonId, LessonResourceType.SLIDE, "Slide", "https://a.com", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LessonResource.create(lessonId, null, "Slide", "https://a.com", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aFailedUpdateLeavesTheResourceUnchanged() {
        LessonResource resource = LessonResource.create(lessonId, LessonResourceType.DOCUMENT, "Giáo trình",
                "https://drive.google.com/file/d/1", 0);

        assertThatThrownBy(() -> resource.update("Bài tập", "javascript:alert(1)", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(resource.getTitle()).isEqualTo("Giáo trình");
        assertThat(resource.getSortOrder()).isZero();

        resource.update("Bài tập", "https://drive.google.com/file/d/2", 1);
        assertThat(resource.getTitle()).isEqualTo("Bài tập");
        assertThat(resource.getUrl()).isEqualTo("https://drive.google.com/file/d/2");
        assertThat(resource.getSortOrder()).isEqualTo(1);
    }

    @Test
    void summaryMarkdownIsStrippedBlankBecomesNullAndLongTextIsRejected() {
        assertThat(LessonSummaryMarkdown.normalize("\n## Outcomes\n1. Describe\n")).isEqualTo("## Outcomes\n1. Describe");
        assertThat(LessonSummaryMarkdown.normalize("   ")).isNull();
        assertThat(LessonSummaryMarkdown.normalize(null)).isNull();
        assertThatThrownBy(() -> LessonSummaryMarkdown.normalize("x".repeat(LessonSummaryMarkdown.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
