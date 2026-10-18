CREATE UNIQUE INDEX uq_notifications_mention_comment_recipient
    ON notifications(comment_id, recipient_id)
    WHERE event_type = 'community.comment-mentioned.v1';
