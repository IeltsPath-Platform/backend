INSERT INTO grading_point_costs (skill, grading_mode, point_cost, effective_from, status)
SELECT 'WRITING', 'AI', 3, CURRENT_TIMESTAMP, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM grading_point_costs
    WHERE skill = 'WRITING' AND grading_mode = 'AI' AND status = 'ACTIVE'
);

INSERT INTO grading_point_costs (skill, grading_mode, point_cost, effective_from, status)
SELECT 'SPEAKING', 'AI', 5, CURRENT_TIMESTAMP, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM grading_point_costs
    WHERE skill = 'SPEAKING' AND grading_mode = 'AI' AND status = 'ACTIVE'
);
