-- AI-authored draft practice material; an IELTS-qualified examiner must review it before learner release.
-- Each distractor mapping is attached directly to its authored wrong option; correct answers are inserted unchanged.

CREATE TEMP TABLE trap_practice_targets (
    seed_no INT PRIMARY KEY,
    error_code TEXT NOT NULL,
    set_no INT NOT NULL,
    parent_code TEXT NOT NULL,
    package_title TEXT NOT NULL,
    skill TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO trap_practice_targets VALUES
(101, 'DETAIL_AS_MAIN_IDEA', 1, 'DR_IDEA_OR_DETAIL', 'Finding the main point in a short passage', 'READING'),
(102, 'DETAIL_AS_MAIN_IDEA', 2, 'DR_IDEA_OR_DETAIL', 'Separating a main point from a supporting fact', 'READING'),
(103, 'DISTRACTOR_CORRECTED', 1, 'L65_CORRECTION', 'Listening for a corrected detail', 'LISTENING'),
(104, 'DISTRACTOR_CORRECTED', 2, 'L65_CORRECTION', 'Following a change in a conversation', 'LISTENING'),
(105, 'INFERENCE_UNSUPPORTED', 2, 'R65_EVIDENCE_INFERENCE', 'Keeping an inference within the evidence', 'READING'),
(106, 'KEYWORD_MATCH_ONLY', 1, 'TR_R_SCANNING', 'Checking how a repeated keyword is used', 'READING'),
(107, 'KEYWORD_MATCH_ONLY', 2, 'TR_R_SCANNING', 'Matching meaning rather than a familiar word', 'READING'),
(108, 'LINKER_MISUSE', 1, 'DEMO_WRITING_LINKING', 'Choosing a connector that matches the logic', 'WRITING'),
(109, 'LINKER_MISUSE', 2, 'DEMO_WRITING_LINKING', 'Using contrast and cause accurately', 'WRITING'),
(110, 'PARAPHRASE_TRAP', 1, 'R65_PARAPHRASE', 'Checking whether a paraphrase preserves meaning', 'READING'),
(111, 'PARAPHRASE_TRAP', 2, 'R65_PARAPHRASE', 'Spotting a change in degree or scope', 'READING'),
(112, 'TFNG_FALSE_AS_NG', 1, 'TFNG_FALSE_VS_NOT_GIVEN', 'Distinguishing a contradiction from missing information', 'READING'),
(113, 'TFNG_FALSE_AS_NG', 2, 'TFNG_FALSE_VS_NOT_GIVEN', 'Testing statements against the passage', 'READING'),
(114, 'TFNG_NG_AS_FALSE', 1, 'TFNG_FALSE_VS_NOT_GIVEN', 'Avoiding assumptions in Not Given items', 'READING'),
(115, 'TFNG_NG_AS_FALSE', 2, 'TFNG_FALSE_VS_NOT_GIVEN', 'Separating an unstated detail from a false one', 'READING'),
(116, 'WRONG_PARAGRAPH', 1, 'PR_MATCHING_INFORMATION', 'Locating a detail in the right paragraph', 'READING'),
(117, 'WRONG_PARAGRAPH', 2, 'PR_MATCHING_INFORMATION', 'Matching information by meaning and location', 'READING'),
(118, 'WRONG_SPEAKER', 1, 'L65_SPEAKER_PURPOSE', 'Identifying who holds a view', 'LISTENING'),
(119, 'WRONG_SPEAKER', 2, 'L65_SPEAKER_PURPOSE', 'Tracking a speaker’s purpose', 'LISTENING');

CREATE TEMP TABLE trap_practice_items (
    seed_no INT NOT NULL,
    question_no INT NOT NULL,
    question_type TEXT NOT NULL,
    stem TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    correct_key TEXT NOT NULL,
    trap_key TEXT,
    explanation TEXT NOT NULL,
    PRIMARY KEY (seed_no, question_no)
) ON COMMIT DROP;

INSERT INTO trap_practice_items VALUES
(101,1,'MULTIPLE_CHOICE','A report describes repaired crossings, two new cycle lanes and a later rise in bicycle commuting. Which sentence best summarizes it?','Street improvements were followed by more cycling.','One crossing reopened in April.','The city has several busy roads.','A','B','The report connects several transport changes with a broader rise in cycling. April is only one detail.'),
(101,2,'MULTIPLE_CHOICE','A passage says rooftop gardens cool buildings, give residents shared space and provide food for insects. What is its central idea?','Some gardens attract insects.','Rooftop gardens can benefit urban buildings and residents in several ways.','Residents often visit public parks.','B','C','The passage gives several benefits of rooftop gardens. Insects are one supporting example, not the whole point.'),
(101,3,'MULTIPLE_CHOICE','A museum introduced timed entry and reported shorter queues during weekends. Which result is explicitly stated?','Visitors spent twice as long inside.','The museum added a new gallery.','Weekend queues became shorter.','C',NULL,'The report states that weekend queues shortened; it does not mention visit length or a gallery.'),
(101,4,'MULTIPLE_CHOICE','A town planted shade trees near schools and added benches along walking routes. What did the follow-up survey measure?','Residents’ comfort when using outdoor spaces.','The height of every tree.','The number of lessons taught at school.','A',NULL,'The survey asked about comfort outdoors. Tree height and school lessons are not reported.'),
(102,1,'MULTIPLE_CHOICE','A river project restored marshes, reopened a footpath and recorded more fish in two monitoring areas. Which option gives the main point?','The footpath reopened in early spring.','Restoring the river habitat was followed by several local improvements.','Every river now has more fish.','B','C','The report summarizes several changes around one restoration project. The fish count is one result, and the claim about every river is unsupported.'),
(102,2,'MULTIPLE_CHOICE','A passage describes a library that lends tools, runs repair workshops and lets neighbours share equipment. What is it mainly about?','One workshop takes place on Saturdays.','The library has a large storage room.','A shared tool library supports practical community use.','C','A','The passage presents the library’s wider community role. Saturday is a single scheduling detail.'),
(102,3,'MULTIPLE_CHOICE','A school changed its lunch menu and later found that more pupils chose vegetables. What changed after the menu update?','More pupils chose vegetables.','The school extended the lunch break.','The cafeteria moved upstairs.','A',NULL,'The reported outcome is an increase in vegetable choices.'),
(102,4,'MULTIPLE_CHOICE','A coastal survey compared walking paths before and after new signs were installed. What was it designed to measure?','The colour of the signs.','How easily walkers could follow the routes.','The number of cars in the town centre.','B',NULL,'The survey concerns route-finding. It does not measure sign colour or town-centre traffic.'),
(103,1,'MULTIPLE_CHOICE','A caller first books a workshop for Tuesday, checks the confirmation and says it is Thursday. Which day is booked?','Thursday','Tuesday','Saturday','A','B','The caller corrects the earlier Tuesday detail. Thursday is the confirmed day.'),
(103,2,'MULTIPLE_CHOICE','A station worker says, “The train was due on platform 4, but it has been moved to platform 6.” Where should passengers wait?','Platform 4','Platform 6','Platform 8','B','A','Platform 4 is replaced by the later update, platform 6.'),
(103,3,'MULTIPLE_CHOICE','A student planned to print a map, but the printer is out of order. Her partner asks her to bring the saved digital map. What should she bring?','A notebook','A paper timetable','The digital map','C',NULL,'The partner explicitly asks for the saved digital map.'),
(103,4,'MULTIPLE_CHOICE','An email first lists room 12. A follow-up message says the meeting has moved to room 18. Which room should attendees use?','Room 18','Room 12','Room 20','A',NULL,'The follow-up message replaces the original room number.'),
(104,1,'MULTIPLE_CHOICE','The tour notice originally gave a 9:15 start. The guide later asks everyone to arrive for 9:25. What is the updated start time?','9:15','9:25','9:35','B','A','The later message changes the start from 9:15 to 9:25.'),
(104,2,'MULTIPLE_CHOICE','A driver says the airport coach used to stop at Gate C, but today it stops outside Gate D. Where is today’s stop?','Gate C','Gate D','Gate B','C','B','Gate C is the earlier arrangement. The driver gives Gate D as today’s stop.'),
(104,3,'MULTIPLE_CHOICE','A colleague offers printed forms, learns the office has run out of paper and agrees to send the forms by email. How will they arrive?','By email','As printed copies','By post','A',NULL,'The colleague changes to email after learning paper is unavailable.'),
(104,4,'MULTIPLE_CHOICE','A booking was first made for 14 June. A confirmation call corrects the date to 16 June. Which date should the guest keep free?','14 June','16 June','18 June','B',NULL,'The call corrects the booking date to 16 June.'),
(105,1,'MULTIPLE_CHOICE','In a four-clinic trial, patients sent reminder texts missed fewer appointments. Which conclusion is supported?','Reminder texts prevent every missed appointment.','The texts were the only cause of attendance changes.','In these clinics, fewer appointments were missed among patients sent reminders.','C','A','The result is limited to the clinics and patients observed. It does not promise a universal effect.'),
(105,2,'MULTIPLE_CHOICE','A survey found that customers visiting a market before 9 a.m. bought more fresh produce on average. What can be concluded?','Early visitors in this survey bought more fresh produce on average.','Arriving early makes every shopper buy more.','Late visitors dislike fresh food.','A','B','The survey reports an average difference, not a cause or a claim about every shopper.'),
(105,3,'MULTIPLE_CHOICE','A pilot on one street found lower evening noise after speed limits were reduced. Which statement matches the evidence?','Lower speed limits always remove traffic noise.','Evening noise fell on the street during the pilot.','Residents in every town prefer lower speed limits.','B',NULL,'The evidence covers one street and the pilot period only.'),
(105,4,'MULTIPLE_CHOICE','An archive recorded more visitors in the month after it extended its opening hours. What does the report establish?','Longer hours caused every new visit.','All archives would gain visitors by staying open later.','The archive recorded more visitors in that following month.','C',NULL,'The report gives a change over time but does not establish its cause or predict results elsewhere.');

INSERT INTO trap_practice_items VALUES
(106,1,'MULTIPLE_CHOICE','A building report calls a new roof panel “light” and says the old structure can support it. What does “light” mean here?','Low in weight','Pale in colour','Easy to read','A','B','The report concerns structural load, so “light” refers to weight, not colour.'),
(106,2,'MULTIPLE_CHOICE','A visitor guide asks people to “address” the coordinator at the desk if they need help. What should visitors do?','Write down the desk location.','Speak to the coordinator.','Prepare a speech for the group.','B','A','In this instruction, “address” means speak to someone. The location meaning does not fit.'),
(106,3,'MULTIPLE_CHOICE','The bridge “links” the island to the mainland. What does “links” mean?','Ranks the two places.','Publishes their names.','Connects them.','C',NULL,'A bridge connects two places.'),
(106,4,'MULTIPLE_CHOICE','A notice says registration is “limited to 40 people”. What does “limited” mean?','Capped at that number.','Explained in a long notice.','Open to any number.','A',NULL,'The notice sets a maximum of 40 registrations.'),
(107,1,'MULTIPLE_CHOICE','The article reports a “sharp” rise in prices over one month. What does “sharp” describe?','A pointed object.','A sudden, large increase.','A carefully written report.','B','A','The word describes the size and speed of the increase, not a physical point.'),
(107,2,'MULTIPLE_CHOICE','A trail map says the northern path “runs” beside the canal. What does “runs” mean?','Operates on a timetable.','Extends along the canal.','Moves quickly.','C','B','The path extends beside the canal; the other meanings do not fit a map.'),
(107,3,'MULTIPLE_CHOICE','A report says a new filter “removed” most large particles from the water. What did it do?','Took most large particles out.','Moved the particles to another room.','Measured the water temperature.','A',NULL,'Here “removed” means took particles out of the water.'),
(107,4,'MULTIPLE_CHOICE','A notice says the west entrance is “available” during construction. What does this tell visitors?','It is under repair and closed.','They can use it.','It is reserved for staff only.','B',NULL,'“Available” means visitors can use the entrance in the stated period.'),
(108,1,'MULTIPLE_CHOICE','Choose the connector: The town added a direct bus; ______, fewer commuters used their cars downtown.','for instance','by contrast','as a result','C','B','The lower car use is presented as an outcome of the bus change; “by contrast” signals opposition.'),
(108,2,'MULTIPLE_CHOICE','Choose the connector: The sample was small; ______, the researchers described their result as preliminary.','therefore','nevertheless','similarly','A','C','A small sample is the reason for treating the result cautiously.'),
(108,3,'MULTIPLE_CHOICE','Choose the connector: The southern path is shorter. ______, it has no lighting after dusk.','As a result','In addition','However','C',NULL,'The lack of lighting contrasts with the advantage of a shorter route.'),
(108,4,'MULTIPLE_CHOICE','Choose the connector: The team repeated the test ______ the first readings varied widely.','because','although','meanwhile','A',NULL,'The variation in the first readings explains why the test was repeated.'),
(109,1,'MULTIPLE_CHOICE','Choose the connector: The new timetable added an early train; ______, some staff no longer needed to drive.','in contrast','as a result','for example','B','A','The change in travel behaviour is an outcome of the earlier train.'),
(109,2,'MULTIPLE_CHOICE','Choose the connector: The route is longer; ______, it avoids the steep hill.','for example','however','therefore','C','B','Avoiding the hill contrasts with the route’s greater length.'),
(109,3,'MULTIPLE_CHOICE','The first survey had missing pages. ______, the team repeated it with a shorter form.','Therefore','Nevertheless','Likewise','A',NULL,'The missing pages explain the decision to repeat the survey.'),
(109,4,'MULTIPLE_CHOICE','Choose the connector: The building is old, ______ its heating system was replaced last year.','because','but','for example','B',NULL,'The new heating system contrasts with the building’s age.'),
(110,1,'MULTIPLE_CHOICE','The passage says “a small number of shops stayed open late”. Which sentence keeps the quantity?','Only a few shops extended their opening hours.','Every shop stayed open later.','Most shops closed before sunset.','A','B','“A small number” means a few, not every shop.'),
(110,2,'MULTIPLE_CHOICE','The report says repairs “may reduce” water loss. Which paraphrase keeps the same certainty?','Repairs have stopped every leak.','Repairs could reduce the amount of water lost.','Repairs will eliminate all water loss.','B','C','“May” expresses possibility; it does not promise a complete result.'),
(110,3,'MULTIPLE_CHOICE','A notice says the pool is closed on Mondays. What does this mean?','The pool opens only on Mondays.','The pool is closed every day.','Visitors cannot use it on Monday.','C',NULL,'The notice specifies one day of closure.'),
(110,4,'MULTIPLE_CHOICE','The station is “within walking distance” of the hotel. What does that mean?','A person can walk between them.','A bus is required for the trip.','The station is inside the hotel.','A',NULL,'The phrase means the station is close enough to reach on foot.'),
(111,1,'MULTIPLE_CHOICE','The notice says “most of the rooms were renovated”. Which paraphrase preserves the scope?','Every room was renovated.','More than half, but not necessarily all, were renovated.','Only one room was renovated.','B','A','“Most” means a majority, not every room.'),
(111,2,'MULTIPLE_CHOICE','The forecast says rain is “unlikely before noon”. Which sentence keeps that meaning?','Rain is possible, but not expected before midday.','Rain is certain before midday.','Rain will not fall at any time.','A','C','“Unlikely” is not the same as impossible for the whole day.'),
(111,3,'MULTIPLE_CHOICE','A guide says the gallery is open “throughout August”. When can visitors go?','Only on the last day of August.','During August.','Only before August begins.','B',NULL,'“Throughout” means during the whole stated period.'),
(111,4,'MULTIPLE_CHOICE','The report says costs “rose slightly”. What happened?','Costs stayed exactly the same.','Costs fell by a large amount.','Costs increased by a small amount.','C',NULL,'“Rose” means increased and “slightly” describes a small change.');

INSERT INTO trap_practice_items VALUES
(112,1,'TRUE_FALSE_NOT_GIVEN','Passage: The ferry from Riverton leaves at 08:30 and 10:00. Statement: The first ferry leaves at 09:00.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives 08:30 as the first departure, directly contradicting 09:00.'),
(112,2,'TRUE_FALSE_NOT_GIVEN','Passage: The public garden opened in 2016. Statement: The garden first opened in 2019.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The stated year is 2016, so the claim about 2019 is contradicted.'),
(112,3,'TRUE_FALSE_NOT_GIVEN','Passage: The reading room is open from 09:00 to 17:00. Statement: The reading room is open in the afternoon.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The stated hours include the afternoon, so the statement agrees with the passage.'),
(112,4,'TRUE_FALSE_NOT_GIVEN','Passage: The footbridge is 24 metres long. Statement: The footbridge is 30 metres long.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage gives a different length, so 30 metres is contradicted.'),
(113,1,'TRUE_FALSE_NOT_GIVEN','Passage: The coastal shuttle leaves Meadowfield at 07:40 and 09:20. Statement: Its first departure is at 08:00.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage states 07:40 as the first departure, which contradicts 08:00.'),
(113,2,'TRUE_FALSE_NOT_GIVEN','Passage: The science centre began admitting visitors in 2015. Statement: It first admitted visitors in 2017.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives 2015, so the statement’s date is false.'),
(113,3,'TRUE_FALSE_NOT_GIVEN','Passage: The archive opens at 10:00 on Saturdays. Statement: The archive opens on Saturday morning.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'10:00 is in the morning, so the statement is supported.'),
(113,4,'TRUE_FALSE_NOT_GIVEN','Passage: The cycling route is 12 kilometres long. Statement: The route is 15 kilometres long.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The stated distance contradicts the 15-kilometre claim.'),
(114,1,'TRUE_FALSE_NOT_GIVEN','Passage: The museum opens at 10:00 and closes at 17:00. Statement: The museum café serves hot meals.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage gives opening hours but says nothing about café food.'),
(114,2,'TRUE_FALSE_NOT_GIVEN','Passage: The forest path is 4 kilometres long and passes through woodland. Statement: Dogs are allowed on the path.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage describes the route but gives no rule about dogs.'),
(114,3,'TRUE_FALSE_NOT_GIVEN','Passage: The visitor centre is open every day except Tuesday. Statement: It is open on Monday.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'Monday is not the stated closure day, so the passage supports the statement.'),
(114,4,'TRUE_FALSE_NOT_GIVEN','Passage: The footpath is closed from November to February. Statement: The footpath is open in December.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'December falls within the stated closure period, so the statement is contradicted.'),
(115,1,'TRUE_FALSE_NOT_GIVEN','Passage: The harbour boat leaves at 11:15 and 13:45. Statement: The boat has a cafe for passengers.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage gives departure times but says nothing about passenger facilities.'),
(115,2,'TRUE_FALSE_NOT_GIVEN','Passage: The community hall opened in 2012. Statement: The hall offers dance classes every week.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage gives the opening year but does not mention classes.'),
(115,3,'TRUE_FALSE_NOT_GIVEN','Passage: The garden centre closes at 18:00 on Fridays. Statement: It is open on Friday afternoon.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The stated closing time means the centre is open during Friday afternoon.'),
(115,4,'TRUE_FALSE_NOT_GIVEN','Passage: The island trail is 7 kilometres long. Statement: The trail is 9 kilometres long.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage gives a different length, so 9 kilometres is false.'),
(116,1,'MULTIPLE_CHOICE','Paragraph A introduces a recycling scheme. Paragraph B says glass is collected on Fridays. Which paragraph gives the collection day?','Paragraph A','Paragraph B','Neither paragraph','B','C','The schedule appears in Paragraph B, not in the introduction.'),
(116,2,'MULTIPLE_CHOICE','Paragraph A describes a station’s history. Paragraph B explains that solar panels supply most daytime electricity. Where is the energy detail?','Both paragraphs','Paragraph A','Paragraph B','C','A','The energy information is in Paragraph B; Paragraph A concerns history.'),
(116,3,'MULTIPLE_CHOICE','Paragraph A explains why a bridge was built. Paragraph B gives its opening year as 2005. Which paragraph states the year?','Paragraph B','Paragraph A','The passage gives no year.','A',NULL,'The opening year is stated in Paragraph B.'),
(116,4,'MULTIPLE_CHOICE','Paragraph A describes a market layout. Paragraph B says most shoppers arrive before noon. Where is the visitor-time information?','Paragraph A','Paragraph B','It is not mentioned.','B',NULL,'The time pattern is in Paragraph B.'),
(117,1,'MULTIPLE_CHOICE','Paragraph A explains how a clinic was founded. Paragraph B says appointments can be booked online after 18:00. Which paragraph gives the booking detail?','Paragraph A','Paragraph B','Neither paragraph','B','C','Online booking hours are described in Paragraph B.'),
(117,2,'MULTIPLE_CHOICE','Paragraph A introduces a new ferry route. Paragraph B reports that bicycles are carried free of charge. Where is the bicycle policy?','Both paragraphs','Paragraph A','Paragraph B','C','A','The bicycle policy is in Paragraph B; Paragraph A only introduces the route.'),
(117,3,'MULTIPLE_CHOICE','Paragraph A describes a wetland’s wildlife. Paragraph B says a boardwalk was completed in 2021. Which paragraph gives the completion year?','Paragraph B','Paragraph A','No year is given.','A',NULL,'The completion year appears in Paragraph B.'),
(117,4,'MULTIPLE_CHOICE','Paragraph A outlines a school garden project. Paragraph B says pupils water the plants twice a week. Where is the routine described?','Paragraph A','Paragraph B','It is not mentioned.','B',NULL,'The watering routine appears in Paragraph B.'),
(118,1,'MULTIPLE_CHOICE','Mia argues that the old hall should become a library. Arun says it should remain a sports centre. Who supports the library plan?','Mia','Arun','Both speakers','A','B','Mia proposes the library; Arun gives the opposing view.'),
(118,2,'MULTIPLE_CHOICE','A guide announces that the east path is closed. A ranger suggests using the west path instead. Who gives the alternative route?','The guide','The visitors','The ranger','C','A','The ranger suggests the west path; the guide only announces the closure.'),
(118,3,'MULTIPLE_CHOICE','A caller asks whether the office is open on Saturday. The receptionist says it closes at noon. Who gives the hours?','The caller','The receptionist','A delivery driver','B',NULL,'The receptionist answers with the closing time.'),
(118,4,'MULTIPLE_CHOICE','A lecturer presents survey results. A student asks whether rural households were included. Who asks about the sample?','The student','The lecturer','The survey team','A',NULL,'The student asks the question while the lecturer presents the results.'),
(119,1,'MULTIPLE_CHOICE','Nadia recommends extending the evening bus service. Pavel says the budget should go to weekend trains. Who recommends evening buses?','Pavel','Nadia','Both speakers','B','A','Nadia makes the evening-bus recommendation; Pavel suggests a different use of the budget.'),
(119,2,'MULTIPLE_CHOICE','A host says the doors open at six. A technician adds that late arrivals should use the side entrance. Who gives the entrance instruction?','Late arrivals','The host','The technician','C','B','The technician gives the side-entrance instruction; the host gives the opening time.'),
(119,3,'MULTIPLE_CHOICE','A customer asks when the shop closes. The manager replies, “At seven this evening.” Who gives the closing time?','The customer','The manager','A neighbour','B',NULL,'The manager answers the customer’s question.'),
(119,4,'MULTIPLE_CHOICE','A researcher explains the survey method. An interviewer asks whether participants were contacted by phone. Who asks about the contact method?','The interviewer','The researcher','The participants','A',NULL,'The interviewer asks the question while the researcher explains the method.');
CREATE TEMP TABLE trap_seed_packages ON COMMIT DROP AS
SELECT t.*,
       ('81000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS package_id,
       ('82000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS package_version_id,
       ('83000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS section_id
FROM trap_practice_targets t;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key)
SELECT package_id, 'PS-TRAP-' || error_code || '-' || set_no,
       'Draft practice: ' || package_title, 'PRACTICE_SET', 'PUBLISHED', NULL
FROM trap_seed_packages;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
SELECT package_version_id, package_id, 1, 'PUBLISHED', '{}'::jsonb, 1, CURRENT_TIMESTAMP
FROM trap_seed_packages;

UPDATE content_packages p
SET current_published_version_id = t.package_version_id
FROM trap_seed_packages t
WHERE p.id = t.package_id;

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, instructions)
SELECT section_id, package_version_id, package_title, skill, 1,
       'Choose the answer supported by the passage or conversation. Review the explanation after each question.'
FROM trap_seed_packages;

CREATE TEMP TABLE trap_seed_questions ON COMMIT DROP AS
SELECT p.seed_no, p.error_code, p.parent_code, p.skill, p.package_id, p.section_id,
       i.question_no, i.question_type, i.stem, i.option_a, i.option_b, i.option_c,
       i.correct_key, i.trap_key, i.explanation,
       ('84000000-0000-4000-8000-' || lpad((p.seed_no * 10 + i.question_no)::text, 12, '0'))::uuid AS question_id,
       ('85000000-0000-4000-8000-' || lpad((p.seed_no * 10 + i.question_no)::text, 12, '0'))::uuid AS question_version_id,
       ('86000000-0000-4000-8000-' || lpad((p.seed_no * 10 + i.question_no)::text, 12, '0'))::uuid AS section_question_id
FROM trap_seed_packages p
JOIN trap_practice_items i ON i.seed_no = p.seed_no;

INSERT INTO questions (id, question_type, skill, purpose, status)
SELECT question_id, question_type, skill, 'LEARNING', 'PUBLISHED'
FROM trap_seed_questions;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, difficulty, status)
SELECT question_version_id, question_id, 1,
       stem,
       CASE WHEN question_type = 'TRUE_FALSE_NOT_GIVEN' THEN jsonb_build_array(
           jsonb_build_object('optionKey', 'TRUE', 'content', option_a, 'sortOrder', 1),
           jsonb_build_object('optionKey', 'FALSE', 'content', option_b, 'sortOrder', 2),
           jsonb_build_object('optionKey', 'NOT_GIVEN', 'content', option_c, 'sortOrder', 3))
       ELSE jsonb_build_array(
           jsonb_build_object('optionKey', 'A', 'content', option_a, 'sortOrder', 1),
           jsonb_build_object('optionKey', 'B', 'content', option_b, 'sortOrder', 2),
           jsonb_build_object('optionKey', 'C', 'content', option_c, 'sortOrder', 3)) END,
       jsonb_build_object('type', 'CHOICE', 'correct', correct_key) ||
           CASE WHEN trap_key IS NULL THEN '{}'::jsonb
                ELSE jsonb_build_object('distractors', jsonb_build_object(trap_key, error_code)) END,
       1, explanation, 'MEDIUM', 'PUBLISHED'
FROM trap_seed_questions;

UPDATE questions q
SET current_published_version_id = s.question_version_id
FROM trap_seed_questions s
WHERE q.id = s.question_id;

INSERT INTO question_knowledge_points (question_version_id, knowledge_point_id, weight)
SELECT s.question_version_id, parent.id, 1.00
FROM trap_seed_questions s
JOIN knowledge_points parent ON parent.code = s.parent_code;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score)
SELECT section_question_id, section_id, question_version_id, question_no, 1.00
FROM trap_seed_questions;
