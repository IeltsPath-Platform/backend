-- How many practice-test milestones a course has, as Content last reported it while serving a learner. The course
-- score needs the count when an assessment event is applied, and a message consumer has no learner token to ask Content.
CREATE TABLE course_milestone_counts (
    course_id       UUID PRIMARY KEY,
    milestone_count INTEGER      NOT NULL CHECK (milestone_count >= 0),
    refreshed_at    TIMESTAMPTZ  NOT NULL DEFAULT clock_timestamp()
);
