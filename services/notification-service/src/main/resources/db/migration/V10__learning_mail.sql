-- Address and name per user, fed from user-service. Learning and account-expiry mails read it; assessment does not know emails.
CREATE TABLE user_contacts (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(150),
    occurred_at TIMESTAMPTZ NOT NULL
);

-- Per-user switches for mail categories. Missing row means enabled. Only the learning category is switchable.
CREATE TABLE mail_preferences (
    user_id UUID NOT NULL,
    category VARCHAR(40) NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, category)
);

-- New grading jobs for an examiner, collected and sent as one digest per hour.
CREATE TABLE examiner_job_digest (
    grading_job_id UUID PRIMARY KEY,
    examiner_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_examiner_job_digest_examiner ON examiner_job_digest (examiner_id);
