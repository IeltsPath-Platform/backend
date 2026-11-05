-- Four Writing essays a learner sent to the demo examiner (examiner@ielts.demo), waiting in the examiner's queue.
-- DEV/DEMO ONLY. Idempotent: every row has a fixed id, so running it twice adds nothing.
--
-- Works before and after assessment-service applies its V17 migration (the learner's choice of examiner):
--   after V17 the job is written with the chosen examiner and the time of the choice;
--   before V17 the job is written with the examiner as its assigned examiner, which V17 turns into the chosen
--   examiner (chosen at the time the job was created) when assessment-service next starts.
-- The learner is any CUSTOMER of user_db; pass the id:
--   docker cp docs/demo/examiner-demo-assessment-db.sql ieltspath-postgres-1:/tmp/examiner-demo-assessment-db.sql
--   docker exec ieltspath-postgres-1 psql -U postgres -d assessment_db -v ON_ERROR_STOP=1 -v learner=<learner user id> -f /tmp/examiner-demo-assessment-db.sql
-- (copy the file in instead of piping it: a pipe through Windows PowerShell re-encodes non-ASCII text).
--
-- Each essay is a standalone review, as the learner flow creates it: a human_review_submissions row, its HUMAN job
-- (chosen examiner and the time they were chosen) and its human_reviews row. The waits are 2, 9, 26 and 40 hours,
-- so none is late yet (the 48 hour deadline).

\if :{?learner}
\else
  \echo 'Pass the learner id: -v learner=<uuid>'
  \quit
\endif

CREATE TEMP TABLE demo_essay AS
SELECT * FROM (VALUES
  (1, 'PRACTICE', 'TASK_2', 2,
   'Some people think that cities should plant more trees to improve the environment, while others believe the money should be spent on public transport. Discuss both views and give your own opinion.',
   'Nowadays many cities face serious problem with pollution and traffic. Some people argue that planting trees is the best solution, whereas other think that public transport deserve more money.

On the one hand, trees makes the air cleaner because they absorb carbon dioxide and give oxygen. In addition, a green street is more pleasant to live, and it can reduce the temperature in summer. For example, many residents in Singapore enjoy parks every weekend.

On the other hand, a good bus or metro system can reduce the number of cars on the road. Therefore, there is less smoke and people spend less time in traffic jams. However, building a metro line cost a huge amount of money and take many years.

In my opinion, both solutions are important but the government should invest in public transport first, because it solve the traffic problem and the pollution at the same time. In conclusion, trees are useful, but better transport bring more benefit for the city.'),
  (2, 'LESSON', 'TASK_1', 9,
   'The bar chart shows the number of students enrolled in three different university courses in 2010 and 2020. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.',
   'The bar chart illustrate the number of students who enrolled in Engineering, Business and Arts in 2010 and 2020.

Overall, the number of students rose in all three courses, and Engineering was the most popular course in both years.

In 2010, around 800 students was enrolled in Engineering, while Business attracted about 950 students and Arts only 750. By 2020, the figure for Engineering have rose dramatically to nearly 1,700 students. Business and Arts also grew, to about 1,150 and 900 respectively.

It is clear that Engineering overtook Business during the period, whereas the gap between Business and Arts stay almost the same.'),
  (3, 'PRACTICE', 'TASK_2', 26,
   'Online learning is becoming more popular than classroom learning. Do the advantages of this development outweigh the disadvantages?',
   'In recent years, online courses have became very popular among students of all ages. In my view, the advantages of this trend are greater than the disadvantages.

The main advantage is flexibility. Students can study at any time and in any place, so people who work full time can still get a degree. Moreover, online courses are usually cheaper because learners does not need to pay for accommodation or transport. A friend of mine, for instance, finished a language course at home while taking care of her baby.

However, there are some drawbacks. Firstly, many learners lacks self discipline and they often postpone their lessons. Secondly, students have fewer chances to talk to teachers and classmates, which can make them feel lonely.

To conclude, although online learning has some weakness, I believe its benefits for working people and for families are more important.'),
  (4, 'LESSON', 'TASK_2', 40,
   'Some people prefer to work from home, while others think it is better to work in an office. Discuss both views and give your own opinion.',
   'Working from home has become common after the pandemic, but many people still prefer going to an office. This essay will discuss both sides before giving my opinion.

People who work at home save a lot of time because they do not need to travel every day. They can also arrange their schedule freely and spend more time with they family. In contrast, an office gives workers a clear separation between work and private life, and it is easier to cooperate with colleagues face to face.

From my point of view, the best solution is a mixture of the two. Employees could work at home for two or three days a week and come to the office for important meetings. This way, they enjoy flexible but also keep good relationships with their team.

In summary, both options have strengths, and companies should let staff choose what fit them best.')
) AS e(n, source, task, hours, stem, essay);

INSERT INTO human_review_submissions (id, request_id, learner_id, source_submission_id, source, task, prompt_snapshot,
                                      essay_text, rubric_snapshot, created_at)
SELECT ('d0000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid,
       ('d1000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid,
       :'learner'::uuid,
       ('d2000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid,
       source, task, jsonb_build_object('stem', stem), essay,
       jsonb_build_object('code', 'IELTS_WRITING_' || task, 'version', 1, 'criteria', '[]'::jsonb),
       now() - make_interval(hours => hours)
FROM demo_essay
ON CONFLICT DO NOTHING;

SELECT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'grading_jobs'
                 AND column_name = 'requested_examiner_id') AS has_selection \gset

\if :has_selection
INSERT INTO grading_jobs (id, submission_id, human_review_submission_id, user_id, skill, grading_mode, status,
                          idempotency_key, reason, requested_examiner_id, requested_at, created_at)
SELECT ('d3000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid, NULL,
       ('d0000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid,
       :'learner'::uuid, 'WRITING', 'HUMAN', 'QUEUED',
       'human-review:d1000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'), 'HUMAN_CREDIT',
       '00000000-0000-0000-0000-0000000000e1'::uuid,
       now() - make_interval(hours => hours), now() - make_interval(hours => hours)
FROM demo_essay
ON CONFLICT DO NOTHING;
\else
INSERT INTO grading_jobs (id, submission_id, human_review_submission_id, user_id, skill, grading_mode, status,
                          idempotency_key, reason, assigned_examiner_id, created_at)
SELECT ('d3000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid, NULL,
       ('d0000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid,
       :'learner'::uuid, 'WRITING', 'HUMAN', 'QUEUED',
       'human-review:d1000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'), 'HUMAN_CREDIT',
       '00000000-0000-0000-0000-0000000000e1'::uuid, now() - make_interval(hours => hours)
FROM demo_essay
ON CONFLICT DO NOTHING;
\endif

INSERT INTO human_reviews (id, grading_job_id, status)
SELECT ('d4000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid,
       ('d3000000-0000-4000-8000-0000000000' || lpad(n::text, 2, '0'))::uuid, 'QUEUED'
FROM demo_essay
ON CONFLICT DO NOTHING;

SELECT j.id AS job, h.source, h.task, j.status, round(extract(epoch FROM now() - j.created_at) / 3600) AS hours_waiting,
       length(h.essay_text) AS essay_chars
FROM grading_jobs j JOIN human_review_submissions h ON h.id = j.human_review_submission_id
WHERE h.request_id::text LIKE 'd1000000-%' ORDER BY j.created_at DESC;
