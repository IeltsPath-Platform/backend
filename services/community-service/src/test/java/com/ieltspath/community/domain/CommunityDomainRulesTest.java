package com.ieltspath.community.domain;

import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.aggregate.Post;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityForbiddenException;
import com.ieltspath.community.domain.vo.ContentStatus;
import com.ieltspath.community.domain.vo.PostCategory;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityDomainRulesTest {

    private static Post post(UUID author) {
        return Post.create(author, PostCategory.GENERAL, "Title", "Body");
    }

    private static Comment comment(UUID author) {
        return Comment.create(UUID.randomUUID(), author, null, "Body");
    }

    @Test
    void allSixCategoriesCanBeUsedForAPost() {
        assertEquals(6, PostCategory.values().length);
        for (PostCategory category : PostCategory.values()) {
            assertEquals(category, Post.create(UUID.randomUUID(), category, null, "Body").getCategory());
        }
    }

    @Test
    void editWithoutACategoryIsRejected() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        var error = assertThrows(CommunityException.class, () -> p.edit(author, null, null, "Body"));
        assertEquals("Category is required", error.getMessage());
    }

    @Test
    void authorCannotEditAHiddenPost() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        p.moderate(ContentStatus.HIDDEN);
        var error = assertThrows(CommunityException.class, () -> p.edit(author, PostCategory.GENERAL, null, "New"));
        assertEquals("Only active posts may be changed", error.getMessage());
        assertEquals("Body", p.getBody());
    }

    @Test
    void authorCannotEditADeletedPost() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        p.delete(author);
        var error = assertThrows(CommunityException.class, () -> p.edit(author, PostCategory.GENERAL, null, "New"));
        assertEquals("Only active posts may be changed", error.getMessage());
        assertEquals("Body", p.getBody());
    }

    @Test
    void anotherUserCannotDeleteAPost() {
        Post p = post(UUID.randomUUID());
        var error = assertThrows(CommunityForbiddenException.class, () -> p.delete(UUID.randomUUID(), false));
        assertEquals("Only the author may change this post", error.getMessage());
        assertEquals(ContentStatus.ACTIVE, p.getStatus());
    }

    @Test
    void aDeletedPostCannotBeDeletedAgain() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        p.delete(author);
        var error = assertThrows(CommunityException.class, () -> p.delete(author));
        assertEquals("Only active posts may be changed", error.getMessage());
        assertEquals(ContentStatus.DELETED, p.getStatus());
    }

    @Test
    void anAdministratorCannotDeleteAHiddenPost() {
        Post p = post(UUID.randomUUID());
        p.moderate(ContentStatus.HIDDEN);
        assertThrows(CommunityException.class, () -> p.delete(UUID.randomUUID(), true));
        assertEquals(ContentStatus.HIDDEN, p.getStatus());
    }

    @Test
    void aHiddenPostCanBeRestored() {
        Post p = post(UUID.randomUUID());
        p.moderate(ContentStatus.HIDDEN);
        p.moderate(ContentStatus.ACTIVE);
        assertEquals(ContentStatus.ACTIVE, p.getStatus());
    }

    @Test
    void aDeletedPostCannotBeHidden() {
        UUID author = UUID.randomUUID();
        Post p = post(author);
        p.delete(author);
        var error = assertThrows(CommunityException.class, () -> p.moderate(ContentStatus.HIDDEN));
        assertEquals("Deleted post cannot be moderated", error.getMessage());
        assertEquals(ContentStatus.DELETED, p.getStatus());
    }

    @Test
    void moderationOnlyAcceptsActiveOrHidden() {
        Post p = post(UUID.randomUUID());
        for (ContentStatus bad : new ContentStatus[]{ContentStatus.DELETED, null}) {
            var error = assertThrows(CommunityException.class, () -> p.moderate(bad));
            assertEquals("Moderation status must be ACTIVE or HIDDEN", error.getMessage());
        }
        assertEquals(ContentStatus.ACTIVE, p.getStatus());
    }

    @Test
    void fourImagesAreAccepted() {
        Post p = post(UUID.randomUUID());
        p.setMedia(List.of("https://images.unsplash.com/1.png", "https://images.unsplash.com/2.png",
                "https://images.unsplash.com/3.png", "https://images.unsplash.com/4.png"), null, null);
        assertEquals(4, p.getImageUrls().size());
    }

    @Test
    void fiveImagesAreRejected() {
        Post p = post(UUID.randomUUID());
        var urls = List.of("https://images.unsplash.com/1.png", "https://images.unsplash.com/2.png",
                "https://images.unsplash.com/3.png", "https://images.unsplash.com/4.png", "https://images.unsplash.com/5.png");
        var error = assertThrows(CommunityException.class, () -> p.setMedia(urls, null, null));
        assertEquals("At most four images are allowed", error.getMessage());
        assertTrue(p.getImageUrls().isEmpty());
    }

    @Test
    void targetBandMustBeBetweenZeroAndNineInHalfSteps() {
        Post p = post(UUID.randomUUID());
        for (String ok : new String[]{"0", "4.5", "9"}) {
            p.setMedia(List.of(), null, new BigDecimal(ok));
            assertEquals(new BigDecimal(ok), p.getTargetBand());
        }
        for (String bad : new String[]{"9.5", "-0.5", "6.3"}) {
            var error = assertThrows(CommunityException.class, () -> p.setMedia(List.of(), null, new BigDecimal(bad)));
            assertEquals("Target band must be between 0 and 9 in half-band steps", error.getMessage());
        }
    }

    @Test
    void anImageLinkMustBeHttps() {
        Post p = post(UUID.randomUUID());
        var error = assertThrows(CommunityException.class,
                () -> p.setMedia(List.of("http://images.unsplash.com/a.png"), null, null));
        assertEquals("Media must be a valid HTTPS URL", error.getMessage());
    }

    @Test
    void anImageLinkOver500CharactersOrWithUserInfoIsRejected() {
        Post p = post(UUID.randomUUID());
        String longUrl = "https://images.unsplash.com/" + "a".repeat(500);
        assertThrows(CommunityException.class, () -> p.setMedia(List.of(longUrl), null, null));
        assertThrows(CommunityException.class,
                () -> p.setMedia(List.of("https://user@images.unsplash.com/a.png"), null, null));
        assertThrows(CommunityException.class,
                () -> p.setMedia(List.of("https://images.unsplash.com:8443/a.png"), null, null));
    }

    @Test
    void authorCannotEditADeletedOrHiddenComment() {
        UUID author = UUID.randomUUID();
        Comment deleted = comment(author);
        deleted.delete(author);
        var error = assertThrows(CommunityException.class, () -> deleted.edit(author, "New"));
        assertEquals("Only active comments may be changed", error.getMessage());
        Comment hidden = comment(author);
        hidden.moderate(ContentStatus.HIDDEN);
        assertThrows(CommunityException.class, () -> hidden.edit(author, "New"));
    }

    @Test
    void anotherUserCannotDeleteAComment() {
        Comment c = comment(UUID.randomUUID());
        var error = assertThrows(CommunityForbiddenException.class, () -> c.delete(UUID.randomUUID(), false));
        assertEquals("Only the author may change this comment", error.getMessage());
        assertEquals(ContentStatus.ACTIVE, c.getStatus());
    }

    @Test
    void aHiddenCommentCanBeRestored() {
        Comment c = comment(UUID.randomUUID());
        c.moderate(ContentStatus.HIDDEN);
        c.moderate(ContentStatus.ACTIVE);
        assertEquals(ContentStatus.ACTIVE, c.getStatus());
    }

    @Test
    void aDeletedCommentCannotBeHidden() {
        UUID author = UUID.randomUUID();
        Comment c = comment(author);
        c.delete(author);
        var error = assertThrows(CommunityException.class, () -> c.moderate(ContentStatus.HIDDEN));
        assertEquals("Deleted comment cannot be moderated", error.getMessage());
        assertEquals(ContentStatus.DELETED, c.getStatus());
    }

    @Test
    void aReportReasonOfExactly1000CharactersIsAccepted() {
        ContentReport report = ContentReport.create(UUID.randomUUID(), ReportContentType.POST, UUID.randomUUID(), "r".repeat(1000));
        assertEquals(1000, report.getReason().length());
        assertEquals(ReportStatus.PENDING, report.getStatus());
    }

    @Test
    void aReportReasonOf1001CharactersIsRejected() {
        var error = assertThrows(CommunityException.class,
                () -> ContentReport.create(UUID.randomUUID(), ReportContentType.POST, UUID.randomUUID(), "r".repeat(1001)));
        assertEquals("Report reason must be at most 1000 characters", error.getMessage());
    }
}
