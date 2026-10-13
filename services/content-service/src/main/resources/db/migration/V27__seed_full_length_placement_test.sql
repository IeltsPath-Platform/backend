-- Placement becomes a full-length IELTS test as version 2 of PLACEMENT-4SKILLS, in the order of the real test:
-- Listening Parts 1-4 (40 questions), Reading Passages 1-3 (40 questions), Writing Task 1 and Task 2, Speaking Parts
-- 1-3. Each part is its own section, so it carries one audio or one passage. Writing Task 1 with its table and the
-- Part 2 cue card are reused from version 1 of the same package; Task 2 is reissued with the real 250-word minimum.
-- IELTS question groups (headings, matching, YES/NO/NOT GIVEN) are seeded as one choice question per item; each
-- group's instructions sit on its section. Listening audio keys listening/placement/part1-4.mp3 must be uploaded to
-- the media bucket. Attempts started on version 1 keep the structure they snapshotted.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2c000000-0000-4000-8000-050000000001', 'AUDIO', 'Receptionist: Good afternoon, Greenfield Sports Centre. How can I help you?
Caller: Hello. I''d like to enrol my son in one of your swimming courses.
Receptionist: Certainly. I just need a few details. What''s your son''s name?
Caller: Daniel Harper. That''s H, A, R, P, E, R.
Receptionist: Thank you. And how old is Daniel?
Caller: He''s nine. He''ll be ten in August.
Receptionist: Then he''d join a junior group. Has he had lessons before?
Caller: Yes, for about a year, so he can swim, but he isn''t very confident yet.
Receptionist: In that case I''d suggest the intermediate level rather than beginners. Those classes are on Tuesdays and Thursdays. Tuesday is full, I''m afraid, but there are still places on Thursday.
Caller: Thursday''s fine. What time does it start?
Receptionist: At a quarter past four, and each lesson lasts forty-five minutes.
Caller: Good, that''s just after school. How much is the course?
Receptionist: A ten-week course costs sixty-five pounds. Members pay fifty-two.
Caller: We''re not members, so sixty-five, then.
Receptionist: Fine. Could I have your address?
Caller: It''s 27 Station Road, and the postcode is BR6 4TJ.
Receptionist: BR6 4TJ. Thank you. Daniel should bring his own goggles, and every child must wear a swimming cap.
Caller: He has goggles, but not a cap.
Receptionist: You can buy one at reception for three pounds. The first lesson is on the twelfth of September. Please arrive ten minutes early so that he can meet the teacher.',
     'listening/placement/part1.mp3', 150, 'VALID'),
    ('2c000000-0000-4000-8000-050000000002', 'AUDIO', 'Guide: Good morning, everyone, and welcome to Westmoor Nature Reserve. My name''s Sam and I''ll be your guide today.
The reserve opened in 1998 on land that used to be a sand quarry. When the digging stopped, the pits filled with water, and today those lakes are home to more than two hundred species of birds. Many visitors assume the reserve is run by the local council, but in fact it is managed by a charity, and most of the work is done by volunteers.
We''ll start with the Lake Trail, which takes about an hour. The path is flat, but after the recent rain parts of it are very muddy, so please stay on the wooden boardwalk where you can. At the end of the trail there''s a bird hide, a small hut where you can watch the birds without disturbing them. Please keep completely silent inside, as even quiet talking can frighten the birds away.
After that we''ll stop at the visitor centre. The café serves hot drinks and sandwiches, and the shop has just started selling detailed maps of the reserve, which show far more than the free leaflet.
In the afternoon you can choose between two activities. The first is pond dipping, where you use nets to catch and study small water creatures. It''s free and very popular with families. The second is a guided photography walk. That costs five pounds and is limited to twelve people, so if you''d like to join, please add your name to the list at the visitor centre before lunch.
Finally, a few rules. Dogs are not allowed anywhere on the reserve, except guide dogs. There are no bins on the trails, so please take your litter home. We removed the bins because they attracted foxes, which then disturbed nesting birds. And the gates close at half past five.',
     'listening/placement/part2.mp3', 180, 'VALID'),
    ('2c000000-0000-4000-8000-050000000003', 'AUDIO', 'Tutor: So, Maya and Tom, how''s your project on food waste in the university canteen going?
Maya: Quite well. We''ve finished collecting the data. We weighed the food students left on their plates every lunchtime for two weeks.
Tutor: Two weeks? I suggested a month.
Tom: We know, but the canteen manager only agreed to two weeks, because weighing the plates slowed his staff down.
Tutor: Fair enough. What did you find?
Maya: The biggest surprise was vegetables. We expected bread to be wasted most, but vegetables made up nearly forty per cent of what was thrown away.
Tom: And waste was much higher on Fridays. We think that''s because portions are bigger at the end of the week, when the kitchen uses up what''s left.
Tutor: Did you ask students why they leave food?
Tom: Yes, in a short questionnaire. Most said the portions were simply too large. Only a few said they didn''t like the taste.
Tutor: So what will you recommend?
Maya: Mainly that students can choose a smaller portion at a lower price. The manager liked that, as it wouldn''t cost the canteen anything.
Tutor: Good. The presentation is in three weeks. How are you dividing the work?
Maya: I''ll design the slides, because I''ve done that before.
Tom: And I''ll write the report, since I did most of the statistics.
Tutor: Who''s going to present the results to the canteen manager?
Tom: We''ll both go. It''s better if he hears it from both of us.
Maya: And I''ll make the poster for the open day.
Tutor: One last thing: make sure you mention the limitations of your study, especially the short time period.
Maya: Yes, we''ll put that in the conclusion.',
     'listening/placement/part3.mp3', 170, 'VALID'),
    ('2c000000-0000-4000-8000-050000000004', 'AUDIO', 'Lecturer: Today I''m going to talk about the early history of the bicycle, and how a machine that began as a toy for the rich became an everyday form of transport.
The first machine is usually said to be the running machine, built in Germany in 1817 by Karl von Drais. It had two wheels but no pedals: the rider sat on it and pushed along the ground with his feet. It was made almost entirely of wood, and it was seen mainly as a fashionable toy.
Pedals arrived in France in the 1860s, fixed directly to the front wheel. These machines were nicknamed boneshakers, because their iron tyres made the ride on stony roads extremely uncomfortable.
To go faster, makers increased the size of the front wheel, which produced the penny-farthing of the 1870s. Its front wheel could be over one and a half metres high. It was fast but dangerous: if the wheel hit a stone, the rider could be thrown over the handlebars. As a result, it was ridden mostly by young men.
The real turning point came in 1885 with the safety bicycle, designed in England by John Kemp Starley. It had two wheels of the same size and, crucially, a chain connecting the pedals to the back wheel. Three years later, the air-filled tyre, patented by John Dunlop, made cycling far more comfortable.
The social effects were enormous. Because bicycles became cheap enough for working people, many could now live further from their workplace. Cycling also gave women a new kind of independence and even changed fashion, as long skirts gave way to more practical clothing. Finally, cyclists'' clubs campaigned for better roads, which were later used by cars.',
     'listening/placement/part4.mp3', 190, 'VALID');

INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('2c000000-0000-4000-8000-050000000005', 'PASSAGE', 'Bees in the City

A few decades ago, keeping bees was seen as an activity for the countryside. Today, hives can be found on the roofs of office blocks, hotels and schools in many large cities. In one European capital, the number of registered hives roughly doubled in just five years, and beekeeping courses there are often fully booked months in advance.

Several factors explain this growth. Many people became worried about reports that honeybee populations were falling in Europe and North America, and they wanted to do something to help. Businesses also saw an opportunity: a hive on the roof was a visible way of showing concern for the environment, and the honey could be given to clients as a gift. In addition, cities turned out to be surprisingly good places for bees. Parks, private gardens, railway banks and tree-lined streets offer a wide variety of flowers, and because towns are warmer than the surrounding countryside, the flowering season often lasts longer. Pesticides, which are widely used on farms, are also less common in urban areas.

However, not everyone is convinced that urban beekeeping is good for nature. A study published in 2020 compared the number of hives in one city with the amount of food available to bees. The researchers concluded that in some central districts there were already too many hives for the available flowers. The problem is that honeybees compete for nectar and pollen with wild bees, of which Britain alone has more than 250 species. Unlike honeybees, many wild species live alone rather than in colonies and can only fly short distances, so if honeybees take most of the food nearby, wild bees may go hungry.

Experts therefore argue that people who want to help bees should focus on plants rather than hives. Growing flowers that bloom at different times of the year, leaving areas of grass uncut and avoiding chemical sprays in gardens can benefit all pollinators, not just honeybees. Some local councils have started to act on this advice, replacing the short grass beside main roads with wildflowers, which has also reduced the cost of cutting.

For those who still want to keep bees, training is essential. Beekeeping associations recommend a course lasting at least one season before buying a hive, as an inexperienced keeper can easily allow disease to spread to neighbouring colonies. A healthy hive also needs regular inspection, usually once a week between spring and late summer.', 'VALID'),
    ('2c000000-0000-4000-8000-050000000006', 'PASSAGE', 'The Return of the Night Train

A. For much of the twentieth century, overnight trains were a normal part of travel in Europe. Passengers could board in one capital in the evening, sleep in a bed or a reclining seat, and wake up in another country the next morning. From the 1990s, however, the network began to shrink. Low-cost airlines offered flights for less than the price of a sleeping compartment, high-speed trains made many daytime journeys much shorter, and by the middle of the 2010s even some of the best-known routes had been closed.

B. The economics of night trains explain much of this decline. A sleeping car carries far fewer passengers than an ordinary carriage, because beds take up more space than seats. Each train needs staff to work through the night, and the carriages, which are used only once every twenty-four hours, spend the day standing in stations. In addition, operators must pay a fee for every kilometre of track they use, and in some countries these charges are higher for international services.

C. Yet just as the network seemed to be disappearing, demand began to grow again. Austrian Federal Railways, which had taken over several routes abandoned by other operators, found that its night services were often fully booked. Much of the new interest came from travellers concerned about the environment. A journey by train generally produces a small fraction of the carbon emissions of the same trip by air, and some travellers began to avoid flying altogether.

D. Supporters also point to advantages that have nothing to do with the climate. A night train runs from city centre to city centre, so passengers avoid the trip to an airport outside town and the long wait at security. Because the journey takes place while they sleep, it also saves the cost of a hotel for one night. For business travellers, arriving at eight in the morning ready for a meeting can be more convenient than catching an early flight.

E. The revival has not been without problems. Many of the carriages in use are more than forty years old, and new ones are costly and take years to build. Passengers have complained about cancelled services, broken air conditioning and trains arriving hours late. Some new routes launched with great publicity were suspended after only a few months because they could not attract enough passengers outside the summer holidays.

F. Whether night trains have a long-term future may depend on governments. Several countries have begun to reduce track charges for international services, and the European Union has supported trial routes. Some experts argue that if the environmental cost of flying were included in the price of air tickets, trains could compete more easily. Others believe night trains will remain a niche product, attractive to a small group of travellers but unable to replace flying over long distances.', 'VALID'),
    ('2c000000-0000-4000-8000-050000000007', 'PASSAGE', 'The Case for Handwriting

A. In many schools, the keyboard has steadily replaced the pen. Some education systems have reduced the time spent teaching joined-up writing, arguing that children will spend their adult lives typing and that lessons would be better spent on other skills. At first sight, this seems a sensible response to technological change. Yet a growing body of research suggests that something valuable may be lost when children stop writing by hand.

B. Much of this research concerns the link between handwriting and reading. In one frequently cited experiment, young children who had not yet learnt to read were shown letters and asked either to copy them by hand, to type them or to trace them. Later, when the children were placed in a brain scanner and shown the same letters, the areas associated with reading were noticeably more active in those who had written the letters freehand. The researchers suggested that the effort of forming a letter, with all its small imperfections, helps the brain to recognise it.

C. Older students may also benefit. Studies comparing university students who took lecture notes on laptops with those who used pen and paper found that the laptop users tended to record far more words, often copying the lecturer almost word for word. The handwriters, unable to keep up, were forced to select and summarise, and in tests of conceptual understanding they generally performed better. It should be said that later attempts to repeat these findings have produced mixed results, and the difference between the groups was sometimes small. Even so, the idea that slower writing encourages deeper processing has proved influential.

D. I find this argument persuasive, but it is easy to draw the wrong conclusion from it. Nobody is seriously suggesting that students should give up computers. Typing is faster, easier to edit and, for many people with physical difficulties, the only practical way to write. The question is not whether to use the keyboard but whether young children should still be taught to write fluently by hand before they move on to it. On the evidence so far, I believe they should.

E. There is also a cultural argument, although it is weaker. Historians and archivists point out that letters and diaries written by hand reveal far more about their authors than typed text: the pressure of the pen, the crossings-out, even the choice of paper. If future generations cannot read joined-up writing, they may find such documents difficult to study. This is a genuine concern, but it is hardly a reason to require every child to write in a particular style; specialists can learn old scripts, just as they already learn to read medieval handwriting.

F. What, then, should schools do? The most sensible approach seems to be a balance: plenty of handwriting practice in the early years, when it appears to support reading, and a gradual introduction of typing as children get older. Teachers should also be careful not to treat neat handwriting as a sign of intelligence; legibility and fluency matter far more than beauty. Above all, decisions should be guided by evidence rather than by nostalgia or enthusiasm for the latest technology.', 'VALID');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2c000000-0000-4000-8000-060000000001', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000002', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000003', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000004', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000005', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000006', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000007', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000008', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000009', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000010', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000011', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000012', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000013', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000014', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000015', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000016', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000017', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000018', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000019', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000020', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000021', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000022', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000023', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000024', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000025', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000026', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000027', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000028', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000029', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000030', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000031', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000032', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000033', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000034', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000035', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000036', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000037', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000038', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000039', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000040', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000041', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000042', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000043', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000044', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000045', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000046', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000047', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000048', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000049', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000050', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000051', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000052', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000053', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000054', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000055', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000056', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000057', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000058', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000059', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000060', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000061', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000062', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000063', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000064', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000065', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000066', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000067', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000068', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000069', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000070', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000071', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000072', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000073', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000074', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000075', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000076', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000077', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000078', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000079', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000080', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000081', 'ESSAY', 'WRITING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000082', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000083', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000084', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000085', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000086', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000087', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-060000000088', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2c000000-0000-4000-8000-070000000001', '2c000000-0000-4000-8000-060000000001', 1, 'Child''s surname: ______', NULL,
     '{"type": "FILL", "accepted": ["harper"]}'::jsonb, 1, 'The caller spells H, A, R, P, E, R.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000002', '2c000000-0000-4000-8000-060000000002', 1, 'Child''s age: ______', NULL,
     '{"type": "FILL", "accepted": ["9", "nine"]}'::jsonb, 1, 'He''s nine; he will be ten in August.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000003', '2c000000-0000-4000-8000-060000000003', 1, 'Recommended level: ______', NULL,
     '{"type": "FILL", "accepted": ["intermediate"]}'::jsonb, 1, 'The receptionist suggests intermediate rather than beginners.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000004', '2c000000-0000-4000-8000-060000000004', 1, 'Day of the class: ______', NULL,
     '{"type": "FILL", "accepted": ["thursday", "thursdays"]}'::jsonb, 1, 'Tuesday is full; there are places on Thursday.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000005', '2c000000-0000-4000-8000-060000000005', 1, 'Start time: ______', NULL,
     '{"type": "FILL", "accepted": ["4.15", "4:15", "16.15", "16:15", "quarter past four", "a quarter past four"]}'::jsonb, 1, 'Classes start at a quarter past four.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000006', '2c000000-0000-4000-8000-060000000006', 1, 'Length of each lesson: ______ minutes', NULL,
     '{"type": "FILL", "accepted": ["45", "forty-five", "forty five"]}'::jsonb, 1, 'Each lesson lasts forty-five minutes.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000007', '2c000000-0000-4000-8000-060000000007', 1, 'Price of the course for non-members: £______', NULL,
     '{"type": "FILL", "accepted": ["65", "sixty-five", "sixty five"]}'::jsonb, 1, 'Members pay fifty-two; the caller is not a member, so sixty-five.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000008', '2c000000-0000-4000-8000-060000000008', 1, 'Postcode: ______', NULL,
     '{"type": "FILL", "accepted": ["br6 4tj", "br64tj"]}'::jsonb, 1, 'The postcode is BR6 4TJ.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000009', '2c000000-0000-4000-8000-060000000009', 1, 'Every child must wear a swimming ______.', NULL,
     '{"type": "FILL", "accepted": ["cap"]}'::jsonb, 1, 'Every child must wear a swimming cap.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000010', '2c000000-0000-4000-8000-060000000010', 1, 'Date of the first lesson: ______ September', NULL,
     '{"type": "FILL", "accepted": ["12", "12th", "twelfth", "the 12th", "the twelfth"]}'::jsonb, 1, 'The first lesson is on the twelfth of September.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000011', '2c000000-0000-4000-8000-060000000011', 1, 'What was the site before the reserve opened?', '[{"optionKey": "A", "content": "Farmland", "sortOrder": 1}, {"optionKey": "B", "content": "A sand quarry", "sortOrder": 2}, {"optionKey": "C", "content": "A forest", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'It opened on land that used to be a sand quarry.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000012', '2c000000-0000-4000-8000-060000000012', 1, 'Who manages the reserve?', '[{"optionKey": "A", "content": "The local council", "sortOrder": 1}, {"optionKey": "B", "content": "A private company", "sortOrder": 2}, {"optionKey": "C", "content": "A charity", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Visitors assume the council runs it, but it is managed by a charity.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000013', '2c000000-0000-4000-8000-060000000013', 1, 'Why should visitors stay on the boardwalk?', '[{"optionKey": "A", "content": "Parts of the path are muddy", "sortOrder": 1}, {"optionKey": "B", "content": "It is the shortest route", "sortOrder": 2}, {"optionKey": "C", "content": "Birds nest beside the path", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'After the recent rain parts of the path are very muddy.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000014', '2c000000-0000-4000-8000-060000000014', 1, 'What must visitors do inside the bird hide?', '[{"optionKey": "A", "content": "Avoid using a camera flash", "sortOrder": 1}, {"optionKey": "B", "content": "Stay completely silent", "sortOrder": 2}, {"optionKey": "C", "content": "Sit at the back", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Even quiet talking can frighten the birds away.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000015', '2c000000-0000-4000-8000-060000000015', 1, 'What is new in the visitor centre shop?', '[{"optionKey": "A", "content": "Detailed maps of the reserve", "sortOrder": 1}, {"optionKey": "B", "content": "Hot drinks and sandwiches", "sortOrder": 2}, {"optionKey": "C", "content": "Free leaflets", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'The shop has just started selling detailed maps.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000016', '2c000000-0000-4000-8000-060000000016', 1, 'How can people join the photography walk?', '[{"optionKey": "A", "content": "By paying at the gate", "sortOrder": 1}, {"optionKey": "B", "content": "By bringing their own camera", "sortOrder": 2}, {"optionKey": "C", "content": "By adding their name to a list before lunch", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Add your name to the list at the visitor centre before lunch.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000017', '2c000000-0000-4000-8000-060000000017', 1, 'Pond dipping: visitors use ______ to catch small water creatures.', NULL,
     '{"type": "FILL", "accepted": ["nets"]}'::jsonb, 1, 'You use nets to catch and study small water creatures.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000018', '2c000000-0000-4000-8000-060000000018', 1, 'Photography walk: costs £______', NULL,
     '{"type": "FILL", "accepted": ["5", "five"]}'::jsonb, 1, 'That costs five pounds.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000019', '2c000000-0000-4000-8000-060000000019', 1, 'The only dogs allowed are ______ dogs.', NULL,
     '{"type": "FILL", "accepted": ["guide"]}'::jsonb, 1, 'Dogs are not allowed, except guide dogs.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000020', '2c000000-0000-4000-8000-060000000020', 1, 'The bins were removed because they attracted ______.', NULL,
     '{"type": "FILL", "accepted": ["foxes"]}'::jsonb, 1, 'The bins attracted foxes, which disturbed nesting birds.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000021', '2c000000-0000-4000-8000-060000000021', 1, 'How long did the students collect data?', '[{"optionKey": "A", "content": "One week", "sortOrder": 1}, {"optionKey": "B", "content": "Two weeks", "sortOrder": 2}, {"optionKey": "C", "content": "One month", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'They weighed the plates every lunchtime for two weeks.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000022', '2c000000-0000-4000-8000-060000000022', 1, 'Why was the data collection shorter than the tutor suggested?', '[{"optionKey": "A", "content": "The canteen manager limited it", "sortOrder": 1}, {"optionKey": "B", "content": "The students ran out of time", "sortOrder": 2}, {"optionKey": "C", "content": "The tutor changed the plan", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'The manager only agreed to two weeks because it slowed his staff down.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000023', '2c000000-0000-4000-8000-060000000023', 1, 'Which food was wasted most?', '[{"optionKey": "A", "content": "Bread", "sortOrder": 1}, {"optionKey": "B", "content": "Meat", "sortOrder": 2}, {"optionKey": "C", "content": "Vegetables", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'They expected bread, but vegetables were nearly forty per cent of the waste.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000024', '2c000000-0000-4000-8000-060000000024', 1, 'Why do the students think more food is wasted on Fridays?', '[{"optionKey": "A", "content": "Fewer students eat in the canteen", "sortOrder": 1}, {"optionKey": "B", "content": "Portions are larger", "sortOrder": 2}, {"optionKey": "C", "content": "The menu is less popular", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Portions are bigger at the end of the week.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000025', '2c000000-0000-4000-8000-060000000025', 1, 'What reason did most students give for leaving food?', '[{"optionKey": "A", "content": "The portions were too large", "sortOrder": 1}, {"optionKey": "B", "content": "They disliked the taste", "sortOrder": 2}, {"optionKey": "C", "content": "They had little time to eat", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Most said the portions were simply too large; only a few mentioned taste.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000026', '2c000000-0000-4000-8000-060000000026', 1, 'Design the slides', '[{"optionKey": "A", "content": "Maya", "sortOrder": 1}, {"optionKey": "B", "content": "Tom", "sortOrder": 2}, {"optionKey": "C", "content": "Both", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Maya will design the slides.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000027', '2c000000-0000-4000-8000-060000000027', 1, 'Write the report', '[{"optionKey": "A", "content": "Maya", "sortOrder": 1}, {"optionKey": "B", "content": "Tom", "sortOrder": 2}, {"optionKey": "C", "content": "Both", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Tom will write the report.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000028', '2c000000-0000-4000-8000-060000000028', 1, 'Present the results to the canteen manager', '[{"optionKey": "A", "content": "Maya", "sortOrder": 1}, {"optionKey": "B", "content": "Tom", "sortOrder": 2}, {"optionKey": "C", "content": "Both", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Tom says they will both go.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000029', '2c000000-0000-4000-8000-060000000029', 1, 'Make the poster for the open day', '[{"optionKey": "A", "content": "Maya", "sortOrder": 1}, {"optionKey": "B", "content": "Tom", "sortOrder": 2}, {"optionKey": "C", "content": "Both", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "A"}'::jsonb, 1, 'Maya will make the poster.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000030', '2c000000-0000-4000-8000-060000000030', 1, 'What does the tutor ask the students to include?', '[{"optionKey": "A", "content": "More data", "sortOrder": 1}, {"optionKey": "B", "content": "The limitations of the study", "sortOrder": 2}, {"optionKey": "C", "content": "Photos of the canteen", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Mention the limitations, especially the short time period.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000031', '2c000000-0000-4000-8000-060000000031', 1, '1817: the first machine was built in ______.', NULL,
     '{"type": "FILL", "accepted": ["germany"]}'::jsonb, 1, 'Built in Germany in 1817 by Karl von Drais.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000032', '2c000000-0000-4000-8000-060000000032', 1, 'The running machine had no ______.', NULL,
     '{"type": "FILL", "accepted": ["pedals"]}'::jsonb, 1, 'It had two wheels but no pedals.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000033', '2c000000-0000-4000-8000-060000000033', 1, 'It was made almost entirely of ______.', NULL,
     '{"type": "FILL", "accepted": ["wood"]}'::jsonb, 1, 'It was made almost entirely of wood.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000034', '2c000000-0000-4000-8000-060000000034', 1, 'Boneshakers were uncomfortable because of their ______ tyres.', NULL,
     '{"type": "FILL", "accepted": ["iron"]}'::jsonb, 1, 'Their iron tyres made the ride uncomfortable.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000035', '2c000000-0000-4000-8000-060000000035', 1, 'The penny-farthing''s front wheel could be over ______ metres high.', NULL,
     '{"type": "FILL", "accepted": ["1.5", "one and a half", "1 1/2", "1½"]}'::jsonb, 1, 'Over one and a half metres high.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000036', '2c000000-0000-4000-8000-060000000036', 1, 'The penny-farthing was ridden mostly by young ______.', NULL,
     '{"type": "FILL", "accepted": ["men"]}'::jsonb, 1, 'It was ridden mostly by young men.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000037', '2c000000-0000-4000-8000-060000000037', 1, '1885: the safety bicycle used a ______ to drive the back wheel.', NULL,
     '{"type": "FILL", "accepted": ["chain"]}'::jsonb, 1, 'A chain connected the pedals to the back wheel.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000038', '2c000000-0000-4000-8000-060000000038', 1, 'The air-filled tyre was patented by John ______.', NULL,
     '{"type": "FILL", "accepted": ["dunlop"]}'::jsonb, 1, 'Patented by John Dunlop.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000039', '2c000000-0000-4000-8000-060000000039', 1, 'Cheap bicycles meant workers could live further from their ______.', NULL,
     '{"type": "FILL", "accepted": ["workplace", "workplaces"]}'::jsonb, 1, 'Many could now live further from their workplace.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000040', '2c000000-0000-4000-8000-060000000040', 1, 'Cyclists'' clubs campaigned for better ______.', NULL,
     '{"type": "FILL", "accepted": ["roads"]}'::jsonb, 1, 'Clubs campaigned for better roads, later used by cars.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000041', '2c000000-0000-4000-8000-060000000041', 1, 'Urban beekeeping has become more popular in recent decades.', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "TRUE"}'::jsonb, 1, 'Paragraph 1: hives are now found on city roofs; registered hives doubled in five years.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000042', '2c000000-0000-4000-8000-060000000042', 1, 'Some businesses give the honey from their hives to clients.', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "TRUE"}'::jsonb, 1, 'Paragraph 2: the honey could be given to clients as a gift.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000043', '2c000000-0000-4000-8000-060000000043', 1, 'The flowering season in towns is usually shorter than in the countryside.', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "FALSE"}'::jsonb, 1, 'Paragraph 2: towns are warmer, so the flowering season often lasts longer.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000044', '2c000000-0000-4000-8000-060000000044', 1, 'Pesticides are banned in most cities.', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "NOT_GIVEN"}'::jsonb, 1, 'Paragraph 2 only says pesticides are less common in urban areas; nothing about a ban.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000045', '2c000000-0000-4000-8000-060000000045', 1, 'The 2020 study found too many hives in every part of the city.', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "FALSE"}'::jsonb, 1, 'Paragraph 3: only in some central districts.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000046', '2c000000-0000-4000-8000-060000000046', 1, 'Honeybees and wild bees compete for the same food.', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "TRUE"}'::jsonb, 1, 'Paragraph 3: honeybees compete for nectar and pollen with wild bees.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000047', '2c000000-0000-4000-8000-060000000047', 1, 'All wild bee species can fly long distances.', '[{"optionKey": "TRUE", "content": "TRUE", "sortOrder": 1}, {"optionKey": "FALSE", "content": "FALSE", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "FALSE"}'::jsonb, 1, 'Paragraph 3: many wild species can only fly short distances.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000048', '2c000000-0000-4000-8000-060000000048', 1, 'Many people began keeping bees after reports that honeybee ______ were falling.', NULL,
     '{"type": "FILL", "accepted": ["populations"]}'::jsonb, 1, 'Paragraph 2: reports that honeybee populations were falling.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000049', '2c000000-0000-4000-8000-060000000049', 1, 'Towns are warmer than the surrounding ______.', NULL,
     '{"type": "FILL", "accepted": ["countryside"]}'::jsonb, 1, 'Paragraph 2: warmer than the surrounding countryside.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000050', '2c000000-0000-4000-8000-060000000050', 1, 'Experts say people who want to help bees should focus on ______ rather than hives.', NULL,
     '{"type": "FILL", "accepted": ["plants"]}'::jsonb, 1, 'Paragraph 4: focus on plants rather than hives.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000051', '2c000000-0000-4000-8000-060000000051', 1, 'Leaving areas of grass ______ benefits all pollinators.', NULL,
     '{"type": "FILL", "accepted": ["uncut"]}'::jsonb, 1, 'Paragraph 4: leaving areas of grass uncut.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000052', '2c000000-0000-4000-8000-060000000052', 1, 'Beginners should take a course lasting at least one ______.', NULL,
     '{"type": "FILL", "accepted": ["season"]}'::jsonb, 1, 'Paragraph 5: at least one season.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000053', '2c000000-0000-4000-8000-060000000053', 1, 'Between spring and late summer, a hive is usually inspected once a ______.', NULL,
     '{"type": "FILL", "accepted": ["week"]}'::jsonb, 1, 'Paragraph 5: usually once a week.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000054', '2c000000-0000-4000-8000-060000000054', 1, 'Paragraph A', '[{"optionKey": "i", "content": "A tradition that faded", "sortOrder": 1}, {"optionKey": "ii", "content": "Why the trains cost so much to run", "sortOrder": 2}, {"optionKey": "iii", "content": "Environmental concerns bring passengers back", "sortOrder": 3}, {"optionKey": "iv", "content": "Practical benefits for travellers", "sortOrder": 4}, {"optionKey": "v", "content": "Difficulties facing the revival", "sortOrder": 5}, {"optionKey": "vi", "content": "A future shaped by political decisions", "sortOrder": 6}, {"optionKey": "vii", "content": "The spread of high-speed rail", "sortOrder": 7}, {"optionKey": "viii", "content": "How sleeping cars are designed", "sortOrder": 8}]'::jsonb,
     '{"type": "CHOICE", "correct": "i"}'::jsonb, 1, 'Paragraph A describes how a once-normal service shrank: a tradition that faded.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000055', '2c000000-0000-4000-8000-060000000055', 1, 'Paragraph B', '[{"optionKey": "i", "content": "A tradition that faded", "sortOrder": 1}, {"optionKey": "ii", "content": "Why the trains cost so much to run", "sortOrder": 2}, {"optionKey": "iii", "content": "Environmental concerns bring passengers back", "sortOrder": 3}, {"optionKey": "iv", "content": "Practical benefits for travellers", "sortOrder": 4}, {"optionKey": "v", "content": "Difficulties facing the revival", "sortOrder": 5}, {"optionKey": "vi", "content": "A future shaped by political decisions", "sortOrder": 6}, {"optionKey": "vii", "content": "The spread of high-speed rail", "sortOrder": 7}, {"optionKey": "viii", "content": "How sleeping cars are designed", "sortOrder": 8}]'::jsonb,
     '{"type": "CHOICE", "correct": "ii"}'::jsonb, 1, 'Paragraph B lists the costs: fewer passengers per car, night staff, idle carriages, track fees.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000056', '2c000000-0000-4000-8000-060000000056', 1, 'Paragraph C', '[{"optionKey": "i", "content": "A tradition that faded", "sortOrder": 1}, {"optionKey": "ii", "content": "Why the trains cost so much to run", "sortOrder": 2}, {"optionKey": "iii", "content": "Environmental concerns bring passengers back", "sortOrder": 3}, {"optionKey": "iv", "content": "Practical benefits for travellers", "sortOrder": 4}, {"optionKey": "v", "content": "Difficulties facing the revival", "sortOrder": 5}, {"optionKey": "vi", "content": "A future shaped by political decisions", "sortOrder": 6}, {"optionKey": "vii", "content": "The spread of high-speed rail", "sortOrder": 7}, {"optionKey": "viii", "content": "How sleeping cars are designed", "sortOrder": 8}]'::jsonb,
     '{"type": "CHOICE", "correct": "iii"}'::jsonb, 1, 'Paragraph C: demand grew from travellers concerned about the environment.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000057', '2c000000-0000-4000-8000-060000000057', 1, 'Paragraph D', '[{"optionKey": "i", "content": "A tradition that faded", "sortOrder": 1}, {"optionKey": "ii", "content": "Why the trains cost so much to run", "sortOrder": 2}, {"optionKey": "iii", "content": "Environmental concerns bring passengers back", "sortOrder": 3}, {"optionKey": "iv", "content": "Practical benefits for travellers", "sortOrder": 4}, {"optionKey": "v", "content": "Difficulties facing the revival", "sortOrder": 5}, {"optionKey": "vi", "content": "A future shaped by political decisions", "sortOrder": 6}, {"optionKey": "vii", "content": "The spread of high-speed rail", "sortOrder": 7}, {"optionKey": "viii", "content": "How sleeping cars are designed", "sortOrder": 8}]'::jsonb,
     '{"type": "CHOICE", "correct": "iv"}'::jsonb, 1, 'Paragraph D: city-centre arrival, no airport trip, a hotel night saved.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000058', '2c000000-0000-4000-8000-060000000058', 1, 'Paragraph E', '[{"optionKey": "i", "content": "A tradition that faded", "sortOrder": 1}, {"optionKey": "ii", "content": "Why the trains cost so much to run", "sortOrder": 2}, {"optionKey": "iii", "content": "Environmental concerns bring passengers back", "sortOrder": 3}, {"optionKey": "iv", "content": "Practical benefits for travellers", "sortOrder": 4}, {"optionKey": "v", "content": "Difficulties facing the revival", "sortOrder": 5}, {"optionKey": "vi", "content": "A future shaped by political decisions", "sortOrder": 6}, {"optionKey": "vii", "content": "The spread of high-speed rail", "sortOrder": 7}, {"optionKey": "viii", "content": "How sleeping cars are designed", "sortOrder": 8}]'::jsonb,
     '{"type": "CHOICE", "correct": "v"}'::jsonb, 1, 'Paragraph E: old carriages, complaints and suspended routes.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000059', '2c000000-0000-4000-8000-060000000059', 1, 'Paragraph F', '[{"optionKey": "i", "content": "A tradition that faded", "sortOrder": 1}, {"optionKey": "ii", "content": "Why the trains cost so much to run", "sortOrder": 2}, {"optionKey": "iii", "content": "Environmental concerns bring passengers back", "sortOrder": 3}, {"optionKey": "iv", "content": "Practical benefits for travellers", "sortOrder": 4}, {"optionKey": "v", "content": "Difficulties facing the revival", "sortOrder": 5}, {"optionKey": "vi", "content": "A future shaped by political decisions", "sortOrder": 6}, {"optionKey": "vii", "content": "The spread of high-speed rail", "sortOrder": 7}, {"optionKey": "viii", "content": "How sleeping cars are designed", "sortOrder": 8}]'::jsonb,
     '{"type": "CHOICE", "correct": "vi"}'::jsonb, 1, 'Paragraph F: the future may depend on governments and their charges.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000060', '2c000000-0000-4000-8000-060000000060', 1, 'A comparison of the space needed for beds and for seats', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}, {"optionKey": "D", "content": "Paragraph D", "sortOrder": 4}, {"optionKey": "E", "content": "Paragraph E", "sortOrder": 5}, {"optionKey": "F", "content": "Paragraph F", "sortOrder": 6}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Paragraph B: beds take up more space than seats.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000061', '2c000000-0000-4000-8000-060000000061', 1, 'The name of an operator that took over abandoned routes', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}, {"optionKey": "D", "content": "Paragraph D", "sortOrder": 4}, {"optionKey": "E", "content": "Paragraph E", "sortOrder": 5}, {"optionKey": "F", "content": "Paragraph F", "sortOrder": 6}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Paragraph C: Austrian Federal Railways took over abandoned routes.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000062', '2c000000-0000-4000-8000-060000000062', 1, 'A reason why some new routes were stopped', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}, {"optionKey": "D", "content": "Paragraph D", "sortOrder": 4}, {"optionKey": "E", "content": "Paragraph E", "sortOrder": 5}, {"optionKey": "F", "content": "Paragraph F", "sortOrder": 6}]'::jsonb,
     '{"type": "CHOICE", "correct": "E"}'::jsonb, 1, 'Paragraph E: they could not attract enough passengers outside the summer holidays.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000063', '2c000000-0000-4000-8000-060000000063', 1, 'The idea that air fares do not reflect the environmental cost of flying', '[{"optionKey": "A", "content": "Paragraph A", "sortOrder": 1}, {"optionKey": "B", "content": "Paragraph B", "sortOrder": 2}, {"optionKey": "C", "content": "Paragraph C", "sortOrder": 3}, {"optionKey": "D", "content": "Paragraph D", "sortOrder": 4}, {"optionKey": "E", "content": "Paragraph E", "sortOrder": 5}, {"optionKey": "F", "content": "Paragraph F", "sortOrder": 6}]'::jsonb,
     '{"type": "CHOICE", "correct": "F"}'::jsonb, 1, 'Paragraph F: if the environmental cost of flying were included in air tickets.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000064', '2c000000-0000-4000-8000-060000000064', 1, 'Night trains declined partly because of competition from low-cost ______.', NULL,
     '{"type": "FILL", "accepted": ["airlines"]}'::jsonb, 1, 'Paragraph A: low-cost airlines offered cheaper flights.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000065', '2c000000-0000-4000-8000-060000000065', 1, 'Passengers on a night train save the cost of a ______ for one night.', NULL,
     '{"type": "FILL", "accepted": ["hotel"]}'::jsonb, 1, 'Paragraph D: it saves the cost of a hotel for one night.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000066', '2c000000-0000-4000-8000-060000000066', 1, 'Many carriages are old, and new ones are ______ and take years to build.', NULL,
     '{"type": "FILL", "accepted": ["costly"]}'::jsonb, 1, 'Paragraph E: new ones are costly and take years to build.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000067', '2c000000-0000-4000-8000-060000000067', 1, 'Why have some schools reduced handwriting lessons?', '[{"optionKey": "A", "content": "Teachers found them hard to organise", "sortOrder": 1}, {"optionKey": "B", "content": "Children will mainly type as adults", "sortOrder": 2}, {"optionKey": "C", "content": "Parents asked for more computer lessons", "sortOrder": 3}, {"optionKey": "D", "content": "Research showed handwriting had no benefits", "sortOrder": 4}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Paragraph A: children will spend their adult lives typing.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000068', '2c000000-0000-4000-8000-060000000068', 1, 'In the experiment in paragraph B, which children showed the most reading-related brain activity?', '[{"optionKey": "A", "content": "Those who typed the letters", "sortOrder": 1}, {"optionKey": "B", "content": "Those who traced the letters", "sortOrder": 2}, {"optionKey": "C", "content": "Those who wrote the letters freehand", "sortOrder": 3}, {"optionKey": "D", "content": "Those who only looked at the letters", "sortOrder": 4}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Paragraph B: more active in those who had written the letters freehand.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000069', '2c000000-0000-4000-8000-060000000069', 1, 'According to paragraph C, students who typed their notes', '[{"optionKey": "A", "content": "wrote down fewer words", "sortOrder": 1}, {"optionKey": "B", "content": "tended to copy the lecturer''s words", "sortOrder": 2}, {"optionKey": "C", "content": "did better in tests of understanding", "sortOrder": 3}, {"optionKey": "D", "content": "summarised more effectively", "sortOrder": 4}]'::jsonb,
     '{"type": "CHOICE", "correct": "B"}'::jsonb, 1, 'Paragraph C: laptop users often copied the lecturer almost word for word.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000070', '2c000000-0000-4000-8000-060000000070', 1, 'What does the writer say about later studies of note-taking?', '[{"optionKey": "A", "content": "They fully confirmed the original findings", "sortOrder": 1}, {"optionKey": "B", "content": "They were badly designed", "sortOrder": 2}, {"optionKey": "C", "content": "Their results were not consistent", "sortOrder": 3}, {"optionKey": "D", "content": "They only studied young children", "sortOrder": 4}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Paragraph C: later attempts produced mixed results.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000071', '2c000000-0000-4000-8000-060000000071', 1, 'In paragraph E, how does the writer suggest old handwritten documents could still be read?', '[{"optionKey": "A", "content": "By teaching every child joined-up writing", "sortOrder": 1}, {"optionKey": "B", "content": "By using computers to read handwriting", "sortOrder": 2}, {"optionKey": "C", "content": "By training specialists", "sortOrder": 3}, {"optionKey": "D", "content": "By typing all historical documents", "sortOrder": 4}]'::jsonb,
     '{"type": "CHOICE", "correct": "C"}'::jsonb, 1, 'Paragraph E: specialists can learn old scripts.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000072', '2c000000-0000-4000-8000-060000000072', 1, 'Replacing pens with keyboards seems reasonable at first.', '[{"optionKey": "YES", "content": "YES", "sortOrder": 1}, {"optionKey": "NO", "content": "NO", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "YES"}'::jsonb, 1, 'Paragraph A: at first sight, this seems a sensible response.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000073', '2c000000-0000-4000-8000-060000000073', 1, 'Students should stop using computers for writing.', '[{"optionKey": "YES", "content": "YES", "sortOrder": 1}, {"optionKey": "NO", "content": "NO", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "NO"}'::jsonb, 1, 'Paragraph D: nobody is seriously suggesting students give up computers.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000074', '2c000000-0000-4000-8000-060000000074', 1, 'Young children should learn to write fluently by hand before they learn to type.', '[{"optionKey": "YES", "content": "YES", "sortOrder": 1}, {"optionKey": "NO", "content": "NO", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "YES"}'::jsonb, 1, 'Paragraph D: I believe they should.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000075', '2c000000-0000-4000-8000-060000000075', 1, 'The cultural argument for handwriting is stronger than the scientific one.', '[{"optionKey": "YES", "content": "YES", "sortOrder": 1}, {"optionKey": "NO", "content": "NO", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "NO"}'::jsonb, 1, 'Paragraph E: there is also a cultural argument, although it is weaker.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000076', '2c000000-0000-4000-8000-060000000076', 1, 'Most teachers prefer handwritten work to typed work.', '[{"optionKey": "YES", "content": "YES", "sortOrder": 1}, {"optionKey": "NO", "content": "NO", "sortOrder": 2}, {"optionKey": "NOT_GIVEN", "content": "NOT GIVEN", "sortOrder": 3}]'::jsonb,
     '{"type": "CHOICE", "correct": "NOT_GIVEN"}'::jsonb, 1, 'The writer never says what most teachers prefer.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000077', '2c000000-0000-4000-8000-060000000077', 1, 'The writer recommends plenty of handwriting practice in the ______, when it seems to support reading.', NULL,
     '{"type": "FILL", "accepted": ["early years"]}'::jsonb, 1, 'Paragraph F: in the early years.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000078', '2c000000-0000-4000-8000-060000000078', 1, 'Teachers should not treat neat handwriting as a sign of ______.', NULL,
     '{"type": "FILL", "accepted": ["intelligence"]}'::jsonb, 1, 'Paragraph F: not a sign of intelligence.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000079', '2c000000-0000-4000-8000-060000000079', 1, 'Legibility and ______ matter far more than beauty.', NULL,
     '{"type": "FILL", "accepted": ["fluency"]}'::jsonb, 1, 'Paragraph F: legibility and fluency.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000080', '2c000000-0000-4000-8000-060000000080', 1, 'Decisions should be guided by ______ rather than nostalgia.', NULL,
     '{"type": "FILL", "accepted": ["evidence"]}'::jsonb, 1, 'Paragraph F: guided by evidence.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000081', '2c000000-0000-4000-8000-060000000081', 1, 'Some people think that cities should plant trees along every street, even if this means fewer parking spaces. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 250 words.', NULL,
     '{"type": "ESSAY", "task": "TASK_2", "minWords": 250, "passBand": 5.5}'::jsonb, 1, 'Graded by the placement essay grader.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000082', '2c000000-0000-4000-8000-060000000082', 1, 'Let''s talk about where you live. Do you live in a house or a flat? What do you like about it?', NULL,
     '{"type": "SPEAKING", "durationSeconds": 45}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000083', '2c000000-0000-4000-8000-060000000083', 1, 'Do you prefer studying in the morning or in the evening? Why?', NULL,
     '{"type": "SPEAKING", "durationSeconds": 45}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000084', '2c000000-0000-4000-8000-060000000084', 1, 'How often do you use public transport? What do you think of it in your area?', NULL,
     '{"type": "SPEAKING", "durationSeconds": 45}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000085', '2c000000-0000-4000-8000-060000000085', 1, 'What kind of music do you enjoy listening to? Has your taste changed since you were a child?', NULL,
     '{"type": "SPEAKING", "durationSeconds": 45}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000086', '2c000000-0000-4000-8000-060000000086', 1, 'Why do some people prefer to spend their free time outdoors while others stay at home?', NULL,
     '{"type": "SPEAKING", "durationSeconds": 90}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000087', '2c000000-0000-4000-8000-060000000087', 1, 'How has the way people spend their free time changed compared with your parents'' generation?', NULL,
     '{"type": "SPEAKING", "durationSeconds": 90}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED'),
    ('2c000000-0000-4000-8000-070000000088', '2c000000-0000-4000-8000-060000000088', 1, 'Should governments spend more money on public places such as parks and libraries? Why or why not?', NULL,
     '{"type": "SPEAKING", "durationSeconds": 90}'::jsonb, 1, 'Recorded answer; scored with the configured placement speaking band.', 'PUBLISHED');

UPDATE questions q SET current_published_version_id = qv.id
FROM question_versions qv
WHERE qv.question_id = q.id AND q.id::text LIKE '2c000000-0000-4000-8000-%';

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
VALUES ('2c000000-0000-4000-8000-090000000001', '2b000000-0000-4000-8000-080000000001', 2, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions) VALUES
    ('2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-090000000001', 'Listening Part 1: joining a swimming course', 'LISTENING', 1, NULL, 'Questions 1–10. Complete the form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-090000000001', 'Listening Part 2: a visit to a nature reserve', 'LISTENING', 2, NULL, 'Questions 11–16: Choose the correct letter, A, B or C. Questions 17–20: Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-090000000001', 'Listening Part 3: a project on canteen food waste', 'LISTENING', 3, NULL, 'Questions 21–25 and 30: Choose the correct letter, A, B or C. Questions 26–29: Who will do each task? Choose A (Maya), B (Tom) or C (both).'),
    ('2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-090000000001', 'Listening Part 4: the early history of the bicycle', 'LISTENING', 4, NULL, 'Questions 31–40. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-090000000001', 'Reading Passage 1: bees in the city', 'READING', 5, 1200, 'Questions 1–7: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 8–13: Complete the sentences. Choose NO MORE THAN TWO WORDS from the passage for each answer. You should spend about 20 minutes on this passage.'),
    ('2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-090000000001', 'Reading Passage 2: the return of the night train', 'READING', 6, 1200, 'Questions 14–19: Choose the correct heading for each paragraph from the list of headings i–viii. Questions 20–23: Which paragraph, A–F, contains the information? Questions 24–26: Complete the summary. Choose ONE WORD ONLY from the passage. You should spend about 20 minutes on this passage.'),
    ('2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-090000000001', 'Reading Passage 3: the case for handwriting', 'READING', 7, 1200, 'Questions 27–31: Choose the correct letter, A, B, C or D. Questions 32–36: Do the statements agree with the views of the writer? Choose YES, NO or NOT GIVEN. Questions 37–40: Complete the summary. Choose NO MORE THAN TWO WORDS from the passage. You should spend about 20 minutes on this passage.'),
    ('2c000000-0000-4000-8000-0a0000000008', '2c000000-0000-4000-8000-090000000001', 'Writing Task 1: museum visitors', 'WRITING', 8, 1200, 'Spend about 20 minutes on this task. Write at least 150 words.'),
    ('2c000000-0000-4000-8000-0a0000000009', '2c000000-0000-4000-8000-090000000001', 'Writing Task 2: trees or parking', 'WRITING', 9, 2400, 'Spend about 40 minutes on this task. Write at least 250 words.'),
    ('2c000000-0000-4000-8000-0a0000000010', '2c000000-0000-4000-8000-090000000001', 'Speaking Part 1: introduction and interview', 'SPEAKING', 10, NULL, 'The examiner asks general questions about yourself and familiar topics. Answer each question in a few sentences.'),
    ('2c000000-0000-4000-8000-0a0000000011', '2c000000-0000-4000-8000-090000000001', 'Speaking Part 2: a place you like', 'SPEAKING', 11, NULL, 'You have one minute to prepare, then speak for one to two minutes.'),
    ('2c000000-0000-4000-8000-0a0000000012', '2c000000-0000-4000-8000-090000000001', 'Speaking Part 3: discussion', 'SPEAKING', 12, NULL, 'The examiner asks broader questions linked to the Part 2 topic. Give full answers with reasons and examples.');

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2c000000-0000-4000-8000-0c0000000001', '2c000000-0000-4000-8000-050000000001', '2c000000-0000-4000-8000-0a0000000001', 0),
    ('2c000000-0000-4000-8000-0c0000000002', '2c000000-0000-4000-8000-050000000002', '2c000000-0000-4000-8000-0a0000000002', 0),
    ('2c000000-0000-4000-8000-0c0000000003', '2c000000-0000-4000-8000-050000000003', '2c000000-0000-4000-8000-0a0000000003', 0),
    ('2c000000-0000-4000-8000-0c0000000004', '2c000000-0000-4000-8000-050000000004', '2c000000-0000-4000-8000-0a0000000004', 0),
    ('2c000000-0000-4000-8000-0c0000000005', '2c000000-0000-4000-8000-050000000005', '2c000000-0000-4000-8000-0a0000000005', 0),
    ('2c000000-0000-4000-8000-0c0000000006', '2c000000-0000-4000-8000-050000000006', '2c000000-0000-4000-8000-0a0000000006', 0),
    ('2c000000-0000-4000-8000-0c0000000007', '2c000000-0000-4000-8000-050000000007', '2c000000-0000-4000-8000-0a0000000007', 0),
    ('2c000000-0000-4000-8000-0c0000000008', '2b000000-0000-4000-8000-050000000003', '2c000000-0000-4000-8000-0a0000000008', 0);

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('2c000000-0000-4000-8000-0b0000000001', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000001', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000002', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000002', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000003', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000003', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000004', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000004', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000005', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000005', 5, 1.0),
    ('2c000000-0000-4000-8000-0b0000000006', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000006', 6, 1.0),
    ('2c000000-0000-4000-8000-0b0000000007', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000007', 7, 1.0),
    ('2c000000-0000-4000-8000-0b0000000008', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000008', 8, 1.0),
    ('2c000000-0000-4000-8000-0b0000000009', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000009', 9, 1.0),
    ('2c000000-0000-4000-8000-0b0000000010', '2c000000-0000-4000-8000-0a0000000001', '2c000000-0000-4000-8000-070000000010', 10, 1.0),
    ('2c000000-0000-4000-8000-0b0000000011', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000011', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000012', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000012', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000013', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000013', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000014', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000014', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000015', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000015', 5, 1.0),
    ('2c000000-0000-4000-8000-0b0000000016', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000016', 6, 1.0),
    ('2c000000-0000-4000-8000-0b0000000017', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000017', 7, 1.0),
    ('2c000000-0000-4000-8000-0b0000000018', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000018', 8, 1.0),
    ('2c000000-0000-4000-8000-0b0000000019', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000019', 9, 1.0),
    ('2c000000-0000-4000-8000-0b0000000020', '2c000000-0000-4000-8000-0a0000000002', '2c000000-0000-4000-8000-070000000020', 10, 1.0),
    ('2c000000-0000-4000-8000-0b0000000021', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000021', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000022', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000022', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000023', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000023', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000024', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000024', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000025', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000025', 5, 1.0),
    ('2c000000-0000-4000-8000-0b0000000026', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000026', 6, 1.0),
    ('2c000000-0000-4000-8000-0b0000000027', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000027', 7, 1.0),
    ('2c000000-0000-4000-8000-0b0000000028', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000028', 8, 1.0),
    ('2c000000-0000-4000-8000-0b0000000029', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000029', 9, 1.0),
    ('2c000000-0000-4000-8000-0b0000000030', '2c000000-0000-4000-8000-0a0000000003', '2c000000-0000-4000-8000-070000000030', 10, 1.0),
    ('2c000000-0000-4000-8000-0b0000000031', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000031', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000032', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000032', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000033', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000033', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000034', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000034', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000035', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000035', 5, 1.0),
    ('2c000000-0000-4000-8000-0b0000000036', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000036', 6, 1.0),
    ('2c000000-0000-4000-8000-0b0000000037', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000037', 7, 1.0),
    ('2c000000-0000-4000-8000-0b0000000038', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000038', 8, 1.0),
    ('2c000000-0000-4000-8000-0b0000000039', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000039', 9, 1.0),
    ('2c000000-0000-4000-8000-0b0000000040', '2c000000-0000-4000-8000-0a0000000004', '2c000000-0000-4000-8000-070000000040', 10, 1.0),
    ('2c000000-0000-4000-8000-0b0000000041', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000041', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000042', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000042', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000043', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000043', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000044', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000044', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000045', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000045', 5, 1.0),
    ('2c000000-0000-4000-8000-0b0000000046', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000046', 6, 1.0),
    ('2c000000-0000-4000-8000-0b0000000047', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000047', 7, 1.0),
    ('2c000000-0000-4000-8000-0b0000000048', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000048', 8, 1.0),
    ('2c000000-0000-4000-8000-0b0000000049', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000049', 9, 1.0),
    ('2c000000-0000-4000-8000-0b0000000050', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000050', 10, 1.0),
    ('2c000000-0000-4000-8000-0b0000000051', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000051', 11, 1.0),
    ('2c000000-0000-4000-8000-0b0000000052', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000052', 12, 1.0),
    ('2c000000-0000-4000-8000-0b0000000053', '2c000000-0000-4000-8000-0a0000000005', '2c000000-0000-4000-8000-070000000053', 13, 1.0),
    ('2c000000-0000-4000-8000-0b0000000054', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000054', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000055', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000055', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000056', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000056', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000057', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000057', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000058', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000058', 5, 1.0),
    ('2c000000-0000-4000-8000-0b0000000059', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000059', 6, 1.0),
    ('2c000000-0000-4000-8000-0b0000000060', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000060', 7, 1.0),
    ('2c000000-0000-4000-8000-0b0000000061', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000061', 8, 1.0),
    ('2c000000-0000-4000-8000-0b0000000062', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000062', 9, 1.0),
    ('2c000000-0000-4000-8000-0b0000000063', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000063', 10, 1.0),
    ('2c000000-0000-4000-8000-0b0000000064', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000064', 11, 1.0),
    ('2c000000-0000-4000-8000-0b0000000065', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000065', 12, 1.0),
    ('2c000000-0000-4000-8000-0b0000000066', '2c000000-0000-4000-8000-0a0000000006', '2c000000-0000-4000-8000-070000000066', 13, 1.0),
    ('2c000000-0000-4000-8000-0b0000000067', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000067', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000068', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000068', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000069', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000069', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000070', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000070', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000071', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000071', 5, 1.0),
    ('2c000000-0000-4000-8000-0b0000000072', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000072', 6, 1.0),
    ('2c000000-0000-4000-8000-0b0000000073', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000073', 7, 1.0),
    ('2c000000-0000-4000-8000-0b0000000074', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000074', 8, 1.0),
    ('2c000000-0000-4000-8000-0b0000000075', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000075', 9, 1.0),
    ('2c000000-0000-4000-8000-0b0000000076', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000076', 10, 1.0),
    ('2c000000-0000-4000-8000-0b0000000077', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000077', 11, 1.0),
    ('2c000000-0000-4000-8000-0b0000000078', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000078', 12, 1.0),
    ('2c000000-0000-4000-8000-0b0000000079', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000079', 13, 1.0),
    ('2c000000-0000-4000-8000-0b0000000080', '2c000000-0000-4000-8000-0a0000000007', '2c000000-0000-4000-8000-070000000080', 14, 1.0),
    ('2c000000-0000-4000-8000-0b0000000081', '2c000000-0000-4000-8000-0a0000000008', '2b000000-0000-4000-8000-070000000013', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000082', '2c000000-0000-4000-8000-0a0000000009', '2c000000-0000-4000-8000-070000000081', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000083', '2c000000-0000-4000-8000-0a0000000010', '2c000000-0000-4000-8000-070000000082', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000084', '2c000000-0000-4000-8000-0a0000000010', '2c000000-0000-4000-8000-070000000083', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000085', '2c000000-0000-4000-8000-0a0000000010', '2c000000-0000-4000-8000-070000000084', 3, 1.0),
    ('2c000000-0000-4000-8000-0b0000000086', '2c000000-0000-4000-8000-0a0000000010', '2c000000-0000-4000-8000-070000000085', 4, 1.0),
    ('2c000000-0000-4000-8000-0b0000000087', '2c000000-0000-4000-8000-0a0000000011', '2b000000-0000-4000-8000-070000000012', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000088', '2c000000-0000-4000-8000-0a0000000012', '2c000000-0000-4000-8000-070000000086', 1, 1.0),
    ('2c000000-0000-4000-8000-0b0000000089', '2c000000-0000-4000-8000-0a0000000012', '2c000000-0000-4000-8000-070000000087', 2, 1.0),
    ('2c000000-0000-4000-8000-0b0000000090', '2c000000-0000-4000-8000-0a0000000012', '2c000000-0000-4000-8000-070000000088', 3, 1.0);

UPDATE content_packages SET current_published_version_id = '2c000000-0000-4000-8000-090000000001', updated_at = CURRENT_TIMESTAMP
WHERE id = '2b000000-0000-4000-8000-080000000001';
