-- Placement exams of the BAND_0_3 pool, published when this migration runs. Generated from
-- scripts/placement-exams/exams by scripts/placement-exams/generate-placement-seed.mjs; edit the sources and
-- regenerate, never this file. Listening audio keys (listening/placement/<package code>/part<N>.mp3) must be
-- uploaded to the media bucket before the migration is deployed.

-- PLACEMENT-B03-A: IELTS placement test, band 0-3 (A)

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('8ca2972c-5b44-5437-8430-3703c6f6733a', 'AUDIO', 'Receptionist: Hello, Bright Language School.
Caller: Hello. I want to join an English class.
Receptionist: Good. What is your name, please?
Caller: My name is Anna Lopez. That is L, O, P, E, Z.
Receptionist: Thank you, Anna. Is this your first English class?
Caller: Yes, it is.
Receptionist: Then you can join the beginner class. It is on Monday and Wednesday.
Caller: What time is the class?
Receptionist: It starts at six o''clock in the evening.
Caller: That is good. I finish work at five. How much is it?
Receptionist: It is eighty pounds for one month.
Caller: OK. Can I pay by card?
Receptionist: Yes, you can. And what is your phone number?
Caller: My number is five, five, one, two, nine, eight.
Receptionist: Thank you. Please bring a pen and a notebook. The first class is on the third of March.
Caller: Thank you very much. Goodbye.', 'listening/placement/placement-b03-a/part1.mp3', 98, 'VALID'),
    ('f9b8f4ed-ea6e-5936-af11-8c8b4249efb2', 'AUDIO', 'Guide: Welcome to Green Park. My name is Ben and I work here. The park opens at eight o''clock every morning and closes at six in the evening. It is free, so you do not need to pay.
Guide: There is a big lake in the middle of the park. You can walk around the lake in twenty minutes. Many ducks live there, so please do not give them bread. It is bad for them.
Guide: On the left, you can see the café. It sells coffee, tea and cakes. A small cake costs two pounds. The toilets are next to the café.
Guide: On Saturdays there is a children''s football game at ten o''clock. On Sundays there is a market near the gate, with fruit and flowers.
Guide: You can bring your dog, but it must stay on a lead. You cannot ride a bike on the grass. There are bike paths at the side of the park.
Guide: If you need help, my office is next to the main gate. Enjoy your day!', 'listening/placement/placement-b03-a/part2.mp3', 113, 'VALID');

INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('83c1693b-bb68-5caf-897a-ac3b64ad433b', 'PASSAGE', 'Maria''s Bakery

Maria has a small bakery in the centre of a town called Oakfield. She opens the shop at six o''clock every morning, six days a week. The shop is closed on Mondays.

Maria learned to bake bread from her grandmother when she was twelve years old. Now she makes about fifty loaves of bread every day, and also cakes and biscuits. Her most popular cake is the apple cake. A slice costs two pounds.

Most customers live near the shop. In the morning, many people buy bread on their way to work. At lunchtime, students from the school next door come in and buy sandwiches.

Last year, Maria started selling her bread online. People can order bread on her website, and a young man called Ali brings it to their homes by bicycle. Maria says the online shop is small, but it is growing every month.

Maria would like to open a second shop in the future, but she says she needs to find a good baker to help her.', 'VALID'),
    ('562eafab-6805-522c-b6ff-14f70fb21352', 'PASSAGE', 'Welcome to Riverside Library

The library is open from Monday to Saturday. On weekdays it opens at nine in the morning and closes at seven in the evening. On Saturdays it closes earlier, at four o''clock. It is closed on Sundays.

Everyone can join the library for free. You only need a photo and a letter with your address. With your library card you can borrow up to six books for three weeks. If you keep a book for longer, you must pay fifty pence for each extra week.

The library has a children''s room on the first floor. Every Saturday morning at eleven o''clock, a teacher reads stories there for young children. There is also a quiet room on the second floor where students can study. You can use the computers for one hour a day. This is free, but you must ask the librarian first.

You cannot eat in the library, but there is a small café outside the front door.

For more information, please phone the library or visit our website.', 'VALID');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('27623798-abe8-5c8d-83f9-af3a68cbc88b', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('16b7ea27-26b1-56a5-83a8-49462e6260e1', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('b8e21ca5-bc3f-566c-9d46-2ca8c675c169', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('ffbc9aeb-d29f-5498-b6d8-d0017cae1d04', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('806bb445-3267-5341-8bc5-7f9bbc47b364', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('21c7a335-1c08-5a23-8f18-1a650b355ae3', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('bf9f0738-68d2-5c04-b731-9fbc0766a349', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('9641b330-a335-50ca-ae72-c9e3344d6629', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('b5afccf8-2f6f-5f9d-b688-854480b2124c', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('194ace6e-54df-548c-bedf-a8f958d0bc77', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('d5d2ec03-6acf-5ed7-937b-db2101593b3d', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('1c49d5b4-a1dc-5d8c-b8cc-c5594bdb3e06', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('95c40387-a3ba-5d1f-b41e-289a586aa657', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('f5a5d0f9-66dc-55fe-abb4-e77544dbe8f2', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('281a3203-b357-540d-8ad3-752ed0f62a8e', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('7e69af5a-02ac-506a-9bc0-e0b8edef5ac8', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2cac79c5-23c8-5f44-badc-d93b3ceb3841', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('ceee3b8c-05eb-5bcf-b2a1-0bf26cc33e38', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('000bf5a3-0f88-5cbe-b028-34eeaae55acd', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('f3927585-ea95-5511-9cf7-259935598563', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('f2300eff-70db-58ed-80d4-8005c35cdde6', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('038b156a-c1d8-50c1-aad5-4dad0813e8b0', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('b408fd28-5506-56a9-8bce-7a2cf93210de', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('25c4e20c-b4f9-5677-b394-2482ee6680e3', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('9e49ad4d-4264-545c-b1cd-ea4059b8d9ef', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('3a15ae71-136d-5b6e-af17-a594b86c0274', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('9d310011-a8e2-529e-9f02-0febaf046bfc', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('1889f128-84a5-5872-a681-3e77fa7fd985', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('a9e4729e-d069-592c-9290-c31b3b4be769', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('91c98b9f-5a41-58d6-b576-46d386e5aead', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('0762936b-d520-5129-b09f-ab7b24b25ad6', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('22503d2e-d7fd-5a4e-9ea1-9ae56851b2e4', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('38ee03a8-2700-52ca-8168-2f4e2f64616e', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('e15da552-cf0d-5346-9793-bf7885e5505c', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('9d159a08-69de-539b-804f-5ec84c8361a5', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('a11899e2-15eb-5553-86b9-5a9cdb5d41e7', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('7e794c62-aabe-5e80-84d2-d3b8a5cb9ce3', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('3dbb6c4f-bd76-5214-8779-c3fcbe6d3e51', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('5b9ab5a9-c414-595e-b539-7a94605733a6', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('5cec706d-8267-5394-a72e-fce8f814f635', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('5cf85cd1-3cd0-55cc-b366-011b5a90c2fd', 'ESSAY', 'WRITING', 'PLACEMENT', 'PUBLISHED'),
    ('93c8b70f-1062-5d61-aad7-535b59446dbe', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('1ff030e7-8835-5af7-91cd-fa1b55b28315', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('26295f52-132a-58d3-8316-606e98b0e85b', '27623798-abe8-5c8d-83f9-af3a68cbc88b', 1, 'Student''s family name: Anna ______', NULL,
     '{"type":"FILL","accepted":["lopez"]}'::jsonb, 1, 'She spells L, O, P, E, Z.', 'PUBLISHED'),
    ('61a92133-9afd-5a62-80bc-6956d4a7d103', '16b7ea27-26b1-56a5-83a8-49462e6260e1', 1, 'Level of the class: ______', NULL,
     '{"type":"FILL","accepted":["beginner"]}'::jsonb, 1, 'It is her first class, so she joins the beginner class.', 'PUBLISHED'),
    ('a8d57cf2-38bd-51e8-9cd1-4a647ea6e905', 'b8e21ca5-bc3f-566c-9d46-2ca8c675c169', 1, 'Days of the class: Monday and ______', NULL,
     '{"type":"FILL","accepted":["wednesday"]}'::jsonb, 1, 'The class is on Monday and Wednesday.', 'PUBLISHED'),
    ('91a2ea09-0cd4-53b7-abd9-e15394d27774', 'ffbc9aeb-d29f-5498-b6d8-d0017cae1d04', 1, 'Start time: ______ in the evening', NULL,
     '{"type":"FILL","accepted":["6","six","6.00","6:00","6 o''clock","six o''clock"]}'::jsonb, 1, 'It starts at six o''clock in the evening.', 'PUBLISHED'),
    ('6b278a55-4867-5f29-b19a-ea00d65c4ee2', '806bb445-3267-5341-8bc5-7f9bbc47b364', 1, 'Price for one month: £______', NULL,
     '{"type":"FILL","accepted":["80","eighty"]}'::jsonb, 1, 'It is eighty pounds for one month.', 'PUBLISHED'),
    ('01015d93-d693-5811-9f9d-bb4499b0bdaf', '21c7a335-1c08-5a23-8f18-1a650b355ae3', 1, 'The student will pay by ______.', NULL,
     '{"type":"FILL","accepted":["card"]}'::jsonb, 1, 'She asks if she can pay by card, and the answer is yes.', 'PUBLISHED'),
    ('76fb00da-f69a-5168-ac92-4bfffd84fe1b', 'bf9f0738-68d2-5c04-b731-9fbc0766a349', 1, 'Phone number: ______', NULL,
     '{"type":"FILL","accepted":["551298","5 5 1 2 9 8"]}'::jsonb, 1, 'The number is five, five, one, two, nine, eight.', 'PUBLISHED'),
    ('3a454131-d57d-5dcc-befe-ecdef587547d', '9641b330-a335-50ca-ae72-c9e3344d6629', 1, 'Please bring a pen and a ______.', NULL,
     '{"type":"FILL","accepted":["notebook"]}'::jsonb, 1, 'The receptionist says to bring a pen and a notebook.', 'PUBLISHED'),
    ('0c19eb03-9422-5c57-8491-a47e7c19bb3e', 'b5afccf8-2f6f-5f9d-b688-854480b2124c', 1, 'Date of the first class: ______ March', NULL,
     '{"type":"FILL","accepted":["3","3rd","third","the 3rd","the third"]}'::jsonb, 1, 'The first class is on the third of March.', 'PUBLISHED'),
    ('f1a1338e-b188-5675-b194-e81e1e09411b', '194ace6e-54df-548c-bedf-a8f958d0bc77', 1, 'What time does Anna finish work?', '[{"optionKey":"A","content":"Four o''clock","sortOrder":1},{"optionKey":"B","content":"Five o''clock","sortOrder":2},{"optionKey":"C","content":"Six o''clock","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'She says she finishes work at five.', 'PUBLISHED'),
    ('995b2d26-999e-5605-a0fe-5da416f066f3', 'd5d2ec03-6acf-5ed7-937b-db2101593b3d', 1, 'The park opens at ______ o''clock in the morning.', NULL,
     '{"type":"FILL","accepted":["8","eight","8.00","8:00"]}'::jsonb, 1, 'The park opens at eight o''clock.', 'PUBLISHED'),
    ('a65968e5-efcd-5896-8839-30ee826cdc18', '1c49d5b4-a1dc-5d8c-b8cc-c5594bdb3e06', 1, 'How much does it cost to enter the park?', '[{"optionKey":"A","content":"Nothing","sortOrder":1},{"optionKey":"B","content":"Two pounds","sortOrder":2},{"optionKey":"C","content":"Five pounds","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'It is free.', 'PUBLISHED'),
    ('f71b7a02-5e49-5467-bd27-aa59debc6504', '95c40387-a3ba-5d1f-b41e-289a586aa657', 1, 'It takes about ______ minutes to walk around the lake.', NULL,
     '{"type":"FILL","accepted":["20","twenty"]}'::jsonb, 1, 'You can walk around the lake in twenty minutes.', 'PUBLISHED'),
    ('25498b59-b618-5683-ba64-242ad00d7a83', 'f5a5d0f9-66dc-55fe-abb4-e77544dbe8f2', 1, 'What should visitors not give to the ducks?', '[{"optionKey":"A","content":"Bread","sortOrder":1},{"optionKey":"B","content":"Water","sortOrder":2},{"optionKey":"C","content":"Cake","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Please do not give them bread.', 'PUBLISHED'),
    ('5a4c44eb-ff47-5153-9e76-a9f208a673d5', '281a3203-b357-540d-8ad3-752ed0f62a8e', 1, 'A small cake in the café costs £______.', NULL,
     '{"type":"FILL","accepted":["2","two"]}'::jsonb, 1, 'A small cake costs two pounds.', 'PUBLISHED'),
    ('347b151c-be56-5201-9601-cfe9614bba8f', '7e69af5a-02ac-506a-9bc0-e0b8edef5ac8', 1, 'Where are the toilets?', '[{"optionKey":"A","content":"Next to the café","sortOrder":1},{"optionKey":"B","content":"Near the lake","sortOrder":2},{"optionKey":"C","content":"By the main gate","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The toilets are next to the café.', 'PUBLISHED'),
    ('68f0a321-2705-50bb-8a79-fae7c7272a3a', '2cac79c5-23c8-5f44-badc-d93b3ceb3841', 1, 'What happens on Saturdays at ten o''clock?', '[{"optionKey":"A","content":"A market","sortOrder":1},{"optionKey":"B","content":"A football game","sortOrder":2},{"optionKey":"C","content":"A music show","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'There is a children''s football game at ten o''clock.', 'PUBLISHED'),
    ('ee829da5-2f62-58b4-a62e-4f7060a70c0d', 'ceee3b8c-05eb-5bcf-b2a1-0bf26cc33e38', 1, 'On Sundays there is a ______ near the gate.', NULL,
     '{"type":"FILL","accepted":["market"]}'::jsonb, 1, 'On Sundays there is a market near the gate.', 'PUBLISHED'),
    ('33281aad-0365-5a5a-a14a-ea99af14ba8d', '000bf5a3-0f88-5cbe-b028-34eeaae55acd', 1, 'Dogs must stay ...', '[{"optionKey":"A","content":"at home","sortOrder":1},{"optionKey":"B","content":"on a lead","sortOrder":2},{"optionKey":"C","content":"outside the park","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'You can bring your dog, but it must stay on a lead.', 'PUBLISHED'),
    ('d7e90218-f50f-55f0-9995-e81c3fea4577', 'f3927585-ea95-5511-9cf7-259935598563', 1, 'Where is the guide''s office?', '[{"optionKey":"A","content":"Next to the café","sortOrder":1},{"optionKey":"B","content":"By the lake","sortOrder":2},{"optionKey":"C","content":"Next to the main gate","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'His office is next to the main gate.', 'PUBLISHED'),
    ('3402e1a4-78b0-501d-ac45-078d015c864d', 'f2300eff-70db-58ed-80d4-8005c35cdde6', 1, 'Maria''s bakery is in a town called Oakfield.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'The bakery is in the centre of a town called Oakfield.', 'PUBLISHED'),
    ('161bbcf4-a681-5876-941d-42a966212805', '038b156a-c1d8-50c1-aad5-4dad0813e8b0', 1, 'The shop is open on Mondays.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'The shop is closed on Mondays.', 'PUBLISHED'),
    ('9ab5140a-5bc7-5201-9bd3-24a4380486ed', 'b408fd28-5506-56a9-8bce-7a2cf93210de', 1, 'Maria learned to bake at school.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'She learned from her grandmother.', 'PUBLISHED'),
    ('c2863c10-b51d-5e81-bd54-83a83bfa8387', '25c4e20c-b4f9-5677-b394-2482ee6680e3', 1, 'Maria sells more cakes than bread.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'The passage does not compare how many cakes and loaves she sells.', 'PUBLISHED'),
    ('86c55c74-2645-58e8-9981-c412205e3394', '9e49ad4d-4264-545c-b1cd-ea4059b8d9ef', 1, 'How old was Maria when she learned to bake?', '[{"optionKey":"A","content":"Six","sortOrder":1},{"optionKey":"B","content":"Twelve","sortOrder":2},{"optionKey":"C","content":"Twenty","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'She learned when she was twelve years old.', 'PUBLISHED'),
    ('c10220ae-99cd-5a09-862d-9d08a1961177', '3a15ae71-136d-5b6e-af17-a594b86c0274', 1, 'Which cake is the most popular?', '[{"optionKey":"A","content":"The apple cake","sortOrder":1},{"optionKey":"B","content":"The chocolate cake","sortOrder":2},{"optionKey":"C","content":"The lemon cake","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Her most popular cake is the apple cake.', 'PUBLISHED'),
    ('fe553933-c871-5046-976b-22029339d34a', '9d310011-a8e2-529e-9f02-0febaf046bfc', 1, 'Who comes to the shop at lunchtime?', '[{"optionKey":"A","content":"People going to work","sortOrder":1},{"optionKey":"B","content":"Students","sortOrder":2},{"optionKey":"C","content":"Tourists","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'At lunchtime, students from the school next door come in.', 'PUBLISHED'),
    ('0e8dcc1a-650b-5a80-8a8f-3f50a7997fd7', '1889f128-84a5-5872-a681-3e77fa7fd985', 1, 'Maria makes about ______ loaves of bread every day.', NULL,
     '{"type":"FILL","accepted":["50","fifty"]}'::jsonb, 1, 'She makes about fifty loaves every day.', 'PUBLISHED'),
    ('4c2311f7-dbbc-5da4-8b9c-029cab1859e3', 'a9e4729e-d069-592c-9290-c31b3b4be769', 1, 'Ali takes the bread to people''s homes by ______.', NULL,
     '{"type":"FILL","accepted":["bicycle"]}'::jsonb, 1, 'Ali brings the bread to homes by bicycle.', 'PUBLISHED'),
    ('490d2b6e-ec30-5068-8fa6-e785798e2ae1', '91c98b9f-5a41-58d6-b576-46d386e5aead', 1, 'Maria needs to find a good ______ to help her.', NULL,
     '{"type":"FILL","accepted":["baker"]}'::jsonb, 1, 'She says she needs to find a good baker to help her.', 'PUBLISHED'),
    ('ae1e9229-be37-50ed-9685-288ddbf66d33', '0762936b-d520-5129-b09f-ab7b24b25ad6', 1, 'The library is open on Sundays.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'It is closed on Sundays.', 'PUBLISHED'),
    ('9c92a2bf-ed60-58bc-aaad-26f387f343fe', '22503d2e-d7fd-5a4e-9ea1-9ae56851b2e4', 1, 'It costs money to join the library.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Everyone can join for free.', 'PUBLISHED'),
    ('915f102b-8016-5cf5-ad46-c13ba32c9b5b', '38ee03a8-2700-52ca-8168-2f4e2f64616e', 1, 'The library has many books in other languages.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'The passage does not say anything about other languages.', 'PUBLISHED'),
    ('d2633c30-728a-5cc1-932c-c8da7fde72c0', 'e15da552-cf0d-5346-9793-bf7885e5505c', 1, 'The library has a room for children.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'There is a children''s room on the first floor.', 'PUBLISHED'),
    ('cb54949f-a780-5f51-96ef-ee91afc0ff27', '9d159a08-69de-539b-804f-5ec84c8361a5', 1, 'On Saturdays the library closes at ...', '[{"optionKey":"A","content":"four o''clock","sortOrder":1},{"optionKey":"B","content":"seven o''clock","sortOrder":2},{"optionKey":"C","content":"nine o''clock","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'On Saturdays it closes at four o''clock.', 'PUBLISHED'),
    ('fd8eb12e-ed5d-5282-b7de-6502bf74cb05', 'a11899e2-15eb-5553-86b9-5a9cdb5d41e7', 1, 'How many books can a member borrow?', '[{"optionKey":"A","content":"Three","sortOrder":1},{"optionKey":"B","content":"Six","sortOrder":2},{"optionKey":"C","content":"Ten","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Members can borrow up to six books.', 'PUBLISHED'),
    ('d0e0ace3-6429-5211-a9ca-99496ec305d7', '7e794c62-aabe-5e80-84d2-d3b8a5cb9ce3', 1, 'What happens on Saturdays at eleven o''clock?', '[{"optionKey":"A","content":"A film for adults","sortOrder":1},{"optionKey":"B","content":"Stories for young children","sortOrder":2},{"optionKey":"C","content":"A class for students","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'A teacher reads stories for young children.', 'PUBLISHED'),
    ('6b7ac112-ca99-5d08-b8ce-a5b099878cff', '3dbb6c4f-bd76-5214-8779-c3fcbe6d3e51', 1, 'A late book costs ______ pence for each extra week.', NULL,
     '{"type":"FILL","accepted":["50","fifty"]}'::jsonb, 1, 'You must pay fifty pence for each extra week.', 'PUBLISHED'),
    ('c7a89880-a84d-5a26-835d-da8d12f726a3', '5b9ab5a9-c414-595e-b539-7a94605733a6', 1, 'The quiet room is on the ______ floor.', NULL,
     '{"type":"FILL","accepted":["second","2nd"]}'::jsonb, 1, 'The quiet room is on the second floor.', 'PUBLISHED'),
    ('5120d9c0-fb6e-5001-9b43-8593e0161e46', '5cec706d-8267-5394-a72e-fce8f814f635', 1, 'Members can use the computers for ______ hour a day.', NULL,
     '{"type":"FILL","accepted":["1","one"]}'::jsonb, 1, 'You can use the computers for one hour a day.', 'PUBLISHED'),
    ('0b9cbe15-7816-51c4-86f2-540f72c27e27', '5cf85cd1-3cd0-55cc-b366-011b5a90c2fd', 1, 'Some people like to live in a city. Other people like to live in a village. Which do you prefer? Give reasons for your answer. Write at least 80 words.', NULL,
     '{"type":"ESSAY","task":"TASK_2","minWords":80,"passBand":3}'::jsonb, 1, 'Graded by the placement essay grader.', 'PUBLISHED'),
    ('ddfd5dbd-ffc0-50bf-9c83-e603eeecc8f7', '93c8b70f-1062-5d61-aad7-535b59446dbe', 1, 'Let''s talk about your family. How many people are in your family? What do you like to do together?', NULL,
     '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED'),
    ('201d28d8-4425-5300-8fa9-e018292a3fea', '1ff030e7-8835-5af7-91cd-fa1b55b28315', 1, 'Describe your favourite food. You should say what it is, who makes it, when you eat it, and why you like it. Speak for about one minute.', NULL,
     '{"type":"SPEAKING","durationSeconds":60}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED');

UPDATE questions q SET current_published_version_id = qv.id
FROM question_versions qv
WHERE qv.question_id = q.id AND qv.id IN (
    '26295f52-132a-58d3-8316-606e98b0e85b',
    '61a92133-9afd-5a62-80bc-6956d4a7d103',
    'a8d57cf2-38bd-51e8-9cd1-4a647ea6e905',
    '91a2ea09-0cd4-53b7-abd9-e15394d27774',
    '6b278a55-4867-5f29-b19a-ea00d65c4ee2',
    '01015d93-d693-5811-9f9d-bb4499b0bdaf',
    '76fb00da-f69a-5168-ac92-4bfffd84fe1b',
    '3a454131-d57d-5dcc-befe-ecdef587547d',
    '0c19eb03-9422-5c57-8491-a47e7c19bb3e',
    'f1a1338e-b188-5675-b194-e81e1e09411b',
    '995b2d26-999e-5605-a0fe-5da416f066f3',
    'a65968e5-efcd-5896-8839-30ee826cdc18',
    'f71b7a02-5e49-5467-bd27-aa59debc6504',
    '25498b59-b618-5683-ba64-242ad00d7a83',
    '5a4c44eb-ff47-5153-9e76-a9f208a673d5',
    '347b151c-be56-5201-9601-cfe9614bba8f',
    '68f0a321-2705-50bb-8a79-fae7c7272a3a',
    'ee829da5-2f62-58b4-a62e-4f7060a70c0d',
    '33281aad-0365-5a5a-a14a-ea99af14ba8d',
    'd7e90218-f50f-55f0-9995-e81c3fea4577',
    '3402e1a4-78b0-501d-ac45-078d015c864d',
    '161bbcf4-a681-5876-941d-42a966212805',
    '9ab5140a-5bc7-5201-9bd3-24a4380486ed',
    'c2863c10-b51d-5e81-bd54-83a83bfa8387',
    '86c55c74-2645-58e8-9981-c412205e3394',
    'c10220ae-99cd-5a09-862d-9d08a1961177',
    'fe553933-c871-5046-976b-22029339d34a',
    '0e8dcc1a-650b-5a80-8a8f-3f50a7997fd7',
    '4c2311f7-dbbc-5da4-8b9c-029cab1859e3',
    '490d2b6e-ec30-5068-8fa6-e785798e2ae1',
    'ae1e9229-be37-50ed-9685-288ddbf66d33',
    '9c92a2bf-ed60-58bc-aaad-26f387f343fe',
    '915f102b-8016-5cf5-ad46-c13ba32c9b5b',
    'd2633c30-728a-5cc1-932c-c8da7fde72c0',
    'cb54949f-a780-5f51-96ef-ee91afc0ff27',
    'fd8eb12e-ed5d-5282-b7de-6502bf74cb05',
    'd0e0ace3-6429-5211-a9ca-99496ec305d7',
    '6b7ac112-ca99-5d08-b8ce-a5b099878cff',
    'c7a89880-a84d-5a26-835d-da8d12f726a3',
    '5120d9c0-fb6e-5001-9b43-8593e0161e46',
    '0b9cbe15-7816-51c4-86f2-540f72c27e27',
    'ddfd5dbd-ffc0-50bf-9c83-e603eeecc8f7',
    '201d28d8-4425-5300-8fa9-e018292a3fea');

INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key, placement_pool)
VALUES ('5ec27c56-7585-5c1e-b31f-111f9299cc0b', 'PLACEMENT-B03-A', 'IELTS placement test, band 0-3 (A)', 'PLACEMENT_TEST', NULL, NULL, NULL, 'PUBLISHED', NULL, 'BAND_0_3');

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
VALUES ('f9f8cdfe-affe-5aef-b380-92e8c3d70af3', '5ec27c56-7585-5c1e-b31f-111f9299cc0b', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions) VALUES
    ('e704504c-6d05-585f-8cf7-c6d1ae4b7af8', 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', 'Listening Part 1: joining an English class', 'LISTENING', 1, NULL, 'Questions 1–10. Complete the form. Write ONE WORD AND/OR A NUMBER for each answer. For question 10, choose A, B or C.'),
    ('2c78227f-9d62-5efe-b788-6a66d82cc55a', 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', 'Listening Part 2: a visit to Green Park', 'LISTENING', 2, NULL, 'Questions 11–20. Choose the correct letter, A, B or C, or write ONE WORD AND/OR A NUMBER where you see a line.'),
    ('c47084bf-b8b3-5a99-b62f-91d6256f4f2a', 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', 'Reading Passage 1: Maria''s bakery', 'READING', 3, 600, 'Questions 21–24: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 25–27: Choose the correct letter, A, B or C. Questions 28–30: Complete the sentences. Write ONE WORD OR A NUMBER from the passage.'),
    ('5e5814eb-7513-585d-8f4d-407bcf4fefd7', 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', 'Reading Passage 2: Riverside Library', 'READING', 4, 600, 'Questions 31–34: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 35–37: Choose the correct letter, A, B or C. Questions 38–40: Complete the sentences. Write ONE WORD OR A NUMBER from the passage.'),
    ('886dad7a-0437-5c09-bbda-1bf3a21da440', 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', 'Writing: city or village', 'WRITING', 5, 1200, 'Spend about 20 minutes on this task. Write at least 80 words. Use simple sentences.'),
    ('5e9eb277-a6d1-5c8b-af67-b2d0f30d0b54', 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', 'Speaking Part 1: your family', 'SPEAKING', 6, NULL, 'Answer the questions in a few short sentences.'),
    ('fdcb3b32-bce7-545e-b41a-d25930189e0e', 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', 'Speaking Part 2: your favourite food', 'SPEAKING', 7, NULL, 'You have one minute to prepare, then speak for about one minute.');

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('cdb4d374-39a2-5442-a45e-ca565bcd3a08', '8ca2972c-5b44-5437-8430-3703c6f6733a', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', 0),
    ('ccacd15b-35ab-50a9-a922-eeefa7632d48', 'f9b8f4ed-ea6e-5936-af11-8c8b4249efb2', '2c78227f-9d62-5efe-b788-6a66d82cc55a', 0),
    ('029bff4a-db03-50cd-a250-3a4f3d2f8f66', '83c1693b-bb68-5caf-897a-ac3b64ad433b', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', 0),
    ('5122418b-88b5-5e2c-b8ff-3d43a9a59c46', '562eafab-6805-522c-b6ff-14f70fb21352', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', 0);

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('2cbb194c-e4b9-5981-bd2d-5ef26427ef77', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '26295f52-132a-58d3-8316-606e98b0e85b', 1, 1.0),
    ('5e86dcb9-3cb7-577a-90cc-7b8ebc1beb31', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '61a92133-9afd-5a62-80bc-6956d4a7d103', 2, 1.0),
    ('40607d7a-509c-55c8-8f13-12f34985523f', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', 'a8d57cf2-38bd-51e8-9cd1-4a647ea6e905', 3, 1.0),
    ('6c83621c-2239-5dbf-83a1-b0394380bffc', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '91a2ea09-0cd4-53b7-abd9-e15394d27774', 4, 1.0),
    ('b09ccae5-f964-579c-8821-e98d2107e9bf', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '6b278a55-4867-5f29-b19a-ea00d65c4ee2', 5, 1.0),
    ('c1dbd0a5-c15b-5b05-95cc-114914a75ceb', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '01015d93-d693-5811-9f9d-bb4499b0bdaf', 6, 1.0),
    ('8b28757b-7d89-57b7-b48e-8bdff5a08841', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '76fb00da-f69a-5168-ac92-4bfffd84fe1b', 7, 1.0),
    ('702f2b97-be1c-57d4-a063-f2033dcc5c52', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '3a454131-d57d-5dcc-befe-ecdef587547d', 8, 1.0),
    ('1680d124-ec04-51bd-9094-a4be7776791e', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', '0c19eb03-9422-5c57-8491-a47e7c19bb3e', 9, 1.0),
    ('e4959c59-28cd-5a43-a9ee-afdf6d776c75', 'e704504c-6d05-585f-8cf7-c6d1ae4b7af8', 'f1a1338e-b188-5675-b194-e81e1e09411b', 10, 1.0),
    ('4427ee42-3515-585a-b716-fb8195053108', '2c78227f-9d62-5efe-b788-6a66d82cc55a', '995b2d26-999e-5605-a0fe-5da416f066f3', 1, 1.0),
    ('3c59dbc5-268d-5c0e-a4fb-287be3b89e58', '2c78227f-9d62-5efe-b788-6a66d82cc55a', 'a65968e5-efcd-5896-8839-30ee826cdc18', 2, 1.0),
    ('696b29ab-3cfb-5f43-8183-ef93f7220b09', '2c78227f-9d62-5efe-b788-6a66d82cc55a', 'f71b7a02-5e49-5467-bd27-aa59debc6504', 3, 1.0),
    ('d838ce19-59e1-52bb-8b72-91f090b54333', '2c78227f-9d62-5efe-b788-6a66d82cc55a', '25498b59-b618-5683-ba64-242ad00d7a83', 4, 1.0),
    ('157911bf-6ef3-5820-9b66-c79a226729ff', '2c78227f-9d62-5efe-b788-6a66d82cc55a', '5a4c44eb-ff47-5153-9e76-a9f208a673d5', 5, 1.0),
    ('09d83bdd-1743-53fa-aa7d-59ff97e58ed2', '2c78227f-9d62-5efe-b788-6a66d82cc55a', '347b151c-be56-5201-9601-cfe9614bba8f', 6, 1.0),
    ('dce2031f-467a-5d74-8a50-f2f6401b0f8f', '2c78227f-9d62-5efe-b788-6a66d82cc55a', '68f0a321-2705-50bb-8a79-fae7c7272a3a', 7, 1.0),
    ('e38a62e9-5386-5054-8ad7-346330e7ecf0', '2c78227f-9d62-5efe-b788-6a66d82cc55a', 'ee829da5-2f62-58b4-a62e-4f7060a70c0d', 8, 1.0),
    ('35c89062-027b-59df-a3d5-e69f70f71172', '2c78227f-9d62-5efe-b788-6a66d82cc55a', '33281aad-0365-5a5a-a14a-ea99af14ba8d', 9, 1.0),
    ('1bd1bf69-4ac4-5b32-a9ea-fbfd22e84c1d', '2c78227f-9d62-5efe-b788-6a66d82cc55a', 'd7e90218-f50f-55f0-9995-e81c3fea4577', 10, 1.0),
    ('d14d4aff-073f-523e-ba61-e21302aafb49', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', '3402e1a4-78b0-501d-ac45-078d015c864d', 1, 1.0),
    ('98e0b518-034d-5194-b6e4-c5b55ee72cd4', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', '161bbcf4-a681-5876-941d-42a966212805', 2, 1.0),
    ('0ac80283-034e-527d-9f2e-b07e40e85b4a', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', '9ab5140a-5bc7-5201-9bd3-24a4380486ed', 3, 1.0),
    ('7b91d439-45d8-5de8-915a-2322b2009d4b', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', 'c2863c10-b51d-5e81-bd54-83a83bfa8387', 4, 1.0),
    ('f7967df9-7116-5635-b744-69b13f3cd7b4', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', '86c55c74-2645-58e8-9981-c412205e3394', 5, 1.0),
    ('534edd95-13b4-5743-8731-9d14fb4aa5e2', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', 'c10220ae-99cd-5a09-862d-9d08a1961177', 6, 1.0),
    ('74dd7bbc-b936-55a1-93a1-14fe30b75a70', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', 'fe553933-c871-5046-976b-22029339d34a', 7, 1.0),
    ('c7cfb0bb-f88d-566b-b12d-ee92c28c863d', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', '0e8dcc1a-650b-5a80-8a8f-3f50a7997fd7', 8, 1.0),
    ('9abd666d-b0b3-5d5d-b8ef-799e42d59b39', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', '4c2311f7-dbbc-5da4-8b9c-029cab1859e3', 9, 1.0),
    ('e812ac30-f5c2-5513-8727-589772520fcf', 'c47084bf-b8b3-5a99-b62f-91d6256f4f2a', '490d2b6e-ec30-5068-8fa6-e785798e2ae1', 10, 1.0),
    ('2c7771da-fa76-55a0-ba80-d28f881016b5', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', 'ae1e9229-be37-50ed-9685-288ddbf66d33', 1, 1.0),
    ('bdf24a05-4e1f-50d4-8813-fd69aa61eba3', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', '9c92a2bf-ed60-58bc-aaad-26f387f343fe', 2, 1.0),
    ('c0a43e5f-114d-5544-82ce-f30f206bfa48', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', '915f102b-8016-5cf5-ad46-c13ba32c9b5b', 3, 1.0),
    ('00fe96cb-4d73-54f0-b362-f7b1a0e12fe2', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', 'd2633c30-728a-5cc1-932c-c8da7fde72c0', 4, 1.0),
    ('f17abbf6-a7e9-579b-a7e4-f867475690a4', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', 'cb54949f-a780-5f51-96ef-ee91afc0ff27', 5, 1.0),
    ('243b3513-174f-529e-b346-788780d3b3ff', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', 'fd8eb12e-ed5d-5282-b7de-6502bf74cb05', 6, 1.0),
    ('23a69454-6257-5e27-879f-d57b65002a65', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', 'd0e0ace3-6429-5211-a9ca-99496ec305d7', 7, 1.0),
    ('478283fb-2d78-5576-9fd2-1fd1546894bd', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', '6b7ac112-ca99-5d08-b8ce-a5b099878cff', 8, 1.0),
    ('66ea0a89-a577-594e-8e05-575c8acdd677', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', 'c7a89880-a84d-5a26-835d-da8d12f726a3', 9, 1.0),
    ('4597df80-5aad-5cc3-ac6e-46c13af9242b', '5e5814eb-7513-585d-8f4d-407bcf4fefd7', '5120d9c0-fb6e-5001-9b43-8593e0161e46', 10, 1.0),
    ('7e7e53a0-6690-5330-8d76-dcce7489f2d3', '886dad7a-0437-5c09-bbda-1bf3a21da440', '0b9cbe15-7816-51c4-86f2-540f72c27e27', 1, 1.0),
    ('3cf8f068-1381-5bbe-b07c-ed491d3c34a4', '5e9eb277-a6d1-5c8b-af67-b2d0f30d0b54', 'ddfd5dbd-ffc0-50bf-9c83-e603eeecc8f7', 1, 1.0),
    ('1677fef9-3226-533b-af72-60cbdd654a73', 'fdcb3b32-bce7-545e-b41a-d25930189e0e', '201d28d8-4425-5300-8fa9-e018292a3fea', 1, 1.0);

UPDATE content_packages SET current_published_version_id = 'f9f8cdfe-affe-5aef-b380-92e8c3d70af3', updated_at = CURRENT_TIMESTAMP
WHERE id = '5ec27c56-7585-5c1e-b31f-111f9299cc0b';

-- PLACEMENT-B03-B: IELTS placement test, band 0-3 (B)

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('6153498d-f861-59a1-a987-17ee2c8fc4dd', 'AUDIO', 'Assistant: Good morning. Can I help you?
Customer: Yes, please. I want to buy a jacket for my brother.
Assistant: What size is he?
Customer: He is size medium.
Assistant: And what colour does he like?
Customer: He likes blue.
Assistant: This blue jacket is very nice. It has two big pockets and it is warm.
Customer: How much is it?
Assistant: It is sixty pounds, but this week there is a sale. You pay forty pounds.
Customer: That is good. I will take it. Do you have a bag for it?
Assistant: Yes. A paper bag is free.
Customer: Can I pay with cash?
Assistant: Yes, of course. Is it a birthday present?
Customer: Yes. His birthday is on Friday.
Assistant: Then you can change it if it is too small. You must come back in fourteen days. When you come back, please bring this paper.
Customer: Thank you. When does the shop close today?
Assistant: We close at seven o''clock.', 'listening/placement/placement-b03-b/part1.mp3', 105, 'VALID'),
    ('cbbbda64-bdd3-504b-a603-2462b09b3a01', 'AUDIO', 'Presenter: Good morning, this is Seaview Radio with your weekend news.
Presenter: First, the weather. On Saturday it will be sunny and warm, about twenty-two degrees. On Sunday there will be rain in the morning, and it will be colder, about fifteen degrees.
Presenter: Now, things to do. On Saturday there is a book fair in the town hall. It starts at ten and finishes at four. Entry is free. In the evening there is a concert in the park. A ticket costs eight pounds.
Presenter: On Sunday, because of the rain, the football match will be in the sports hall, not on the field. It starts at two o''clock.
Presenter: One more thing. The number twelve bus will not run on Sunday. Please use the train. That is all for now. Have a good weekend!', 'listening/placement/placement-b03-b/part2.mp3', 88, 'VALID');

INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('7895c3ac-9acb-551d-961e-20e7b3e638eb', 'PASSAGE', 'An email from Leo

Hi Sara,

How are you? Last Friday my class went to the city zoo. We went by bus and the trip took one hour. We left school at nine o''clock and we came back at four.

My favourite animals were the penguins. There are twelve penguins at the zoo, and they eat fish three times a day. We watched the feeding at two o''clock. The zoo also has a new baby elephant. Her name is Lulu and she was born in January. She is very small, but she drinks twenty litres of milk every day!

We had lunch in a park near the zoo. My teacher, Mr Ford, bought an ice cream for everyone. It was a hot day, so it was very nice.

The tickets were cheap for students. We paid only five pounds each. Adults pay ten pounds.

You must come with us next time!

Love,
Leo', 'VALID'),
    ('6007da17-8399-5913-b680-78ade4252435', 'PASSAGE', 'Sunnyside Swimming Pool

Opening times: The pool is open every day. In summer it opens at seven in the morning and closes at nine in the evening. In winter it closes at eight.

Prices: A ticket for an adult is four pounds. Children under twelve pay two pounds, and children under three swim for free. You can also buy a monthly card for thirty pounds. With this card you can swim as often as you like.

Rules: You must take a shower before you go into the pool. Children under eight must always be with an adult. You cannot run near the pool, and you cannot take food or drinks into the swimming area. Please put your bag in a locker. The locker costs one pound, but you get the money back when you leave.

Lessons: We have swimming lessons for children every Tuesday and Thursday after school, at half past four. Each lesson is forty minutes. Please ask at the front desk if you want to join.

The pool has a small café where you can buy sandwiches, hot drinks and fruit.', 'VALID'),
    ('ade1c0a7-f541-570a-a5a0-0459c905b22c', 'PASSAGE', 'Student | 2022 | 2023
Anna | 8 | 12
Ben | 5 | 5
Carla | 10 | 7
Dan | 3 | 9', 'VALID');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('11abb532-75f4-5a46-a911-6d058eb7a05f', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('29392dab-735f-52ef-a1ec-1107d304ee2c', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('284f53ab-de09-5e8e-8b3b-adce73ee4c6e', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2ba0c222-f15e-5452-94af-9ef02b4ae5b8', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('1b1903e5-939f-5d3d-8ecf-25d328f75e9d', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('43df0978-925f-5510-a78c-9c931099a590', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('90838480-87da-5aa9-bb31-034e7aaa632c', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('42623acf-8ebe-59af-a3c8-7c829a815e48', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('383f4729-a19e-5d59-bbc5-a894fc892b55', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('a9c2c90f-ef99-530c-b779-7ac03df8de7d', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('e6dfe8fd-bcab-5220-90a9-a227432fefab', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('0eaea131-6b14-5a82-ad0b-9ee378a9c979', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('0b35a5bb-ffa7-5013-a4b8-daf274c24773', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('65eea1b1-bd42-5cd0-b0fd-16d7886c5768', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('b52273e3-3dbd-5bf1-a0cd-9e8b6cdee21c', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('397a68af-d08c-53ad-85ef-a444595ab796', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('9a57d6a5-189d-5c00-afcf-c23afd539373', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('d9d28018-fb84-5db9-82a3-e33a94bd648a', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('43ea056e-216e-5d43-86b2-b720db0a24cc', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('7ebdd678-a267-5009-b8e8-7c2478801e06', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('0debcedd-3e31-54ec-b9a9-306646ee8a3d', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('c5177a33-cdf0-590a-9252-d373f7381bec', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('d4fb11e8-3989-5c2a-ba4b-888c4eba342f', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('a4bb655f-77d8-5163-be15-8c9227471c4b', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('e119bca1-0be2-57b1-90d3-5a32abc4da42', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('3c491cb2-8bcd-5948-a4bc-a676c068c58a', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('108c8808-7787-592f-a787-b233a8f2d43b', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('3375c0d0-bd9d-5c90-beaa-8f4650e3dbbc', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('61b9b432-f7fa-5dfc-886a-f2fb9cfd96de', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2700fc1d-fe44-59f1-b656-a01b02f5cf08', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('ac629cbc-05cc-50be-8307-cdaed4faf3a2', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('847bd31e-c56e-57c0-8ecf-6ce6ab4a18fb', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('bc3c26ab-e76c-53ee-bd75-c7992e5431c3', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('b13d60e6-37ca-54b2-849e-7dc1254923f6', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('8d009838-bdb8-5bf3-ac1b-dd752be123f2', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('c6dea57b-5558-5b8d-b382-ef4611c78d7e', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('202b4836-da19-55bc-a397-9aca31f050fb', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('78e528b2-f784-5a43-a9bb-b3e2fbc2637b', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('f86da65d-069e-5467-b47d-350044732e47', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('9e5ddbe2-72be-5a22-9196-2ee6d16a9562', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('f278c559-4e7b-5fc4-a006-09e3f7dbb853', 'ESSAY', 'WRITING', 'PLACEMENT', 'PUBLISHED'),
    ('081a523d-3b1c-55bb-b53c-61a88d89e535', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('bf14f703-a1a8-513b-8dcd-760c1ef66915', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('da4354b5-ec99-5335-96d5-2f5f3c56f0bf', '11abb532-75f4-5a46-a911-6d058eb7a05f', 1, 'The customer wants to buy a ______ for his brother.', NULL,
     '{"type":"FILL","accepted":["jacket"]}'::jsonb, 1, 'He says he wants to buy a jacket for his brother.', 'PUBLISHED'),
    ('aaae7cc5-6dd4-52e9-8f67-fc2142814ec1', '29392dab-735f-52ef-a1ec-1107d304ee2c', 1, 'Size of the jacket: ______', NULL,
     '{"type":"FILL","accepted":["medium"]}'::jsonb, 1, 'He is size medium.', 'PUBLISHED'),
    ('a77b7eee-86d6-56fc-afce-cb8647d12443', '284f53ab-de09-5e8e-8b3b-adce73ee4c6e', 1, 'Colour: ______', NULL,
     '{"type":"FILL","accepted":["blue"]}'::jsonb, 1, 'He likes blue.', 'PUBLISHED'),
    ('093911e0-82ca-54ec-8311-ad11284e2160', '2ba0c222-f15e-5452-94af-9ef02b4ae5b8', 1, 'How many pockets does the jacket have?', '[{"optionKey":"A","content":"One","sortOrder":1},{"optionKey":"B","content":"Two","sortOrder":2},{"optionKey":"C","content":"Three","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'It has two big pockets.', 'PUBLISHED'),
    ('1a5eb71b-1540-5e88-8da9-839c0f3bc094', '1b1903e5-939f-5d3d-8ecf-25d328f75e9d', 1, 'Usual price: £______', NULL,
     '{"type":"FILL","accepted":["60","sixty"]}'::jsonb, 1, 'It is usually sixty pounds.', 'PUBLISHED'),
    ('5f25789d-eaea-5df4-8d58-3562b01c4530', '43df0978-925f-5510-a78c-9c931099a590', 1, 'Sale price: £______', NULL,
     '{"type":"FILL","accepted":["40","forty"]}'::jsonb, 1, 'In the sale you pay forty pounds.', 'PUBLISHED'),
    ('2bf85c81-7d2a-5ad6-a4a7-7f835a44e9e6', '90838480-87da-5aa9-bb31-034e7aaa632c', 1, 'How much is a paper bag?', '[{"optionKey":"A","content":"Nothing","sortOrder":1},{"optionKey":"B","content":"One pound","sortOrder":2},{"optionKey":"C","content":"Two pounds","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'A paper bag is free.', 'PUBLISHED'),
    ('1a66849b-cd6e-56bd-9bb5-fa916767139e', '42623acf-8ebe-59af-a3c8-7c829a815e48', 1, 'The customer will pay with ______.', NULL,
     '{"type":"FILL","accepted":["cash"]}'::jsonb, 1, 'He asks to pay with cash and the answer is yes.', 'PUBLISHED'),
    ('e892bb81-5b8c-50ed-91a8-707b53009bb9', '383f4729-a19e-5d59-bbc5-a894fc892b55', 1, 'The brother''s birthday is on ______.', NULL,
     '{"type":"FILL","accepted":["friday"]}'::jsonb, 1, 'His birthday is on Friday.', 'PUBLISHED'),
    ('ac9e7eec-7377-5e3f-8876-978d69fd39c6', 'a9c2c90f-ef99-530c-b779-7ac03df8de7d', 1, 'The shop closes at ...', '[{"optionKey":"A","content":"five o''clock","sortOrder":1},{"optionKey":"B","content":"six o''clock","sortOrder":2},{"optionKey":"C","content":"seven o''clock","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'We close at seven o''clock.', 'PUBLISHED'),
    ('a0014eff-f9e3-5aad-97b5-0bff8a035b3b', 'e6dfe8fd-bcab-5220-90a9-a227432fefab', 1, 'What will the weather be on Saturday?', '[{"optionKey":"A","content":"Sunny","sortOrder":1},{"optionKey":"B","content":"Rainy","sortOrder":2},{"optionKey":"C","content":"Cold","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'On Saturday it will be sunny and warm.', 'PUBLISHED'),
    ('10676d8d-d4f3-5b57-9146-915bc6713ae0', '0eaea131-6b14-5a82-ad0b-9ee378a9c979', 1, 'The temperature on Saturday will be about ______ degrees.', NULL,
     '{"type":"FILL","accepted":["22","twenty-two","twenty two"]}'::jsonb, 1, 'About twenty-two degrees.', 'PUBLISHED'),
    ('f6d9280d-9706-5caa-a1d4-432a77a5a417', '0b35a5bb-ffa7-5013-a4b8-daf274c24773', 1, 'When will it rain?', '[{"optionKey":"A","content":"On Saturday morning","sortOrder":1},{"optionKey":"B","content":"On Sunday morning","sortOrder":2},{"optionKey":"C","content":"On Sunday evening","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'On Sunday there will be rain in the morning.', 'PUBLISHED'),
    ('bb55d37b-4b66-5282-a11f-53c8f99986ae', '65eea1b1-bd42-5cd0-b0fd-16d7886c5768', 1, 'On Sunday it will be about ______ degrees.', NULL,
     '{"type":"FILL","accepted":["15","fifteen"]}'::jsonb, 1, 'It will be colder, about fifteen degrees.', 'PUBLISHED'),
    ('f797b3b6-3ab1-53bd-aee9-a04b2d827372', 'b52273e3-3dbd-5bf1-a0cd-9e8b6cdee21c', 1, 'The book fair is in the town ______.', NULL,
     '{"type":"FILL","accepted":["hall"]}'::jsonb, 1, 'There is a book fair in the town hall.', 'PUBLISHED'),
    ('68c76c4f-8db2-5f86-89a6-2773bb55f7ba', '397a68af-d08c-53ad-85ef-a444595ab796', 1, 'How much does it cost to enter the book fair?', '[{"optionKey":"A","content":"Nothing","sortOrder":1},{"optionKey":"B","content":"Four pounds","sortOrder":2},{"optionKey":"C","content":"Eight pounds","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Entry is free.', 'PUBLISHED'),
    ('b8b9984b-5abd-5372-a447-28f5f8d22bb2', '9a57d6a5-189d-5c00-afcf-c23afd539373', 1, 'The book fair finishes at ______ o''clock.', NULL,
     '{"type":"FILL","accepted":["4","four","4.00","4:00"]}'::jsonb, 1, 'It starts at ten and finishes at four.', 'PUBLISHED'),
    ('4729f110-2b80-51fa-8c8d-73e3862952dc', 'd9d28018-fb84-5db9-82a3-e33a94bd648a', 1, 'A ticket for the concert costs £______.', NULL,
     '{"type":"FILL","accepted":["8","eight"]}'::jsonb, 1, 'A ticket costs eight pounds.', 'PUBLISHED'),
    ('ae607db8-4162-5db6-bf32-53e82da0dd05', '43ea056e-216e-5d43-86b2-b720db0a24cc', 1, 'Where will the football match be on Sunday?', '[{"optionKey":"A","content":"On the field","sortOrder":1},{"optionKey":"B","content":"In the sports hall","sortOrder":2},{"optionKey":"C","content":"In the park","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Because of the rain, it will be in the sports hall.', 'PUBLISHED'),
    ('ba860e4b-b9c3-5cc7-9643-206debda95bb', '7ebdd678-a267-5009-b8e8-7c2478801e06', 1, 'Which bus will not run on Sunday?', '[{"optionKey":"A","content":"Number twelve","sortOrder":1},{"optionKey":"B","content":"Number fifteen","sortOrder":2},{"optionKey":"C","content":"Number twenty","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The number twelve bus will not run on Sunday.', 'PUBLISHED'),
    ('6003f4ce-7a25-59a4-9a88-1be275616d69', '0debcedd-3e31-54ec-b9a9-306646ee8a3d', 1, 'Leo''s class went to the zoo on Friday.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Last Friday my class went to the city zoo.', 'PUBLISHED'),
    ('537a3097-db00-5da8-9c71-cd1ae14263fc', 'c5177a33-cdf0-590a-9252-d373f7381bec', 1, 'The class travelled to the zoo by train.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'They went by bus.', 'PUBLISHED'),
    ('eb074a3c-f44f-54d1-bb9e-4d4b9b52489c', 'd4fb11e8-3989-5c2a-ba4b-888c4eba342f', 1, 'Leo''s teacher took photos of the penguins.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'The email does not mention photos.', 'PUBLISHED'),
    ('bdda3471-c371-56dc-a25f-af15795d2f98', 'a4bb655f-77d8-5163-be15-8c9227471c4b', 1, 'The weather was hot on the day of the trip.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'It was a hot day.', 'PUBLISHED'),
    ('d943e86a-d9b0-51d3-860b-7d88670b0684', 'e119bca1-0be2-57b1-90d3-5a32abc4da42', 1, 'How long was the bus trip?', '[{"optionKey":"A","content":"One hour","sortOrder":1},{"optionKey":"B","content":"Two hours","sortOrder":2},{"optionKey":"C","content":"Three hours","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The trip took one hour.', 'PUBLISHED'),
    ('67f787a9-2f0d-53f7-b431-57c00ebd2806', '3c491cb2-8bcd-5948-a4bc-a676c068c58a', 1, 'What time did the class come back to school?', '[{"optionKey":"A","content":"Two o''clock","sortOrder":1},{"optionKey":"B","content":"Four o''clock","sortOrder":2},{"optionKey":"C","content":"Nine o''clock","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'They came back at four.', 'PUBLISHED'),
    ('f35d8cb4-41bc-59f3-b135-14b6e9bd1702', '108c8808-7787-592f-a787-b233a8f2d43b', 1, 'What is Lulu?', '[{"optionKey":"A","content":"A penguin","sortOrder":1},{"optionKey":"B","content":"A baby elephant","sortOrder":2},{"optionKey":"C","content":"A teacher","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Lulu is the new baby elephant.', 'PUBLISHED'),
    ('62382d63-53cf-5cb9-9607-0fe11d78056c', '3375c0d0-bd9d-5c90-beaa-8f4650e3dbbc', 1, 'The penguins eat fish ______ times a day.', NULL,
     '{"type":"FILL","accepted":["3","three"]}'::jsonb, 1, 'They eat fish three times a day.', 'PUBLISHED'),
    ('51b938d2-6043-5adc-bb5a-9aaff0ea3de2', '61b9b432-f7fa-5dfc-886a-f2fb9cfd96de', 1, 'Lulu drinks ______ litres of milk every day.', NULL,
     '{"type":"FILL","accepted":["20","twenty"]}'::jsonb, 1, 'She drinks twenty litres of milk every day.', 'PUBLISHED'),
    ('2fd4363a-345c-5537-8069-b947d452bba9', '2700fc1d-fe44-59f1-b656-a01b02f5cf08', 1, 'Students paid £______ each for a ticket.', NULL,
     '{"type":"FILL","accepted":["5","five"]}'::jsonb, 1, 'They paid only five pounds each.', 'PUBLISHED'),
    ('74340615-84b5-5527-a39e-9a05464a1b66', 'ac629cbc-05cc-50be-8307-cdaed4faf3a2', 1, 'The pool is open every day.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'The pool is open every day.', 'PUBLISHED'),
    ('974d63cf-46c4-5ed9-89f0-7a779274069a', '847bd31e-c56e-57c0-8ecf-6ce6ab4a18fb', 1, 'Children under three pay two pounds.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Children under three swim for free.', 'PUBLISHED'),
    ('78bcf493-46a6-5cb1-b865-dab8100edbf4', 'bc3c26ab-e76c-53ee-bd75-c7992e5431c3', 1, 'There is a diving board at the pool.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'A diving board is not mentioned.', 'PUBLISHED'),
    ('5fccd006-4d73-517a-bf0d-68c5ad83695a', 'b13d60e6-37ca-54b2-849e-7dc1254923f6', 1, 'Visitors can take drinks into the swimming area.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'You cannot take food or drinks into the swimming area.', 'PUBLISHED'),
    ('7e428ead-eef9-5f2d-8c44-f74e791dab95', '8d009838-bdb8-5bf3-ac1b-dd752be123f2', 1, 'In winter the pool closes at ...', '[{"optionKey":"A","content":"seven o''clock","sortOrder":1},{"optionKey":"B","content":"eight o''clock","sortOrder":2},{"optionKey":"C","content":"nine o''clock","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'In winter it closes at eight.', 'PUBLISHED'),
    ('f54e5933-3d31-5791-9c6f-5f1bf3cccfff', 'c6dea57b-5558-5b8d-b382-ef4611c78d7e', 1, 'How much is a monthly card?', '[{"optionKey":"A","content":"Four pounds","sortOrder":1},{"optionKey":"B","content":"Twenty pounds","sortOrder":2},{"optionKey":"C","content":"Thirty pounds","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'A monthly card costs thirty pounds.', 'PUBLISHED'),
    ('b82baaa4-0a82-5894-b2fb-223e5d5721b5', '202b4836-da19-55bc-a397-9aca31f050fb', 1, 'Who must always be with an adult?', '[{"optionKey":"A","content":"Children under eight","sortOrder":1},{"optionKey":"B","content":"Children under twelve","sortOrder":2},{"optionKey":"C","content":"All children","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Children under eight must always be with an adult.', 'PUBLISHED'),
    ('fb5d0d1f-cc7f-597d-9e23-13d883b3b9ff', '78e528b2-f784-5a43-a9bb-b3e2fbc2637b', 1, 'You must take a ______ before you go into the pool.', NULL,
     '{"type":"FILL","accepted":["shower"]}'::jsonb, 1, 'You must take a shower before you go into the pool.', 'PUBLISHED'),
    ('de90bbbb-2b32-5b2b-9ac0-32ef2ceae30f', 'f86da65d-069e-5467-b47d-350044732e47', 1, 'A locker costs ______ pound, but you get it back.', NULL,
     '{"type":"FILL","accepted":["1","one"]}'::jsonb, 1, 'The locker costs one pound.', 'PUBLISHED'),
    ('a746c2aa-476d-5c40-8a71-d76399695fa4', '9e5ddbe2-72be-5a22-9196-2ee6d16a9562', 1, 'Swimming lessons for children are on Tuesday and ______.', NULL,
     '{"type":"FILL","accepted":["thursday"]}'::jsonb, 1, 'Lessons are every Tuesday and Thursday.', 'PUBLISHED'),
    ('3b85caa6-02ea-5cd5-b3a5-d2d91f2186b4', 'f278c559-4e7b-5fc4-a006-09e3f7dbb853', 1, 'The table below shows how many books four students read in 2022 and in 2023. Write about the information in the table. Say who read the most books, and who read more books in 2023 than in 2022. Write at least 60 words.', NULL,
     '{"type":"ESSAY","task":"TASK_1","minWords":60,"passBand":3,"chartFacts":"Number of books read by four students in 2022 and 2023. Anna: 8 in 2022, 12 in 2023. Ben: 5 in 2022, 5 in 2023. Carla: 10 in 2022, 7 in 2023. Dan: 3 in 2022, 9 in 2023. Key features: Carla read the most books in 2022 (10) and Anna read the most in 2023 (12); Anna (8 to 12) and Dan (3 to 9) read more books in 2023; Carla read fewer (10 to 7); Ben read the same number in both years (5)."}'::jsonb, 1, 'Graded by the placement essay grader.', 'PUBLISHED'),
    ('d4990f6d-9844-5eb6-b920-b4ed43bcdb37', '081a523d-3b1c-55bb-b53c-61a88d89e535', 1, 'Let''s talk about your home. Do you live in a house or a flat? What is your favourite room, and why?', NULL,
     '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED'),
    ('7202313d-5b4a-55df-929a-935d3a469d34', 'bf14f703-a1a8-513b-8dcd-760c1ef66915', 1, 'Describe a friend. You should say who the friend is, how you met, what you do together, and why you like this person. Speak for about one minute.', NULL,
     '{"type":"SPEAKING","durationSeconds":60}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED');

UPDATE questions q SET current_published_version_id = qv.id
FROM question_versions qv
WHERE qv.question_id = q.id AND qv.id IN (
    'da4354b5-ec99-5335-96d5-2f5f3c56f0bf',
    'aaae7cc5-6dd4-52e9-8f67-fc2142814ec1',
    'a77b7eee-86d6-56fc-afce-cb8647d12443',
    '093911e0-82ca-54ec-8311-ad11284e2160',
    '1a5eb71b-1540-5e88-8da9-839c0f3bc094',
    '5f25789d-eaea-5df4-8d58-3562b01c4530',
    '2bf85c81-7d2a-5ad6-a4a7-7f835a44e9e6',
    '1a66849b-cd6e-56bd-9bb5-fa916767139e',
    'e892bb81-5b8c-50ed-91a8-707b53009bb9',
    'ac9e7eec-7377-5e3f-8876-978d69fd39c6',
    'a0014eff-f9e3-5aad-97b5-0bff8a035b3b',
    '10676d8d-d4f3-5b57-9146-915bc6713ae0',
    'f6d9280d-9706-5caa-a1d4-432a77a5a417',
    'bb55d37b-4b66-5282-a11f-53c8f99986ae',
    'f797b3b6-3ab1-53bd-aee9-a04b2d827372',
    '68c76c4f-8db2-5f86-89a6-2773bb55f7ba',
    'b8b9984b-5abd-5372-a447-28f5f8d22bb2',
    '4729f110-2b80-51fa-8c8d-73e3862952dc',
    'ae607db8-4162-5db6-bf32-53e82da0dd05',
    'ba860e4b-b9c3-5cc7-9643-206debda95bb',
    '6003f4ce-7a25-59a4-9a88-1be275616d69',
    '537a3097-db00-5da8-9c71-cd1ae14263fc',
    'eb074a3c-f44f-54d1-bb9e-4d4b9b52489c',
    'bdda3471-c371-56dc-a25f-af15795d2f98',
    'd943e86a-d9b0-51d3-860b-7d88670b0684',
    '67f787a9-2f0d-53f7-b431-57c00ebd2806',
    'f35d8cb4-41bc-59f3-b135-14b6e9bd1702',
    '62382d63-53cf-5cb9-9607-0fe11d78056c',
    '51b938d2-6043-5adc-bb5a-9aaff0ea3de2',
    '2fd4363a-345c-5537-8069-b947d452bba9',
    '74340615-84b5-5527-a39e-9a05464a1b66',
    '974d63cf-46c4-5ed9-89f0-7a779274069a',
    '78bcf493-46a6-5cb1-b865-dab8100edbf4',
    '5fccd006-4d73-517a-bf0d-68c5ad83695a',
    '7e428ead-eef9-5f2d-8c44-f74e791dab95',
    'f54e5933-3d31-5791-9c6f-5f1bf3cccfff',
    'b82baaa4-0a82-5894-b2fb-223e5d5721b5',
    'fb5d0d1f-cc7f-597d-9e23-13d883b3b9ff',
    'de90bbbb-2b32-5b2b-9ac0-32ef2ceae30f',
    'a746c2aa-476d-5c40-8a71-d76399695fa4',
    '3b85caa6-02ea-5cd5-b3a5-d2d91f2186b4',
    'd4990f6d-9844-5eb6-b920-b4ed43bcdb37',
    '7202313d-5b4a-55df-929a-935d3a469d34');

INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key, placement_pool)
VALUES ('ffcb9733-57c9-5c36-800d-9722568f5c0a', 'PLACEMENT-B03-B', 'IELTS placement test, band 0-3 (B)', 'PLACEMENT_TEST', NULL, NULL, NULL, 'PUBLISHED', NULL, 'BAND_0_3');

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
VALUES ('2271a62f-152a-5f64-bf5a-9a25b8761471', 'ffcb9733-57c9-5c36-800d-9722568f5c0a', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions) VALUES
    ('2d961cb2-0a44-546f-a125-404d06ad0c5e', '2271a62f-152a-5f64-bf5a-9a25b8761471', 'Listening Part 1: buying a jacket', 'LISTENING', 1, NULL, 'Questions 1–10. Choose the correct letter, A, B or C, or write ONE WORD AND/OR A NUMBER where you see a line.'),
    ('b35b4e36-4e29-56c2-80e8-61bcf22232c2', '2271a62f-152a-5f64-bf5a-9a25b8761471', 'Listening Part 2: the weekend news on the radio', 'LISTENING', 2, NULL, 'Questions 11–20. Choose the correct letter, A, B or C, or write ONE WORD AND/OR A NUMBER where you see a line.'),
    ('9304536b-64e6-5da2-95b5-9958d19e8ba6', '2271a62f-152a-5f64-bf5a-9a25b8761471', 'Reading Passage 1: a trip to the zoo', 'READING', 3, 600, 'Questions 21–24: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 25–27: Choose the correct letter, A, B or C. Questions 28–30: Complete the sentences. Write ONE WORD OR A NUMBER from the passage.'),
    ('038fe6b6-0be0-54bc-bb43-dbce6b46b75d', '2271a62f-152a-5f64-bf5a-9a25b8761471', 'Reading Passage 2: Sunnyside Swimming Pool', 'READING', 4, 600, 'Questions 31–34: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 35–37: Choose the correct letter, A, B or C. Questions 38–40: Complete the sentences. Write ONE WORD OR A NUMBER from the passage.'),
    ('ce7d1b8f-faaa-5127-b423-22d098bed502', '2271a62f-152a-5f64-bf5a-9a25b8761471', 'Writing: books read by four students', 'WRITING', 5, 1200, 'Spend about 20 minutes on this task. Write at least 60 words. Use simple sentences.'),
    ('92ffaea2-a586-5961-9665-6dcf658bbd77', '2271a62f-152a-5f64-bf5a-9a25b8761471', 'Speaking Part 1: your home', 'SPEAKING', 6, NULL, 'Answer the questions in a few short sentences.'),
    ('b3764057-e26f-5798-b6c2-0183ce03a442', '2271a62f-152a-5f64-bf5a-9a25b8761471', 'Speaking Part 2: a friend', 'SPEAKING', 7, NULL, 'You have one minute to prepare, then speak for about one minute.');

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('9ccc1ead-4083-52e8-8e18-03baf7e1c421', '6153498d-f861-59a1-a987-17ee2c8fc4dd', '2d961cb2-0a44-546f-a125-404d06ad0c5e', 0),
    ('12087886-ad7f-5cce-9c3a-fc9c960def27', 'cbbbda64-bdd3-504b-a603-2462b09b3a01', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 0),
    ('238e9b12-ad4c-54bc-af8e-0bd1c0a1fbaa', '7895c3ac-9acb-551d-961e-20e7b3e638eb', '9304536b-64e6-5da2-95b5-9958d19e8ba6', 0),
    ('95f52977-6697-58f1-9890-f3700425bbf1', '6007da17-8399-5913-b680-78ade4252435', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', 0),
    ('1c6aade0-2b2b-53a2-9b0f-ef902744a641', 'ade1c0a7-f541-570a-a5a0-0459c905b22c', 'ce7d1b8f-faaa-5127-b423-22d098bed502', 0);

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('f4d1a5f9-8d5b-5b7d-bac4-73f2e3bf7ccb', '2d961cb2-0a44-546f-a125-404d06ad0c5e', 'da4354b5-ec99-5335-96d5-2f5f3c56f0bf', 1, 1.0),
    ('9b1a5a72-1eb2-511b-82f0-0375b8b6b352', '2d961cb2-0a44-546f-a125-404d06ad0c5e', 'aaae7cc5-6dd4-52e9-8f67-fc2142814ec1', 2, 1.0),
    ('5352f84b-01af-56ce-b16f-8a7ffbc01ff1', '2d961cb2-0a44-546f-a125-404d06ad0c5e', 'a77b7eee-86d6-56fc-afce-cb8647d12443', 3, 1.0),
    ('0b19ff9c-00cc-58b8-815e-cd3ed106bf1a', '2d961cb2-0a44-546f-a125-404d06ad0c5e', '093911e0-82ca-54ec-8311-ad11284e2160', 4, 1.0),
    ('932fc12b-97bc-5b5c-843d-3ace1260177c', '2d961cb2-0a44-546f-a125-404d06ad0c5e', '1a5eb71b-1540-5e88-8da9-839c0f3bc094', 5, 1.0),
    ('d0690a26-a69d-5e20-bf35-13de8c1f23ef', '2d961cb2-0a44-546f-a125-404d06ad0c5e', '5f25789d-eaea-5df4-8d58-3562b01c4530', 6, 1.0),
    ('7376af78-8669-568c-b0a5-7e485c123113', '2d961cb2-0a44-546f-a125-404d06ad0c5e', '2bf85c81-7d2a-5ad6-a4a7-7f835a44e9e6', 7, 1.0),
    ('9d560a97-bcaa-5899-9afb-19e2d54ff8ac', '2d961cb2-0a44-546f-a125-404d06ad0c5e', '1a66849b-cd6e-56bd-9bb5-fa916767139e', 8, 1.0),
    ('20da39b7-72a9-5e01-bb1a-c3bc69b099a4', '2d961cb2-0a44-546f-a125-404d06ad0c5e', 'e892bb81-5b8c-50ed-91a8-707b53009bb9', 9, 1.0),
    ('02dfa1f7-2ffa-58c5-9030-0990fd84056b', '2d961cb2-0a44-546f-a125-404d06ad0c5e', 'ac9e7eec-7377-5e3f-8876-978d69fd39c6', 10, 1.0),
    ('334f07a6-c859-5944-ae57-d35c7a0790a9', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 'a0014eff-f9e3-5aad-97b5-0bff8a035b3b', 1, 1.0),
    ('004f9b6e-04ed-59c0-845e-100bdf1cd9e8', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', '10676d8d-d4f3-5b57-9146-915bc6713ae0', 2, 1.0),
    ('3977757a-4b7e-5c19-a5d6-2f0317cabb2a', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 'f6d9280d-9706-5caa-a1d4-432a77a5a417', 3, 1.0),
    ('0f5b52a4-ae80-52b3-b70d-4d703403cfd7', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 'bb55d37b-4b66-5282-a11f-53c8f99986ae', 4, 1.0),
    ('55fe73ee-8e9f-5a15-979a-7b304da2dcde', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 'f797b3b6-3ab1-53bd-aee9-a04b2d827372', 5, 1.0),
    ('034be3ac-99b3-50da-9c1c-fd9881de4aa0', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', '68c76c4f-8db2-5f86-89a6-2773bb55f7ba', 6, 1.0),
    ('49508251-0105-59be-b2bd-e1b1d4915767', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 'b8b9984b-5abd-5372-a447-28f5f8d22bb2', 7, 1.0),
    ('77885b72-ceeb-5e9e-a47d-36a93bdc1a05', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', '4729f110-2b80-51fa-8c8d-73e3862952dc', 8, 1.0),
    ('03a18f17-d4df-534b-8782-103eece34305', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 'ae607db8-4162-5db6-bf32-53e82da0dd05', 9, 1.0),
    ('74d3821c-2f5e-577b-98f2-f13e8b3952fa', 'b35b4e36-4e29-56c2-80e8-61bcf22232c2', 'ba860e4b-b9c3-5cc7-9643-206debda95bb', 10, 1.0),
    ('e21e3d64-895a-5a86-9cf1-e6b563563671', '9304536b-64e6-5da2-95b5-9958d19e8ba6', '6003f4ce-7a25-59a4-9a88-1be275616d69', 1, 1.0),
    ('9adde59e-f89f-5263-aee8-09bdaa799fb3', '9304536b-64e6-5da2-95b5-9958d19e8ba6', '537a3097-db00-5da8-9c71-cd1ae14263fc', 2, 1.0),
    ('12e92d2b-f627-5806-9e79-4391355dd3f2', '9304536b-64e6-5da2-95b5-9958d19e8ba6', 'eb074a3c-f44f-54d1-bb9e-4d4b9b52489c', 3, 1.0),
    ('35f2f624-6f32-5e65-92fb-8614c8c46063', '9304536b-64e6-5da2-95b5-9958d19e8ba6', 'bdda3471-c371-56dc-a25f-af15795d2f98', 4, 1.0),
    ('41ee706c-fab0-5da3-a559-b49e400a66ed', '9304536b-64e6-5da2-95b5-9958d19e8ba6', 'd943e86a-d9b0-51d3-860b-7d88670b0684', 5, 1.0),
    ('15c7d70c-0ec7-5e70-a351-187b4b527eff', '9304536b-64e6-5da2-95b5-9958d19e8ba6', '67f787a9-2f0d-53f7-b431-57c00ebd2806', 6, 1.0),
    ('f110c5ca-39cc-53d6-8232-2ef68e5b37fe', '9304536b-64e6-5da2-95b5-9958d19e8ba6', 'f35d8cb4-41bc-59f3-b135-14b6e9bd1702', 7, 1.0),
    ('cca8bad8-caf3-5730-85c2-44b1c856c752', '9304536b-64e6-5da2-95b5-9958d19e8ba6', '62382d63-53cf-5cb9-9607-0fe11d78056c', 8, 1.0),
    ('bcba9685-b739-5341-966d-e024b07879d9', '9304536b-64e6-5da2-95b5-9958d19e8ba6', '51b938d2-6043-5adc-bb5a-9aaff0ea3de2', 9, 1.0),
    ('ba575c13-b963-54ae-a247-a75fb95e4470', '9304536b-64e6-5da2-95b5-9958d19e8ba6', '2fd4363a-345c-5537-8069-b947d452bba9', 10, 1.0),
    ('0e32c1d0-522f-5761-a435-5a4d295427a7', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', '74340615-84b5-5527-a39e-9a05464a1b66', 1, 1.0),
    ('56b24178-9b8d-5c07-8b0c-bc1dccf077e8', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', '974d63cf-46c4-5ed9-89f0-7a779274069a', 2, 1.0),
    ('3a4873e4-77a6-58e1-be7c-3ccceaf871bd', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', '78bcf493-46a6-5cb1-b865-dab8100edbf4', 3, 1.0),
    ('c518a2a7-4322-59af-89ef-49ced020c85e', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', '5fccd006-4d73-517a-bf0d-68c5ad83695a', 4, 1.0),
    ('96eb55ff-b6b0-57f9-8138-10b38ab01082', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', '7e428ead-eef9-5f2d-8c44-f74e791dab95', 5, 1.0),
    ('96ce752d-7b9d-5d76-9c6f-20e92fb353ce', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', 'f54e5933-3d31-5791-9c6f-5f1bf3cccfff', 6, 1.0),
    ('40858a1c-a0cf-55c2-8f33-ac5dec58065a', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', 'b82baaa4-0a82-5894-b2fb-223e5d5721b5', 7, 1.0),
    ('2d8420b2-461c-5f88-afb5-777a17bdd58b', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', 'fb5d0d1f-cc7f-597d-9e23-13d883b3b9ff', 8, 1.0),
    ('438b1700-5ad6-5715-8ecd-d4e254555f81', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', 'de90bbbb-2b32-5b2b-9ac0-32ef2ceae30f', 9, 1.0),
    ('40cf1f3e-657d-5e99-9286-c0ce92f3655a', '038fe6b6-0be0-54bc-bb43-dbce6b46b75d', 'a746c2aa-476d-5c40-8a71-d76399695fa4', 10, 1.0),
    ('6cf939c5-c8ba-5828-9e1e-809fecf4f2dc', 'ce7d1b8f-faaa-5127-b423-22d098bed502', '3b85caa6-02ea-5cd5-b3a5-d2d91f2186b4', 1, 1.0),
    ('56f95b65-6bbb-53b3-9692-1fe8c958e3e1', '92ffaea2-a586-5961-9665-6dcf658bbd77', 'd4990f6d-9844-5eb6-b920-b4ed43bcdb37', 1, 1.0),
    ('9d89665f-404c-5854-92af-6c5b5ab529b3', 'b3764057-e26f-5798-b6c2-0183ce03a442', '7202313d-5b4a-55df-929a-935d3a469d34', 1, 1.0);

UPDATE content_packages SET current_published_version_id = '2271a62f-152a-5f64-bf5a-9a25b8761471', updated_at = CURRENT_TIMESTAMP
WHERE id = 'ffcb9733-57c9-5c36-800d-9722568f5c0a';
