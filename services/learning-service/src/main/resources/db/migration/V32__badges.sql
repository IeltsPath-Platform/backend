-- The badge catalog is fixed; an admin can switch a badge on or off but the conditions live in the code. A badge is awarded
-- once and never taken back. Its points go through the reward queue when it is first awarded.
CREATE TABLE badges (
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(300) NOT NULL,
    reward_points INTEGER NOT NULL DEFAULT 0 CHECK (reward_points BETWEEN 0 AND 100),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE learner_badges (
    user_id UUID NOT NULL,
    badge_code VARCHAR(40) NOT NULL REFERENCES badges (code),
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, badge_code)
);

INSERT INTO badges (code, name, description, reward_points) VALUES
    ('STREAK_7', 'Chuỗi 7 ngày', 'Học liên tục 7 ngày.', 5),
    ('STREAK_30', 'Chuỗi 30 ngày', 'Học liên tục 30 ngày.', 15),
    ('STREAK_100', 'Chuỗi 100 ngày', 'Học liên tục 100 ngày.', 50),
    ('KP_MAINTAIN_10', '10 kiến thức vững', 'Có 10 điểm kiến thức ở mức vững (Maintain).', 5),
    ('KP_MAINTAIN_50', '50 kiến thức vững', 'Có 50 điểm kiến thức ở mức vững (Maintain).', 20),
    ('FIRST_TOPIC_TEST', 'Bài kiểm tra chủ đề đầu tiên', 'Hoàn thành bài kiểm tra chủ đề đầu tiên.', 3),
    ('COURSE_PASSED', 'Hoàn thành khóa học', 'Đạt bài kiểm tra cuối khóa.', 20),
    ('LEVEL_10', 'Cấp độ 10', 'Đạt cấp độ 10.', 20),
    ('QUESTS_30', '30 nhiệm vụ ngày', 'Hoàn thành 30 nhiệm vụ ngày.', 10);
