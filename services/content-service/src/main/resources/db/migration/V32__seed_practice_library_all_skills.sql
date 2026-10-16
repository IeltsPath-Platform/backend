-- Practice test library data for every skill: more Reading tests with new question formats (matching headings, summary
-- completion), plus Listening (3 tests x 4 sections), Writing (6 tests x Task 1 and Task 2) and Speaking (6 tests x
-- Parts 1-3). Passages, transcripts, prompts and questions are original text written for this project. Listening audio
-- is referenced by object key under the media base URL (no audio files ship with the migration). Cover images are small
-- inline SVG data URIs. Every statement is idempotent.

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000001001', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjc2NmUiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001001', 'PASSAGE', 'A. The first bicycles had no pedals at all. Riders sat on a wooden frame and pushed themselves along with their feet, which made the machine fast downhill but tiring on a long road. Many people found it more of a toy than a means of travel.

B. A safer design arrived when makers fitted two wheels of equal size and a chain to drive the rear one. Riders sat lower and could stop with their feet on the ground, so the new machine was far less dangerous than earlier models.

C. At first, doctors and newspapers warned that cycling was unhealthy. Within a decade, however, the same newspapers were praising it, and ordinary people began to buy bicycles because they were cheaper to run than a horse.

D. Soon, riding for pleasure became a fashion. Clubs organised weekend trips into the countryside, and for the first time many office workers could travel beyond their own town on a day off.', NULL, NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001002', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM2ZDI4ZDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001002', 'PASSAGE', 'A. Every night, most people spend about two hours dreaming, although they usually forget almost all of it by morning. The most vivid dreams occur during a stage of sleep in which the eyes move rapidly beneath the lids.

B. One theory holds that dreaming helps the brain sort the day''s memories, strengthening useful ones and discarding the rest. Students who slept after learning a list of words, for example, remembered more of it than students who stayed awake.

C. Another theory suggests that dreams are a kind of rehearsal for danger. Many common dreams involve being chased or falling, and supporters of this idea say they prepare the mind to react quickly in real life.

D. Neither theory has been proved, and some scientists doubt that dreams serve any purpose at all. They argue that dreams may simply be a by-product of the brain''s activity during sleep.', NULL, NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001003', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiOTFjMWMiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001003', 'PASSAGE', 'A. Microplastics are fragments smaller than five millimetres, and they now turn up almost everywhere that scientists look, from mountain snow to the deepest ocean trenches. Many come from synthetic clothing, which sheds tiny fibres each time it is washed.

B. Small animals swallow these fragments by mistake, and the particles move up the food chain when larger animals eat them. Researchers have found plastic in fish sold for food, although the amounts found so far are very small.

C. The health effects on humans remain unclear. Laboratory studies show that high doses can harm cells, but the doses used are far higher than anything a person is likely to eat. Several teams are now measuring how much plastic people actually take in.

D. Meanwhile, some manufacturers have begun to fit filters to washing machines, and a few countries are considering laws that would require them. Campaigners say that reducing plastic at its source remains the only lasting solution.', NULL, NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001004', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZDRlZDgiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001004', 'PASSAGE', 'A. Tides rise and fall twice a day with remarkable regularity, which makes them far easier to predict than wind or sunshine. Engineers have long dreamed of turning this movement into electricity.

B. The simplest design is a barrage, a low dam built across a river mouth. Water flows through turbines as the tide comes in and again as it goes out. The largest barrage, in France, has produced power since 1966.

C. Barrages are costly and can change the habitats of fish and birds. For this reason, many newer projects use underwater turbines that look like small wind turbines and turn in the moving water without blocking the whole channel.

D. Supporters argue that tidal power could supply a steady share of a country''s energy. Critics reply that suitable sites are few and that the equipment must survive years of corrosion by salt water.', NULL, NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001005', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwMzY5YTEiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001005', 'AUDIO', 'Receptionist: Hello, Riverside Leisure Centre.
Caller: Hi, I would like to enrol my daughter in a swimming course.
Receptionist: Certainly. What is her name?
Caller: Emily Parker. That is P, A, R, K, E, R.
Receptionist: And how old is she?
Caller: She is nine.
Receptionist: We have a beginners class on Wednesday at four thirty. It costs forty-five pounds for six weeks.
Caller: Perfect. Can I pay by card?
Receptionist: Yes. And a contact number, please?
Caller: It is 07700 900 482.', 'listening/practice/pt-listening-01-s1.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001006', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxNTgwM2QiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001006', 'AUDIO', 'Guide: Welcome to the Greenfield Farm Park. Let me explain how the day will work.
Guide: The farm opens at nine thirty, but the animal feeding starts at eleven, so please arrive early if you want to take part.
Guide: The tractor ride is the most popular activity, and it leaves from the main barn every hour.
Guide: If you are bringing young children, the best place for a picnic is beside the lake, because it has benches and shade.
Guide: Please do not feed the animals anything you have brought yourself. Special food is on sale at the shop for two pounds a bag.
Guide: The park closes at five, and the last tractor ride leaves at four.', 'listening/practice/pt-listening-01-s2.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001007', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM3ZTIyY2UiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001007', 'AUDIO', 'Tutor: So, your group project is on renewable energy. Who will do which part?
Maya: I would like to do the research on solar power, because I studied it last year.
Jon: Then I will take wind power. I have already found some good articles.
Maya: Someone also needs to design the presentation slides.
Jon: I am not good at design. Why don''t you do the slides, Maya, and I will write the final report?
Maya: Fine. What about the survey of students?
Tutor: I suggest Jon carries it out, since he has the free time on Fridays.
Jon: OK. We should also decide when to practise together. Thursday afternoon is best for me.
Maya: Thursday works. And the deadline is the twentieth of March.', 'listening/practice/pt-listening-01-s3.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001008', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiNDUzMDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlM0PC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001008', 'AUDIO', 'Lecturer: Today I want to talk about honeybees. A single hive may contain fifty thousand bees in summer, but only one queen. The queen lays up to two thousand eggs a day. Worker bees, all of them female, collect nectar and pollen, and they also keep the hive at a steady temperature by beating their wings. To tell other bees where food is, a worker performs a waggle dance, and the angle of the dance shows the direction of the flowers. In winter, the bees form a tight cluster to stay warm and feed on the honey they stored.', 'listening/practice/pt-listening-01-s4.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001009', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiZTE4NWQiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001009', 'AUDIO', 'Receptionist: Good afternoon, Harbour View Hotel.
Caller: Hello, I would like to book a double room for three nights.
Receptionist: Certainly. What name is it under?
Caller: Delaney. D, E, L, A, N, E, Y.
Receptionist: And when will you arrive?
Caller: On the twelfth of June.
Receptionist: A double room with a sea view is ninety pounds a night.
Caller: That is fine. Does that include breakfast?
Receptionist: Yes, breakfast is served from seven until ten.', 'listening/practice/pt-listening-02-s1.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001010', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZTc0OTAiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001010', 'AUDIO', 'Guide: Welcome to the City Science Museum. We will begin on the ground floor, where the space exhibition is.
Guide: The space exhibition is the newest part of the museum, and it opened only last spring.
Guide: Please note that photography is allowed everywhere except in the planetarium.
Guide: The planetarium shows start every forty minutes, and tickets are free but must be collected at the desk.
Guide: On the second floor you will find the cafe, which is open until four o''clock.
Guide: Lockers cost one pound, and you get the coin back when you return the key.', 'listening/practice/pt-listening-02-s2.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001011', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM0ZDdjMGYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001011', 'AUDIO', 'Dr Hughes: You need to pick a research topic by Friday. Priya, have you decided?
Priya: I am interested in how social media affects sleep, and I will collect the data by interviewing students.
Tom: I would like to study noise in open-plan offices. I will use a questionnaire.
Dr Hughes: Good. Tom, you should check the university''s ethics rules before you begin.
Priya: Dr Hughes, could you recommend a book on research methods?
Dr Hughes: I will email you a reading list this afternoon.
Tom: When should we book the library study room?
Dr Hughes: The best time is Tuesday morning, as it is usually empty.', 'listening/practice/pt-listening-02-s3.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001012', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwNDc4NTciLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlM0PC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001012', 'AUDIO', 'Lecturer: A green roof is a roof covered with soil and plants. Such roofs can reduce the temperature inside a building by several degrees in summer, because the plants provide natural insulation. They also absorb rainwater, which reduces the risk of flooding in heavy storms. In some cities, roof gardens are used to grow vegetables, and they provide a habitat for insects such as bees. The main drawback is weight: the roof must be strong enough to support the soil, so the initial cost is higher.', 'listening/practice/pt-listening-02-s4.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001013', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNjMjQxMGMiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001013', 'AUDIO', 'Receptionist: FitLife Club, how can I help?
Caller: I would like to become a member. My name is Okafor, O, K, A, F, O, R.
Receptionist: And what kind of membership? We have monthly or yearly.
Caller: Yearly, please.
Receptionist: A yearly membership is two hundred and forty pounds.
Caller: Can I start next Monday?
Receptionist: Of course. And which sport are you most interested in?
Caller: Swimming, mainly.', 'listening/practice/pt-listening-03-s1.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001014', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNhMjFjYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001014', 'AUDIO', 'Organiser: Thank you all for coming. The festival will take place on the last Saturday of September.
Organiser: Last year it rained, so this year we have booked the community hall as a back-up.
Organiser: The main stage will be in the park, and the music begins at two in the afternoon.
Organiser: Volunteers are needed to help with parking. Please speak to Mrs Clark if you can help.
Organiser: Food stalls will be near the entrance, and each stall must pay a fee of fifteen pounds.
Organiser: Posters will be printed this week, and we need someone to put them up in shops.', 'listening/practice/pt-listening-03-s2.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001015', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjc2NmUiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlMzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001015', 'AUDIO', 'Mr Brown: Lena, Sam, I have read both your essays. Lena, your introduction is very clear.
Lena: Thank you. I spent a long time on the conclusion.
Mr Brown: The conclusion is too short, I am afraid. Sam, you need to check your references.
Sam: I used a lot of websites. Should I use more books?
Mr Brown: Yes, at least three books. And Lena, your grammar is excellent, but you should avoid long sentences.
Lena: I will try. When is the final version due?
Mr Brown: By next Wednesday at noon.', 'listening/practice/pt-listening-03-s3.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001016', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZTQwYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlM0PC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-020000001016', 'AUDIO', 'Lecturer: Early people told the time by the position of the sun, and the first sundials appeared about five thousand years ago. Later, water clocks measured time by the flow of water, and they worked at night as well. Mechanical clocks appeared in Europe in the fourteenth century, and they used a weight to drive the hands. In 1656 a scientist invented the pendulum clock, which was far more accurate. Today, atomic clocks lose less than one second in millions of years.', 'listening/practice/pt-listening-03-s4.mp3', 90, 'VALID'),
    ('2d000000-0000-4000-8000-010000001017', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZDRlZDgiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001018', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM3YzNhZWQiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001019', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwNDc4NTciLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001020', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiNDUzMDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001021', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiZTEyM2MiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001022', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjc2NmUiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001023', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwMzY5YTEiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001024', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNhMjFjYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001025', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNjMjQxMGMiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001026', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM0ZDdjMGYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001027', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZTQwYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001028', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM2ZDI4ZDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlQyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001029', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwMzY5YTEiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001030', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZDRlZDgiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001031', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZDRlZDgiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001032', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM3YzNhZWQiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001033', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM2ZDI4ZDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001034', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM2ZDI4ZDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001035', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiNDUzMDkiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001036', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwNDc4NTciLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001037', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwNDc4NTciLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001038', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiZTEyM2MiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001039', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjc2NmUiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001040', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjc2NmUiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001041', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNjMjQxMGMiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001042', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNhMjFjYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001043', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNhMjFjYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001044', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM0ZDdjMGYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001045', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZTQwYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000001046', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMxZTQwYWYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', NULL, 'VALID')
