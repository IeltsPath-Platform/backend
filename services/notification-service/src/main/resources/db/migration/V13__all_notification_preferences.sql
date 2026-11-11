ALTER TABLE notification_preferences DROP CONSTRAINT notification_preferences_event_type_check;

ALTER TABLE notification_preferences ADD CONSTRAINT notification_preferences_event_type_check CHECK (event_type IN (
    'community.post-commented.v1',
    'community.comment-replied.v1',
    'community.comment-mentioned.v1',
    'community.post-reacted.v1',
    'community.user-followed.v1',
    'assessment.grading-requested.v1',
    'assessment.grading-due-soon.v1',
    'assessment.grading-overdue.v1',
    'assessment.grading-completed.v1',
    'access.subscription-granted.v1'
));
