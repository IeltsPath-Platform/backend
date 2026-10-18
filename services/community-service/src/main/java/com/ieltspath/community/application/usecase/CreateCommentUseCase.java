package com.ieltspath.community.application.usecase;

import com.ieltspath.community.application.CommunityActor;
import com.ieltspath.community.application.command.CreateCommentCommand;
import com.ieltspath.community.application.result.CommentResult;
import com.ieltspath.community.domain.aggregate.Comment;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.exception.CommunityNotFoundException;
import com.ieltspath.community.domain.repository.CommentRepository;
import com.ieltspath.community.domain.repository.PostRepository;
import com.ieltspath.community.application.port.CommunityEventOutbox;
import com.ieltspath.community.application.port.CommunityEventTypes;
import com.ieltspath.community.application.port.PublicProfileClient;
import lombok.RequiredArgsConstructor;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class CreateCommentUseCase {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final CommunityEventOutbox outbox;
    private final PublicProfileClient profiles;
    private final TransactionTemplate transaction;
    private static final Pattern MENTION = Pattern.compile("@\\[([^\\]]+)]\\(([0-9a-fA-F-]{36})\\)");

    public CommentResult execute(CommunityActor actor, CreateCommentCommand command) {
        // Mentions are verified against user-service before any row is locked: a slow remote call must not
        // hold the parent comment lock or a database connection.
        Set<UUID> mentionIds = requireValidMentions(command.body());
        return transaction.execute(status -> save(actor, command, mentionIds));
    }

    private Set<UUID> requireValidMentions(String body) {
        var mentionIds = new LinkedHashSet<UUID>();
        var matcher = MENTION.matcher(body);
        while (matcher.find()) {
            try { mentionIds.add(UUID.fromString(matcher.group(2))); }
            catch (IllegalArgumentException invalid) { throw new CommunityException("Mention contains an invalid user"); }
        }
        if (mentionIds.size() > 5) throw new CommunityException("A comment can mention at most five users");
        if (!mentionIds.isEmpty() && !profiles.findProfiles(mentionIds).keySet().containsAll(mentionIds))
            throw new CommunityException("Mentioned user was not found or is not visible");
        return mentionIds;
    }

    private CommentResult save(CommunityActor actor, CreateCommentCommand command, Set<UUID> mentionIds) {
        var post = CommunityApplicationSupport.requireVisiblePost(posts, command.postId(), actor);
        var recipientId = post.getAuthorId();
        var reply = command.parentCommentId() != null;
        if (command.parentCommentId() != null) {
            var parent = comments.findByIdForUpdate(command.parentCommentId())
                    .orElseThrow(() -> new CommunityNotFoundException("Comment not found"));
            if (!parent.getPostId().equals(command.postId()) || parent.getStatus() != com.ieltspath.community.domain.vo.ContentStatus.ACTIVE) {
                throw new CommunityException("Parent comment must be active and belong to the same post");
            }
            recipientId = parent.getAuthorId();
            var rootId = parent.getParentCommentId() == null ? parent.getId() : parent.getParentCommentId();
            command = new CreateCommentCommand(command.postId(), rootId, command.body());
        }
        var saved = comments.save(Comment.create(
                command.postId(), actor.userId(), command.parentCommentId(), command.body()
        ));
        for (UUID mentionId : mentionIds) {
            if (!actor.userId().equals(mentionId))
                outbox.record(CommunityEventTypes.COMMENT_MENTIONED, actor.userId(), mentionId, command.postId(), saved.getId());
        }
        if (!actor.userId().equals(recipientId) && !mentionIds.contains(recipientId)) {
            outbox.record(reply ? CommunityEventTypes.COMMENT_REPLIED : CommunityEventTypes.POST_COMMENTED,
                    actor.userId(), recipientId, command.postId(), saved.getId());
        }
        return CommentResult.from(saved);
    }
}