ON CONFLICT DO NOTHING;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000001001', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001002', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001003', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001004', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001005', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001006', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001007', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001008', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001009', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001010', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001011', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001012', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001013', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001014', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001015', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001016', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001017', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001018', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001019', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001020', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001021', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001022', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001023', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001024', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001025', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001026', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001027', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001028', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001029', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001030', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001031', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001032', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001033', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001034', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001035', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001036', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001037', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001038', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001039', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001040', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001041', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001042', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001043', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001044', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001045', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001046', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001047', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001048', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001049', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001050', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001051', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001052', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001053', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001054', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001055', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001056', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001057', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001058', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001059', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001060', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001061', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001062', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001063', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001064', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001065', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001066', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001067', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001068', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001069', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001070', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001071', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001072', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001073', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001074', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001075', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001076', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001077', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001078', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001079', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001080', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001081', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001082', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001083', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001084', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001085', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001086', 'MATCHING', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001087', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001088', 'MULTIPLE_CHOICE', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001089', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001090', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001091', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001092', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001093', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001094', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001095', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001096', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001097', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001098', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001099', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001100', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001101', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001102', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001103', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001104', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001105', 'ESSAY', 'WRITING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001106', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001107', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001108', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001109', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001110', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001111', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001112', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001113', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001114', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001115', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001116', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001117', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001118', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001119', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001120', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001121', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001122', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001123', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001124', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001125', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001126', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001127', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001128', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001129', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001130', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001131', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001132', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001133', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001134', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001135', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001136', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001137', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001138', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001139', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001140', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001141', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001142', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001143', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001144', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001145', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001146', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001147', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001148', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001149', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001150', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001151', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001152', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000001153', 'SPEAKING', 'SPEAKING', 'EXAM', 'PUBLISHED')
ON CONFLICT DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000001001', '2d000000-0000-4000-8000-030000001001', 1, 'Paragraph A', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph A describes the first pedal-less designs.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001002', '2d000000-0000-4000-8000-030000001002', 1, 'Paragraph B', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph B: equal wheels and a chain made the machine safer.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001003', '2d000000-0000-4000-8000-030000001003', 1, 'Paragraph C', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Paragraph C: doctors and newspapers changed their minds.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001004', '2d000000-0000-4000-8000-030000001004', 1, 'Paragraph D', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"E"}'::jsonb, 1, 'Paragraph D: riding became a leisure activity.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001005', '2d000000-0000-4000-8000-030000001005', 1, 'The first bicycles were pushed along with the rider''s ______.', NULL, '{"type":"FILL","accepted":["feet"]}'::jsonb, 1, 'Paragraph A: riders pushed with their feet.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001006', '2d000000-0000-4000-8000-030000001006', 1, 'Makers added a ______ to drive the rear wheel.', NULL, '{"type":"FILL","accepted":["chain"]}'::jsonb, 1, 'Paragraph B: a chain to drive the rear one.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001007', '2d000000-0000-4000-8000-030000001007', 1, 'Bicycles were cheaper to run than a ______.', NULL, '{"type":"FILL","accepted":["horse"]}'::jsonb, 1, 'Paragraph C: cheaper to run than a horse.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001008', '2d000000-0000-4000-8000-030000001008', 1, 'Clubs organised weekend trips into the ______.', NULL, '{"type":"FILL","accepted":["countryside"]}'::jsonb, 1, 'Paragraph D: weekend trips into the countryside.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001009', '2d000000-0000-4000-8000-030000001009', 1, 'Most people remember their dreams clearly in the morning.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Paragraph A: they usually forget almost all of it.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001010', '2d000000-0000-4000-8000-030000001010', 1, 'The most vivid dreams occur in a stage of sleep with rapid eye movement.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Paragraph A: the eyes move rapidly beneath the lids.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001011', '2d000000-0000-4000-8000-030000001011', 1, 'Students who slept after learning words remembered more of them.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Paragraph B: they remembered more.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001012', '2d000000-0000-4000-8000-030000001012', 1, 'Children dream more often than adults.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'Age is not discussed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001013', '2d000000-0000-4000-8000-030000001013', 1, 'What does the first theory say about dreaming?', '[{"optionKey":"A","content":"It sorts the day''s memories","sortOrder":1},{"optionKey":"B","content":"It predicts the future","sortOrder":2},{"optionKey":"C","content":"It replaces sleep","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph B: the brain sorts memories.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001014', '2d000000-0000-4000-8000-030000001014', 1, 'Which dreams are mentioned as common?', '[{"optionKey":"A","content":"Dreams of flying","sortOrder":1},{"optionKey":"B","content":"Dreams of being chased or falling","sortOrder":2},{"optionKey":"C","content":"Dreams of exams","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph C: being chased or falling.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001015', '2d000000-0000-4000-8000-030000001015', 1, 'What do some scientists believe about dreams?', '[{"optionKey":"A","content":"They are a warning from the body","sortOrder":1},{"optionKey":"B","content":"They may serve no purpose","sortOrder":2},{"optionKey":"C","content":"They are caused by food","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph D: some doubt that dreams serve any purpose.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001016', '2d000000-0000-4000-8000-030000001016', 1, 'What is said about the two theories?', '[{"optionKey":"A","content":"Both are proved","sortOrder":1},{"optionKey":"B","content":"Neither has been proved","sortOrder":2},{"optionKey":"C","content":"One has been disproved","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph D: neither has been proved.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001017', '2d000000-0000-4000-8000-030000001017', 1, 'The writer believes microplastics are found only in the sea.', '[{"optionKey":"YES","content":"YES","sortOrder":1},{"optionKey":"NO","content":"NO","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NO"}'::jsonb, 1, 'Paragraph A: they turn up from snow to ocean trenches.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001018', '2d000000-0000-4000-8000-030000001018', 1, 'The writer thinks the plastic found in fish is a serious danger to health.', '[{"optionKey":"YES","content":"YES","sortOrder":1},{"optionKey":"NO","content":"NO","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'The writer says only that amounts are small and effects unclear.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001019', '2d000000-0000-4000-8000-030000001019', 1, 'The writer suggests that laboratory doses are unrealistically high.', '[{"optionKey":"YES","content":"YES","sortOrder":1},{"optionKey":"NO","content":"NO","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"YES"}'::jsonb, 1, 'Paragraph C: far higher than anything a person is likely to eat.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001020', '2d000000-0000-4000-8000-030000001020', 1, 'Where do many microplastics come from?', '[{"optionKey":"A","content":"Car tyres only","sortOrder":1},{"optionKey":"B","content":"Synthetic clothing","sortOrder":2},{"optionKey":"C","content":"Bottled water only","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph A: synthetic clothing sheds tiny fibres.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001021', '2d000000-0000-4000-8000-030000001021', 1, 'How do particles move up the food chain?', '[{"optionKey":"A","content":"Larger animals eat smaller ones","sortOrder":1},{"optionKey":"B","content":"They are dissolved in rain","sortOrder":2},{"optionKey":"C","content":"They are carried by birds","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph B: larger animals eat the small ones.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001022', '2d000000-0000-4000-8000-030000001022', 1, 'What have some manufacturers started to fit?', '[{"optionKey":"A","content":"Filters on washing machines","sortOrder":1},{"optionKey":"B","content":"Plastic-free bottles","sortOrder":2},{"optionKey":"C","content":"Sensors on boats","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph D: filters on washing machines.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001023', '2d000000-0000-4000-8000-030000001023', 1, 'Microplastics are fragments smaller than five ______.', NULL, '{"type":"FILL","accepted":["millimetres","millimeters"]}'::jsonb, 1, 'Paragraph A: smaller than five millimetres.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001024', '2d000000-0000-4000-8000-030000001024', 1, 'Campaigners say reducing plastic at its ______ is the only lasting solution.', NULL, '{"type":"FILL","accepted":["source"]}'::jsonb, 1, 'Paragraph D: at its source.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001025', '2d000000-0000-4000-8000-030000001025', 1, 'Tides are easier to predict than wind.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Paragraph A: far easier to predict than wind or sunshine.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001026', '2d000000-0000-4000-8000-030000001026', 1, 'The French barrage began producing power before 1970.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Paragraph B: since 1966.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001027', '2d000000-0000-4000-8000-030000001027', 1, 'Underwater turbines are cheaper than barrages.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'Relative cost of turbines is not given.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001028', '2d000000-0000-4000-8000-030000001028', 1, 'Paragraph A', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph A introduces the idea of predictable tides.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001029', '2d000000-0000-4000-8000-030000001029', 1, 'Paragraph B', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph B describes a simple barrage.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001030', '2d000000-0000-4000-8000-030000001030', 1, 'Paragraph C', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'Paragraph C: costs and environmental effects of barrages.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001031', '2d000000-0000-4000-8000-030000001031', 1, 'Paragraph D', '[{"optionKey":"A","content":"The earliest designs","sortOrder":1},{"optionKey":"B","content":"A safer machine","sortOrder":2},{"optionKey":"C","content":"Why people changed their minds","sortOrder":3},{"optionKey":"D","content":"Rising costs of repair","sortOrder":4},{"optionKey":"E","content":"A new age of leisure","sortOrder":5},{"optionKey":"F","content":"Racing and competition","sortOrder":6}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Paragraph D: supporters and critics.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001032', '2d000000-0000-4000-8000-030000001032', 1, 'A barrage is a low ______ built across a river mouth.', NULL, '{"type":"FILL","accepted":["dam"]}'::jsonb, 1, 'Paragraph B: a low dam.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001033', '2d000000-0000-4000-8000-030000001033', 1, 'Critics say equipment must survive ______ by salt water.', NULL, '{"type":"FILL","accepted":["corrosion"]}'::jsonb, 1, 'Paragraph D: corrosion by salt water.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001034', '2d000000-0000-4000-8000-030000001034', 1, 'Surname: ______', NULL, '{"type":"FILL","accepted":["Parker"]}'::jsonb, 1, 'The surname is spelled P, A, R, K, E, R.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001035', '2d000000-0000-4000-8000-030000001035', 1, 'Age of child: ______', NULL, '{"type":"FILL","accepted":["nine","9"]}'::jsonb, 1, 'She is nine.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001036', '2d000000-0000-4000-8000-030000001036', 1, 'Day of class: ______', NULL, '{"type":"FILL","accepted":["Wednesday"]}'::jsonb, 1, 'A beginners class on Wednesday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001037', '2d000000-0000-4000-8000-030000001037', 1, 'Cost for six weeks: £ ______', NULL, '{"type":"FILL","accepted":["45","forty-five","forty five"]}'::jsonb, 1, 'Forty-five pounds for six weeks.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001038', '2d000000-0000-4000-8000-030000001038', 1, 'Contact number: ______', NULL, '{"type":"FILL","accepted":["07700 900 482","07700900482"]}'::jsonb, 1, 'The number is 07700 900 482.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001039', '2d000000-0000-4000-8000-030000001039', 1, 'When does the animal feeding start?', '[{"optionKey":"A","content":"Nine thirty","sortOrder":1},{"optionKey":"B","content":"Eleven","sortOrder":2},{"optionKey":"C","content":"Four","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The animal feeding starts at eleven.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001040', '2d000000-0000-4000-8000-030000001040', 1, 'Where does the tractor ride leave from?', '[{"optionKey":"A","content":"The lake","sortOrder":1},{"optionKey":"B","content":"The shop","sortOrder":2},{"optionKey":"C","content":"The main barn","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'It leaves from the main barn.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001041', '2d000000-0000-4000-8000-030000001041', 1, 'Where is the best place for a picnic?', '[{"optionKey":"A","content":"Beside the lake","sortOrder":1},{"optionKey":"B","content":"In the main barn","sortOrder":2},{"optionKey":"C","content":"Near the shop","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Beside the lake, with benches and shade.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001042', '2d000000-0000-4000-8000-030000001042', 1, 'Special animal food costs £ ______ a bag.', NULL, '{"type":"FILL","accepted":["2","two"]}'::jsonb, 1, 'Two pounds a bag.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001043', '2d000000-0000-4000-8000-030000001043', 1, 'The last tractor ride leaves at ______.', NULL, '{"type":"FILL","accepted":["four","4","4 pm","4.00"]}'::jsonb, 1, 'The last tractor ride leaves at four.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001044', '2d000000-0000-4000-8000-030000001044', 1, 'Research solar power', '[{"optionKey":"A","content":"Maya","sortOrder":1},{"optionKey":"B","content":"Jon","sortOrder":2},{"optionKey":"C","content":"The tutor","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Maya will research solar power.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001045', '2d000000-0000-4000-8000-030000001045', 1, 'Write the final report', '[{"optionKey":"A","content":"Maya","sortOrder":1},{"optionKey":"B","content":"Jon","sortOrder":2},{"optionKey":"C","content":"The tutor","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Jon will write the final report.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001046', '2d000000-0000-4000-8000-030000001046', 1, 'Carry out the student survey', '[{"optionKey":"A","content":"Maya","sortOrder":1},{"optionKey":"B","content":"Jon","sortOrder":2},{"optionKey":"C","content":"The tutor","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The tutor suggests Jon does the survey.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001047', '2d000000-0000-4000-8000-030000001047', 1, 'When will the group practise together?', '[{"optionKey":"A","content":"Thursday afternoon","sortOrder":1},{"optionKey":"B","content":"Friday morning","sortOrder":2},{"optionKey":"C","content":"Monday evening","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Thursday afternoon is best.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001048', '2d000000-0000-4000-8000-030000001048', 1, 'When is the deadline?', '[{"optionKey":"A","content":"13th March","sortOrder":1},{"optionKey":"B","content":"20th March","sortOrder":2},{"optionKey":"C","content":"27th March","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The deadline is the twentieth of March.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001049', '2d000000-0000-4000-8000-030000001049', 1, 'A hive may contain ______ bees in summer.', NULL, '{"type":"FILL","accepted":["fifty thousand","50000","50,000"]}'::jsonb, 1, 'Fifty thousand bees in summer.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001050', '2d000000-0000-4000-8000-030000001050', 1, 'The queen can lay up to ______ eggs a day.', NULL, '{"type":"FILL","accepted":["two thousand","2000","2,000"]}'::jsonb, 1, 'Up to two thousand eggs a day.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001051', '2d000000-0000-4000-8000-030000001051', 1, 'Worker bees are all ______.', NULL, '{"type":"FILL","accepted":["female"]}'::jsonb, 1, 'All of them female.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001052', '2d000000-0000-4000-8000-030000001052', 1, 'Bees tell others where food is with a ______ dance.', NULL, '{"type":"FILL","accepted":["waggle"]}'::jsonb, 1, 'A waggle dance.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001053', '2d000000-0000-4000-8000-030000001053', 1, 'In winter bees form a tight ______.', NULL, '{"type":"FILL","accepted":["cluster"]}'::jsonb, 1, 'A tight cluster to stay warm.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001054', '2d000000-0000-4000-8000-030000001054', 1, 'Surname: ______', NULL, '{"type":"FILL","accepted":["Delaney"]}'::jsonb, 1, 'Spelled D, E, L, A, N, E, Y.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001055', '2d000000-0000-4000-8000-030000001055', 1, 'Arrival date: ______ June', NULL, '{"type":"FILL","accepted":["12","12th","twelfth"]}'::jsonb, 1, 'On the twelfth of June.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001056', '2d000000-0000-4000-8000-030000001056', 1, 'Number of nights: ______', NULL, '{"type":"FILL","accepted":["three","3"]}'::jsonb, 1, 'A double room for three nights.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001057', '2d000000-0000-4000-8000-030000001057', 1, 'Price per night: £ ______', NULL, '{"type":"FILL","accepted":["90","ninety"]}'::jsonb, 1, 'Ninety pounds a night.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001058', '2d000000-0000-4000-8000-030000001058', 1, 'Breakfast is served until ______', NULL, '{"type":"FILL","accepted":["ten","10","10 am"]}'::jsonb, 1, 'Served from seven until ten.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001059', '2d000000-0000-4000-8000-030000001059', 1, 'Which exhibition is the newest?', '[{"optionKey":"A","content":"Dinosaurs","sortOrder":1},{"optionKey":"B","content":"Space","sortOrder":2},{"optionKey":"C","content":"The ocean","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The space exhibition opened last spring.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001060', '2d000000-0000-4000-8000-030000001060', 1, 'Where is photography not allowed?', '[{"optionKey":"A","content":"In the cafe","sortOrder":1},{"optionKey":"B","content":"In the planetarium","sortOrder":2},{"optionKey":"C","content":"On the ground floor","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Allowed everywhere except in the planetarium.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001061', '2d000000-0000-4000-8000-030000001061', 1, 'Where is the cafe?', '[{"optionKey":"A","content":"Ground floor","sortOrder":1},{"optionKey":"B","content":"First floor","sortOrder":2},{"optionKey":"C","content":"Second floor","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'On the second floor.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001062', '2d000000-0000-4000-8000-030000001062', 1, 'Planetarium shows start every ______ minutes.', NULL, '{"type":"FILL","accepted":["forty","40"]}'::jsonb, 1, 'Every forty minutes.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001063', '2d000000-0000-4000-8000-030000001063', 1, 'Lockers cost £ ______.', NULL, '{"type":"FILL","accepted":["1","one"]}'::jsonb, 1, 'Lockers cost one pound.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001064', '2d000000-0000-4000-8000-030000001064', 1, 'Will interview students', '[{"optionKey":"A","content":"Priya","sortOrder":1},{"optionKey":"B","content":"Tom","sortOrder":2},{"optionKey":"C","content":"Dr Hughes","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Priya will interview students.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001065', '2d000000-0000-4000-8000-030000001065', 1, 'Will use a questionnaire', '[{"optionKey":"A","content":"Priya","sortOrder":1},{"optionKey":"B","content":"Tom","sortOrder":2},{"optionKey":"C","content":"Dr Hughes","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Tom will use a questionnaire.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001066', '2d000000-0000-4000-8000-030000001066', 1, 'Will send a reading list', '[{"optionKey":"A","content":"Priya","sortOrder":1},{"optionKey":"B","content":"Tom","sortOrder":2},{"optionKey":"C","content":"Dr Hughes","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Dr Hughes will email a reading list.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001067', '2d000000-0000-4000-8000-030000001067', 1, 'What must Tom check before starting?', '[{"optionKey":"A","content":"The budget","sortOrder":1},{"optionKey":"B","content":"The ethics rules","sortOrder":2},{"optionKey":"C","content":"The library hours","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Check the ethics rules.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001068', '2d000000-0000-4000-8000-030000001068', 1, 'When is the best time to book the study room?', '[{"optionKey":"A","content":"Monday afternoon","sortOrder":1},{"optionKey":"B","content":"Tuesday morning","sortOrder":2},{"optionKey":"C","content":"Friday morning","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Tuesday morning.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001069', '2d000000-0000-4000-8000-030000001069', 1, 'A green roof is covered with soil and ______.', NULL, '{"type":"FILL","accepted":["plants"]}'::jsonb, 1, 'Covered with soil and plants.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001070', '2d000000-0000-4000-8000-030000001070', 1, 'Plants provide natural ______.', NULL, '{"type":"FILL","accepted":["insulation"]}'::jsonb, 1, 'Natural insulation.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001071', '2d000000-0000-4000-8000-030000001071', 1, 'Green roofs absorb ______, reducing the risk of flooding.', NULL, '{"type":"FILL","accepted":["rainwater"]}'::jsonb, 1, 'They absorb rainwater.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001072', '2d000000-0000-4000-8000-030000001072', 1, 'Roof gardens provide a habitat for insects such as ______.', NULL, '{"type":"FILL","accepted":["bees"]}'::jsonb, 1, 'Insects such as bees.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001073', '2d000000-0000-4000-8000-030000001073', 1, 'The main drawback is ______.', NULL, '{"type":"FILL","accepted":["weight"]}'::jsonb, 1, 'The main drawback is weight.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001074', '2d000000-0000-4000-8000-030000001074', 1, 'Surname: ______', NULL, '{"type":"FILL","accepted":["Okafor"]}'::jsonb, 1, 'Spelled O, K, A, F, O, R.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001075', '2d000000-0000-4000-8000-030000001075', 1, 'Type of membership: ______', NULL, '{"type":"FILL","accepted":["yearly"]}'::jsonb, 1, 'Yearly, please.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001076', '2d000000-0000-4000-8000-030000001076', 1, 'Cost: £ ______', NULL, '{"type":"FILL","accepted":["240","two hundred and forty","two hundred forty"]}'::jsonb, 1, 'Two hundred and forty pounds.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001077', '2d000000-0000-4000-8000-030000001077', 1, 'Start day: ______', NULL, '{"type":"FILL","accepted":["Monday"]}'::jsonb, 1, 'Next Monday.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001078', '2d000000-0000-4000-8000-030000001078', 1, 'Main interest: ______', NULL, '{"type":"FILL","accepted":["swimming"]}'::jsonb, 1, 'Swimming, mainly.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001079', '2d000000-0000-4000-8000-030000001079', 1, 'When will the festival take place?', '[{"optionKey":"A","content":"Last Saturday of September","sortOrder":1},{"optionKey":"B","content":"First Sunday of October","sortOrder":2},{"optionKey":"C","content":"Last Saturday of August","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The last Saturday of September.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001080', '2d000000-0000-4000-8000-030000001080', 1, 'Why has the community hall been booked?', '[{"optionKey":"A","content":"For the music","sortOrder":1},{"optionKey":"B","content":"As a back-up in case of rain","sortOrder":2},{"optionKey":"C","content":"For the food stalls","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Last year it rained.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001081', '2d000000-0000-4000-8000-030000001081', 1, 'Who should volunteers for parking speak to?', '[{"optionKey":"A","content":"Mrs Clark","sortOrder":1},{"optionKey":"B","content":"The stall holders","sortOrder":2},{"optionKey":"C","content":"The printer","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Speak to Mrs Clark.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001082', '2d000000-0000-4000-8000-030000001082', 1, 'The music begins at ______ in the afternoon.', NULL, '{"type":"FILL","accepted":["two","2"]}'::jsonb, 1, 'At two in the afternoon.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001083', '2d000000-0000-4000-8000-030000001083', 1, 'Each food stall must pay £ ______.', NULL, '{"type":"FILL","accepted":["15","fifteen"]}'::jsonb, 1, 'A fee of fifteen pounds.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001084', '2d000000-0000-4000-8000-030000001084', 1, 'Wrote a very clear introduction', '[{"optionKey":"A","content":"Lena","sortOrder":1},{"optionKey":"B","content":"Mr Brown","sortOrder":2},{"optionKey":"C","content":"Sam","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Your introduction is very clear.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001085', '2d000000-0000-4000-8000-030000001085', 1, 'Needs to check the references', '[{"optionKey":"A","content":"Lena","sortOrder":1},{"optionKey":"B","content":"Mr Brown","sortOrder":2},{"optionKey":"C","content":"Sam","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Sam, you need to check your references.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001086', '2d000000-0000-4000-8000-030000001086', 1, 'Wrote a conclusion that is too short', '[{"optionKey":"A","content":"Lena","sortOrder":1},{"optionKey":"B","content":"Mr Brown","sortOrder":2},{"optionKey":"C","content":"Sam","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The conclusion is too short.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001087', '2d000000-0000-4000-8000-030000001087', 1, 'How many books should Sam use at least?', '[{"optionKey":"A","content":"Two","sortOrder":1},{"optionKey":"B","content":"Three","sortOrder":2},{"optionKey":"C","content":"Five","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'At least three books.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001088', '2d000000-0000-4000-8000-030000001088', 1, 'When is the final version due?', '[{"optionKey":"A","content":"Wednesday at noon","sortOrder":1},{"optionKey":"B","content":"Friday at noon","sortOrder":2},{"optionKey":"C","content":"Monday morning","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'By next Wednesday at noon.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001089', '2d000000-0000-4000-8000-030000001089', 1, 'The first sundials appeared about ______ years ago.', NULL, '{"type":"FILL","accepted":["five thousand","5000","5,000"]}'::jsonb, 1, 'About five thousand years ago.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001090', '2d000000-0000-4000-8000-030000001090', 1, 'Water clocks also worked at ______.', NULL, '{"type":"FILL","accepted":["night"]}'::jsonb, 1, 'They worked at night as well.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001091', '2d000000-0000-4000-8000-030000001091', 1, 'Mechanical clocks used a ______ to drive the hands.', NULL, '{"type":"FILL","accepted":["weight"]}'::jsonb, 1, 'They used a weight.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001092', '2d000000-0000-4000-8000-030000001092', 1, 'The pendulum clock was invented in ______.', NULL, '{"type":"FILL","accepted":["1656"]}'::jsonb, 1, 'In 1656.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001093', '2d000000-0000-4000-8000-030000001093', 1, 'Atomic clocks lose less than one ______ in millions of years.', NULL, '{"type":"FILL","accepted":["second"]}'::jsonb, 1, 'Less than one second.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001094', '2d000000-0000-4000-8000-030000001094', 1, 'The line graph below shows the percentage of households owning four types of electronic devices between 1995 and 2015. Mobile phones rose from 5% to 95%; computers from 20% to 85%; tablets rose from 0% to 60% after 2010; MP3 players peaked at 45% in 2010 and then fell to 20%. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001095', '2d000000-0000-4000-8000-030000001095', 1, 'Some people think that working from home is better for both employees and employers. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001096', '2d000000-0000-4000-8000-030000001096', 1, 'The bar chart compares the average number of hours students in five countries spent studying each week in 2020: Japan 14, Korea 18, Germany 11, Brazil 9 and Canada 12. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001097', '2d000000-0000-4000-8000-030000001097', 1, 'Some people believe that university education should be free for everyone, while others think students should pay for it. Discuss both views and give your own opinion. Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001098', '2d000000-0000-4000-8000-030000001098', 1, 'The pie charts show how an average household used energy in 1990 and in 2020. In 1990: heating 52%, lighting 20%, appliances 18%, cooking 10%. In 2020: heating 40%, lighting 8%, appliances 38%, cooking 14%. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001099', '2d000000-0000-4000-8000-030000001099', 1, 'In many cities, traffic congestion is getting worse. What problems does this cause, and what measures could be taken to reduce it? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001100', '2d000000-0000-4000-8000-030000001100', 1, 'The table shows the percentage of people in four age groups who visited the cinema at least once a month in 2000, 2010 and 2020. Ages 15-24: 48%, 52%, 40%; ages 25-34: 35%, 38%, 33%; ages 35-49: 22%, 25%, 24%; ages 50 and over: 10%, 14%, 18%. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001101', '2d000000-0000-4000-8000-030000001101', 1, 'More and more students choose to study in another country. Do the advantages of this trend outweigh the disadvantages? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001102', '2d000000-0000-4000-8000-030000001102', 1, 'The diagram shows the stages in the production of tea bags: leaves are picked, withered, rolled, dried, sorted by size, packed into paper bags and then boxed. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001103', '2d000000-0000-4000-8000-030000001103', 1, 'Fewer young people read newspapers or watch the news today. Why is this happening? Is it a problem for society? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001104', '2d000000-0000-4000-8000-030000001104', 1, 'The two maps show a village centre in 1995 and today. Farmland has been replaced by housing, the old school has become a library, a new car park has been built beside the station and the pond has been filled in. Summarise the information by selecting and reporting the main features, and make comparisons where relevant.', NULL, '{"type":"ESSAY","task":"TASK_1","minWords":150,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001105', '2d000000-0000-4000-8000-030000001105', 1, 'Some people think that children should start learning a foreign language at primary school. To what extent do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience.', NULL, '{"type":"ESSAY","task":"TASK_2","minWords":250,"passBand":5.5}'::jsonb, 1, 'Graded by the essay grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001106', '2d000000-0000-4000-8000-030000001106', 1, 'Where is your hometown?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001107', '2d000000-0000-4000-8000-030000001107', 1, 'What do you like most about living there?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001108', '2d000000-0000-4000-8000-030000001108', 1, 'Has your hometown changed much in recent years?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001109', '2d000000-0000-4000-8000-030000001109', 1, 'Would you like to live there in the future?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001110', '2d000000-0000-4000-8000-030000001110', 1, 'Describe a piece of technology you use every day. You should say: what it is, how often you use it, what you use it for, and explain why it is important to you.', NULL, '{"type":"SPEAKING","durationSeconds":120}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001111', '2d000000-0000-4000-8000-030000001111', 1, 'How has technology changed the way people communicate?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001112', '2d000000-0000-4000-8000-030000001112', 1, 'Do you think people depend too much on technology?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001113', '2d000000-0000-4000-8000-030000001113', 1, 'What technology do you think will be common in the future?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001114', '2d000000-0000-4000-8000-030000001114', 1, 'Are you a student or do you work?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001115', '2d000000-0000-4000-8000-030000001115', 1, 'What do you enjoy most about your studies or your job?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001116', '2d000000-0000-4000-8000-030000001116', 1, 'Is there anything you would like to change about it?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001117', '2d000000-0000-4000-8000-030000001117', 1, 'What would be your ideal job?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001118', '2d000000-0000-4000-8000-030000001118', 1, 'Describe a teacher who influenced you. You should say: who the teacher was, what subject they taught, what they were like, and explain why they influenced you.', NULL, '{"type":"SPEAKING","durationSeconds":120}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001119', '2d000000-0000-4000-8000-030000001119', 1, 'What makes a good teacher?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001120', '2d000000-0000-4000-8000-030000001120', 1, 'Should students be allowed to choose their own subjects at school?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001121', '2d000000-0000-4000-8000-030000001121', 1, 'How might classrooms change in the future?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001122', '2d000000-0000-4000-8000-030000001122', 1, 'What kind of food do you like to eat?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001123', '2d000000-0000-4000-8000-030000001123', 1, 'Do you enjoy cooking?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001124', '2d000000-0000-4000-8000-030000001124', 1, 'Who usually cooks in your family?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001125', '2d000000-0000-4000-8000-030000001125', 1, 'Is there any food you disliked as a child but like now?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001126', '2d000000-0000-4000-8000-030000001126', 1, 'Describe a memorable journey you made. You should say: where you went, who you went with, how you travelled, and explain why the journey was memorable.', NULL, '{"type":"SPEAKING","durationSeconds":120}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001127', '2d000000-0000-4000-8000-030000001127', 1, 'Why do people like to travel?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001128', '2d000000-0000-4000-8000-030000001128', 1, 'Is tourism good for local communities?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001129', '2d000000-0000-4000-8000-030000001129', 1, 'How has travel changed in recent decades?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001130', '2d000000-0000-4000-8000-030000001130', 1, 'Do you have a large family?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001131', '2d000000-0000-4000-8000-030000001131', 1, 'How often do you meet your friends?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001132', '2d000000-0000-4000-8000-030000001132', 1, 'What do you usually do together?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001133', '2d000000-0000-4000-8000-030000001133', 1, 'Do you think friends are more important than family?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001134', '2d000000-0000-4000-8000-030000001134', 1, 'Describe a place in nature you would like to visit. You should say: where it is, how you know about it, what you would do there, and explain why you want to go.', NULL, '{"type":"SPEAKING","durationSeconds":120}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001135', '2d000000-0000-4000-8000-030000001135', 1, 'What environmental problems does your country face?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001136', '2d000000-0000-4000-8000-030000001136', 1, 'Who should take responsibility for protecting the environment?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001137', '2d000000-0000-4000-8000-030000001137', 1, 'Can individuals really make a difference?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001138', '2d000000-0000-4000-8000-030000001138', 1, 'What time do you usually get up?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001139', '2d000000-0000-4000-8000-030000001139', 1, 'Which part of the day do you like best?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001140', '2d000000-0000-4000-8000-030000001140', 1, 'Is your routine different at the weekend?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001141', '2d000000-0000-4000-8000-030000001141', 1, 'Would you like to change your daily routine?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001142', '2d000000-0000-4000-8000-030000001142', 1, 'Describe something you do to keep healthy. You should say: what you do, how often you do it, who you do it with, and explain how it helps you.', NULL, '{"type":"SPEAKING","durationSeconds":120}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001143', '2d000000-0000-4000-8000-030000001143', 1, 'Why do some people find it hard to stay fit?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001144', '2d000000-0000-4000-8000-030000001144', 1, 'Should the government encourage healthy lifestyles?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001145', '2d000000-0000-4000-8000-030000001145', 1, 'How do eating habits differ between generations?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001146', '2d000000-0000-4000-8000-030000001146', 1, 'What do you like to do in your free time?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001147', '2d000000-0000-4000-8000-030000001147', 1, 'How did you become interested in this hobby?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001148', '2d000000-0000-4000-8000-030000001148', 1, 'Do you prefer to do hobbies alone or with others?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001149', '2d000000-0000-4000-8000-030000001149', 1, 'Is there a new hobby you would like to try?', NULL, '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001150', '2d000000-0000-4000-8000-030000001150', 1, 'Describe a festival or celebration in your country. You should say: what it is, when it takes place, what people do, and explain why it is important.', NULL, '{"type":"SPEAKING","durationSeconds":120}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001151', '2d000000-0000-4000-8000-030000001151', 1, 'Why are traditions important?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001152', '2d000000-0000-4000-8000-030000001152', 1, 'How are festivals changing?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000001153', '2d000000-0000-4000-8000-030000001153', 1, 'Do young people still value traditions?', NULL, '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; scored by the speaking grader.', 'PUBLISHED')
ON CONFLICT DO NOTHING;

UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001001' WHERE id = '2d000000-0000-4000-8000-030000001001';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001002' WHERE id = '2d000000-0000-4000-8000-030000001002';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001003' WHERE id = '2d000000-0000-4000-8000-030000001003';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001004' WHERE id = '2d000000-0000-4000-8000-030000001004';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001005' WHERE id = '2d000000-0000-4000-8000-030000001005';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001006' WHERE id = '2d000000-0000-4000-8000-030000001006';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001007' WHERE id = '2d000000-0000-4000-8000-030000001007';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001008' WHERE id = '2d000000-0000-4000-8000-030000001008';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001009' WHERE id = '2d000000-0000-4000-8000-030000001009';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001010' WHERE id = '2d000000-0000-4000-8000-030000001010';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001011' WHERE id = '2d000000-0000-4000-8000-030000001011';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001012' WHERE id = '2d000000-0000-4000-8000-030000001012';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001013' WHERE id = '2d000000-0000-4000-8000-030000001013';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001014' WHERE id = '2d000000-0000-4000-8000-030000001014';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001015' WHERE id = '2d000000-0000-4000-8000-030000001015';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001016' WHERE id = '2d000000-0000-4000-8000-030000001016';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001017' WHERE id = '2d000000-0000-4000-8000-030000001017';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001018' WHERE id = '2d000000-0000-4000-8000-030000001018';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001019' WHERE id = '2d000000-0000-4000-8000-030000001019';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001020' WHERE id = '2d000000-0000-4000-8000-030000001020';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001021' WHERE id = '2d000000-0000-4000-8000-030000001021';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001022' WHERE id = '2d000000-0000-4000-8000-030000001022';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001023' WHERE id = '2d000000-0000-4000-8000-030000001023';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001024' WHERE id = '2d000000-0000-4000-8000-030000001024';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001025' WHERE id = '2d000000-0000-4000-8000-030000001025';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001026' WHERE id = '2d000000-0000-4000-8000-030000001026';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001027' WHERE id = '2d000000-0000-4000-8000-030000001027';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001028' WHERE id = '2d000000-0000-4000-8000-030000001028';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001029' WHERE id = '2d000000-0000-4000-8000-030000001029';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001030' WHERE id = '2d000000-0000-4000-8000-030000001030';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001031' WHERE id = '2d000000-0000-4000-8000-030000001031';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001032' WHERE id = '2d000000-0000-4000-8000-030000001032';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001033' WHERE id = '2d000000-0000-4000-8000-030000001033';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001034' WHERE id = '2d000000-0000-4000-8000-030000001034';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001035' WHERE id = '2d000000-0000-4000-8000-030000001035';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001036' WHERE id = '2d000000-0000-4000-8000-030000001036';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001037' WHERE id = '2d000000-0000-4000-8000-030000001037';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001038' WHERE id = '2d000000-0000-4000-8000-030000001038';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001039' WHERE id = '2d000000-0000-4000-8000-030000001039';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001040' WHERE id = '2d000000-0000-4000-8000-030000001040';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001041' WHERE id = '2d000000-0000-4000-8000-030000001041';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001042' WHERE id = '2d000000-0000-4000-8000-030000001042';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001043' WHERE id = '2d000000-0000-4000-8000-030000001043';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001044' WHERE id = '2d000000-0000-4000-8000-030000001044';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001045' WHERE id = '2d000000-0000-4000-8000-030000001045';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001046' WHERE id = '2d000000-0000-4000-8000-030000001046';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001047' WHERE id = '2d000000-0000-4000-8000-030000001047';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001048' WHERE id = '2d000000-0000-4000-8000-030000001048';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001049' WHERE id = '2d000000-0000-4000-8000-030000001049';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001050' WHERE id = '2d000000-0000-4000-8000-030000001050';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001051' WHERE id = '2d000000-0000-4000-8000-030000001051';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001052' WHERE id = '2d000000-0000-4000-8000-030000001052';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001053' WHERE id = '2d000000-0000-4000-8000-030000001053';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001054' WHERE id = '2d000000-0000-4000-8000-030000001054';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001055' WHERE id = '2d000000-0000-4000-8000-030000001055';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001056' WHERE id = '2d000000-0000-4000-8000-030000001056';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001057' WHERE id = '2d000000-0000-4000-8000-030000001057';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001058' WHERE id = '2d000000-0000-4000-8000-030000001058';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001059' WHERE id = '2d000000-0000-4000-8000-030000001059';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001060' WHERE id = '2d000000-0000-4000-8000-030000001060';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001061' WHERE id = '2d000000-0000-4000-8000-030000001061';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001062' WHERE id = '2d000000-0000-4000-8000-030000001062';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001063' WHERE id = '2d000000-0000-4000-8000-030000001063';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001064' WHERE id = '2d000000-0000-4000-8000-030000001064';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001065' WHERE id = '2d000000-0000-4000-8000-030000001065';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001066' WHERE id = '2d000000-0000-4000-8000-030000001066';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001067' WHERE id = '2d000000-0000-4000-8000-030000001067';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001068' WHERE id = '2d000000-0000-4000-8000-030000001068';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001069' WHERE id = '2d000000-0000-4000-8000-030000001069';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001070' WHERE id = '2d000000-0000-4000-8000-030000001070';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001071' WHERE id = '2d000000-0000-4000-8000-030000001071';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001072' WHERE id = '2d000000-0000-4000-8000-030000001072';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001073' WHERE id = '2d000000-0000-4000-8000-030000001073';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001074' WHERE id = '2d000000-0000-4000-8000-030000001074';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001075' WHERE id = '2d000000-0000-4000-8000-030000001075';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001076' WHERE id = '2d000000-0000-4000-8000-030000001076';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001077' WHERE id = '2d000000-0000-4000-8000-030000001077';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001078' WHERE id = '2d000000-0000-4000-8000-030000001078';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001079' WHERE id = '2d000000-0000-4000-8000-030000001079';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001080' WHERE id = '2d000000-0000-4000-8000-030000001080';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001081' WHERE id = '2d000000-0000-4000-8000-030000001081';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001082' WHERE id = '2d000000-0000-4000-8000-030000001082';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001083' WHERE id = '2d000000-0000-4000-8000-030000001083';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001084' WHERE id = '2d000000-0000-4000-8000-030000001084';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001085' WHERE id = '2d000000-0000-4000-8000-030000001085';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001086' WHERE id = '2d000000-0000-4000-8000-030000001086';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001087' WHERE id = '2d000000-0000-4000-8000-030000001087';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001088' WHERE id = '2d000000-0000-4000-8000-030000001088';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001089' WHERE id = '2d000000-0000-4000-8000-030000001089';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001090' WHERE id = '2d000000-0000-4000-8000-030000001090';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001091' WHERE id = '2d000000-0000-4000-8000-030000001091';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001092' WHERE id = '2d000000-0000-4000-8000-030000001092';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001093' WHERE id = '2d000000-0000-4000-8000-030000001093';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001094' WHERE id = '2d000000-0000-4000-8000-030000001094';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001095' WHERE id = '2d000000-0000-4000-8000-030000001095';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001096' WHERE id = '2d000000-0000-4000-8000-030000001096';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001097' WHERE id = '2d000000-0000-4000-8000-030000001097';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001098' WHERE id = '2d000000-0000-4000-8000-030000001098';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001099' WHERE id = '2d000000-0000-4000-8000-030000001099';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001100' WHERE id = '2d000000-0000-4000-8000-030000001100';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001101' WHERE id = '2d000000-0000-4000-8000-030000001101';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001102' WHERE id = '2d000000-0000-4000-8000-030000001102';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001103' WHERE id = '2d000000-0000-4000-8000-030000001103';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001104' WHERE id = '2d000000-0000-4000-8000-030000001104';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001105' WHERE id = '2d000000-0000-4000-8000-030000001105';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001106' WHERE id = '2d000000-0000-4000-8000-030000001106';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001107' WHERE id = '2d000000-0000-4000-8000-030000001107';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001108' WHERE id = '2d000000-0000-4000-8000-030000001108';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001109' WHERE id = '2d000000-0000-4000-8000-030000001109';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001110' WHERE id = '2d000000-0000-4000-8000-030000001110';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001111' WHERE id = '2d000000-0000-4000-8000-030000001111';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001112' WHERE id = '2d000000-0000-4000-8000-030000001112';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001113' WHERE id = '2d000000-0000-4000-8000-030000001113';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001114' WHERE id = '2d000000-0000-4000-8000-030000001114';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001115' WHERE id = '2d000000-0000-4000-8000-030000001115';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001116' WHERE id = '2d000000-0000-4000-8000-030000001116';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001117' WHERE id = '2d000000-0000-4000-8000-030000001117';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001118' WHERE id = '2d000000-0000-4000-8000-030000001118';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001119' WHERE id = '2d000000-0000-4000-8000-030000001119';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001120' WHERE id = '2d000000-0000-4000-8000-030000001120';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001121' WHERE id = '2d000000-0000-4000-8000-030000001121';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001122' WHERE id = '2d000000-0000-4000-8000-030000001122';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001123' WHERE id = '2d000000-0000-4000-8000-030000001123';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001124' WHERE id = '2d000000-0000-4000-8000-030000001124';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001125' WHERE id = '2d000000-0000-4000-8000-030000001125';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001126' WHERE id = '2d000000-0000-4000-8000-030000001126';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001127' WHERE id = '2d000000-0000-4000-8000-030000001127';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001128' WHERE id = '2d000000-0000-4000-8000-030000001128';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001129' WHERE id = '2d000000-0000-4000-8000-030000001129';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001130' WHERE id = '2d000000-0000-4000-8000-030000001130';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001131' WHERE id = '2d000000-0000-4000-8000-030000001131';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001132' WHERE id = '2d000000-0000-4000-8000-030000001132';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001133' WHERE id = '2d000000-0000-4000-8000-030000001133';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001134' WHERE id = '2d000000-0000-4000-8000-030000001134';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001135' WHERE id = '2d000000-0000-4000-8000-030000001135';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001136' WHERE id = '2d000000-0000-4000-8000-030000001136';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001137' WHERE id = '2d000000-0000-4000-8000-030000001137';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001138' WHERE id = '2d000000-0000-4000-8000-030000001138';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001139' WHERE id = '2d000000-0000-4000-8000-030000001139';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001140' WHERE id = '2d000000-0000-4000-8000-030000001140';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001141' WHERE id = '2d000000-0000-4000-8000-030000001141';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001142' WHERE id = '2d000000-0000-4000-8000-030000001142';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001143' WHERE id = '2d000000-0000-4000-8000-030000001143';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001144' WHERE id = '2d000000-0000-4000-8000-030000001144';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001145' WHERE id = '2d000000-0000-4000-8000-030000001145';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001146' WHERE id = '2d000000-0000-4000-8000-030000001146';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001147' WHERE id = '2d000000-0000-4000-8000-030000001147';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001148' WHERE id = '2d000000-0000-4000-8000-030000001148';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001149' WHERE id = '2d000000-0000-4000-8000-030000001149';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001150' WHERE id = '2d000000-0000-4000-8000-030000001150';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001151' WHERE id = '2d000000-0000-4000-8000-030000001151';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001152' WHERE id = '2d000000-0000-4000-8000-030000001152';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000001153' WHERE id = '2d000000-0000-4000-8000-030000001153';

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000001001', 'PT-READING-ISP-03', 'Reading Practice Test 6', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000001002', 'PT-READING-ACT-02', 'Reading Practice Test 7', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'ACTUAL_TESTS'),
    ('2d000000-0000-4000-8000-050000001003', 'PT-LISTENING-01', 'Listening Practice Test 1', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000001004', 'PT-LISTENING-02', 'Listening Practice Test 2', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000001005', 'PT-LISTENING-03', 'Listening Practice Test 3', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS'),
    ('2d000000-0000-4000-8000-050000001006', 'PT-WRITING-01', 'Writing Practice Test 1', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000001007', 'PT-WRITING-02', 'Writing Practice Test 2', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000001008', 'PT-WRITING-03', 'Writing Practice Test 3', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS'),
    ('2d000000-0000-4000-8000-050000001009', 'PT-WRITING-04', 'Writing Practice Test 4', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000001010', 'PT-WRITING-05', 'Writing Practice Test 5', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000001011', 'PT-WRITING-06', 'Writing Practice Test 6', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS'),
    ('2d000000-0000-4000-8000-050000001012', 'PT-SPEAKING-01', 'Speaking Practice Test 1', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000001013', 'PT-SPEAKING-02', 'Speaking Practice Test 2', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000001014', 'PT-SPEAKING-03', 'Speaking Practice Test 3', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS'),
    ('2d000000-0000-4000-8000-050000001015', 'PT-SPEAKING-04', 'Speaking Practice Test 4', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000001016', 'PT-SPEAKING-05', 'Speaking Practice Test 5', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000001017', 'PT-SPEAKING-06', 'Speaking Practice Test 6', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS')
ON CONFLICT DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000001001', '2d000000-0000-4000-8000-050000001001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001002', '2d000000-0000-4000-8000-050000001002', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001003', '2d000000-0000-4000-8000-050000001003', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001004', '2d000000-0000-4000-8000-050000001004', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001005', '2d000000-0000-4000-8000-050000001005', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001006', '2d000000-0000-4000-8000-050000001006', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001007', '2d000000-0000-4000-8000-050000001007', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001008', '2d000000-0000-4000-8000-050000001008', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001009', '2d000000-0000-4000-8000-050000001009', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001010', '2d000000-0000-4000-8000-050000001010', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001011', '2d000000-0000-4000-8000-050000001011', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001012', '2d000000-0000-4000-8000-050000001012', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001013', '2d000000-0000-4000-8000-050000001013', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001014', '2d000000-0000-4000-8000-050000001014', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001015', '2d000000-0000-4000-8000-050000001015', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001016', '2d000000-0000-4000-8000-050000001016', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000001017', '2d000000-0000-4000-8000-050000001017', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001001' WHERE id = '2d000000-0000-4000-8000-050000001001';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001002' WHERE id = '2d000000-0000-4000-8000-050000001002';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001003' WHERE id = '2d000000-0000-4000-8000-050000001003';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001004' WHERE id = '2d000000-0000-4000-8000-050000001004';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001005' WHERE id = '2d000000-0000-4000-8000-050000001005';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001006' WHERE id = '2d000000-0000-4000-8000-050000001006';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001007' WHERE id = '2d000000-0000-4000-8000-050000001007';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001008' WHERE id = '2d000000-0000-4000-8000-050000001008';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001009' WHERE id = '2d000000-0000-4000-8000-050000001009';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001010' WHERE id = '2d000000-0000-4000-8000-050000001010';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001011' WHERE id = '2d000000-0000-4000-8000-050000001011';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001012' WHERE id = '2d000000-0000-4000-8000-050000001012';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001013' WHERE id = '2d000000-0000-4000-8000-050000001013';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001014' WHERE id = '2d000000-0000-4000-8000-050000001014';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001015' WHERE id = '2d000000-0000-4000-8000-050000001015';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001016' WHERE id = '2d000000-0000-4000-8000-050000001016';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000001017' WHERE id = '2d000000-0000-4000-8000-050000001017';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no, cover_asset_id) VALUES
    ('2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-060000001001', 'Passage 1: The story of the bicycle', 'READING', 1, 1200, 'You should spend about 20 minutes on Questions 1-8, which are based on the reading passage below.', 1, '2d000000-0000-4000-8000-010000001001'),
    ('2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-060000001001', 'Passage 2: Why we dream', 'READING', 2, 1200, 'You should spend about 20 minutes on Questions 1-8, which are based on the reading passage below.', 2, '2d000000-0000-4000-8000-010000001002'),
    ('2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-060000001001', 'Passage 3: Microplastics in the food chain', 'READING', 3, 1200, 'You should spend about 20 minutes on Questions 1-8, which are based on the reading passage below.', 3, '2d000000-0000-4000-8000-010000001003'),
    ('2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-060000001002', 'Passage 1: Harnessing the tides', 'READING', 1, 1200, 'You should spend about 20 minutes on Questions 1-9, which are based on the reading passage below.', 1, '2d000000-0000-4000-8000-010000001004'),
    ('2d000000-0000-4000-8000-070000001005', '2d000000-0000-4000-8000-060000001003', 'Section 1: Booking a swimming course', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 1, '2d000000-0000-4000-8000-010000001005'),
    ('2d000000-0000-4000-8000-070000001006', '2d000000-0000-4000-8000-060000001003', 'Section 2: A day at the farm park', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 2, '2d000000-0000-4000-8000-010000001006'),
    ('2d000000-0000-4000-8000-070000001007', '2d000000-0000-4000-8000-060000001003', 'Section 3: Planning a group project', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 3, '2d000000-0000-4000-8000-010000001007'),
    ('2d000000-0000-4000-8000-070000001008', '2d000000-0000-4000-8000-060000001003', 'Section 4: The life of honeybees', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 4, '2d000000-0000-4000-8000-010000001008'),
    ('2d000000-0000-4000-8000-070000001009', '2d000000-0000-4000-8000-060000001004', 'Section 1: Booking a hotel room', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 1, '2d000000-0000-4000-8000-010000001009'),
    ('2d000000-0000-4000-8000-070000001010', '2d000000-0000-4000-8000-060000001004', 'Section 2: A tour of the science museum', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 2, '2d000000-0000-4000-8000-010000001010'),
    ('2d000000-0000-4000-8000-070000001011', '2d000000-0000-4000-8000-060000001004', 'Section 3: Choosing a research topic', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 3, '2d000000-0000-4000-8000-010000001011'),
    ('2d000000-0000-4000-8000-070000001012', '2d000000-0000-4000-8000-060000001004', 'Section 4: Green roofs in cities', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 4, '2d000000-0000-4000-8000-010000001012'),
    ('2d000000-0000-4000-8000-070000001013', '2d000000-0000-4000-8000-060000001005', 'Section 1: Joining a fitness club', 'LISTENING', 1, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 1, '2d000000-0000-4000-8000-010000001013'),
    ('2d000000-0000-4000-8000-070000001014', '2d000000-0000-4000-8000-060000001005', 'Section 2: Community festival arrangements', 'LISTENING', 2, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 2, '2d000000-0000-4000-8000-010000001014'),
    ('2d000000-0000-4000-8000-070000001015', '2d000000-0000-4000-8000-060000001005', 'Section 3: Feedback on an essay', 'LISTENING', 3, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 3, '2d000000-0000-4000-8000-010000001015'),
    ('2d000000-0000-4000-8000-070000001016', '2d000000-0000-4000-8000-060000001005', 'Section 4: The history of the clock', 'LISTENING', 4, 600, 'You will hear a recording. Answer Questions 1-5 as you listen.', 4, '2d000000-0000-4000-8000-010000001016'),
    ('2d000000-0000-4000-8000-070000001017', '2d000000-0000-4000-8000-060000001006', 'Task 1: Household device ownership', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000001017'),
    ('2d000000-0000-4000-8000-070000001018', '2d000000-0000-4000-8000-060000001006', 'Task 2: Working from home', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000001018'),
    ('2d000000-0000-4000-8000-070000001019', '2d000000-0000-4000-8000-060000001007', 'Task 1: Weekly study time by country', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000001019'),
    ('2d000000-0000-4000-8000-070000001020', '2d000000-0000-4000-8000-060000001007', 'Task 2: Free university education', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000001020'),
    ('2d000000-0000-4000-8000-070000001021', '2d000000-0000-4000-8000-060000001008', 'Task 1: Household energy use', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000001021'),
    ('2d000000-0000-4000-8000-070000001022', '2d000000-0000-4000-8000-060000001008', 'Task 2: Traffic congestion', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000001022'),
    ('2d000000-0000-4000-8000-070000001023', '2d000000-0000-4000-8000-060000001009', 'Task 1: Cinema attendance by age group', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000001023'),
    ('2d000000-0000-4000-8000-070000001024', '2d000000-0000-4000-8000-060000001009', 'Task 2: Studying abroad', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000001024'),
    ('2d000000-0000-4000-8000-070000001025', '2d000000-0000-4000-8000-060000001010', 'Task 1: How tea bags are made', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000001025'),
    ('2d000000-0000-4000-8000-070000001026', '2d000000-0000-4000-8000-060000001010', 'Task 2: Young people and the news', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000001026'),
    ('2d000000-0000-4000-8000-070000001027', '2d000000-0000-4000-8000-060000001011', 'Task 1: Changes to a village centre', 'WRITING', 1, 1200, 'You should spend about 20 minutes on this task. Write at least 150 words.', 1, '2d000000-0000-4000-8000-010000001027'),
    ('2d000000-0000-4000-8000-070000001028', '2d000000-0000-4000-8000-060000001011', 'Task 2: Languages in primary school', 'WRITING', 2, 2400, 'You should spend about 40 minutes on this task. Write at least 250 words.', 2, '2d000000-0000-4000-8000-010000001028'),
    ('2d000000-0000-4000-8000-070000001029', '2d000000-0000-4000-8000-060000001012', 'Part 1: Hometown', 'SPEAKING', 1, 300, 'The examiner asks you general questions about familiar topics. Answer each question in about 20-30 seconds.', 1, '2d000000-0000-4000-8000-010000001029'),
    ('2d000000-0000-4000-8000-070000001030', '2d000000-0000-4000-8000-060000001012', 'Part 2: Technology', 'SPEAKING', 2, 240, 'You have one minute to prepare, then speak for one to two minutes on the topic on the card.', 2, '2d000000-0000-4000-8000-010000001030'),
    ('2d000000-0000-4000-8000-070000001031', '2d000000-0000-4000-8000-060000001012', 'Part 3: Technology', 'SPEAKING', 3, 300, 'The examiner asks you more abstract questions related to the Part 2 topic.', 3, '2d000000-0000-4000-8000-010000001031'),
    ('2d000000-0000-4000-8000-070000001032', '2d000000-0000-4000-8000-060000001013', 'Part 1: Study and work', 'SPEAKING', 1, 300, 'The examiner asks you general questions about familiar topics. Answer each question in about 20-30 seconds.', 1, '2d000000-0000-4000-8000-010000001032'),
    ('2d000000-0000-4000-8000-070000001033', '2d000000-0000-4000-8000-060000001013', 'Part 2: Education', 'SPEAKING', 2, 240, 'You have one minute to prepare, then speak for one to two minutes on the topic on the card.', 2, '2d000000-0000-4000-8000-010000001033'),
    ('2d000000-0000-4000-8000-070000001034', '2d000000-0000-4000-8000-060000001013', 'Part 3: Education', 'SPEAKING', 3, 300, 'The examiner asks you more abstract questions related to the Part 2 topic.', 3, '2d000000-0000-4000-8000-010000001034'),
    ('2d000000-0000-4000-8000-070000001035', '2d000000-0000-4000-8000-060000001014', 'Part 1: Food and cooking', 'SPEAKING', 1, 300, 'The examiner asks you general questions about familiar topics. Answer each question in about 20-30 seconds.', 1, '2d000000-0000-4000-8000-010000001035'),
    ('2d000000-0000-4000-8000-070000001036', '2d000000-0000-4000-8000-060000001014', 'Part 2: Travel', 'SPEAKING', 2, 240, 'You have one minute to prepare, then speak for one to two minutes on the topic on the card.', 2, '2d000000-0000-4000-8000-010000001036'),
    ('2d000000-0000-4000-8000-070000001037', '2d000000-0000-4000-8000-060000001014', 'Part 3: Travel', 'SPEAKING', 3, 300, 'The examiner asks you more abstract questions related to the Part 2 topic.', 3, '2d000000-0000-4000-8000-010000001037'),
    ('2d000000-0000-4000-8000-070000001038', '2d000000-0000-4000-8000-060000001015', 'Part 1: Family and friends', 'SPEAKING', 1, 300, 'The examiner asks you general questions about familiar topics. Answer each question in about 20-30 seconds.', 1, '2d000000-0000-4000-8000-010000001038'),
    ('2d000000-0000-4000-8000-070000001039', '2d000000-0000-4000-8000-060000001015', 'Part 2: Environment', 'SPEAKING', 2, 240, 'You have one minute to prepare, then speak for one to two minutes on the topic on the card.', 2, '2d000000-0000-4000-8000-010000001039'),
    ('2d000000-0000-4000-8000-070000001040', '2d000000-0000-4000-8000-060000001015', 'Part 3: Environment', 'SPEAKING', 3, 300, 'The examiner asks you more abstract questions related to the Part 2 topic.', 3, '2d000000-0000-4000-8000-010000001040'),
    ('2d000000-0000-4000-8000-070000001041', '2d000000-0000-4000-8000-060000001016', 'Part 1: Daily routine', 'SPEAKING', 1, 300, 'The examiner asks you general questions about familiar topics. Answer each question in about 20-30 seconds.', 1, '2d000000-0000-4000-8000-010000001041'),
    ('2d000000-0000-4000-8000-070000001042', '2d000000-0000-4000-8000-060000001016', 'Part 2: Health', 'SPEAKING', 2, 240, 'You have one minute to prepare, then speak for one to two minutes on the topic on the card.', 2, '2d000000-0000-4000-8000-010000001042'),
    ('2d000000-0000-4000-8000-070000001043', '2d000000-0000-4000-8000-060000001016', 'Part 3: Health', 'SPEAKING', 3, 300, 'The examiner asks you more abstract questions related to the Part 2 topic.', 3, '2d000000-0000-4000-8000-010000001043'),
    ('2d000000-0000-4000-8000-070000001044', '2d000000-0000-4000-8000-060000001017', 'Part 1: Hobbies', 'SPEAKING', 1, 300, 'The examiner asks you general questions about familiar topics. Answer each question in about 20-30 seconds.', 1, '2d000000-0000-4000-8000-010000001044'),
    ('2d000000-0000-4000-8000-070000001045', '2d000000-0000-4000-8000-060000001017', 'Part 2: Culture', 'SPEAKING', 2, 240, 'You have one minute to prepare, then speak for one to two minutes on the topic on the card.', 2, '2d000000-0000-4000-8000-010000001045'),
    ('2d000000-0000-4000-8000-070000001046', '2d000000-0000-4000-8000-060000001017', 'Part 3: Culture', 'SPEAKING', 3, 300, 'The examiner asks you more abstract questions related to the Part 2 topic.', 3, '2d000000-0000-4000-8000-010000001046')
ON CONFLICT DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000001001', '2d000000-0000-4000-8000-070000001001', 1, 'MATCHING_HEADINGS', 'Questions 1-4. The passage has four paragraphs, A-D. Choose the correct heading for each paragraph from the list of headings.'),
    ('2d000000-0000-4000-8000-090000001002', '2d000000-0000-4000-8000-070000001001', 2, 'SUMMARY_COMPLETION', 'Questions 5-8. Complete the summary. Write NO MORE THAN TWO WORDS from the passage for each answer.'),
    ('2d000000-0000-4000-8000-090000001003', '2d000000-0000-4000-8000-070000001002', 1, 'TRUE_FALSE_NOT_GIVEN', 'Questions 1-4. Do the following statements agree with the information given in the passage? Choose TRUE if the statement agrees with the information, FALSE if it contradicts the information, or NOT GIVEN if there is no information on this.'),
    ('2d000000-0000-4000-8000-090000001004', '2d000000-0000-4000-8000-070000001002', 2, 'MULTIPLE_CHOICE_ONE', 'Questions 5-8. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001005', '2d000000-0000-4000-8000-070000001003', 1, 'YES_NO_NOT_GIVEN', 'Questions 1-3. Do the following statements agree with the views of the writer? Choose YES if the statement agrees with the writer, NO if it contradicts the writer, or NOT GIVEN if it is impossible to say what the writer thinks.'),
    ('2d000000-0000-4000-8000-090000001006', '2d000000-0000-4000-8000-070000001003', 2, 'MULTIPLE_CHOICE_ONE', 'Questions 4-6. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001007', '2d000000-0000-4000-8000-070000001003', 3, 'SENTENCE_COMPLETION', 'Questions 7-8. Complete the sentences. Write NO MORE THAN TWO WORDS from the passage for each answer.'),
    ('2d000000-0000-4000-8000-090000001008', '2d000000-0000-4000-8000-070000001004', 1, 'TRUE_FALSE_NOT_GIVEN', 'Questions 1-3. Do the following statements agree with the information given in the passage? Choose TRUE if the statement agrees with the information, FALSE if it contradicts the information, or NOT GIVEN if there is no information on this.'),
    ('2d000000-0000-4000-8000-090000001009', '2d000000-0000-4000-8000-070000001004', 2, 'MATCHING_HEADINGS', 'Questions 4-7. The passage has four paragraphs, A-D. Choose the correct heading for each paragraph from the list of headings.'),
    ('2d000000-0000-4000-8000-090000001010', '2d000000-0000-4000-8000-070000001004', 3, 'GAP_FILLING', 'Questions 8-9. Complete the sentences. Write NO MORE THAN TWO WORDS from the passage for each answer.'),
    ('2d000000-0000-4000-8000-090000001011', '2d000000-0000-4000-8000-070000001005', 1, 'FORM_COMPLETION', 'Questions 1-5. Complete the form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001012', '2d000000-0000-4000-8000-070000001006', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 1-3. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001013', '2d000000-0000-4000-8000-070000001006', 2, 'NOTE_COMPLETION', 'Questions 4-5. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001014', '2d000000-0000-4000-8000-070000001007', 1, 'MATCHING_FEATURES', 'Questions 1-3. Who will do each task? Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001015', '2d000000-0000-4000-8000-070000001007', 2, 'MULTIPLE_CHOICE_ONE', 'Questions 4-5. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001016', '2d000000-0000-4000-8000-070000001008', 1, 'SUMMARY_COMPLETION', 'Questions 1-5. Complete the summary below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001017', '2d000000-0000-4000-8000-070000001009', 1, 'FORM_COMPLETION', 'Questions 1-5. Complete the form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001018', '2d000000-0000-4000-8000-070000001010', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 1-3. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001019', '2d000000-0000-4000-8000-070000001010', 2, 'NOTE_COMPLETION', 'Questions 4-5. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001020', '2d000000-0000-4000-8000-070000001011', 1, 'MATCHING_FEATURES', 'Questions 1-3. Who will do each task? Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001021', '2d000000-0000-4000-8000-070000001011', 2, 'MULTIPLE_CHOICE_ONE', 'Questions 4-5. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001022', '2d000000-0000-4000-8000-070000001012', 1, 'SUMMARY_COMPLETION', 'Questions 1-5. Complete the summary below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001023', '2d000000-0000-4000-8000-070000001013', 1, 'FORM_COMPLETION', 'Questions 1-5. Complete the form below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001024', '2d000000-0000-4000-8000-070000001014', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 1-3. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001025', '2d000000-0000-4000-8000-070000001014', 2, 'NOTE_COMPLETION', 'Questions 4-5. Complete the notes below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001026', '2d000000-0000-4000-8000-070000001015', 1, 'MATCHING_FEATURES', 'Questions 1-3. Who will do each task? Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001027', '2d000000-0000-4000-8000-070000001015', 2, 'MULTIPLE_CHOICE_ONE', 'Questions 4-5. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000001028', '2d000000-0000-4000-8000-070000001016', 1, 'SUMMARY_COMPLETION', 'Questions 1-5. Complete the summary below. Write ONE WORD AND/OR A NUMBER for each answer.'),
    ('2d000000-0000-4000-8000-090000001029', '2d000000-0000-4000-8000-070000001017', 1, 'LINE_GRAPH', 'Questions 1-1. line graph task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000001030', '2d000000-0000-4000-8000-070000001018', 1, 'OPINION_ESSAY', 'Questions 1-1. opinion essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000001031', '2d000000-0000-4000-8000-070000001019', 1, 'BAR_CHART', 'Questions 1-1. bar chart task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000001032', '2d000000-0000-4000-8000-070000001020', 1, 'DISCUSSION_ESSAY', 'Questions 1-1. discussion essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000001033', '2d000000-0000-4000-8000-070000001021', 1, 'PIE_CHART', 'Questions 1-1. pie chart task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000001034', '2d000000-0000-4000-8000-070000001022', 1, 'PROBLEM_SOLUTION_ESSAY', 'Questions 1-1. problem solution essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000001035', '2d000000-0000-4000-8000-070000001023', 1, 'TABLE_CHART', 'Questions 1-1. table chart task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000001036', '2d000000-0000-4000-8000-070000001024', 1, 'ADVANTAGES_DISADVANTAGES_ESSAY', 'Questions 1-1. advantages disadvantages essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000001037', '2d000000-0000-4000-8000-070000001025', 1, 'PROCESS_DIAGRAM', 'Questions 1-1. process diagram task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000001038', '2d000000-0000-4000-8000-070000001026', 1, 'TWO_PART_QUESTION', 'Questions 1-1. two part question task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000001039', '2d000000-0000-4000-8000-070000001027', 1, 'MAP_DIAGRAM', 'Questions 1-1. map diagram task. Write at least 150 words.'),
    ('2d000000-0000-4000-8000-090000001040', '2d000000-0000-4000-8000-070000001028', 1, 'OPINION_ESSAY', 'Questions 1-1. opinion essay task. Write at least 250 words.'),
    ('2d000000-0000-4000-8000-090000001041', '2d000000-0000-4000-8000-070000001029', 1, 'HOMETOWN', 'Questions 1-4. Part 1 interview about hometown.'),
    ('2d000000-0000-4000-8000-090000001042', '2d000000-0000-4000-8000-070000001030', 1, 'TECHNOLOGY', 'Questions 1-1. Part 2 long turn: technology.'),
    ('2d000000-0000-4000-8000-090000001043', '2d000000-0000-4000-8000-070000001031', 1, 'TECHNOLOGY', 'Questions 1-3. Part 3 discussion: technology.'),
    ('2d000000-0000-4000-8000-090000001044', '2d000000-0000-4000-8000-070000001032', 1, 'STUDY_AND_WORK', 'Questions 1-4. Part 1 interview about study and work.'),
    ('2d000000-0000-4000-8000-090000001045', '2d000000-0000-4000-8000-070000001033', 1, 'EDUCATION', 'Questions 1-1. Part 2 long turn: education.'),
    ('2d000000-0000-4000-8000-090000001046', '2d000000-0000-4000-8000-070000001034', 1, 'EDUCATION', 'Questions 1-3. Part 3 discussion: education.'),
    ('2d000000-0000-4000-8000-090000001047', '2d000000-0000-4000-8000-070000001035', 1, 'FOOD_AND_COOKING', 'Questions 1-4. Part 1 interview about food and cooking.'),
    ('2d000000-0000-4000-8000-090000001048', '2d000000-0000-4000-8000-070000001036', 1, 'TRAVEL', 'Questions 1-1. Part 2 long turn: travel.'),
    ('2d000000-0000-4000-8000-090000001049', '2d000000-0000-4000-8000-070000001037', 1, 'TRAVEL', 'Questions 1-3. Part 3 discussion: travel.'),
    ('2d000000-0000-4000-8000-090000001050', '2d000000-0000-4000-8000-070000001038', 1, 'FAMILY_AND_FRIENDS', 'Questions 1-4. Part 1 interview about family and friends.'),
    ('2d000000-0000-4000-8000-090000001051', '2d000000-0000-4000-8000-070000001039', 1, 'ENVIRONMENT', 'Questions 1-1. Part 2 long turn: environment.'),
    ('2d000000-0000-4000-8000-090000001052', '2d000000-0000-4000-8000-070000001040', 1, 'ENVIRONMENT', 'Questions 1-3. Part 3 discussion: environment.'),
    ('2d000000-0000-4000-8000-090000001053', '2d000000-0000-4000-8000-070000001041', 1, 'DAILY_ROUTINE', 'Questions 1-4. Part 1 interview about daily routine.'),
    ('2d000000-0000-4000-8000-090000001054', '2d000000-0000-4000-8000-070000001042', 1, 'HEALTH', 'Questions 1-1. Part 2 long turn: health.'),
    ('2d000000-0000-4000-8000-090000001055', '2d000000-0000-4000-8000-070000001043', 1, 'HEALTH', 'Questions 1-3. Part 3 discussion: health.'),
    ('2d000000-0000-4000-8000-090000001056', '2d000000-0000-4000-8000-070000001044', 1, 'HOBBIES', 'Questions 1-4. Part 1 interview about hobbies.'),
    ('2d000000-0000-4000-8000-090000001057', '2d000000-0000-4000-8000-070000001045', 1, 'CULTURE', 'Questions 1-1. Part 2 long turn: culture.'),
    ('2d000000-0000-4000-8000-090000001058', '2d000000-0000-4000-8000-070000001046', 1, 'CULTURE', 'Questions 1-3. Part 3 discussion: culture.')
ON CONFLICT DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000001001', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001001', 1, 1.0, '2d000000-0000-4000-8000-090000001001'),
    ('2d000000-0000-4000-8000-080000001002', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001002', 2, 1.0, '2d000000-0000-4000-8000-090000001001'),
    ('2d000000-0000-4000-8000-080000001003', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001003', 3, 1.0, '2d000000-0000-4000-8000-090000001001'),
    ('2d000000-0000-4000-8000-080000001004', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001004', 4, 1.0, '2d000000-0000-4000-8000-090000001001'),
    ('2d000000-0000-4000-8000-080000001005', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001005', 5, 1.0, '2d000000-0000-4000-8000-090000001002'),
    ('2d000000-0000-4000-8000-080000001006', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001006', 6, 1.0, '2d000000-0000-4000-8000-090000001002'),
    ('2d000000-0000-4000-8000-080000001007', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001007', 7, 1.0, '2d000000-0000-4000-8000-090000001002'),
    ('2d000000-0000-4000-8000-080000001008', '2d000000-0000-4000-8000-070000001001', '2d000000-0000-4000-8000-040000001008', 8, 1.0, '2d000000-0000-4000-8000-090000001002'),
    ('2d000000-0000-4000-8000-080000001009', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001009', 1, 1.0, '2d000000-0000-4000-8000-090000001003'),
    ('2d000000-0000-4000-8000-080000001010', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001010', 2, 1.0, '2d000000-0000-4000-8000-090000001003'),
    ('2d000000-0000-4000-8000-080000001011', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001011', 3, 1.0, '2d000000-0000-4000-8000-090000001003'),
    ('2d000000-0000-4000-8000-080000001012', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001012', 4, 1.0, '2d000000-0000-4000-8000-090000001003'),
    ('2d000000-0000-4000-8000-080000001013', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001013', 5, 1.0, '2d000000-0000-4000-8000-090000001004'),
    ('2d000000-0000-4000-8000-080000001014', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001014', 6, 1.0, '2d000000-0000-4000-8000-090000001004'),
    ('2d000000-0000-4000-8000-080000001015', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001015', 7, 1.0, '2d000000-0000-4000-8000-090000001004'),
    ('2d000000-0000-4000-8000-080000001016', '2d000000-0000-4000-8000-070000001002', '2d000000-0000-4000-8000-040000001016', 8, 1.0, '2d000000-0000-4000-8000-090000001004'),
    ('2d000000-0000-4000-8000-080000001017', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001017', 1, 1.0, '2d000000-0000-4000-8000-090000001005'),
    ('2d000000-0000-4000-8000-080000001018', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001018', 2, 1.0, '2d000000-0000-4000-8000-090000001005'),
    ('2d000000-0000-4000-8000-080000001019', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001019', 3, 1.0, '2d000000-0000-4000-8000-090000001005'),
    ('2d000000-0000-4000-8000-080000001020', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001020', 4, 1.0, '2d000000-0000-4000-8000-090000001006'),
    ('2d000000-0000-4000-8000-080000001021', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001021', 5, 1.0, '2d000000-0000-4000-8000-090000001006'),
    ('2d000000-0000-4000-8000-080000001022', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001022', 6, 1.0, '2d000000-0000-4000-8000-090000001006'),
    ('2d000000-0000-4000-8000-080000001023', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001023', 7, 1.0, '2d000000-0000-4000-8000-090000001007'),
    ('2d000000-0000-4000-8000-080000001024', '2d000000-0000-4000-8000-070000001003', '2d000000-0000-4000-8000-040000001024', 8, 1.0, '2d000000-0000-4000-8000-090000001007'),
    ('2d000000-0000-4000-8000-080000001025', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001025', 1, 1.0, '2d000000-0000-4000-8000-090000001008'),
    ('2d000000-0000-4000-8000-080000001026', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001026', 2, 1.0, '2d000000-0000-4000-8000-090000001008'),
    ('2d000000-0000-4000-8000-080000001027', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001027', 3, 1.0, '2d000000-0000-4000-8000-090000001008'),
    ('2d000000-0000-4000-8000-080000001028', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001028', 4, 1.0, '2d000000-0000-4000-8000-090000001009'),
    ('2d000000-0000-4000-8000-080000001029', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001029', 5, 1.0, '2d000000-0000-4000-8000-090000001009'),
    ('2d000000-0000-4000-8000-080000001030', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001030', 6, 1.0, '2d000000-0000-4000-8000-090000001009'),
    ('2d000000-0000-4000-8000-080000001031', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001031', 7, 1.0, '2d000000-0000-4000-8000-090000001009'),
    ('2d000000-0000-4000-8000-080000001032', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001032', 8, 1.0, '2d000000-0000-4000-8000-090000001010'),
    ('2d000000-0000-4000-8000-080000001033', '2d000000-0000-4000-8000-070000001004', '2d000000-0000-4000-8000-040000001033', 9, 1.0, '2d000000-0000-4000-8000-090000001010'),
    ('2d000000-0000-4000-8000-080000001034', '2d000000-0000-4000-8000-070000001005', '2d000000-0000-4000-8000-040000001034', 1, 1.0, '2d000000-0000-4000-8000-090000001011'),
    ('2d000000-0000-4000-8000-080000001035', '2d000000-0000-4000-8000-070000001005', '2d000000-0000-4000-8000-040000001035', 2, 1.0, '2d000000-0000-4000-8000-090000001011'),
    ('2d000000-0000-4000-8000-080000001036', '2d000000-0000-4000-8000-070000001005', '2d000000-0000-4000-8000-040000001036', 3, 1.0, '2d000000-0000-4000-8000-090000001011'),
    ('2d000000-0000-4000-8000-080000001037', '2d000000-0000-4000-8000-070000001005', '2d000000-0000-4000-8000-040000001037', 4, 1.0, '2d000000-0000-4000-8000-090000001011'),
    ('2d000000-0000-4000-8000-080000001038', '2d000000-0000-4000-8000-070000001005', '2d000000-0000-4000-8000-040000001038', 5, 1.0, '2d000000-0000-4000-8000-090000001011'),
    ('2d000000-0000-4000-8000-080000001039', '2d000000-0000-4000-8000-070000001006', '2d000000-0000-4000-8000-040000001039', 1, 1.0, '2d000000-0000-4000-8000-090000001012'),
    ('2d000000-0000-4000-8000-080000001040', '2d000000-0000-4000-8000-070000001006', '2d000000-0000-4000-8000-040000001040', 2, 1.0, '2d000000-0000-4000-8000-090000001012'),
    ('2d000000-0000-4000-8000-080000001041', '2d000000-0000-4000-8000-070000001006', '2d000000-0000-4000-8000-040000001041', 3, 1.0, '2d000000-0000-4000-8000-090000001012'),
    ('2d000000-0000-4000-8000-080000001042', '2d000000-0000-4000-8000-070000001006', '2d000000-0000-4000-8000-040000001042', 4, 1.0, '2d000000-0000-4000-8000-090000001013'),
    ('2d000000-0000-4000-8000-080000001043', '2d000000-0000-4000-8000-070000001006', '2d000000-0000-4000-8000-040000001043', 5, 1.0, '2d000000-0000-4000-8000-090000001013'),
    ('2d000000-0000-4000-8000-080000001044', '2d000000-0000-4000-8000-070000001007', '2d000000-0000-4000-8000-040000001044', 1, 1.0, '2d000000-0000-4000-8000-090000001014'),
    ('2d000000-0000-4000-8000-080000001045', '2d000000-0000-4000-8000-070000001007', '2d000000-0000-4000-8000-040000001045', 2, 1.0, '2d000000-0000-4000-8000-090000001014'),
    ('2d000000-0000-4000-8000-080000001046', '2d000000-0000-4000-8000-070000001007', '2d000000-0000-4000-8000-040000001046', 3, 1.0, '2d000000-0000-4000-8000-090000001014'),
    ('2d000000-0000-4000-8000-080000001047', '2d000000-0000-4000-8000-070000001007', '2d000000-0000-4000-8000-040000001047', 4, 1.0, '2d000000-0000-4000-8000-090000001015'),
    ('2d000000-0000-4000-8000-080000001048', '2d000000-0000-4000-8000-070000001007', '2d000000-0000-4000-8000-040000001048', 5, 1.0, '2d000000-0000-4000-8000-090000001015'),
    ('2d000000-0000-4000-8000-080000001049', '2d000000-0000-4000-8000-070000001008', '2d000000-0000-4000-8000-040000001049', 1, 1.0, '2d000000-0000-4000-8000-090000001016'),
    ('2d000000-0000-4000-8000-080000001050', '2d000000-0000-4000-8000-070000001008', '2d000000-0000-4000-8000-040000001050', 2, 1.0, '2d000000-0000-4000-8000-090000001016'),
    ('2d000000-0000-4000-8000-080000001051', '2d000000-0000-4000-8000-070000001008', '2d000000-0000-4000-8000-040000001051', 3, 1.0, '2d000000-0000-4000-8000-090000001016'),
    ('2d000000-0000-4000-8000-080000001052', '2d000000-0000-4000-8000-070000001008', '2d000000-0000-4000-8000-040000001052', 4, 1.0, '2d000000-0000-4000-8000-090000001016'),
    ('2d000000-0000-4000-8000-080000001053', '2d000000-0000-4000-8000-070000001008', '2d000000-0000-4000-8000-040000001053', 5, 1.0, '2d000000-0000-4000-8000-090000001016'),
    ('2d000000-0000-4000-8000-080000001054', '2d000000-0000-4000-8000-070000001009', '2d000000-0000-4000-8000-040000001054', 1, 1.0, '2d000000-0000-4000-8000-090000001017'),
    ('2d000000-0000-4000-8000-080000001055', '2d000000-0000-4000-8000-070000001009', '2d000000-0000-4000-8000-040000001055', 2, 1.0, '2d000000-0000-4000-8000-090000001017'),
    ('2d000000-0000-4000-8000-080000001056', '2d000000-0000-4000-8000-070000001009', '2d000000-0000-4000-8000-040000001056', 3, 1.0, '2d000000-0000-4000-8000-090000001017'),
    ('2d000000-0000-4000-8000-080000001057', '2d000000-0000-4000-8000-070000001009', '2d000000-0000-4000-8000-040000001057', 4, 1.0, '2d000000-0000-4000-8000-090000001017'),
    ('2d000000-0000-4000-8000-080000001058', '2d000000-0000-4000-8000-070000001009', '2d000000-0000-4000-8000-040000001058', 5, 1.0, '2d000000-0000-4000-8000-090000001017'),
    ('2d000000-0000-4000-8000-080000001059', '2d000000-0000-4000-8000-070000001010', '2d000000-0000-4000-8000-040000001059', 1, 1.0, '2d000000-0000-4000-8000-090000001018'),
    ('2d000000-0000-4000-8000-080000001060', '2d000000-0000-4000-8000-070000001010', '2d000000-0000-4000-8000-040000001060', 2, 1.0, '2d000000-0000-4000-8000-090000001018'),
    ('2d000000-0000-4000-8000-080000001061', '2d000000-0000-4000-8000-070000001010', '2d000000-0000-4000-8000-040000001061', 3, 1.0, '2d000000-0000-4000-8000-090000001018'),
    ('2d000000-0000-4000-8000-080000001062', '2d000000-0000-4000-8000-070000001010', '2d000000-0000-4000-8000-040000001062', 4, 1.0, '2d000000-0000-4000-8000-090000001019'),
    ('2d000000-0000-4000-8000-080000001063', '2d000000-0000-4000-8000-070000001010', '2d000000-0000-4000-8000-040000001063', 5, 1.0, '2d000000-0000-4000-8000-090000001019'),
    ('2d000000-0000-4000-8000-080000001064', '2d000000-0000-4000-8000-070000001011', '2d000000-0000-4000-8000-040000001064', 1, 1.0, '2d000000-0000-4000-8000-090000001020'),
    ('2d000000-0000-4000-8000-080000001065', '2d000000-0000-4000-8000-070000001011', '2d000000-0000-4000-8000-040000001065', 2, 1.0, '2d000000-0000-4000-8000-090000001020'),
    ('2d000000-0000-4000-8000-080000001066', '2d000000-0000-4000-8000-070000001011', '2d000000-0000-4000-8000-040000001066', 3, 1.0, '2d000000-0000-4000-8000-090000001020'),
    ('2d000000-0000-4000-8000-080000001067', '2d000000-0000-4000-8000-070000001011', '2d000000-0000-4000-8000-040000001067', 4, 1.0, '2d000000-0000-4000-8000-090000001021'),
    ('2d000000-0000-4000-8000-080000001068', '2d000000-0000-4000-8000-070000001011', '2d000000-0000-4000-8000-040000001068', 5, 1.0, '2d000000-0000-4000-8000-090000001021'),
    ('2d000000-0000-4000-8000-080000001069', '2d000000-0000-4000-8000-070000001012', '2d000000-0000-4000-8000-040000001069', 1, 1.0, '2d000000-0000-4000-8000-090000001022'),
    ('2d000000-0000-4000-8000-080000001070', '2d000000-0000-4000-8000-070000001012', '2d000000-0000-4000-8000-040000001070', 2, 1.0, '2d000000-0000-4000-8000-090000001022'),
    ('2d000000-0000-4000-8000-080000001071', '2d000000-0000-4000-8000-070000001012', '2d000000-0000-4000-8000-040000001071', 3, 1.0, '2d000000-0000-4000-8000-090000001022'),
    ('2d000000-0000-4000-8000-080000001072', '2d000000-0000-4000-8000-070000001012', '2d000000-0000-4000-8000-040000001072', 4, 1.0, '2d000000-0000-4000-8000-090000001022'),
    ('2d000000-0000-4000-8000-080000001073', '2d000000-0000-4000-8000-070000001012', '2d000000-0000-4000-8000-040000001073', 5, 1.0, '2d000000-0000-4000-8000-090000001022'),
    ('2d000000-0000-4000-8000-080000001074', '2d000000-0000-4000-8000-070000001013', '2d000000-0000-4000-8000-040000001074', 1, 1.0, '2d000000-0000-4000-8000-090000001023'),
    ('2d000000-0000-4000-8000-080000001075', '2d000000-0000-4000-8000-070000001013', '2d000000-0000-4000-8000-040000001075', 2, 1.0, '2d000000-0000-4000-8000-090000001023'),
    ('2d000000-0000-4000-8000-080000001076', '2d000000-0000-4000-8000-070000001013', '2d000000-0000-4000-8000-040000001076', 3, 1.0, '2d000000-0000-4000-8000-090000001023'),
    ('2d000000-0000-4000-8000-080000001077', '2d000000-0000-4000-8000-070000001013', '2d000000-0000-4000-8000-040000001077', 4, 1.0, '2d000000-0000-4000-8000-090000001023'),
    ('2d000000-0000-4000-8000-080000001078', '2d000000-0000-4000-8000-070000001013', '2d000000-0000-4000-8000-040000001078', 5, 1.0, '2d000000-0000-4000-8000-090000001023'),
    ('2d000000-0000-4000-8000-080000001079', '2d000000-0000-4000-8000-070000001014', '2d000000-0000-4000-8000-040000001079', 1, 1.0, '2d000000-0000-4000-8000-090000001024'),
    ('2d000000-0000-4000-8000-080000001080', '2d000000-0000-4000-8000-070000001014', '2d000000-0000-4000-8000-040000001080', 2, 1.0, '2d000000-0000-4000-8000-090000001024'),
    ('2d000000-0000-4000-8000-080000001081', '2d000000-0000-4000-8000-070000001014', '2d000000-0000-4000-8000-040000001081', 3, 1.0, '2d000000-0000-4000-8000-090000001024'),
    ('2d000000-0000-4000-8000-080000001082', '2d000000-0000-4000-8000-070000001014', '2d000000-0000-4000-8000-040000001082', 4, 1.0, '2d000000-0000-4000-8000-090000001025'),
    ('2d000000-0000-4000-8000-080000001083', '2d000000-0000-4000-8000-070000001014', '2d000000-0000-4000-8000-040000001083', 5, 1.0, '2d000000-0000-4000-8000-090000001025'),
    ('2d000000-0000-4000-8000-080000001084', '2d000000-0000-4000-8000-070000001015', '2d000000-0000-4000-8000-040000001084', 1, 1.0, '2d000000-0000-4000-8000-090000001026'),
    ('2d000000-0000-4000-8000-080000001085', '2d000000-0000-4000-8000-070000001015', '2d000000-0000-4000-8000-040000001085', 2, 1.0, '2d000000-0000-4000-8000-090000001026'),
    ('2d000000-0000-4000-8000-080000001086', '2d000000-0000-4000-8000-070000001015', '2d000000-0000-4000-8000-040000001086', 3, 1.0, '2d000000-0000-4000-8000-090000001026'),
    ('2d000000-0000-4000-8000-080000001087', '2d000000-0000-4000-8000-070000001015', '2d000000-0000-4000-8000-040000001087', 4, 1.0, '2d000000-0000-4000-8000-090000001027'),
    ('2d000000-0000-4000-8000-080000001088', '2d000000-0000-4000-8000-070000001015', '2d000000-0000-4000-8000-040000001088', 5, 1.0, '2d000000-0000-4000-8000-090000001027'),
    ('2d000000-0000-4000-8000-080000001089', '2d000000-0000-4000-8000-070000001016', '2d000000-0000-4000-8000-040000001089', 1, 1.0, '2d000000-0000-4000-8000-090000001028'),
    ('2d000000-0000-4000-8000-080000001090', '2d000000-0000-4000-8000-070000001016', '2d000000-0000-4000-8000-040000001090', 2, 1.0, '2d000000-0000-4000-8000-090000001028'),
    ('2d000000-0000-4000-8000-080000001091', '2d000000-0000-4000-8000-070000001016', '2d000000-0000-4000-8000-040000001091', 3, 1.0, '2d000000-0000-4000-8000-090000001028'),
    ('2d000000-0000-4000-8000-080000001092', '2d000000-0000-4000-8000-070000001016', '2d000000-0000-4000-8000-040000001092', 4, 1.0, '2d000000-0000-4000-8000-090000001028'),
    ('2d000000-0000-4000-8000-080000001093', '2d000000-0000-4000-8000-070000001016', '2d000000-0000-4000-8000-040000001093', 5, 1.0, '2d000000-0000-4000-8000-090000001028'),
    ('2d000000-0000-4000-8000-080000001094', '2d000000-0000-4000-8000-070000001017', '2d000000-0000-4000-8000-040000001094', 1, 1.0, '2d000000-0000-4000-8000-090000001029'),
    ('2d000000-0000-4000-8000-080000001095', '2d000000-0000-4000-8000-070000001018', '2d000000-0000-4000-8000-040000001095', 1, 1.0, '2d000000-0000-4000-8000-090000001030'),
    ('2d000000-0000-4000-8000-080000001096', '2d000000-0000-4000-8000-070000001019', '2d000000-0000-4000-8000-040000001096', 1, 1.0, '2d000000-0000-4000-8000-090000001031'),
    ('2d000000-0000-4000-8000-080000001097', '2d000000-0000-4000-8000-070000001020', '2d000000-0000-4000-8000-040000001097', 1, 1.0, '2d000000-0000-4000-8000-090000001032'),
    ('2d000000-0000-4000-8000-080000001098', '2d000000-0000-4000-8000-070000001021', '2d000000-0000-4000-8000-040000001098', 1, 1.0, '2d000000-0000-4000-8000-090000001033'),
    ('2d000000-0000-4000-8000-080000001099', '2d000000-0000-4000-8000-070000001022', '2d000000-0000-4000-8000-040000001099', 1, 1.0, '2d000000-0000-4000-8000-090000001034'),
    ('2d000000-0000-4000-8000-080000001100', '2d000000-0000-4000-8000-070000001023', '2d000000-0000-4000-8000-040000001100', 1, 1.0, '2d000000-0000-4000-8000-090000001035'),
    ('2d000000-0000-4000-8000-080000001101', '2d000000-0000-4000-8000-070000001024', '2d000000-0000-4000-8000-040000001101', 1, 1.0, '2d000000-0000-4000-8000-090000001036'),
    ('2d000000-0000-4000-8000-080000001102', '2d000000-0000-4000-8000-070000001025', '2d000000-0000-4000-8000-040000001102', 1, 1.0, '2d000000-0000-4000-8000-090000001037'),
    ('2d000000-0000-4000-8000-080000001103', '2d000000-0000-4000-8000-070000001026', '2d000000-0000-4000-8000-040000001103', 1, 1.0, '2d000000-0000-4000-8000-090000001038'),
    ('2d000000-0000-4000-8000-080000001104', '2d000000-0000-4000-8000-070000001027', '2d000000-0000-4000-8000-040000001104', 1, 1.0, '2d000000-0000-4000-8000-090000001039'),
    ('2d000000-0000-4000-8000-080000001105', '2d000000-0000-4000-8000-070000001028', '2d000000-0000-4000-8000-040000001105', 1, 1.0, '2d000000-0000-4000-8000-090000001040'),
    ('2d000000-0000-4000-8000-080000001106', '2d000000-0000-4000-8000-070000001029', '2d000000-0000-4000-8000-040000001106', 1, 1.0, '2d000000-0000-4000-8000-090000001041'),
    ('2d000000-0000-4000-8000-080000001107', '2d000000-0000-4000-8000-070000001029', '2d000000-0000-4000-8000-040000001107', 2, 1.0, '2d000000-0000-4000-8000-090000001041'),
    ('2d000000-0000-4000-8000-080000001108', '2d000000-0000-4000-8000-070000001029', '2d000000-0000-4000-8000-040000001108', 3, 1.0, '2d000000-0000-4000-8000-090000001041'),
    ('2d000000-0000-4000-8000-080000001109', '2d000000-0000-4000-8000-070000001029', '2d000000-0000-4000-8000-040000001109', 4, 1.0, '2d000000-0000-4000-8000-090000001041'),
    ('2d000000-0000-4000-8000-080000001110', '2d000000-0000-4000-8000-070000001030', '2d000000-0000-4000-8000-040000001110', 1, 1.0, '2d000000-0000-4000-8000-090000001042'),
    ('2d000000-0000-4000-8000-080000001111', '2d000000-0000-4000-8000-070000001031', '2d000000-0000-4000-8000-040000001111', 1, 1.0, '2d000000-0000-4000-8000-090000001043'),
    ('2d000000-0000-4000-8000-080000001112', '2d000000-0000-4000-8000-070000001031', '2d000000-0000-4000-8000-040000001112', 2, 1.0, '2d000000-0000-4000-8000-090000001043'),
    ('2d000000-0000-4000-8000-080000001113', '2d000000-0000-4000-8000-070000001031', '2d000000-0000-4000-8000-040000001113', 3, 1.0, '2d000000-0000-4000-8000-090000001043'),
    ('2d000000-0000-4000-8000-080000001114', '2d000000-0000-4000-8000-070000001032', '2d000000-0000-4000-8000-040000001114', 1, 1.0, '2d000000-0000-4000-8000-090000001044'),
    ('2d000000-0000-4000-8000-080000001115', '2d000000-0000-4000-8000-070000001032', '2d000000-0000-4000-8000-040000001115', 2, 1.0, '2d000000-0000-4000-8000-090000001044'),
    ('2d000000-0000-4000-8000-080000001116', '2d000000-0000-4000-8000-070000001032', '2d000000-0000-4000-8000-040000001116', 3, 1.0, '2d000000-0000-4000-8000-090000001044'),
    ('2d000000-0000-4000-8000-080000001117', '2d000000-0000-4000-8000-070000001032', '2d000000-0000-4000-8000-040000001117', 4, 1.0, '2d000000-0000-4000-8000-090000001044'),
    ('2d000000-0000-4000-8000-080000001118', '2d000000-0000-4000-8000-070000001033', '2d000000-0000-4000-8000-040000001118', 1, 1.0, '2d000000-0000-4000-8000-090000001045'),
    ('2d000000-0000-4000-8000-080000001119', '2d000000-0000-4000-8000-070000001034', '2d000000-0000-4000-8000-040000001119', 1, 1.0, '2d000000-0000-4000-8000-090000001046'),
    ('2d000000-0000-4000-8000-080000001120', '2d000000-0000-4000-8000-070000001034', '2d000000-0000-4000-8000-040000001120', 2, 1.0, '2d000000-0000-4000-8000-090000001046'),
    ('2d000000-0000-4000-8000-080000001121', '2d000000-0000-4000-8000-070000001034', '2d000000-0000-4000-8000-040000001121', 3, 1.0, '2d000000-0000-4000-8000-090000001046'),
    ('2d000000-0000-4000-8000-080000001122', '2d000000-0000-4000-8000-070000001035', '2d000000-0000-4000-8000-040000001122', 1, 1.0, '2d000000-0000-4000-8000-090000001047'),
    ('2d000000-0000-4000-8000-080000001123', '2d000000-0000-4000-8000-070000001035', '2d000000-0000-4000-8000-040000001123', 2, 1.0, '2d000000-0000-4000-8000-090000001047'),
    ('2d000000-0000-4000-8000-080000001124', '2d000000-0000-4000-8000-070000001035', '2d000000-0000-4000-8000-040000001124', 3, 1.0, '2d000000-0000-4000-8000-090000001047'),
    ('2d000000-0000-4000-8000-080000001125', '2d000000-0000-4000-8000-070000001035', '2d000000-0000-4000-8000-040000001125', 4, 1.0, '2d000000-0000-4000-8000-090000001047'),
    ('2d000000-0000-4000-8000-080000001126', '2d000000-0000-4000-8000-070000001036', '2d000000-0000-4000-8000-040000001126', 1, 1.0, '2d000000-0000-4000-8000-090000001048'),
    ('2d000000-0000-4000-8000-080000001127', '2d000000-0000-4000-8000-070000001037', '2d000000-0000-4000-8000-040000001127', 1, 1.0, '2d000000-0000-4000-8000-090000001049'),
    ('2d000000-0000-4000-8000-080000001128', '2d000000-0000-4000-8000-070000001037', '2d000000-0000-4000-8000-040000001128', 2, 1.0, '2d000000-0000-4000-8000-090000001049'),
    ('2d000000-0000-4000-8000-080000001129', '2d000000-0000-4000-8000-070000001037', '2d000000-0000-4000-8000-040000001129', 3, 1.0, '2d000000-0000-4000-8000-090000001049'),
    ('2d000000-0000-4000-8000-080000001130', '2d000000-0000-4000-8000-070000001038', '2d000000-0000-4000-8000-040000001130', 1, 1.0, '2d000000-0000-4000-8000-090000001050'),
    ('2d000000-0000-4000-8000-080000001131', '2d000000-0000-4000-8000-070000001038', '2d000000-0000-4000-8000-040000001131', 2, 1.0, '2d000000-0000-4000-8000-090000001050'),
    ('2d000000-0000-4000-8000-080000001132', '2d000000-0000-4000-8000-070000001038', '2d000000-0000-4000-8000-040000001132', 3, 1.0, '2d000000-0000-4000-8000-090000001050'),
    ('2d000000-0000-4000-8000-080000001133', '2d000000-0000-4000-8000-070000001038', '2d000000-0000-4000-8000-040000001133', 4, 1.0, '2d000000-0000-4000-8000-090000001050'),
    ('2d000000-0000-4000-8000-080000001134', '2d000000-0000-4000-8000-070000001039', '2d000000-0000-4000-8000-040000001134', 1, 1.0, '2d000000-0000-4000-8000-090000001051'),
    ('2d000000-0000-4000-8000-080000001135', '2d000000-0000-4000-8000-070000001040', '2d000000-0000-4000-8000-040000001135', 1, 1.0, '2d000000-0000-4000-8000-090000001052'),
    ('2d000000-0000-4000-8000-080000001136', '2d000000-0000-4000-8000-070000001040', '2d000000-0000-4000-8000-040000001136', 2, 1.0, '2d000000-0000-4000-8000-090000001052'),
    ('2d000000-0000-4000-8000-080000001137', '2d000000-0000-4000-8000-070000001040', '2d000000-0000-4000-8000-040000001137', 3, 1.0, '2d000000-0000-4000-8000-090000001052'),
    ('2d000000-0000-4000-8000-080000001138', '2d000000-0000-4000-8000-070000001041', '2d000000-0000-4000-8000-040000001138', 1, 1.0, '2d000000-0000-4000-8000-090000001053'),
    ('2d000000-0000-4000-8000-080000001139', '2d000000-0000-4000-8000-070000001041', '2d000000-0000-4000-8000-040000001139', 2, 1.0, '2d000000-0000-4000-8000-090000001053'),
    ('2d000000-0000-4000-8000-080000001140', '2d000000-0000-4000-8000-070000001041', '2d000000-0000-4000-8000-040000001140', 3, 1.0, '2d000000-0000-4000-8000-090000001053'),
    ('2d000000-0000-4000-8000-080000001141', '2d000000-0000-4000-8000-070000001041', '2d000000-0000-4000-8000-040000001141', 4, 1.0, '2d000000-0000-4000-8000-090000001053'),
    ('2d000000-0000-4000-8000-080000001142', '2d000000-0000-4000-8000-070000001042', '2d000000-0000-4000-8000-040000001142', 1, 1.0, '2d000000-0000-4000-8000-090000001054'),
    ('2d000000-0000-4000-8000-080000001143', '2d000000-0000-4000-8000-070000001043', '2d000000-0000-4000-8000-040000001143', 1, 1.0, '2d000000-0000-4000-8000-090000001055'),
    ('2d000000-0000-4000-8000-080000001144', '2d000000-0000-4000-8000-070000001043', '2d000000-0000-4000-8000-040000001144', 2, 1.0, '2d000000-0000-4000-8000-090000001055'),
    ('2d000000-0000-4000-8000-080000001145', '2d000000-0000-4000-8000-070000001043', '2d000000-0000-4000-8000-040000001145', 3, 1.0, '2d000000-0000-4000-8000-090000001055'),
    ('2d000000-0000-4000-8000-080000001146', '2d000000-0000-4000-8000-070000001044', '2d000000-0000-4000-8000-040000001146', 1, 1.0, '2d000000-0000-4000-8000-090000001056'),
    ('2d000000-0000-4000-8000-080000001147', '2d000000-0000-4000-8000-070000001044', '2d000000-0000-4000-8000-040000001147', 2, 1.0, '2d000000-0000-4000-8000-090000001056'),
    ('2d000000-0000-4000-8000-080000001148', '2d000000-0000-4000-8000-070000001044', '2d000000-0000-4000-8000-040000001148', 3, 1.0, '2d000000-0000-4000-8000-090000001056'),
    ('2d000000-0000-4000-8000-080000001149', '2d000000-0000-4000-8000-070000001044', '2d000000-0000-4000-8000-040000001149', 4, 1.0, '2d000000-0000-4000-8000-090000001056'),
    ('2d000000-0000-4000-8000-080000001150', '2d000000-0000-4000-8000-070000001045', '2d000000-0000-4000-8000-040000001150', 1, 1.0, '2d000000-0000-4000-8000-090000001057'),
    ('2d000000-0000-4000-8000-080000001151', '2d000000-0000-4000-8000-070000001046', '2d000000-0000-4000-8000-040000001151', 1, 1.0, '2d000000-0000-4000-8000-090000001058'),
    ('2d000000-0000-4000-8000-080000001152', '2d000000-0000-4000-8000-070000001046', '2d000000-0000-4000-8000-040000001152', 2, 1.0, '2d000000-0000-4000-8000-090000001058'),
    ('2d000000-0000-4000-8000-080000001153', '2d000000-0000-4000-8000-070000001046', '2d000000-0000-4000-8000-040000001153', 3, 1.0, '2d000000-0000-4000-8000-090000001058')
ON CONFLICT DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2d000000-0000-4000-8000-0a0000001001', '2d000000-0000-4000-8000-020000001001', '2d000000-0000-4000-8000-070000001001', 0),
    ('2d000000-0000-4000-8000-0a0000001002', '2d000000-0000-4000-8000-020000001002', '2d000000-0000-4000-8000-070000001002', 0),
    ('2d000000-0000-4000-8000-0a0000001003', '2d000000-0000-4000-8000-020000001003', '2d000000-0000-4000-8000-070000001003', 0),
    ('2d000000-0000-4000-8000-0a0000001004', '2d000000-0000-4000-8000-020000001004', '2d000000-0000-4000-8000-070000001004', 0),
    ('2d000000-0000-4000-8000-0a0000001005', '2d000000-0000-4000-8000-020000001005', '2d000000-0000-4000-8000-070000001005', 0),
    ('2d000000-0000-4000-8000-0a0000001006', '2d000000-0000-4000-8000-020000001006', '2d000000-0000-4000-8000-070000001006', 0),
    ('2d000000-0000-4000-8000-0a0000001007', '2d000000-0000-4000-8000-020000001007', '2d000000-0000-4000-8000-070000001007', 0),
    ('2d000000-0000-4000-8000-0a0000001008', '2d000000-0000-4000-8000-020000001008', '2d000000-0000-4000-8000-070000001008', 0),
    ('2d000000-0000-4000-8000-0a0000001009', '2d000000-0000-4000-8000-020000001009', '2d000000-0000-4000-8000-070000001009', 0),
    ('2d000000-0000-4000-8000-0a0000001010', '2d000000-0000-4000-8000-020000001010', '2d000000-0000-4000-8000-070000001010', 0),
    ('2d000000-0000-4000-8000-0a0000001011', '2d000000-0000-4000-8000-020000001011', '2d000000-0000-4000-8000-070000001011', 0),
    ('2d000000-0000-4000-8000-0a0000001012', '2d000000-0000-4000-8000-020000001012', '2d000000-0000-4000-8000-070000001012', 0),
    ('2d000000-0000-4000-8000-0a0000001013', '2d000000-0000-4000-8000-020000001013', '2d000000-0000-4000-8000-070000001013', 0),
    ('2d000000-0000-4000-8000-0a0000001014', '2d000000-0000-4000-8000-020000001014', '2d000000-0000-4000-8000-070000001014', 0),
    ('2d000000-0000-4000-8000-0a0000001015', '2d000000-0000-4000-8000-020000001015', '2d000000-0000-4000-8000-070000001015', 0),
    ('2d000000-0000-4000-8000-0a0000001016', '2d000000-0000-4000-8000-020000001016', '2d000000-0000-4000-8000-070000001016', 0)
ON CONFLICT DO NOTHING;
