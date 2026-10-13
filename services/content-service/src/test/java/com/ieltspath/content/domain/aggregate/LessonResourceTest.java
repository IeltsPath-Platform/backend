package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.LessonResourceType;
import com.ieltspath.content.domain.vo.LessonSummaryMarkdown;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
