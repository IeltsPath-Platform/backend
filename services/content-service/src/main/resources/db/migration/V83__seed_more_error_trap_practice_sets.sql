-- Additional practice sets for the error-trap reviews, so a learner who keeps repeating a mistake does not run
-- out of unseen sets. Mock content for demos and manual testing; an IELTS-qualified examiner must review it
-- before it goes to real learners. Each set has four questions: two carry the trap option and two do not,
-- matching the structure of the earlier trap sets. Codes with no earlier set start at set 1.

CREATE TEMP TABLE trap_practice_targets (
    seed_no INT PRIMARY KEY,
    error_code TEXT NOT NULL,
    set_no INT NOT NULL,
    parent_code TEXT NOT NULL,
    package_title TEXT NOT NULL,
    skill TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO trap_practice_targets VALUES
(201, 'DETAIL_AS_MAIN_IDEA', 3, 'DR_IDEA_OR_DETAIL', 'Spotting the central idea of a leaflet', 'READING'),
(202, 'DETAIL_AS_MAIN_IDEA', 4, 'DR_IDEA_OR_DETAIL', 'Choosing the point that covers the whole text', 'READING'),
(203, 'DETAIL_AS_MAIN_IDEA', 5, 'DR_IDEA_OR_DETAIL', 'Telling a headline idea from a single fact', 'READING'),
(204, 'DETAIL_AS_MAIN_IDEA', 6, 'DR_IDEA_OR_DETAIL', 'Summarizing a report in one sentence', 'READING'),
(205, 'DISTRACTOR_CORRECTED', 3, 'L65_CORRECTION', 'Catching a speaker’s self-correction', 'LISTENING'),
(206, 'DISTRACTOR_CORRECTED', 4, 'L65_CORRECTION', 'Using the final version of a detail', 'LISTENING'),
(207, 'DISTRACTOR_CORRECTED', 5, 'L65_CORRECTION', 'Listening for “actually” and “sorry”', 'LISTENING'),
(208, 'DISTRACTOR_CORRECTED', 6, 'L65_CORRECTION', 'Following a changed arrangement', 'LISTENING'),
(209, 'INFERENCE_UNSUPPORTED', 3, 'R65_EVIDENCE_INFERENCE', 'Drawing only the conclusion the text supports', 'READING'),
(210, 'INFERENCE_UNSUPPORTED', 4, 'R65_EVIDENCE_INFERENCE', 'Separating evidence from assumption', 'READING'),
(211, 'INFERENCE_UNSUPPORTED', 5, 'R65_EVIDENCE_INFERENCE', 'Judging how far a result can be stretched', 'READING'),
(212, 'INFERENCE_UNSUPPORTED', 6, 'R65_EVIDENCE_INFERENCE', 'Staying within a small study', 'READING'),
(213, 'KEYWORD_MATCH_ONLY', 3, 'TR_R_SCANNING', 'Looking past a repeated word', 'READING'),
(214, 'KEYWORD_MATCH_ONLY', 4, 'TR_R_SCANNING', 'Checking the full sentence, not one keyword', 'READING'),
(215, 'KEYWORD_MATCH_ONLY', 5, 'TR_R_SCANNING', 'Why a matching word can mislead', 'READING'),
(216, 'KEYWORD_MATCH_ONLY', 6, 'TR_R_SCANNING', 'Answering the question that was asked', 'READING'),
(217, 'LINKER_MISUSE', 3, 'DEMO_WRITING_LINKING', 'Matching a connector to the relationship', 'WRITING'),
(218, 'LINKER_MISUSE', 4, 'DEMO_WRITING_LINKING', 'Contrast, result and addition in context', 'WRITING'),
(219, 'LINKER_MISUSE', 5, 'DEMO_WRITING_LINKING', 'Avoiding a linker that changes the logic', 'WRITING'),
(220, 'LINKER_MISUSE', 6, 'DEMO_WRITING_LINKING', 'Linking ideas across sentences', 'WRITING'),
(221, 'PARAPHRASE_TRAP', 3, 'R65_PARAPHRASE', 'Keeping degree and certainty in a paraphrase', 'READING'),
(222, 'PARAPHRASE_TRAP', 4, 'R65_PARAPHRASE', 'Same words, different meaning', 'READING'),
(223, 'PARAPHRASE_TRAP', 5, 'R65_PARAPHRASE', 'Checking quantity words in a rewrite', 'READING'),
(224, 'PARAPHRASE_TRAP', 6, 'R65_PARAPHRASE', 'Matching a rewrite to the original', 'READING'),
(225, 'TFNG_FALSE_AS_NG', 3, 'TFNG_FALSE_VS_NOT_GIVEN', 'Spotting statements the passage contradicts', 'READING'),
(226, 'TFNG_FALSE_AS_NG', 4, 'TFNG_FALSE_VS_NOT_GIVEN', 'Telling False from Not Given in schedules', 'READING'),
(227, 'TFNG_FALSE_AS_NG', 5, 'TFNG_FALSE_VS_NOT_GIVEN', 'Checking figures against the passage', 'READING'),
(228, 'TFNG_FALSE_AS_NG', 6, 'TFNG_FALSE_VS_NOT_GIVEN', 'Deciding when a passage disagrees', 'READING'),
(229, 'TFNG_NG_AS_FALSE', 3, 'TFNG_FALSE_VS_NOT_GIVEN', 'Recognising information the passage leaves out', 'READING'),
(230, 'TFNG_NG_AS_FALSE', 4, 'TFNG_FALSE_VS_NOT_GIVEN', 'Not Given versus False in short notices', 'READING'),
(231, 'TFNG_NG_AS_FALSE', 5, 'TFNG_FALSE_VS_NOT_GIVEN', 'Resisting the urge to call a silence false', 'READING'),
(232, 'TFNG_NG_AS_FALSE', 6, 'TFNG_FALSE_VS_NOT_GIVEN', 'Deciding when a passage is simply silent', 'READING'),
(233, 'WRONG_PARAGRAPH', 3, 'PR_MATCHING_INFORMATION', 'Finding a detail in the right paragraph', 'READING'),
(234, 'WRONG_PARAGRAPH', 4, 'PR_MATCHING_INFORMATION', 'Locating figures and dates by paragraph', 'READING'),
(235, 'WRONG_PARAGRAPH', 5, 'PR_MATCHING_INFORMATION', 'Matching information to the paragraph that holds it', 'READING'),
(236, 'WRONG_PARAGRAPH', 6, 'PR_MATCHING_INFORMATION', 'Paragraph by paragraph: where is the detail?', 'READING'),
(237, 'WRONG_SPEAKER', 3, 'L65_SPEAKER_PURPOSE', 'Attributing a view to the right speaker', 'LISTENING'),
(238, 'WRONG_SPEAKER', 4, 'L65_SPEAKER_PURPOSE', 'Who said it? Following two voices', 'LISTENING'),
(239, 'WRONG_SPEAKER', 5, 'L65_SPEAKER_PURPOSE', 'Matching opinions with speakers', 'LISTENING'),
(240, 'WRONG_SPEAKER', 6, 'L65_SPEAKER_PURPOSE', 'Keeping track of who suggests what', 'LISTENING'),
(241, 'SCOPE_OVERGENERALISED', 1, 'R70_HEDGING_SCOPE', 'Noticing limiting words such as “some” and “often”', 'READING'),
(242, 'SCOPE_OVERGENERALISED', 2, 'R70_HEDGING_SCOPE', 'Keeping a claim within the study’s scope', 'READING'),
(243, 'SCOPE_OVERGENERALISED', 3, 'R70_HEDGING_SCOPE', 'Hedges that change what a statement means', 'READING'),
(244, 'SCOPE_OVERGENERALISED', 4, 'R70_HEDGING_SCOPE', 'Not stretching a result beyond the passage', 'READING'),
(245, 'NUMBER_FORMAT', 1, 'LS_NUM', 'Hearing the difference between thirteen and thirty', 'LISTENING'),
(246, 'NUMBER_FORMAT', 2, 'LS_NUM', 'Teen numbers versus tens', 'LISTENING'),
(247, 'NUMBER_FORMAT', 3, 'LS_NUM', 'Prices, quantities and times heard clearly', 'LISTENING'),
(248, 'NUMBER_FORMAT', 4, 'LS_NUM', 'Numbers that sound alike', 'LISTENING'),
(249, 'WORD_CHOICE', 1, 'W70_LEXICAL_PRECISION', 'Choosing the most precise word', 'WRITING'),
(250, 'WORD_CHOICE', 2, 'W70_LEXICAL_PRECISION', 'Close in meaning, wrong in context', 'WRITING'),
(251, 'WORD_CHOICE', 3, 'W70_LEXICAL_PRECISION', 'Picking the verb that fits the sentence', 'WRITING'),
(252, 'WORD_CHOICE', 4, 'W70_LEXICAL_PRECISION', 'Precise vocabulary for academic writing', 'WRITING'),
(253, 'COLLOCATION', 1, 'W70_LEXICAL_PRECISION', 'Natural word partnerships', 'WRITING'),
(254, 'COLLOCATION', 2, 'W70_LEXICAL_PRECISION', 'Make or do? Heavy or strong?', 'WRITING'),
(255, 'COLLOCATION', 3, 'W70_LEXICAL_PRECISION', 'Verbs that go with common nouns', 'WRITING'),
(256, 'COLLOCATION', 4, 'W70_LEXICAL_PRECISION', 'Collocations for academic essays', 'WRITING'),
(257, 'UNDERDEVELOPED_IDEA', 1, 'W70_IDEA_DEVELOPMENT', 'Giving a reason for every main idea', 'WRITING'),
(258, 'UNDERDEVELOPED_IDEA', 2, 'W70_IDEA_DEVELOPMENT', 'From a claim to a supported paragraph', 'WRITING'),
(259, 'UNDERDEVELOPED_IDEA', 3, 'W70_IDEA_DEVELOPMENT', 'Supporting an idea with a reason and example', 'WRITING'),
(260, 'UNDERDEVELOPED_IDEA', 4, 'W70_IDEA_DEVELOPMENT', 'Moving beyond a restated opinion', 'WRITING');

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

-- DETAIL_AS_MAIN_IDEA
INSERT INTO trap_practice_items VALUES
(201,1,'MULTIPLE_CHOICE','A leaflet explains that a new tram line shortens commutes, lowers street noise and links two hospitals. What is its main point?','The tram line brings several benefits to the city.','The tram line has two stops near hospitals.','Street noise is measured in decibels.','A','B','The leaflet lists several benefits. The hospital stops are one supporting detail.'),
(201,2,'MULTIPLE_CHOICE','A report says a school garden teaches pupils to grow food, reduces waste through composting and brings families together. What is the central idea?','The garden has a composting area.','A school garden supports learning and the community in several ways.','Pupils dislike vegetables.','B','A','The report covers learning, waste and community. Composting is only one supporting detail.'),
(201,3,'MULTIPLE_CHOICE','A café reported that its new loyalty card was used by 300 customers in March. How many customers used the card?','30','3,000','300','C',NULL,'The report gives the figure 300.'),
(201,4,'MULTIPLE_CHOICE','A ferry company added a late sailing on Fridays after passengers asked for it. What did the company add?','A late Friday sailing','A Sunday sailing','A cheaper ticket','A',NULL,'The addition was a late sailing on Fridays.'),
(202,1,'MULTIPLE_CHOICE','An article describes how a village shop now sells local bread, hosts a post office counter and offers a meeting corner. Which sentence gives its main idea?','The shop sells bread from nearby bakers.','The village shop has become a multi-purpose community hub.','The village has a church.','B','A','The article is about the shop’s widening role. The bread is one example of it.'),
(202,2,'MULTIPLE_CHOICE','A text notes that the lake path was widened, new lights were fitted and benches were added, making evening walks safer and more pleasant. What is the main point?','The lake is the largest in the region.','The lake path was widened.','Upgrades have made the lake path better for evening walks.','C','B','The text links all the upgrades to better evening walks. Widening the path is one of them.'),
(202,3,'MULTIPLE_CHOICE','The council replaced old bins with covered ones to stop birds spreading litter. Why were covered bins introduced?','To keep birds from spreading litter','To save money','To widen the pavement','A',NULL,'The reason given is stopping birds spreading litter.'),
(202,4,'MULTIPLE_CHOICE','A bookshop moved its children’s section to the ground floor so families could find it easily. What was moved?','The café','The children’s section','The cash desk','B',NULL,'Only the children’s section was moved.'),
(203,1,'MULTIPLE_CHOICE','A guide to a museum’s new app says it offers audio tours, floor maps and a quiz for children. What is the guide mostly about?','The app has a quiz for children.','The museum closes on Mondays.','The app helps visitors explore the museum in different ways.','C','A','The guide presents the app’s range of features. The quiz is one of them.'),
(203,2,'MULTIPLE_CHOICE','A news item reports that a bakery cut packaging, switched to local flour and started a bottle-return scheme. What is the main idea?','The bakery has taken several steps to become more sustainable.','The bakery uses flour from nearby farms.','The bakery opens at six.','A','B','All three changes point to sustainability. Local flour is just one of them.'),
(203,3,'MULTIPLE_CHOICE','A gym lowered its off-peak prices on weekday mornings. When did prices fall?','Weekend evenings','Weekday mornings','Public holidays','B',NULL,'The cheaper period is weekday mornings.'),
(203,4,'MULTIPLE_CHOICE','The bus company introduced contactless payment on all routes in June. What began in June?','Free transfers','Night buses','Contactless payment on all routes','C',NULL,'Contactless payment began in June.'),
(204,1,'MULTIPLE_CHOICE','A passage explains that cycling to work saves money, improves fitness and reduces traffic. What is it mainly about?','Cycling to work has several advantages.','Cycling can reduce traffic.','Most employees live far away.','A','B','The passage gathers three advantages. Traffic reduction is only one.'),
(204,2,'MULTIPLE_CHOICE','A brochure says the campus library now has longer hours, quiet study rooms and free printing. What does it mainly announce?','Free printing is available.','Library services have been improved for students.','Students must return books on time.','B','A','The brochure announces a set of improvements. Printing is one detail among them.'),
(204,3,'MULTIPLE_CHOICE','Researchers interviewed forty residents about local parks. How many residents were interviewed?','Four','Four hundred','Forty','C',NULL,'The number given is forty.'),
(204,4,'MULTIPLE_CHOICE','A museum extended its Saturday hours until 8 p.m. for the summer. Until when is it open on Saturdays?','8 p.m.','7 p.m.','6 p.m.','A',NULL,'The new closing time is 8 p.m.');

-- DISTRACTOR_CORRECTED
INSERT INTO trap_practice_items VALUES
(205,1,'MULTIPLE_CHOICE','A caller says the class starts on Monday, then corrects herself: it starts on Wednesday. When does the class start?','Monday','Wednesday','Friday','B','A','The speaker replaces Monday with Wednesday.'),
(205,2,'MULTIPLE_CHOICE','A man says his flight lands at 6:40, then adds, “Sorry, 7:40.” What is the arrival time?','6:40','7:10','7:40','C','A','The corrected time, 7:40, replaces 6:40.'),
(205,3,'MULTIPLE_CHOICE','A caller books a haircut for Tuesday at 10 a.m. Which day is booked?','Tuesday','Monday','Thursday','A',NULL,'The booking is for Tuesday.'),
(205,4,'MULTIPLE_CHOICE','A speaker says the library opens at 9 a.m. and closes at 5 p.m. When does it close?','6 p.m.','5 p.m.','9 p.m.','B',NULL,'The closing time is 5 p.m.'),
(206,1,'MULTIPLE_CHOICE','A receptionist gives room 204, then says, “Actually, it’s room 240.” Which room should the guest go to?','204','420','240','C','A','Room 240 is the corrected number.'),
(206,2,'MULTIPLE_CHOICE','A student says the essay is due on the 12th, then notes the date has moved to the 19th. When is it due?','The 19th','The 15th','The 12th','A','C','The deadline was moved to the 19th.'),
(206,3,'MULTIPLE_CHOICE','A woman asks for the vegetarian option. What does she order?','A fish dish','A vegetarian dish','A chicken dish','B',NULL,'She asks for the vegetarian option.'),
(206,4,'MULTIPLE_CHOICE','The tutor says the workshop lasts two hours. How long is it?','One hour','Three hours','Two hours','C',NULL,'The length stated is two hours.'),
(207,1,'MULTIPLE_CHOICE','A driver says the meeting point is the north car park, but a colleague says it is now the east car park. Where do they meet?','East car park','North car park','West car park','A','B','The colleague updates the location to the east car park.'),
(207,2,'MULTIPLE_CHOICE','A speaker says tickets cost £15, then corrects: £12 for students. How much do students pay?','£15','£12','£10','B','A','Students pay the corrected price of £12.'),
(207,3,'MULTIPLE_CHOICE','The agent says the flat has two bedrooms and a balcony. What does it have besides bedrooms?','A garden','A garage','A balcony','C',NULL,'The extra feature is the balcony.'),
(207,4,'MULTIPLE_CHOICE','A sender says the parcel went by courier on Thursday. How was it sent?','By courier','By post','By train','A',NULL,'The parcel was sent by courier.'),
(208,1,'MULTIPLE_CHOICE','The guide says the tour includes the castle, but later says the castle is closed and replaces it with the abbey. What will the group visit?','The castle','The abbey','The harbour','B','A','The abbey replaces the castle.'),
(208,2,'MULTIPLE_CHOICE','A woman first asks for a table for four, then says, “No, five – my brother is coming.” How many seats are needed?','Four','Six','Five','C','A','The booking changes from four to five.'),
(208,3,'MULTIPLE_CHOICE','The announcer says the 14:20 train to Leeds leaves from platform 3. Which platform is it?','Platform 3','Platform 2','Platform 13','A',NULL,'The platform given is 3.'),
(208,4,'MULTIPLE_CHOICE','A customer asks for the blue jacket in size medium. What size does she want?','Small','Medium','Large','B',NULL,'She asks for medium.');

-- INFERENCE_UNSUPPORTED
INSERT INTO trap_practice_items VALUES
(209,1,'MULTIPLE_CHOICE','A study of 120 volunteers found that those who slept eight hours scored higher on a memory test. Which conclusion is supported?','Sleep is the only factor that affects memory.','Everyone should sleep nine hours.','Among these volunteers, longer sleep went with higher memory scores.','C','A','The study reports an association in one group, not a sole cause.'),
(209,2,'MULTIPLE_CHOICE','A café’s sales rose after it switched to paper cups. What can be concluded?','Sales rose after the switch to paper cups.','Customers bought more because they dislike plastic.','Paper cups are cheaper than china.','A','B','The text gives the order of events only; the customers’ reasons are not stated.'),
(209,3,'MULTIPLE_CHOICE','A report states that the museum received 4,000 visitors in July. How many visitors were recorded?','400','4,000','40,000','B',NULL,'The figure stated is 4,000.'),
(209,4,'MULTIPLE_CHOICE','The author says the bridge was completed in 1998. When was it completed?','1989','2008','1998','C',NULL,'The completion year is 1998.'),
(210,1,'MULTIPLE_CHOICE','A town recorded fewer accidents after installing speed cameras on one road. Which statement is supported?','Fewer accidents were recorded on that road after the cameras were installed.','Speed cameras stop all accidents.','Drivers in other towns speed more.','A','B','The result is limited to one road and does not promise a total stop.'),
(210,2,'MULTIPLE_CHOICE','A survey of 50 hotel guests found that most rated breakfast highly. What does the survey show?','All hotel breakfasts in the country are excellent.','Most of the surveyed guests rated breakfast highly.','Guests only care about breakfast.','B','A','The finding covers the 50 guests surveyed, not hotels nationwide.'),
(210,3,'MULTIPLE_CHOICE','The text says the lake freezes in January. When does it freeze?','December','March','January','C',NULL,'The month given is January.'),
(210,4,'MULTIPLE_CHOICE','According to the article, the festival is held in the old market square. Where is it held?','In the old market square','In the harbour','In the town hall','A',NULL,'The venue is the old market square.'),
(211,1,'MULTIPLE_CHOICE','A gardener noticed that tomatoes grown beside basil produced more fruit this year. What is supported?','Basil makes tomatoes grow larger in every garden.','This year, the tomatoes beside basil produced more fruit.','Tomatoes need no water.','B','A','The observation is about one garden and one year.'),
(211,2,'MULTIPLE_CHOICE','After a school added a reading hour, library loans rose by 20%. What can we conclude?','The reading hour caused the entire rise.','Pupils now prefer books to sport.','Library loans rose by 20% after the reading hour was added.','C','A','The text shows a rise after the change, but does not prove the change caused all of it.'),
(211,3,'MULTIPLE_CHOICE','The passage says the survey covered three districts. How many districts did it cover?','Three','Two','Five','A',NULL,'It covered three districts.'),
(211,4,'MULTIPLE_CHOICE','The leaflet says guided tours start at 11 a.m. When do tours start?','10 a.m.','11 a.m.','12 noon','B',NULL,'Tours begin at 11 a.m.'),
(212,1,'MULTIPLE_CHOICE','A bakery sold out of rye loaves by noon on three weekends. Which is supported?','Rye bread is the most popular food in the city.','The bakery should raise its prices.','The rye loaves sold out by noon on those weekends.','C','A','Only the bakery’s three weekends are reported.'),
(212,2,'MULTIPLE_CHOICE','A pilot scheme gave free bus passes to 30 students, and their attendance improved. What is supported?','Attendance improved among the 30 students in the pilot.','Free passes will improve attendance in every school.','Buses are always late.','A','B','The improvement is reported for the pilot group only.'),
(212,3,'MULTIPLE_CHOICE','The article says the new clinic employs twelve nurses. How many nurses work there?','Two','Twelve','Twenty','B',NULL,'The number stated is twelve.'),
(212,4,'MULTIPLE_CHOICE','The report says the recycling rate rose to 45%. What was the new rate?','15%','35%','45%','C',NULL,'The rate rose to 45%.');

-- KEYWORD_MATCH_ONLY
INSERT INTO trap_practice_items VALUES
(213,1,'MULTIPLE_CHOICE','Passage: “Heavy rain closed the coastal road for three days, so deliveries went by rail.” How did deliveries reach the town?','By rail','By the coastal road','By sea','A','B','The road was closed, so rail was used. The road is named but not used.'),
(213,2,'MULTIPLE_CHOICE','Passage: “The museum charges adults £10, but children enter free on Sundays.” Who pays nothing on Sundays?','Adults','Children','Teachers','B','A','Adults is mentioned in the sentence, but they are the ones who pay.'),
(213,3,'MULTIPLE_CHOICE','Passage: “The workshop is held in Room 5 on the second floor.” Where is the workshop held?','The ground floor','Room 2','Room 5','C',NULL,'The room stated is Room 5.'),
(213,4,'MULTIPLE_CHOICE','Passage: “Members receive a newsletter every month.” How often do members receive a newsletter?','Monthly','Weekly','Yearly','A',NULL,'The newsletter is monthly.'),
(214,1,'MULTIPLE_CHOICE','Passage: “Although the lake is famous for its swans, the main attraction is the lighthouse.” What is the main attraction?','The swans','The lighthouse','The café','B','A','The swans are mentioned only as what the lake is famous for.'),
(214,2,'MULTIPLE_CHOICE','Passage: “Sales of electric bikes fell in 2020 but recovered strongly in 2021.” What happened in 2021?','Sales fell','Production stopped','Sales recovered','C','A','The fall belongs to 2020; 2021 is the recovery.'),
(214,3,'MULTIPLE_CHOICE','Passage: “The shop sells fresh fish on Fridays only.” When is fresh fish sold?','On Fridays','Every day','At weekends','A',NULL,'Fresh fish is sold on Fridays only.'),
(214,4,'MULTIPLE_CHOICE','Passage: “The tour guide speaks English, French and Spanish.” Which language is NOT mentioned?','Spanish','German','French','B',NULL,'German is not among the languages listed.'),
(215,1,'MULTIPLE_CHOICE','Passage: “The library lends books for three weeks; DVDs may be kept for one week.” How long can DVDs be kept?','Three weeks','Two weeks','One week','C','A','Three weeks refers to books, not DVDs.'),
(215,2,'MULTIPLE_CHOICE','Passage: “Dr Lee studied birds on the island, while her colleague studied plants.” What did Dr Lee study?','Birds','Plants','Rocks','A','B','Plants belong to the colleague.'),
(215,3,'MULTIPLE_CHOICE','Passage: “The path leads to a quiet beach about two miles away.” How far is the beach?','About one mile','About two miles','About ten miles','B',NULL,'The distance given is about two miles.'),
(215,4,'MULTIPLE_CHOICE','Passage: “Applications must be sent by post; emails are not accepted.” How must applications be sent?','By email','In person','By post','C',NULL,'Only postal applications are accepted.'),
(216,1,'MULTIPLE_CHOICE','Passage: “The festival began as a small market and is now a week-long event.” What is the festival now?','A week-long event','A small market','A one-day fair','A','B','The small market describes how it began.'),
(216,2,'MULTIPLE_CHOICE','Passage: “Tickets bought online are cheaper than tickets bought at the door.” Where are tickets cheaper?','At the door','Online','On the train','B','A','The door price is the more expensive one.'),
(216,3,'MULTIPLE_CHOICE','Passage: “The new cinema has six screens and a café.” How many screens does it have?','Three','Sixteen','Six','C',NULL,'The cinema has six screens.'),
(216,4,'MULTIPLE_CHOICE','Passage: “Students can borrow laptops for up to four hours.” What is the maximum loan time?','Four hours','Two hours','One day','A',NULL,'The limit is four hours.');

-- LINKER_MISUSE
INSERT INTO trap_practice_items VALUES
(217,1,'MULTIPLE_CHOICE','Choose the connector: Prices rose sharply; ______, many families cut their spending.','for example','as a result','in contrast','B','C','The spending cut is a consequence of the price rise.'),
(217,2,'MULTIPLE_CHOICE','Choose the connector: The experiment failed; ______, the team learned which materials to avoid.','for instance','therefore','nevertheless','C','B','Learning something useful despite failure is a contrast, not a result.'),
(217,3,'MULTIPLE_CHOICE','Choose the connector: Exercise improves health. ______, it helps people sleep better.','In addition','However','Instead','A',NULL,'Both are benefits, so addition fits.'),
(217,4,'MULTIPLE_CHOICE','Choose the connector: Many animals are endangered. ______, tigers have lost most of their habitat.','Whereas','For instance','Unless','B',NULL,'Tigers are an example of endangered animals.'),
(218,1,'MULTIPLE_CHOICE','Choose the connector: Cars are convenient. ______, they add to air pollution.','Moreover','Therefore','However','C','A','The pollution is a drawback, so the logic is contrast.'),
(218,2,'MULTIPLE_CHOICE','Choose the connector: Many students work part time. ______, they often have little time to study.','As a result','Similarly','In contrast','A','C','Little study time follows from working part time.'),
(218,3,'MULTIPLE_CHOICE','Choose the connector: The team trained daily ______ they wanted to win the final.','although','because','unless','B',NULL,'Wanting to win is the reason for training.'),
(218,4,'MULTIPLE_CHOICE','Choose the connector: ______ it was raining, the match went ahead.','Because','So','Although','C',NULL,'The match went ahead despite the rain.'),
(219,1,'MULTIPLE_CHOICE','Choose the connector: Solar power is clean. ______, it depends on sunny weather.','However','Likewise','As a result','A','B','Weather dependence is a limitation, so contrast fits.'),
(219,2,'MULTIPLE_CHOICE','Choose the connector: Some cities offer free bus travel; ______, Tallinn has done so for years.','but','for example','therefore','B','C','Tallinn is given as an example of such a city.'),
(219,3,'MULTIPLE_CHOICE','Choose the connector: Rural areas are quiet, ______ cities are noisy and crowded.','because','so','whereas','C',NULL,'The two places are contrasted.'),
(219,4,'MULTIPLE_CHOICE','Choose the connector: ______, preheat the oven to 180 degrees.','Firstly','However','Although','A',NULL,'It opens a sequence of steps.'),
(220,1,'MULTIPLE_CHOICE','Choose the connector: The road was closed, ______ the buses took a longer route.','although','so','but','B','C','The longer route is the consequence of the closure.'),
(220,2,'MULTIPLE_CHOICE','Choose the connector: Online shopping saves time. ______, it can lead to impulse buying.','Therefore','For example','However','C','A','Impulse buying is a drawback, so the relationship is contrast.'),
(220,3,'MULTIPLE_CHOICE','Choose the connector: You will not pass ______ you revise regularly.','unless','because','whereas','A',NULL,'It states a condition.'),
(220,4,'MULTIPLE_CHOICE','Choose the connector: She was tired. ______, she finished the report.','Therefore','Nevertheless','Similarly','B',NULL,'She finished despite her tiredness.');

-- PARAPHRASE_TRAP
INSERT INTO trap_practice_items VALUES
(221,1,'MULTIPLE_CHOICE','Original: “Only a handful of applicants had previous experience.” Which sentence keeps the meaning?','No applicants had experience.','Most applicants had experience.','Very few applicants had experience.','C','B','“A handful” means a small number, not most.'),
(221,2,'MULTIPLE_CHOICE','Original: “Prices will probably increase next year.” Which sentence keeps the meaning?','Prices are likely to rise next year.','Prices are certain to increase.','Prices will fall next year.','A','B','“Probably” is weaker than “certain”.'),
(221,3,'MULTIPLE_CHOICE','Original: “The shop closes at six.” Which sentence keeps the meaning?','The shop opens at six.','The shop shuts at 6:00.','The shop closes at sixteen.','B',NULL,'“Shuts” is a synonym for “closes”.'),
(221,4,'MULTIPLE_CHOICE','Original: “She is fond of classical music.” Which sentence keeps the meaning?','She dislikes classical music.','She teaches classical music.','She enjoys classical music.','C',NULL,'“Fond of” means likes.'),
(222,1,'MULTIPLE_CHOICE','Original: “The theatre was almost full.” Which sentence keeps the meaning?','Nearly all the seats were taken.','Half the seats were empty.','The theatre was completely full.','A','C','“Almost” is not the same as completely.'),
(222,2,'MULTIPLE_CHOICE','Original: “The company rarely hires graduates.” Which sentence keeps the meaning?','The company never hires graduates.','The company seldom hires graduates.','The company often hires graduates.','B','A','“Rarely” means seldom, not never.'),
(222,3,'MULTIPLE_CHOICE','Original: “The report was published last month.” Which sentence keeps the meaning?','It was never printed.','It will come out next month.','It came out the previous month.','C',NULL,'“Last month” equals the previous month.'),
(222,4,'MULTIPLE_CHOICE','Original: “Tom was reluctant to move house.” Which sentence keeps the meaning?','Tom did not want to move house.','Tom was eager to move house.','Tom had already moved house.','A',NULL,'“Reluctant” means unwilling.'),
(223,1,'MULTIPLE_CHOICE','Original: “Many residents objected to the plan.” Which sentence keeps the meaning?','All residents objected to the plan.','A large number of residents opposed the plan.','Residents welcomed the plan.','B','A','“Many” does not mean all.'),
(223,2,'MULTIPLE_CHOICE','Original: “The experiment took roughly two hours.” Which sentence keeps the meaning?','The experiment took exactly two hours.','The experiment took over a day.','The experiment took about two hours.','C','A','“Roughly” signals an approximate time.'),
(223,3,'MULTIPLE_CHOICE','Original: “The course is suitable for beginners.” Which sentence keeps the meaning?','Newcomers can take the course.','Only experts can join.','The course has been cancelled.','A',NULL,'“Beginners” and “newcomers” are close in meaning.'),
(223,4,'MULTIPLE_CHOICE','Original: “The lecture lasted forty minutes.” Which sentence keeps the meaning?','The talk began after forty minutes.','The talk went on for forty minutes.','The talk was forty miles away.','B',NULL,'“Lasted” and “went on for” match.'),
(224,1,'MULTIPLE_CHOICE','Original: “The scheme could reduce costs.” Which sentence keeps the meaning?','The scheme will definitely cut costs.','The scheme increases costs.','The scheme may lower costs.','C','A','“Could” expresses possibility, not certainty.'),
(224,2,'MULTIPLE_CHOICE','Original: “A few trains were delayed.” Which sentence keeps the meaning?','A small number of trains ran late.','All trains were delayed.','No trains ran.','A','B','“A few” means a small number.'),
(224,3,'MULTIPLE_CHOICE','Original: “Fuel prices have gone up.” Which sentence keeps the meaning?','Fuel has become cheaper.','Fuel has become more expensive.','Fuel is no longer sold.','B',NULL,'“Gone up” means risen.'),
(224,4,'MULTIPLE_CHOICE','Original: “He apologised for being late.” Which sentence keeps the meaning?','He refused to explain.','He blamed others for being early.','He said sorry for arriving late.','C',NULL,'“Apologised” means said sorry.');

-- TFNG_FALSE_AS_NG
INSERT INTO trap_practice_items VALUES
(225,1,'TRUE_FALSE_NOT_GIVEN','Passage: The ferry from Harbour Point leaves at 07:30 and 09:00. Statement: The first ferry leaves at 08:00.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(225,2,'TRUE_FALSE_NOT_GIVEN','Passage: The city museum opened in 1998 and has three floors. Statement: The museum has five floors.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(225,3,'TRUE_FALSE_NOT_GIVEN','Passage: The community centre offers yoga on Tuesdays and Thursdays. Statement: Yoga classes are held twice a week.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(225,4,'TRUE_FALSE_NOT_GIVEN','Passage: The glacier has retreated by 300 metres since 1990. Statement: The glacier has grown since 1990.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.'),
(226,1,'TRUE_FALSE_NOT_GIVEN','Passage: The Elm Street pool is open from 06:00 to 21:00 on weekdays. Statement: The pool closes at 19:00 on weekdays.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(226,2,'TRUE_FALSE_NOT_GIVEN','Passage: The coastal path is 18 kilometres long and takes about six hours to walk. Statement: The path is 25 kilometres long.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(226,3,'TRUE_FALSE_NOT_GIVEN','Passage: The new tunnel is 3.5 kilometres long and opened in March. Statement: The tunnel opened in March.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(226,4,'TRUE_FALSE_NOT_GIVEN','Passage: The tram line opened in 2014 and carries 20,000 passengers daily. Statement: The tram line opened in 2010.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.'),
(227,1,'TRUE_FALSE_NOT_GIVEN','Passage: Oakfield School was founded in 1952 and has 640 pupils. Statement: The school has about 400 pupils.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(227,2,'TRUE_FALSE_NOT_GIVEN','Passage: The night bus runs every 30 minutes between midnight and 05:00. Statement: The night bus runs every 15 minutes.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(227,3,'TRUE_FALSE_NOT_GIVEN','Passage: The bakery on Mill Road sells about 300 loaves each morning. Statement: The bakery sells hundreds of loaves each morning.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(227,4,'TRUE_FALSE_NOT_GIVEN','Passage: The local choir meets every Wednesday evening at the church hall. Statement: The choir meets on Saturday mornings.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.'),
(228,1,'TRUE_FALSE_NOT_GIVEN','Passage: The botanical garden has 4,000 plant species, including 200 orchids. Statement: The garden grows 20 kinds of orchid.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(228,2,'TRUE_FALSE_NOT_GIVEN','Passage: The library lends books for three weeks and charges 20 pence for each late day. Statement: Books can be borrowed for two weeks.','TRUE','FALSE','NOT GIVEN','FALSE','NOT_GIVEN','The passage gives the opposite fact, so the answer is False. Not Given is for information the passage omits.'),
(228,3,'TRUE_FALSE_NOT_GIVEN','Passage: The conference will take place in Lisbon from 4 to 6 June. Statement: The conference lasts three days.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(228,4,'TRUE_FALSE_NOT_GIVEN','Passage: The bridge was closed for repairs for eight months in 2019. Statement: The bridge was closed in 2021.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.');

-- TFNG_NG_AS_FALSE
INSERT INTO trap_practice_items VALUES
(229,1,'TRUE_FALSE_NOT_GIVEN','Passage: The community centre offers yoga on Tuesdays and Thursdays. Statement: Yoga mats are provided free.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(229,2,'TRUE_FALSE_NOT_GIVEN','Passage: The new tunnel is 3.5 kilometres long and opened in March. Statement: The tunnel cost 200 million pounds.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(229,3,'TRUE_FALSE_NOT_GIVEN','Passage: The glacier has retreated by 300 metres since 1990. Statement: The glacier has shrunk since 1990.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(229,4,'TRUE_FALSE_NOT_GIVEN','Passage: The ferry from Harbour Point leaves at 07:30 and 09:00. Statement: The first ferry leaves at 08:00.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.'),
(230,1,'TRUE_FALSE_NOT_GIVEN','Passage: The bakery on Mill Road sells about 300 loaves each morning. Statement: The bakery delivers to local cafés.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(230,2,'TRUE_FALSE_NOT_GIVEN','Passage: The conference will take place in Lisbon from 4 to 6 June. Statement: Lunch is included in the registration fee.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(230,3,'TRUE_FALSE_NOT_GIVEN','Passage: The tram line opened in 2014 and carries 20,000 passengers daily. Statement: The tram line opened in 2014.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(230,4,'TRUE_FALSE_NOT_GIVEN','Passage: The city museum opened in 1998 and has three floors. Statement: The museum has five floors.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.'),
(231,1,'TRUE_FALSE_NOT_GIVEN','Passage: Visitors to the observatory must book at least two days in advance. Statement: The observatory has a gift shop.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(231,2,'TRUE_FALSE_NOT_GIVEN','Passage: The football club was formed in 1905 by factory workers. Statement: The club has won a national trophy.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(231,3,'TRUE_FALSE_NOT_GIVEN','Passage: The local choir meets every Wednesday evening at the church hall. Statement: The choir meets in the evening.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(231,4,'TRUE_FALSE_NOT_GIVEN','Passage: The Elm Street pool is open from 06:00 to 21:00 on weekdays. Statement: The pool closes at 19:00 on weekdays.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.'),
(232,1,'TRUE_FALSE_NOT_GIVEN','Passage: The orchestra has 70 members and performs monthly at the town hall. Statement: The orchestra tours abroad each year.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(232,2,'TRUE_FALSE_NOT_GIVEN','Passage: Parking at the hospital costs £2 per hour, with a daily maximum of £10. Statement: Disabled visitors park for free.','TRUE','FALSE','NOT GIVEN','NOT_GIVEN','FALSE','The passage says nothing about this, so the answer is Not Given. False needs a statement that contradicts the passage.'),
(232,3,'TRUE_FALSE_NOT_GIVEN','Passage: The bridge was closed for repairs for eight months in 2019. Statement: The bridge was closed in 2019.','TRUE','FALSE','NOT GIVEN','TRUE',NULL,'The passage supports this statement.'),
(232,4,'TRUE_FALSE_NOT_GIVEN','Passage: The coastal path is 18 kilometres long and takes about six hours to walk. Statement: The path is 25 kilometres long.','TRUE','FALSE','NOT GIVEN','FALSE',NULL,'The passage contradicts this statement.');

-- WRONG_PARAGRAPH
INSERT INTO trap_practice_items VALUES
(233,1,'MULTIPLE_CHOICE','Paragraph A introduces a cycling festival. Paragraph B describes the route. Paragraph C lists the entry fees. Which paragraph gives the entry fees?','Paragraph A','Paragraph B','Paragraph C','C','A','The fees are listed in Paragraph C; Paragraph A only introduces the festival.'),
(233,2,'MULTIPLE_CHOICE','Paragraph A explains why a library was built. Paragraph B gives its opening hours. Paragraph C describes its children’s section. Where are the opening hours?','Paragraph B','Paragraph A','Paragraph C','A','B','The hours appear in Paragraph B.'),
(233,3,'MULTIPLE_CHOICE','Paragraph A introduces a charity run. Paragraph B gives the start time. Paragraph C lists the prizes. Which paragraph introduces the event?','Paragraph B','Paragraph A','Paragraph C','B',NULL,'Paragraph A is the introduction.'),
(233,4,'MULTIPLE_CHOICE','Paragraph A explains a recycling plan. Paragraph B gives the collection day. Paragraph C lists accepted materials. Which paragraph names the accepted materials?','Paragraph A','Paragraph B','Paragraph C','C',NULL,'The materials are listed in Paragraph C.'),
(234,1,'MULTIPLE_CHOICE','Paragraph A outlines a hospital merger. Paragraph B reports staff numbers. Paragraph C explains how patients were affected. Which paragraph gives the staff numbers?','Paragraph B','Paragraph A','Paragraph C','A','C','Staff numbers are in Paragraph B, not in the discussion of patients.'),
(234,2,'MULTIPLE_CHOICE','Paragraph A describes a river’s wildlife. Paragraph B reports pollution levels in 2020. Paragraph C describes clean-up plans. Where are the pollution levels?','Paragraph A','Paragraph B','Paragraph C','B','C','The 2020 pollution levels are reported in Paragraph B.'),
(234,3,'MULTIPLE_CHOICE','Paragraph A describes a harbour. Paragraph B gives sailing times. Paragraph C warns about weather. Which paragraph gives sailing times?','Paragraph A','Paragraph C','Paragraph B','C',NULL,'Sailing times are in Paragraph B.'),
(234,4,'MULTIPLE_CHOICE','Paragraph A introduces a language school. Paragraph B lists its courses. Paragraph C gives its address. Where is the address?','Paragraph C','Paragraph B','Paragraph A','A',NULL,'The address is in Paragraph C.'),
(235,1,'MULTIPLE_CHOICE','Paragraph A introduces a city’s bike-share scheme. Paragraph B says the scheme began in 2017. Paragraph C describes planned expansion. Which paragraph gives the start year?','Paragraph A','Paragraph B','Paragraph C','B','A','The year is in Paragraph B; Paragraph A is only an introduction.'),
(235,2,'MULTIPLE_CHOICE','Paragraph A covers a school’s history. Paragraph B describes its new science wing. Paragraph C says the wing cost £2 million. Which paragraph states the cost?','Paragraph A','Paragraph B','Paragraph C','C','B','The cost appears in Paragraph C, not in the description of the wing.'),
(235,3,'MULTIPLE_CHOICE','Paragraph A describes a park. Paragraph B explains its history. Paragraph C lists its opening hours. Which paragraph gives the history?','Paragraph B','Paragraph A','Paragraph C','A',NULL,'The history is in Paragraph B.'),
(235,4,'MULTIPLE_CHOICE','Paragraph A gives the aim of a study. Paragraph B describes the method. Paragraph C presents the results. Which paragraph presents the results?','Paragraph A','Paragraph C','Paragraph B','B',NULL,'The results are in Paragraph C.'),
(236,1,'MULTIPLE_CHOICE','Paragraph A summarizes a survey on commuting. Paragraph B explains how respondents were chosen. Paragraph C reports that most people travel by train. Where is the most common transport mode?','Paragraph A','Paragraph B','Paragraph C','C','A','The finding is in Paragraph C; Paragraph A only summarizes the survey.'),
(236,2,'MULTIPLE_CHOICE','Paragraph A describes a mountain village. Paragraph B says winters last five months. Paragraph C describes summer festivals. Which paragraph gives the length of winter?','Paragraph B','Paragraph A','Paragraph C','A','C','Winter length is stated in Paragraph B.'),
(236,3,'MULTIPLE_CHOICE','Paragraph A introduces a theatre. Paragraph B lists ticket prices. Paragraph C describes seating. Which paragraph gives ticket prices?','Paragraph A','Paragraph B','Paragraph C','B',NULL,'Ticket prices are in Paragraph B.'),
(236,4,'MULTIPLE_CHOICE','Paragraph A explains why a bridge was needed. Paragraph B describes its construction. Paragraph C describes its opening. Which paragraph describes the opening?','Paragraph A','Paragraph B','Paragraph C','C',NULL,'The opening is described in Paragraph C.');

-- WRONG_SPEAKER
INSERT INTO trap_practice_items VALUES
(237,1,'MULTIPLE_CHOICE','Ana wants to hold the sale in March. Ben prefers April. Who prefers April?','Ben','Ana','Both speakers','A','B','Ben states the preference for April.'),
(237,2,'MULTIPLE_CHOICE','Cora says the deadline is too tight. Dan thinks it is fair. Who thinks the deadline is fair?','Cora','Dan','Neither speaker','B','A','Dan accepts the deadline; Cora objects.'),
(237,3,'MULTIPLE_CHOICE','A caller asks about parking. The receptionist explains the car park closes at 6. Who gives the closing time?','The caller','A security guard','The receptionist','C',NULL,'The receptionist gives the time.'),
(237,4,'MULTIPLE_CHOICE','A customer complains about a late delivery. The assistant apologises. Who apologises?','The assistant','The customer','The driver','A',NULL,'The assistant apologises.'),
(238,1,'MULTIPLE_CHOICE','Mei proposes moving the meeting online. Raj insists on meeting in person. Who proposes meeting online?','Raj','Mei','Both speakers','B','A','Mei makes the online proposal.'),
(238,2,'MULTIPLE_CHOICE','The lecturer explains the results. A student asks about the sample size. Who asks about the sample size?','The researcher','The lecturer','The student','C','B','The student asks the question.'),
(238,3,'MULTIPLE_CHOICE','Tom suggests a picnic. Priya checks the weather forecast. Who suggests a picnic?','Tom','Priya','Both speakers','A',NULL,'Tom makes the suggestion.'),
(238,4,'MULTIPLE_CHOICE','The tutor sets the essay topic. The student asks for an extension. Who asks for an extension?','The tutor','The student','A classmate','B',NULL,'The student makes the request.'),
(239,1,'MULTIPLE_CHOICE','Hugo suggests booking the coach now. Ines says prices will fall later. Who thinks prices will fall?','Hugo','Both speakers','Ines','C','A','Ines predicts lower prices.'),
(239,2,'MULTIPLE_CHOICE','A manager announces a new rota. An employee points out that it clashes with evening classes. Who raises the problem?','The employee','A colleague','The manager','A','C','The employee raises the clash.'),
(239,3,'MULTIPLE_CHOICE','An interviewer asks about previous jobs. The candidate describes a role in retail. Who describes a retail role?','The interviewer','The candidate','A referee','B',NULL,'The candidate describes the role.'),
(239,4,'MULTIPLE_CHOICE','A doctor explains the treatment. The patient asks about side effects. Who asks about side effects?','The doctor','The nurse','The patient','C',NULL,'The patient asks.'),
(240,1,'MULTIPLE_CHOICE','Lena wants to study abroad. Her father worries about the cost. Who is worried about cost?','Her father','Lena','Her tutor','A','B','Her father raises the cost concern.'),
(240,2,'MULTIPLE_CHOICE','The guide recommends the morning tour. A visitor prefers the afternoon one. Who prefers the afternoon tour?','The guide','The visitor','Both speakers','B','A','The visitor states the preference.'),
(240,3,'MULTIPLE_CHOICE','Jo offers to book the hotel. Sam will arrange the transport. Who will arrange the transport?','Both speakers','Jo','Sam','C',NULL,'Sam takes on the transport.'),
(240,4,'MULTIPLE_CHOICE','A presenter introduces the speaker. The speaker thanks the audience. Who thanks the audience?','The speaker','The presenter','The chairperson','A',NULL,'The speaker gives the thanks.');

-- SCOPE_OVERGENERALISED
INSERT INTO trap_practice_items VALUES
(241,1,'MULTIPLE_CHOICE','Passage: “Some commuters now work from home two days a week.” Which statement is supported?','All commuters work from home.','Some commuters work from home part of the week.','Nobody commutes any more.','B','A','“Some” limits the claim; “all” goes beyond it.'),
(241,2,'MULTIPLE_CHOICE','Passage: “In this study, teenagers who played chess scored higher in maths.” Which statement matches?','All chess players are good at maths.','Chess improves exam results everywhere.','In this study, teenage chess players scored higher in maths.','C','A','The result is limited to this study’s teenagers.'),
(241,3,'MULTIPLE_CHOICE','Passage: “The reservoir supplies water to three towns.” How many towns does it supply?','Three','Two','Four','A',NULL,'It supplies three towns.'),
(241,4,'MULTIPLE_CHOICE','Passage: “The scheme was introduced in 2015.” When was it introduced?','2005','2015','2025','B',NULL,'The year is 2015.'),
(242,1,'MULTIPLE_CHOICE','Passage: “Many cafés in the city centre now accept card payments only.” Which statement is supported?','Every café in the country is cashless.','Cash is illegal.','Many city-centre cafés accept cards only.','C','A','“Many … in the city centre” is narrower than “every … in the country”.'),
(242,2,'MULTIPLE_CHOICE','Passage: “Early morning flights are often cheaper.” Which statement is supported?','Early flights are often cheaper.','Early flights are always cheaper.','Evening flights do not exist.','A','B','“Often” is not “always”.'),
(242,3,'MULTIPLE_CHOICE','Passage: “The survey asked parents about school meals.” Whom did the survey ask?','Pupils','Parents','Teachers','B',NULL,'It asked parents.'),
(242,4,'MULTIPLE_CHOICE','Passage: “The festival takes place every other year.” How often is the festival held?','Every year','Every month','Every second year','C',NULL,'“Every other year” means every second year.'),
(243,1,'MULTIPLE_CHOICE','Passage: “The new method worked well in the trial at three schools.” Which statement is supported?','The method worked well at the three trial schools.','The method will work in every school.','Schools dislike new methods.','A','B','The result covers the three trial schools only.'),
(243,2,'MULTIPLE_CHOICE','Passage: “A number of tourists complained about the queues.” Which statement is supported?','Every tourist complained.','Some tourists complained about queues.','The queues were short.','B','A','“A number of” means some, not every.'),
(243,3,'MULTIPLE_CHOICE','Passage: “Entry is free for under-fives.” Who gets free entry?','Over-sixties','Students','Under-fives','C',NULL,'Free entry applies to under-fives.'),
(243,4,'MULTIPLE_CHOICE','Passage: “The trial lasted six months.” How long did it last?','Six months','Six weeks','Six years','A',NULL,'It lasted six months.'),
(244,1,'MULTIPLE_CHOICE','Passage: “Most shops on the high street stay open until 8 p.m.” Which statement is supported?','Every shop on the high street closes at 8 p.m.','Most high-street shops stay open until 8 p.m.','Shops open at midnight.','B','A','“Most” leaves room for exceptions.'),
(244,2,'MULTIPLE_CHOICE','Passage: “In some regions, rainfall has increased over the past decade.” Which statement is supported?','Rainfall has increased everywhere.','Deserts are growing.','Rainfall has increased in some regions.','C','A','“In some regions” limits the scope.'),
(244,3,'MULTIPLE_CHOICE','Passage: “The council plans to plant 500 trees.” How many trees will be planted?','500','150','50','A',NULL,'The plan is for 500 trees.'),
(244,4,'MULTIPLE_CHOICE','Passage: “Lessons are taught in small groups.” How are lessons taught?','Online only','In small groups','In large halls','B',NULL,'They are taught in small groups.');

-- NUMBER_FORMAT
INSERT INTO trap_practice_items VALUES
(245,1,'MULTIPLE_CHOICE','The speaker says: “The deposit is thirty pounds.” How much is the deposit?','£13','£3','£30','C','A','“Thirty” is 30; “thirteen” would be 13.'),
(245,2,'MULTIPLE_CHOICE','The speaker says: “The ticket costs fifty pounds.” How much is it?','£50','£5','£15','A','C','“Fifty” is 50; “fifteen” is 15.'),
(245,3,'MULTIPLE_CHOICE','The speaker says: “There are twenty-two seats.” How many seats are there?','12','22','32','B',NULL,'The number is twenty-two.'),
(245,4,'MULTIPLE_CHOICE','The speaker says: “The party starts on the third of May.” When does it start?','30 May','13 May','3 May','C',NULL,'“The third” is the 3rd.'),
(246,1,'MULTIPLE_CHOICE','The speaker says: “The class is forty minutes long.” How long is it?','40 minutes','14 minutes','4 minutes','A','B','“Forty” is 40; “fourteen” is 14.'),
(246,2,'MULTIPLE_CHOICE','The speaker says: “The coach has sixty seats.” How many seats are there?','16','60','6','B','A','“Sixty” is 60; “sixteen” is 16.'),
(246,3,'MULTIPLE_CHOICE','The speaker says: “The room number is two-oh-five.” Which room is it?','250','25','205','C',NULL,'Two-oh-five is 205.'),
(246,4,'MULTIPLE_CHOICE','The speaker says: “It costs two pounds fifty.” How much is it?','£2.50','£25','£2.15','A',NULL,'Two pounds fifty is £2.50.'),
(247,1,'MULTIPLE_CHOICE','The speaker says: “The jacket costs seventy dollars.” How much is it?','$7','$70','$17','B','C','“Seventy” is 70; “seventeen” is 17.'),
(247,2,'MULTIPLE_CHOICE','The speaker says: “Eighty people attended.” How many attended?','8','18','80','C','B','“Eighty” is 80; “eighteen” is 18.'),
(247,3,'MULTIPLE_CHOICE','The speaker says: “Flight six-four-one.” Which flight is it?','641','416','614','A',NULL,'Six-four-one is 641.'),
(247,4,'MULTIPLE_CHOICE','The speaker says: “It is a hundred and twenty miles away.” How far is it?','102 miles','120 miles','1,200 miles','B',NULL,'A hundred and twenty is 120.'),
(248,1,'MULTIPLE_CHOICE','The speaker says: “The hotel is ninety metres from the station.” How far is it?','19 metres','9 metres','90 metres','C','A','“Ninety” is 90; “nineteen” is 19.'),
(248,2,'MULTIPLE_CHOICE','The speaker says: “The train leaves at twelve fifteen.” When does it leave?','12:15','12:50','15:12','A','B','“Fifteen” is 15, not fifty.'),
(248,3,'MULTIPLE_CHOICE','The speaker says: “The meeting is at half past three.” When is it?','2:30','3:30','3:15','B',NULL,'Half past three is 3:30.'),
(248,4,'MULTIPLE_CHOICE','The speaker says: “It was built in nineteen ninety-eight.” When was it built?','1989','1908','1998','C',NULL,'Nineteen ninety-eight is 1998.');

-- WORD_CHOICE
INSERT INTO trap_practice_items VALUES
(249,1,'MULTIPLE_CHOICE','Heavy rain ______ the match, so it was played the next day.','postponed','cancelled','finished','A','B','The match was still played, so it was postponed, not cancelled.'),
(249,2,'MULTIPLE_CHOICE','The scientist’s results were ______; other labs repeated them and got the same numbers.','famous','reliable','expensive','B','A','Repeating the numbers shows reliability, not fame.'),
(249,3,'MULTIPLE_CHOICE','The instructions were so ______ that anyone could follow them.','quiet','pale','clear','C',NULL,'Instructions that anyone can follow are clear.'),
(249,4,'MULTIPLE_CHOICE','She was ______ to hear that the exam had been postponed.','relieved','relief','relieving','A',NULL,'The adjective “relieved” describes how she felt.'),
(250,1,'MULTIPLE_CHOICE','The new law will ______ the number of cars in the city centre.','lose','reduce','break','B','A','“Reduce” fits a number; “lose” does not.'),
(250,2,'MULTIPLE_CHOICE','Students who live abroad often gain a greater ______ of other cultures.','remembering','knowing','awareness','C','B','“Awareness” is the noun that fits; “knowing” is not used this way.'),
(250,3,'MULTIPLE_CHOICE','The film received ______ reviews from critics.','positive','fortunate','smooth','A',NULL,'Reviews are described as positive or negative.'),
(250,4,'MULTIPLE_CHOICE','We need to ______ the problem before it gets worse.','bake','address','float','B',NULL,'“Address” means deal with.'),
(251,1,'MULTIPLE_CHOICE','The company needs to ______ more staff to meet demand.','accept','lend','recruit','C','A','“Recruit” is the standard word for taking on staff.'),
(251,2,'MULTIPLE_CHOICE','Her essay was ______, covering every point in the question clearly.','thorough','thick','heavy','A','B','“Thorough” means complete and careful; “thick” describes physical width.'),
(251,3,'MULTIPLE_CHOICE','He is ______ about starting his new job next week.','enthusiasm','enthusiastic','enthusiastically','B',NULL,'An adjective is needed after “is”.'),
(251,4,'MULTIPLE_CHOICE','The speaker gave a ______ account of the accident, with every detail included.','detailing','detail','detailed','C',NULL,'“Detailed” is the adjective that fits.'),
(252,1,'MULTIPLE_CHOICE','The two reports ______ on the main cause of the problem.','differ','separate','oppose','A','B','“Differ on” is the natural pattern for reports that disagree.'),
(252,2,'MULTIPLE_CHOICE','Many tourists find the old town ______, with its narrow streets and historic buildings.','charged','charming','chosen','B','A','“Charging” sounds similar but means something else.'),
(252,3,'MULTIPLE_CHOICE','Prices ______ sharply during the holiday season.','arise','raise','rise','C',NULL,'“Rise” does not need an object.'),
(252,4,'MULTIPLE_CHOICE','The manager decided to ______ the meeting until Monday.','postpone','preserve','prevent','A',NULL,'“Postpone” means delay.');

-- COLLOCATION
INSERT INTO trap_practice_items VALUES
(253,1,'MULTIPLE_CHOICE','Scientists need to ______ more research into the disease.','make','do','hold','B','A','We “do research”; we do not “make research”.'),
(253,2,'MULTIPLE_CHOICE','The company has ______ great progress this year.','taken','done','made','C','B','The natural collocation is “make progress”.'),
(253,3,'MULTIPLE_CHOICE','I have to ______ my homework tonight.','do','make','take','A',NULL,'We “do homework”.'),
(253,4,'MULTIPLE_CHOICE','Please ______ a decision by Friday.','do','make','give','B',NULL,'We “make a decision”.'),
(254,1,'MULTIPLE_CHOICE','There was ______ traffic on the motorway this morning.','strong','big','heavy','C','A','Traffic is “heavy”, not “strong”.'),
(254,2,'MULTIPLE_CHOICE','Students should ______ attention to the instructions.','pay','spend','lend','A','B','The set phrase is “pay attention”.'),
(254,3,'MULTIPLE_CHOICE','She ______ a good impression at the interview.','did','made','put','B',NULL,'We “make an impression”.'),
(254,4,'MULTIPLE_CHOICE','They had a ______ argument about money.','sharpened','tall','fierce','C',NULL,'An argument can be fierce.'),
(255,1,'MULTIPLE_CHOICE','Young people are often willing to ______ risks.','take','hold','do','A','C','We “take risks”.'),
(255,2,'MULTIPLE_CHOICE','The government plans to ______ a ban on single-use plastics.','make','impose','carry','B','A','A ban is “imposed”, not “made”.'),
(255,3,'MULTIPLE_CHOICE','He ______ a lot of effort into his studies.','made','did','put','C',NULL,'We “put effort into” something.'),
(255,4,'MULTIPLE_CHOICE','The film is ______ to be released next month.','expected','wished','waited','A',NULL,'“Is expected to” is the natural pattern.'),
(256,1,'MULTIPLE_CHOICE','It is hard to ______ a balance between work and family life.','knock','strike','kick','B','A','The set phrase is “strike a balance”.'),
(256,2,'MULTIPLE_CHOICE','The news came as a ______ surprise to everyone.','wide','full','complete','C','B','“Complete surprise” is natural; “full surprise” is not.'),
(256,3,'MULTIPLE_CHOICE','We ______ our best to finish on time.','did','made','took','A',NULL,'We “do our best”.'),
(256,4,'MULTIPLE_CHOICE','The city has ______ a lot of changes in recent years.','overcome','undergone','undertaken','B',NULL,'Cities “undergo changes”.');

-- UNDERDEVELOPED_IDEA
INSERT INTO trap_practice_items VALUES
(257,1,'MULTIPLE_CHOICE','Which sentence develops the idea “Public libraries are still important” best?','Public libraries are still important, and everyone should agree with this.','Libraries have many books.','Public libraries are still important because they give free internet access to people who cannot afford it at home.','C','A','The best sentence adds a reason. Option A only repeats the claim.'),
(257,2,'MULTIPLE_CHOICE','Which sentence develops the idea “School uniforms should be abolished” best?','Uniforms should be abolished because they limit self-expression and can be costly for large families.','Uniforms are worn at school.','School uniforms should be abolished, as I said before.','A','C','A reason and a consequence support the claim.'),
(257,3,'MULTIPLE_CHOICE','Which sentence gives a specific example to support: “Exercise is good for mental health”?','Exercise is good.','For example, a 2019 study found that adults who walked 30 minutes daily reported lower stress.','Many people exercise.','B',NULL,'A study with a figure is a specific example.'),
(257,4,'MULTIPLE_CHOICE','Which is the best topic sentence for a paragraph about the benefits of volunteering?','Volunteers wear badges.','I volunteered last year.','Volunteering gives young people practical experience and a sense of purpose.','C',NULL,'It states a clear, arguable idea.'),
(258,1,'MULTIPLE_CHOICE','Which sentence develops the idea “Remote work benefits employees” best?','Remote work saves commuting time, which gives employees more hours for family and rest.','Remote work is good for employees, which is why it is beneficial.','Many people work.','A','B','Option B only restates the claim in different words.'),
(258,2,'MULTIPLE_CHOICE','Which sentence develops the idea “Cities need more cycle lanes” best?','More cycle lanes are definitely needed.','Cycle lanes make cycling safer, which persuades more residents to leave their cars at home.','Cycle lanes are painted lines.','B','A','The second sentence explains why more lanes help.'),
(258,3,'MULTIPLE_CHOICE','Which sentence best explains WHY cities should plant more trees?','Trees are green.','Cities have buildings.','Trees lower summer temperatures by providing shade and cooling the air.','C',NULL,'It gives a reason.'),
(258,4,'MULTIPLE_CHOICE','Which sentence adds a relevant consequence to: “Many teenagers sleep too little.”?','As a result, their concentration in lessons often suffers.','Sleep is a word.','Teenagers like phones.','A',NULL,'It links the cause to a result.'),
(259,1,'MULTIPLE_CHOICE','Which sentence develops the idea “Fast food should be taxed” best?','Fast food should be taxed, and the government should do it.','Fast food should be taxed because a tax would raise prices and encourage people to choose healthier meals, reducing health costs.','Fast food is sold in restaurants.','B','A','A reason and an effect develop the idea.'),
(259,2,'MULTIPLE_CHOICE','Which sentence develops the idea “Online learning is flexible” best?','Online learning is flexible in many ways.','Students use laptops.','Online learning is flexible because students can watch lessons at any time, which helps those with part-time jobs.','C','A','A specific reason and example support the claim.'),
(259,3,'MULTIPLE_CHOICE','Which sentence best concludes an essay arguing for shorter working weeks?','In conclusion, a shorter working week would benefit both employees and employers.','Work is hard.','The sky is blue.','A',NULL,'It sums up the argument.'),
(259,4,'MULTIPLE_CHOICE','Which sentence provides evidence for “Public transport reduces pollution”?','Public transport exists.','Cities with extensive metro networks report lower levels of traffic-related emissions.','Pollution is bad.','B',NULL,'It cites a pattern that supports the claim.'),
(260,1,'MULTIPLE_CHOICE','Which sentence develops the idea “Museums should be free” best?','Museums have old objects.','Museums should be free for everyone, in my opinion.','Free entry would let low-income families visit, so more children would learn from the collections.','C','B','Option B states an opinion without any reason.'),
(260,2,'MULTIPLE_CHOICE','Which sentence develops the idea “Plastic bags should be banned” best?','A ban would reduce litter in rivers, protecting the fish that many coastal communities rely on.','Plastic bags should be banned, because banning them is the right thing to do.','Plastic is a material.','A','B','The second sentence gives a concrete effect.'),
(260,3,'MULTIPLE_CHOICE','Which is the most relevant supporting detail for “Online shopping has changed high streets”?','Online shopping uses the internet.','Many town centres have lost shops as customers buy goods online instead.','High streets have names.','B',NULL,'It shows the change.'),
(260,4,'MULTIPLE_CHOICE','Which sentence best introduces a counter-argument?','Tourism is a word.','Culture is old.','Some people argue that tourism damages local culture; however, it also creates jobs.','C',NULL,'It states an opposing view and responds to it.');

CREATE TEMP TABLE trap_seed_packages ON COMMIT DROP AS
SELECT t.*,
       ('81000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS package_id,
       ('82000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS package_version_id,
       ('83000000-0000-4000-8000-' || lpad(t.seed_no::text, 12, '0'))::uuid AS section_id
FROM trap_practice_targets t;

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key)
SELECT package_id, 'PS-TRAP-' || error_code || '-' || set_no, package_title, 'PRACTICE_SET', 'PUBLISHED', NULL
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
