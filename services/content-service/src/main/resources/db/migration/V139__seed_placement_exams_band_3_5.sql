-- Placement exams of the BAND_3_5 pool, published when this migration runs. Generated from
-- scripts/placement-exams/exams by scripts/placement-exams/generate-placement-seed.mjs; edit the sources and
-- regenerate, never this file. Listening audio keys (listening/placement/<package code>/part<N>.mp3) must be
-- uploaded to the media bucket before the migration is deployed.

-- PLACEMENT-B35-A: IELTS placement test, band 3-5 (A)

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('ea4f403c-a679-5e70-b585-f2b741783645', 'AUDIO', 'Receptionist: Good afternoon, Lakeside Hotel. How can I help you?
Guest: Hello. I''d like to book a room for a weekend in October, please.
Receptionist: Certainly. Which dates would you like?
Guest: We''d like to arrive on Friday the eleventh and leave on Sunday the thirteenth.
Receptionist: That''s two nights. How many people is the room for?
Guest: There will be three of us: me, my husband and our daughter, who is seven.
Receptionist: Then I''d suggest a family room. It has one double bed and a small bed for your daughter. It costs ninety-five pounds a night, and breakfast is included.
Guest: That sounds good. Does the room have a view of the lake?
Receptionist: I''m afraid not. The rooms with a lake view are all full that weekend, but this one looks onto the garden.
Guest: That''s fine. Is there a place to park the car?
Receptionist: Yes, there''s a free car park behind the hotel.
Guest: Wonderful. Also, my husband can''t eat nuts. Is that a problem at breakfast?
Receptionist: Not at all. Just tell the kitchen when you arrive. Could I take your name, please?
Guest: Yes, it''s Helen Walker, W, A, L, K, E, R.
Receptionist: Thank you, Mrs Walker. You''ll need to pay a deposit of twenty pounds when you book, and the rest when you arrive.
Guest: I''ll pay by card now, then.', 'listening/placement/placement-b35-a/part1.mp3', 130, 'VALID'),
    ('c174285a-2e11-5c58-bb9d-64c614510699', 'AUDIO', 'Guide: Good morning, everyone. Welcome to the Harbour Museum. Before we begin the tour, I''d like to explain a few things.
Guide: The museum is in an old warehouse that was built in 1890. For almost a hundred years it was used to store tea and spices, which arrived by ship from Asia. When the harbour became too small for large ships, the warehouse was empty for twenty years. It became a museum in 1995.
Guide: There are three floors. On the ground floor you''ll find the model ships. The most popular is a model of the Eagle, a ship that sailed to India in 1840. On the first floor there is an exhibition about the lives of sailors, with real uniforms and tools. And on the top floor there''s a cinema, where you can watch a short film about the harbour. It lasts twenty minutes and starts every hour.
Guide: Our tour will take about ninety minutes. First we''ll visit the model ships, then the sailors'' exhibition. After that, you''ll have free time, and you can have lunch in the café, which is on the top floor, next to the cinema. The café has a lovely view of the harbour.
Guide: Please don''t touch the objects in the glass cases. You may take photographs, as long as you don''t use a flash. Bags must be left in the lockers near the entrance. The lockers need a one-pound coin, which you get back later.
Guide: Now, shall we start?', 'listening/placement/placement-b35-a/part2.mp3', 139, 'VALID');

INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('a6abf915-c52a-5968-a16c-f12b193fc208', 'PASSAGE', 'Gardens on the Roof

In many big cities there is very little space for gardens. Buildings take up most of the land, and what is left is often used for roads and car parks. However, in recent years more and more people have started to grow plants and vegetables in an unusual place: on the roofs of buildings.

One example is a school in a large city. The school''s roof used to be empty and unused. Three years ago, a teacher asked the headteacher if students could use it to grow food. After some discussion, the school agreed to pay for soil and boxes. Today, about sixty students look after the garden, and they grow tomatoes, beans and herbs. The food is used in the school kitchen, and some is sold to parents at the end of each term.

Roof gardens have other advantages as well. In summer, a building with plants on its roof stays cooler, so it needs less air conditioning. The plants also catch rainwater, which reduces the risk of floods in the streets below. Insects such as bees and butterflies visit the flowers, which helps to improve the environment of the city.

However, roof gardens are not simple to build. The roof must be strong enough to carry the weight of the soil, and an engineer must check this first. Water is also a problem, because plants on a roof dry out quickly in hot weather. Some schools use old rain barrels to collect water for this reason.

In spite of these difficulties, many students say the gardens are worth the effort. ''Before, I never ate vegetables,'' said one fourteen-year-old. ''Now I eat tomatoes every day.''', 'VALID'),
    ('6dd2c19c-57c6-5ff3-bec4-f19f9c3a639c', 'PASSAGE', 'The Story of the Umbrella

Most people think of the umbrella as something British, but the idea is much older. The first umbrellas were used about four thousand years ago in Egypt and China, but they were not for rain. They were made to give shade from the sun, and in many countries only important people, such as kings, were allowed to use them.

The Chinese were probably the first to cover paper umbrellas with wax, which made them waterproof. By the sixteenth century, umbrellas for rain had reached Europe, but at first they were used mainly by women. Men who carried one were often laughed at in the street.

This changed because of a London man, Jonas Hanway, who started using an umbrella in the 1750s. He carried one every day, even when the weather was fine, and he quickly became well known for it. At first, coach drivers were not pleased, because people with umbrellas did not need to take a coach when it rained. In time, however, more men began to follow his example.

The early umbrellas were heavy. They had a wooden frame and a cover made of oiled cloth, and they were difficult to open. In 1852, a British inventor named Samuel Fox made a lighter frame from thin steel, which made umbrellas cheaper and easier to carry.

Today, umbrellas are made in many shapes and sizes, and some are designed to survive strong wind. Yet a great many are lost or thrown away every year, and few can be recycled easily.', 'VALID');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('6457cc1e-0aa6-5321-839a-91df0d0a7563', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('1a9d3bda-4107-517d-afeb-6cdc4a5a710c', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('fd7b4e11-1b39-5699-8ee4-125d7154bcf0', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('7d1fd832-f80e-593e-8cfd-9c21d702c5be', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('b042a332-e230-5e72-a4df-be63399e0bfd', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('fea733c0-0da1-5720-ad33-77f7566e04df', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('7781bce7-e107-52bc-b816-871c529dfb69', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('03963175-65b1-5fd0-9bde-7c0aaab736c0', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('cfbf1a2b-e64d-5c60-91df-d8db9a871ded', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('b12c7b2d-d4d1-5247-adc3-a8a63c41959c', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('5cd09aa5-da52-5b7a-9942-1c61a9375fc2', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('3047c23b-a443-5789-972f-0dedc653cb8e', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('23a34e2d-c552-51a4-aebd-272d7a025bc1', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('712fb19e-fbf6-5a26-a486-dfe47c809f25', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('bfe2c7a7-cec1-538f-a417-741cd11dd845', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('c5732577-c7d4-5290-96dc-d0fa554b5676', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('2ce0576e-0c88-5caf-bda0-c3dbb72f482a', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('1c59a926-e31d-5215-b583-1a3ae7311812', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('64d08a0b-1874-566e-ae2b-a486639c43e6', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('132dd53a-36e8-537d-b769-fa60060152a4', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('8e319c65-cda2-58be-bbfd-51f8de15528d', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('ab9a402a-bb60-560c-a181-6bc632dbc577', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('ef96d7ea-c8e2-5e1c-81ea-c9b48c7eca50', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('a75a5fbd-ee59-5c78-9ae6-647c36dedaad', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('ba366588-1e40-5907-93e8-b46ee91aa9de', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('dbb349d3-c209-5381-801f-03a40c92a20a', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('1fc4d0fc-b9bf-53a2-9ad8-737632a21d92', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('3a87a373-692c-5a15-afe9-dcdf2424122e', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('92f678ec-32d6-5f72-b25c-e98aac06854d', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('e422b144-1a5b-5972-87be-efa178175694', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('8d1e70c1-4d21-5c41-a270-93e119c4888e', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('b9d3b44d-0d15-559f-b073-51d016ffc3f4', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2ff6837e-58f0-5407-aece-272b89ba93db', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('dabcb09c-b834-5471-9047-c8a25e7c6e1f', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('011c86c4-865c-5d42-ac49-ed5fd55e5d1f', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('4d768c36-7b8e-544a-af6f-2910e9fdefb4', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('fff492ab-1b21-513c-92ed-bcf3a16e26ad', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('aa6a7430-7f12-59a9-917a-8ed816932592', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('b3c9c622-1f45-584e-9ce3-9e656dd0c701', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('f12b71b7-b2ea-53e3-8acf-645938dbf2e1', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('e27e409d-f06f-5b1f-8339-cb8d68c3e315', 'ESSAY', 'WRITING', 'PLACEMENT', 'PUBLISHED'),
    ('d24cd99b-2f17-59f6-a294-3f2161f7c66c', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('bbc6407b-7f3b-5395-a9a4-0b5642caf4c0', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('918111a6-af1d-53ff-8985-b213c90f3dba', '6457cc1e-0aa6-5321-839a-91df0d0a7563', 1, 'Date of arrival: Friday ______ October', NULL,
     '{"type":"FILL","accepted":["11","11th","eleventh","the eleventh","the 11th"]}'::jsonb, 1, 'They arrive on Friday the eleventh.', 'PUBLISHED'),
    ('3fa63539-9ef2-5320-ae02-bd4d5386e021', '1a9d3bda-4107-517d-afeb-6cdc4a5a710c', 1, 'Number of nights: ______', NULL,
     '{"type":"FILL","accepted":["2","two"]}'::jsonb, 1, 'Friday to Sunday is two nights.', 'PUBLISHED'),
    ('aeb64065-31ca-5523-8be6-90ef5eb35966', 'fd7b4e11-1b39-5699-8ee4-125d7154bcf0', 1, 'Guests: two adults and one ______', NULL,
     '{"type":"FILL","accepted":["daughter","child","girl"]}'::jsonb, 1, 'The third person is their seven-year-old daughter.', 'PUBLISHED'),
    ('06ca16d5-fc17-50d3-b692-e145f6d00b9d', '7d1fd832-f80e-593e-8cfd-9c21d702c5be', 1, 'Type of room:', '[{"optionKey":"A","content":"Single room","sortOrder":1},{"optionKey":"B","content":"Family room","sortOrder":2},{"optionKey":"C","content":"Twin room","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The receptionist suggests a family room.', 'PUBLISHED'),
    ('4f663f25-3fd3-5ea5-b693-61f549008f3f', 'b042a332-e230-5e72-a4df-be63399e0bfd', 1, 'Price per night: £______', NULL,
     '{"type":"FILL","accepted":["95","ninety-five","ninety five"]}'::jsonb, 1, 'It costs ninety-five pounds a night.', 'PUBLISHED'),
    ('61d6bd4d-b962-5def-a73c-d3110ae30a3d', 'fea733c0-0da1-5720-ad33-77f7566e04df', 1, 'Which meal is included in the price?', '[{"optionKey":"A","content":"Breakfast","sortOrder":1},{"optionKey":"B","content":"Lunch","sortOrder":2},{"optionKey":"C","content":"Dinner","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Breakfast is included.', 'PUBLISHED'),
    ('24fb9b2b-9749-5d75-aa0e-366db9bdd3c6', '7781bce7-e107-52bc-b816-871c529dfb69', 1, 'The room looks onto the ______.', NULL,
     '{"type":"FILL","accepted":["garden"]}'::jsonb, 1, 'The lake-view rooms are full; this one looks onto the garden.', 'PUBLISHED'),
    ('e8b9b037-9c94-5a8a-8ebf-ad2043ed8855', '03963175-65b1-5fd0-9bde-7c0aaab736c0', 1, 'Special diet: the husband cannot eat ______.', NULL,
     '{"type":"FILL","accepted":["nuts"]}'::jsonb, 1, 'She says her husband can''t eat nuts.', 'PUBLISHED'),
    ('fa4386a3-758c-5543-b14c-3604f97c52c1', 'cfbf1a2b-e64d-5c60-91df-d8db9a871ded', 1, 'Name: Helen ______', NULL,
     '{"type":"FILL","accepted":["walker"]}'::jsonb, 1, 'She spells W, A, L, K, E, R.', 'PUBLISHED'),
    ('f8cd24f1-ee45-5840-9d0d-50399aa2bf0c', 'b12c7b2d-d4d1-5247-adc3-a8a63c41959c', 1, 'Deposit to pay now: £______', NULL,
     '{"type":"FILL","accepted":["20","twenty"]}'::jsonb, 1, 'A deposit of twenty pounds is needed when booking.', 'PUBLISHED'),
    ('c4989736-577c-5700-b03f-85d675c9f0a2', '5cd09aa5-da52-5b7a-9942-1c61a9375fc2', 1, 'For almost a hundred years the building was used to store ...', '[{"optionKey":"A","content":"coal","sortOrder":1},{"optionKey":"B","content":"tea and spices","sortOrder":2},{"optionKey":"C","content":"fish","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'It was used to store tea and spices.', 'PUBLISHED'),
    ('2d5aa2cb-b5ad-5af0-a7b2-db99e65c2dda', '3047c23b-a443-5789-972f-0dedc653cb8e', 1, 'The warehouse was built in ______.', NULL,
     '{"type":"FILL","accepted":["1890"]}'::jsonb, 1, 'It was built in 1890.', 'PUBLISHED'),
    ('a5af8db3-cea8-5fef-9069-ed966c5ff136', '23a34e2d-c552-51a4-aebd-272d7a025bc1', 1, 'What happened when the harbour became too small for large ships?', '[{"optionKey":"A","content":"The warehouse was empty","sortOrder":1},{"optionKey":"B","content":"It became a hotel","sortOrder":2},{"optionKey":"C","content":"It was knocked down","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The warehouse was empty for twenty years.', 'PUBLISHED'),
    ('9894995c-08da-5175-a758-ab8087c6e78d', '712fb19e-fbf6-5a26-a486-dfe47c809f25', 1, 'The building became a museum in ______.', NULL,
     '{"type":"FILL","accepted":["1995"]}'::jsonb, 1, 'It became a museum in 1995.', 'PUBLISHED'),
    ('42925e04-26c5-5057-8d00-fca78c17b554', 'bfe2c7a7-cec1-538f-a417-741cd11dd845', 1, 'What can visitors see on the ground floor?', '[{"optionKey":"A","content":"Real uniforms","sortOrder":1},{"optionKey":"B","content":"Model ships","sortOrder":2},{"optionKey":"C","content":"A film","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'On the ground floor you''ll find the model ships.', 'PUBLISHED'),
    ('6a841b90-a36c-5e19-93b2-f976362db75a', 'c5732577-c7d4-5290-96dc-d0fa554b5676', 1, 'The film about the harbour lasts ______ minutes.', NULL,
     '{"type":"FILL","accepted":["20","twenty"]}'::jsonb, 1, 'It lasts twenty minutes.', 'PUBLISHED'),
    ('4578fa82-3714-52dc-ba32-ca153ead544e', '2ce0576e-0c88-5caf-bda0-c3dbb72f482a', 1, 'How long will the tour take?', '[{"optionKey":"A","content":"About 45 minutes","sortOrder":1},{"optionKey":"B","content":"About 90 minutes","sortOrder":2},{"optionKey":"C","content":"About two hours","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The tour will take about ninety minutes.', 'PUBLISHED'),
    ('a94c28b7-81aa-5398-8243-916e91ead150', '1c59a926-e31d-5215-b583-1a3ae7311812', 1, 'Where is the café?', '[{"optionKey":"A","content":"On the top floor","sortOrder":1},{"optionKey":"B","content":"On the ground floor","sortOrder":2},{"optionKey":"C","content":"Near the entrance","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The café is on the top floor, next to the cinema.', 'PUBLISHED'),
    ('586a0034-eca5-5ba2-a773-b9bae430d505', '64d08a0b-1874-566e-ae2b-a486639c43e6', 1, 'What are visitors not allowed to do?', '[{"optionKey":"A","content":"Take photographs","sortOrder":1},{"optionKey":"B","content":"Use a flash","sortOrder":2},{"optionKey":"C","content":"Use the lockers","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Photographs are allowed as long as you don''t use a flash.', 'PUBLISHED'),
    ('d08dc5af-8d97-5caa-a2ad-78b1f2094373', '132dd53a-36e8-537d-b769-fa60060152a4', 1, 'The lockers need a ______ coin.', NULL,
     '{"type":"FILL","accepted":["one-pound","one pound","1 pound","£1"]}'::jsonb, 1, 'The lockers need a one-pound coin, which you get back later.', 'PUBLISHED'),
    ('54b311c7-7a92-5e96-b4cd-55030968595c', '8e319c65-cda2-58be-bbfd-51f8de15528d', 1, 'Most of the land in big cities is used for gardens.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Buildings, roads and car parks take up most of the land.', 'PUBLISHED'),
    ('5205fbb6-97a9-5c0f-a5bd-5d564af78bdf', 'ab9a402a-bb60-560c-a181-6bc632dbc577', 1, 'A teacher suggested using the school roof to grow food.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'A teacher asked the headteacher if students could use the roof.', 'PUBLISHED'),
    ('b1b7325e-ef73-5e08-bf4b-f4781e3ce431', 'ef96d7ea-c8e2-5e1c-81ea-c9b48c7eca50', 1, 'The school paid for the soil and the boxes.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'The school agreed to pay for soil and boxes.', 'PUBLISHED'),
    ('e6033b09-fd1f-59d5-a3cc-a20a784ded0d', 'a75a5fbd-ee59-5c78-9ae6-647c36dedaad', 1, 'The school has won a prize for its garden.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'No prize is mentioned.', 'PUBLISHED'),
    ('9ebf67aa-c1f5-5009-b660-c30fc5158a89', 'ba366588-1e40-5907-93e8-b46ee91aa9de', 1, 'How long ago did the school garden begin?', '[{"optionKey":"A","content":"One year ago","sortOrder":1},{"optionKey":"B","content":"Three years ago","sortOrder":2},{"optionKey":"C","content":"Ten years ago","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Three years ago a teacher asked to use the roof.', 'PUBLISHED'),
    ('a32b094d-3824-5b87-8ad1-00720b5052c3', 'dbb349d3-c209-5381-801f-03a40c92a20a', 1, 'What happens to some of the food from the garden?', '[{"optionKey":"A","content":"It is given to the students to take home","sortOrder":1},{"optionKey":"B","content":"It is sold to parents","sortOrder":2},{"optionKey":"C","content":"It is sent to restaurants","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Some is sold to parents at the end of each term.', 'PUBLISHED'),
    ('5215aa95-f66f-5b3d-af9e-d7087a025cad', '1fc4d0fc-b9bf-53a2-9ad8-737632a21d92', 1, 'Why must an engineer check the roof?', '[{"optionKey":"A","content":"To see if it is strong enough","sortOrder":1},{"optionKey":"B","content":"To choose the plants","sortOrder":2},{"optionKey":"C","content":"To measure the rain","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The roof must be strong enough to carry the weight of the soil.', 'PUBLISHED'),
    ('2a0a7db3-878a-51c7-bc9a-8e221162c482', '3a87a373-692c-5a15-afe9-dcdf2424122e', 1, 'A building with plants on its roof needs less ______ in summer.', NULL,
     '{"type":"FILL","accepted":["air conditioning"]}'::jsonb, 1, 'It stays cooler, so it needs less air conditioning.', 'PUBLISHED'),
    ('d4a97e82-e444-56b5-8700-2a24e1d06c0d', '92f678ec-32d6-5f72-b25c-e98aac06854d', 1, 'The plants catch ______, which reduces the risk of floods.', NULL,
     '{"type":"FILL","accepted":["rainwater"]}'::jsonb, 1, 'The plants catch rainwater.', 'PUBLISHED'),
    ('10f40643-0958-5fcc-b896-b4f61d1700ff', 'e422b144-1a5b-5972-87be-efa178175694', 1, 'Some schools use old ______ to collect water.', NULL,
     '{"type":"FILL","accepted":["rain barrels","barrels"]}'::jsonb, 1, 'Some schools use old rain barrels to collect water.', 'PUBLISHED'),
    ('0962d204-dc0a-5c6b-8358-bef51a49e0d1', '8d1e70c1-4d21-5c41-a270-93e119c4888e', 1, 'The first umbrellas were designed to keep off the rain.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'They were made to give shade from the sun.', 'PUBLISHED'),
    ('b453f1e6-a7cd-5ef1-b81b-cd53186b1fa0', 'b9d3b44d-0d15-559f-b073-51d016ffc3f4', 1, 'In some countries only important people could use umbrellas.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Only important people, such as kings, were allowed to use them.', 'PUBLISHED'),
    ('d99d0f8e-d85f-5305-bcdc-02d6e4999444', '2ff6837e-58f0-5407-aece-272b89ba93db', 1, 'Jonas Hanway sold umbrellas in his own shop.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'The passage does not say what he did for a living.', 'PUBLISHED'),
    ('3212dff4-6b83-5419-a223-f28e382b927c', 'dabcb09c-b834-5471-9047-c8a25e7c6e1f', 1, 'Coach drivers were pleased when umbrellas became popular.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'At first, coach drivers were not pleased.', 'PUBLISHED'),
    ('76ee157b-6a04-5175-b52c-3ca764f2677a', '011c86c4-865c-5d42-ac49-ed5fd55e5d1f', 1, 'What was special about the Chinese umbrellas?', '[{"optionKey":"A","content":"They were made of silk","sortOrder":1},{"optionKey":"B","content":"They were covered with wax","sortOrder":2},{"optionKey":"C","content":"They were very large","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'They covered paper umbrellas with wax, which made them waterproof.', 'PUBLISHED'),
    ('7024fe6a-d6f7-5ff9-a37a-a2773122fcdd', '4d768c36-7b8e-544a-af6f-2910e9fdefb4', 1, 'In sixteenth-century Europe, umbrellas for rain were used mainly by ...', '[{"optionKey":"A","content":"kings","sortOrder":1},{"optionKey":"B","content":"women","sortOrder":2},{"optionKey":"C","content":"coach drivers","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'At first they were used mainly by women.', 'PUBLISHED'),
    ('0ac2df42-b65a-51af-b91d-8a63fc4e8d78', 'fff492ab-1b21-513c-92ed-bcf3a16e26ad', 1, 'What did Samuel Fox do?', '[{"optionKey":"A","content":"He made a lighter frame","sortOrder":1},{"optionKey":"B","content":"He invented waterproof cloth","sortOrder":2},{"optionKey":"C","content":"He opened the first umbrella shop","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'He made a lighter frame from thin steel.', 'PUBLISHED'),
    ('6b56b537-e7aa-5afa-bfa6-77b432554046', 'aa6a7430-7f12-59a9-917a-8ed816932592', 1, 'The first umbrellas gave ______ from the sun.', NULL,
     '{"type":"FILL","accepted":["shade"]}'::jsonb, 1, 'They were made to give shade from the sun.', 'PUBLISHED'),
    ('7d03bcd7-685e-5f58-a9b9-914be5af9915', 'b3c9c622-1f45-584e-9ce3-9e656dd0c701', 1, 'Early umbrellas had a wooden frame and a cover made of ______ cloth.', NULL,
     '{"type":"FILL","accepted":["oiled"]}'::jsonb, 1, 'The cover was made of oiled cloth.', 'PUBLISHED'),
    ('e0cbee5c-ee1a-5ccf-8371-d66fbf03448e', 'f12b71b7-b2ea-53e3-8acf-645938dbf2e1', 1, 'Samuel Fox made a frame from thin ______.', NULL,
     '{"type":"FILL","accepted":["steel"]}'::jsonb, 1, 'He made a lighter frame from thin steel.', 'PUBLISHED'),
    ('b7e4c3eb-d286-5693-8fc9-674c2f145f05', 'e27e409d-f06f-5b1f-8339-cb8d68c3e315', 1, 'Some people think that all children should learn to cook at school. Do you agree or disagree? Give reasons for your answer and include any relevant examples from your own knowledge or experience. Write at least 120 words.', NULL,
     '{"type":"ESSAY","task":"TASK_2","minWords":120,"passBand":4.5}'::jsonb, 1, 'Graded by the placement essay grader.', 'PUBLISHED'),
    ('c374e4f7-45b2-5b3b-96c0-5c623ba45014', 'd24cd99b-2f17-59f6-a294-3f2161f7c66c', 1, 'Let''s talk about your daily life. What do you usually do in the evening? Has this changed in the last few years?', NULL,
     '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED'),
    ('f1bfa557-6171-5bdb-a01e-28dca95f4b19', 'bbc6407b-7f3b-5395-a9a4-0b5642caf4c0', 1, 'Describe a journey you remember well. You should say where you went, who you went with, how you travelled, and why you remember it. Speak for one to two minutes.', NULL,
     '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED');

UPDATE questions q SET current_published_version_id = qv.id
FROM question_versions qv
WHERE qv.question_id = q.id AND qv.id IN (
    '918111a6-af1d-53ff-8985-b213c90f3dba',
    '3fa63539-9ef2-5320-ae02-bd4d5386e021',
    'aeb64065-31ca-5523-8be6-90ef5eb35966',
    '06ca16d5-fc17-50d3-b692-e145f6d00b9d',
    '4f663f25-3fd3-5ea5-b693-61f549008f3f',
    '61d6bd4d-b962-5def-a73c-d3110ae30a3d',
    '24fb9b2b-9749-5d75-aa0e-366db9bdd3c6',
    'e8b9b037-9c94-5a8a-8ebf-ad2043ed8855',
    'fa4386a3-758c-5543-b14c-3604f97c52c1',
    'f8cd24f1-ee45-5840-9d0d-50399aa2bf0c',
    'c4989736-577c-5700-b03f-85d675c9f0a2',
    '2d5aa2cb-b5ad-5af0-a7b2-db99e65c2dda',
    'a5af8db3-cea8-5fef-9069-ed966c5ff136',
    '9894995c-08da-5175-a758-ab8087c6e78d',
    '42925e04-26c5-5057-8d00-fca78c17b554',
    '6a841b90-a36c-5e19-93b2-f976362db75a',
    '4578fa82-3714-52dc-ba32-ca153ead544e',
    'a94c28b7-81aa-5398-8243-916e91ead150',
    '586a0034-eca5-5ba2-a773-b9bae430d505',
    'd08dc5af-8d97-5caa-a2ad-78b1f2094373',
    '54b311c7-7a92-5e96-b4cd-55030968595c',
    '5205fbb6-97a9-5c0f-a5bd-5d564af78bdf',
    'b1b7325e-ef73-5e08-bf4b-f4781e3ce431',
    'e6033b09-fd1f-59d5-a3cc-a20a784ded0d',
    '9ebf67aa-c1f5-5009-b660-c30fc5158a89',
    'a32b094d-3824-5b87-8ad1-00720b5052c3',
    '5215aa95-f66f-5b3d-af9e-d7087a025cad',
    '2a0a7db3-878a-51c7-bc9a-8e221162c482',
    'd4a97e82-e444-56b5-8700-2a24e1d06c0d',
    '10f40643-0958-5fcc-b896-b4f61d1700ff',
    '0962d204-dc0a-5c6b-8358-bef51a49e0d1',
    'b453f1e6-a7cd-5ef1-b81b-cd53186b1fa0',
    'd99d0f8e-d85f-5305-bcdc-02d6e4999444',
    '3212dff4-6b83-5419-a223-f28e382b927c',
    '76ee157b-6a04-5175-b52c-3ca764f2677a',
    '7024fe6a-d6f7-5ff9-a37a-a2773122fcdd',
    '0ac2df42-b65a-51af-b91d-8a63fc4e8d78',
    '6b56b537-e7aa-5afa-bfa6-77b432554046',
    '7d03bcd7-685e-5f58-a9b9-914be5af9915',
    'e0cbee5c-ee1a-5ccf-8371-d66fbf03448e',
    'b7e4c3eb-d286-5693-8fc9-674c2f145f05',
    'c374e4f7-45b2-5b3b-96c0-5c623ba45014',
    'f1bfa557-6171-5bdb-a01e-28dca95f4b19');

INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key, placement_pool)
VALUES ('eb0ab3d4-debb-5210-b938-c625b32c182a', 'PLACEMENT-B35-A', 'IELTS placement test, band 3-5 (A)', 'PLACEMENT_TEST', NULL, NULL, NULL, 'PUBLISHED', NULL, 'BAND_3_5');

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
VALUES ('d05b1caf-4aab-5efe-bdf4-c4e04c088169', 'eb0ab3d4-debb-5210-b938-c625b32c182a', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions) VALUES
    ('b1835591-7a6a-554c-91d6-82554b53f2a4', 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', 'Listening Part 1: booking a hotel room', 'LISTENING', 1, NULL, 'Questions 1–10. Complete the booking form. Write ONE WORD AND/OR A NUMBER for each answer. For questions 4 and 6, choose A, B or C.'),
    ('7bcd87e1-f234-5b0f-958a-94afc17146a7', 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', 'Listening Part 2: a tour of the Harbour Museum', 'LISTENING', 2, NULL, 'Questions 11–20. Choose the correct letter, A, B or C, or write ONE WORD AND/OR A NUMBER where you see a line.'),
    ('265b30e7-cbf0-57c8-8f97-edcf374894c2', 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', 'Reading Passage 1: gardens on the roof', 'READING', 3, 600, 'Questions 21–24: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 25–27: Choose the correct letter, A, B or C. Questions 28–30: Complete the sentences. Choose NO MORE THAN TWO WORDS from the passage.'),
    ('612ed48c-ee86-577d-93d3-972f2c09e2c5', 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', 'Reading Passage 2: the story of the umbrella', 'READING', 4, 600, 'Questions 31–34: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 35–37: Choose the correct letter, A, B or C. Questions 38–40: Complete the sentences. Choose ONE WORD ONLY from the passage.'),
    ('4abca10e-e6e4-5cb0-aca9-f9b5dd67de73', 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', 'Writing: cooking at school', 'WRITING', 5, 1500, 'Spend about 25 minutes on this task. Write at least 120 words.'),
    ('eb05991c-d392-597d-9c7c-ea5084e73a33', 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', 'Speaking Part 1: daily life', 'SPEAKING', 6, NULL, 'Answer the questions in a few sentences.'),
    ('f20a1c49-9dde-5082-ac6a-94c2b0570970', 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', 'Speaking Part 2: a journey you remember', 'SPEAKING', 7, NULL, 'You have one minute to prepare, then speak for one to two minutes.');

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('afb42e37-42ad-58f6-9978-8cd8fa2b5a6d', 'ea4f403c-a679-5e70-b585-f2b741783645', 'b1835591-7a6a-554c-91d6-82554b53f2a4', 0),
    ('06f4c36d-2487-5f3b-8551-c35300b914c8', 'c174285a-2e11-5c58-bb9d-64c614510699', '7bcd87e1-f234-5b0f-958a-94afc17146a7', 0),
    ('5d36a646-205d-5b28-abe3-37fd6f014ea9', 'a6abf915-c52a-5968-a16c-f12b193fc208', '265b30e7-cbf0-57c8-8f97-edcf374894c2', 0),
    ('6dfc873d-2085-5613-8346-104581b078b2', '6dd2c19c-57c6-5ff3-bec4-f19f9c3a639c', '612ed48c-ee86-577d-93d3-972f2c09e2c5', 0);

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('0b5497a6-431d-5462-bfbc-1507734e6cf3', 'b1835591-7a6a-554c-91d6-82554b53f2a4', '918111a6-af1d-53ff-8985-b213c90f3dba', 1, 1.0),
    ('5bc81071-9655-594c-bf7e-a9bb4d613d27', 'b1835591-7a6a-554c-91d6-82554b53f2a4', '3fa63539-9ef2-5320-ae02-bd4d5386e021', 2, 1.0),
    ('ed459415-09a9-5604-aaee-d090fb7316eb', 'b1835591-7a6a-554c-91d6-82554b53f2a4', 'aeb64065-31ca-5523-8be6-90ef5eb35966', 3, 1.0),
    ('6c613837-80fb-55b6-bade-48215e6a428e', 'b1835591-7a6a-554c-91d6-82554b53f2a4', '06ca16d5-fc17-50d3-b692-e145f6d00b9d', 4, 1.0),
    ('86b3551b-b883-5a7d-892a-f849ed7a412e', 'b1835591-7a6a-554c-91d6-82554b53f2a4', '4f663f25-3fd3-5ea5-b693-61f549008f3f', 5, 1.0),
    ('df77fe51-6a09-5df3-8757-42e866929fb3', 'b1835591-7a6a-554c-91d6-82554b53f2a4', '61d6bd4d-b962-5def-a73c-d3110ae30a3d', 6, 1.0),
    ('7b2e3c6a-35ad-5785-9e8e-8570f9b230d3', 'b1835591-7a6a-554c-91d6-82554b53f2a4', '24fb9b2b-9749-5d75-aa0e-366db9bdd3c6', 7, 1.0),
    ('c035e655-8905-5312-a7a4-8120c3045465', 'b1835591-7a6a-554c-91d6-82554b53f2a4', 'e8b9b037-9c94-5a8a-8ebf-ad2043ed8855', 8, 1.0),
    ('b793c6af-93b3-5cd9-8c10-518c6f450454', 'b1835591-7a6a-554c-91d6-82554b53f2a4', 'fa4386a3-758c-5543-b14c-3604f97c52c1', 9, 1.0),
    ('0376fb37-3b00-59a9-bba8-99b4e34629e0', 'b1835591-7a6a-554c-91d6-82554b53f2a4', 'f8cd24f1-ee45-5840-9d0d-50399aa2bf0c', 10, 1.0),
    ('b350bd5a-3020-5897-962b-b3c83afa7727', '7bcd87e1-f234-5b0f-958a-94afc17146a7', 'c4989736-577c-5700-b03f-85d675c9f0a2', 1, 1.0),
    ('4655ebb5-c2ed-549e-af7b-242da7742371', '7bcd87e1-f234-5b0f-958a-94afc17146a7', '2d5aa2cb-b5ad-5af0-a7b2-db99e65c2dda', 2, 1.0),
    ('96c6c641-d029-5b3e-92a8-a2dc6e008980', '7bcd87e1-f234-5b0f-958a-94afc17146a7', 'a5af8db3-cea8-5fef-9069-ed966c5ff136', 3, 1.0),
    ('541c1af3-e446-51b5-9a3a-e104fa8336bb', '7bcd87e1-f234-5b0f-958a-94afc17146a7', '9894995c-08da-5175-a758-ab8087c6e78d', 4, 1.0),
    ('4f0eede3-d8f2-59b9-8203-b6bfbf0ea805', '7bcd87e1-f234-5b0f-958a-94afc17146a7', '42925e04-26c5-5057-8d00-fca78c17b554', 5, 1.0),
    ('aeab9e02-81d6-53fc-bd4d-8f289de1e9a3', '7bcd87e1-f234-5b0f-958a-94afc17146a7', '6a841b90-a36c-5e19-93b2-f976362db75a', 6, 1.0),
    ('84f73a5d-e0a4-551d-a2a2-7f7d1912889f', '7bcd87e1-f234-5b0f-958a-94afc17146a7', '4578fa82-3714-52dc-ba32-ca153ead544e', 7, 1.0),
    ('d7a4a492-25ed-5a91-8341-357ada1bc1ac', '7bcd87e1-f234-5b0f-958a-94afc17146a7', 'a94c28b7-81aa-5398-8243-916e91ead150', 8, 1.0),
    ('710e026f-c763-5c5a-8d71-a021b7815db6', '7bcd87e1-f234-5b0f-958a-94afc17146a7', '586a0034-eca5-5ba2-a773-b9bae430d505', 9, 1.0),
    ('bfafaed4-df55-5eac-a7d5-96eb21245440', '7bcd87e1-f234-5b0f-958a-94afc17146a7', 'd08dc5af-8d97-5caa-a2ad-78b1f2094373', 10, 1.0),
    ('dd4dafd9-3757-564a-87c1-6d462300ccf8', '265b30e7-cbf0-57c8-8f97-edcf374894c2', '54b311c7-7a92-5e96-b4cd-55030968595c', 1, 1.0),
    ('c826471f-26be-5532-bc70-a9e576e1f324', '265b30e7-cbf0-57c8-8f97-edcf374894c2', '5205fbb6-97a9-5c0f-a5bd-5d564af78bdf', 2, 1.0),
    ('9ed338a7-7919-5bb3-ba4a-216d30e4bf26', '265b30e7-cbf0-57c8-8f97-edcf374894c2', 'b1b7325e-ef73-5e08-bf4b-f4781e3ce431', 3, 1.0),
    ('e410cc8f-47f7-5c46-8211-a5f5076cff73', '265b30e7-cbf0-57c8-8f97-edcf374894c2', 'e6033b09-fd1f-59d5-a3cc-a20a784ded0d', 4, 1.0),
    ('2913b0d1-6ddd-5219-af63-446bd72e1146', '265b30e7-cbf0-57c8-8f97-edcf374894c2', '9ebf67aa-c1f5-5009-b660-c30fc5158a89', 5, 1.0),
    ('2b30a87b-09ee-5a78-a255-9a624d3d2198', '265b30e7-cbf0-57c8-8f97-edcf374894c2', 'a32b094d-3824-5b87-8ad1-00720b5052c3', 6, 1.0),
    ('306d9cf3-0e19-5f0a-9ee7-68b567c8d628', '265b30e7-cbf0-57c8-8f97-edcf374894c2', '5215aa95-f66f-5b3d-af9e-d7087a025cad', 7, 1.0),
    ('3bc6976b-0ad6-550b-9f97-e46a72239f5f', '265b30e7-cbf0-57c8-8f97-edcf374894c2', '2a0a7db3-878a-51c7-bc9a-8e221162c482', 8, 1.0),
    ('8c42628b-d740-5041-b02e-3bd46014d222', '265b30e7-cbf0-57c8-8f97-edcf374894c2', 'd4a97e82-e444-56b5-8700-2a24e1d06c0d', 9, 1.0),
    ('8a52219c-4dac-56ea-988a-6457a6806d1c', '265b30e7-cbf0-57c8-8f97-edcf374894c2', '10f40643-0958-5fcc-b896-b4f61d1700ff', 10, 1.0),
    ('c25107e5-8483-5bb9-875b-733462bdf4d9', '612ed48c-ee86-577d-93d3-972f2c09e2c5', '0962d204-dc0a-5c6b-8358-bef51a49e0d1', 1, 1.0),
    ('6db8b418-b2ac-590d-92b1-bdeb287cffd7', '612ed48c-ee86-577d-93d3-972f2c09e2c5', 'b453f1e6-a7cd-5ef1-b81b-cd53186b1fa0', 2, 1.0),
    ('b02baa06-37ef-553c-83e1-3af1a1e11dce', '612ed48c-ee86-577d-93d3-972f2c09e2c5', 'd99d0f8e-d85f-5305-bcdc-02d6e4999444', 3, 1.0),
    ('1760c2a7-5630-58c4-8b04-29f47b9a496d', '612ed48c-ee86-577d-93d3-972f2c09e2c5', '3212dff4-6b83-5419-a223-f28e382b927c', 4, 1.0),
    ('a9b14490-11e2-55ea-8f57-847ded0e7803', '612ed48c-ee86-577d-93d3-972f2c09e2c5', '76ee157b-6a04-5175-b52c-3ca764f2677a', 5, 1.0),
    ('f6f2b309-2934-5e86-8984-ca4db7abbf1c', '612ed48c-ee86-577d-93d3-972f2c09e2c5', '7024fe6a-d6f7-5ff9-a37a-a2773122fcdd', 6, 1.0),
    ('49587f02-49b6-5455-889d-51aa90fab56c', '612ed48c-ee86-577d-93d3-972f2c09e2c5', '0ac2df42-b65a-51af-b91d-8a63fc4e8d78', 7, 1.0),
    ('54817670-0aea-588e-be4e-e92200ce47cd', '612ed48c-ee86-577d-93d3-972f2c09e2c5', '6b56b537-e7aa-5afa-bfa6-77b432554046', 8, 1.0),
    ('64600e84-1fe1-53ba-a824-845726395fcb', '612ed48c-ee86-577d-93d3-972f2c09e2c5', '7d03bcd7-685e-5f58-a9b9-914be5af9915', 9, 1.0),
    ('f7388313-6291-54c0-b318-e6c2a0189e35', '612ed48c-ee86-577d-93d3-972f2c09e2c5', 'e0cbee5c-ee1a-5ccf-8371-d66fbf03448e', 10, 1.0),
    ('772df35c-8e95-5c68-ba97-dffa1d21e2aa', '4abca10e-e6e4-5cb0-aca9-f9b5dd67de73', 'b7e4c3eb-d286-5693-8fc9-674c2f145f05', 1, 1.0),
    ('a958cc51-a886-5221-bdd0-3b601cdfcba4', 'eb05991c-d392-597d-9c7c-ea5084e73a33', 'c374e4f7-45b2-5b3b-96c0-5c623ba45014', 1, 1.0),
    ('8e651286-5e4f-55a4-a45a-822f78c35203', 'f20a1c49-9dde-5082-ac6a-94c2b0570970', 'f1bfa557-6171-5bdb-a01e-28dca95f4b19', 1, 1.0);

UPDATE content_packages SET current_published_version_id = 'd05b1caf-4aab-5efe-bdf4-c4e04c088169', updated_at = CURRENT_TIMESTAMP
WHERE id = 'eb0ab3d4-debb-5210-b938-c625b32c182a';

-- PLACEMENT-B35-B: IELTS placement test, band 3-5 (B)

INSERT INTO content_assets (id, asset_type, text_content, media_reference, duration_seconds, validation_status) VALUES
    ('1e38b7eb-9a61-5595-9eb1-077a48c7c461', 'AUDIO', 'Receptionist: Good morning, Campus Fitness Centre.
Student: Hello. I''m a student and I''d like to ask about membership.
Receptionist: Of course. We have two types. Standard membership is thirty pounds a month and lets you use the gym and the swimming pool. Premium is forty-five pounds, and it includes fitness classes as well.
Student: I''m mainly interested in the classes. Which ones do you offer?
Receptionist: There''s yoga on Mondays, dance on Wednesdays, and boxing on Saturday mornings.
Student: Dance sounds fun. What time is it?
Receptionist: It starts at half past six in the evening and lasts one hour.
Student: Perfect. Is there a discount for students?
Receptionist: Yes, students get twenty per cent off if they show a student card.
Student: That''s great. Do I need to pay a joining fee?
Receptionist: Normally there''s a one-off fee of ten pounds, but it''s free this month.
Student: How long is the contract?
Receptionist: You can choose three months or twelve months.
Student: I''ll take three months for now. Where can I sign up?
Receptionist: You can come to the reception desk any time before eight in the evening. Please bring your student card and a photo.
Student: Thank you. I''ll come on Thursday.', 'listening/placement/placement-b35-b/part1.mp3', 116, 'VALID'),
    ('c930da51-b8de-57d7-a933-e53dca801ca4', 'AUDIO', 'Speaker: Good evening, everyone. Tonight I''d like to tell you about the new bike scheme in our city, which starts next month.
Speaker: The idea is simple. There will be four hundred bikes at forty stations across the city. You take a bike from one station and leave it at any other station. The first thirty minutes of every journey are free, and after that it costs one pound for each extra half hour.
Speaker: To use the scheme, you must first register on the website or on the phone app. Registration is free, but you have to give your bank card details, because you pay a deposit of fifty pounds, which is returned if you stop using the bikes.
Speaker: Why is the council doing this? There are three main reasons. First, the centre of the city is very crowded, and the roads are often blocked. Second, many people cannot park near their work. And finally, the council wants people to be healthier.
Speaker: Safety is important. All the bikes have lights that work automatically, and helmets are for sale at some stations for five pounds. Riders under sixteen must wear a helmet, but for adults it is a recommendation, not a rule.
Speaker: The first stations will open near the railway station, the university and the main shopping street. The other stations will open over the next six months. If the scheme is a success, the council hopes to double the number of bikes within two years.
Speaker: Are there any questions?', 'listening/placement/placement-b35-b/part2.mp3', 142, 'VALID');

INSERT INTO content_assets (id, asset_type, text_content, validation_status) VALUES
    ('8c7ba390-fa8c-5b37-add6-bd4657e114b6', 'PASSAGE', 'The Rise of the Podcast

Twenty years ago, hardly anyone had heard the word ''podcast''. Today, millions of people listen to them every week, on their way to work, while they cook, or during a long walk. A podcast is simply a series of audio programmes that can be downloaded from the internet and played whenever the listener likes.

One reason for this growth is the cost. Making a podcast needs very little equipment: a microphone, a computer and some free software are enough. As a result, almost anyone with an interesting idea can start one, and this has produced shows on subjects as different as cooking, history, football and mental health. Unlike radio stations, podcasters do not need a licence from the government.

Another reason is convenience. Listeners decide when and where to listen, and they can stop and start as they wish. Many people say that podcasts are better than television, because they can do other things while they listen. A recent survey found that one third of listeners hear their favourite shows while driving, and another quarter listen while doing housework.

Not everything about podcasts is positive, however. Because anyone can make one, the quality varies greatly, and some podcasts give information that is not correct. Making money is also difficult. Most small podcasts earn little or nothing, so the people who make them usually have other jobs. Only a small number earn enough from advertising to become full-time podcasters.

Despite these problems, experts expect the number of listeners to continue growing, especially among young people, who are now the largest group.', 'VALID'),
    ('4c5788cb-19f7-5407-ac1b-7eab99cfcc81', 'PASSAGE', 'Starting Again at Forty

When Rosa Martinez turned forty, she did something that surprised her friends and family: she left her job as an accountant and began studying to become a nurse. ''I had worked in an office for sixteen years,'' she says, ''and I was never really happy. One day I looked at the list of things I wanted to do in life, and I realised that I had not done any of them.''

Rosa''s decision was not easy. She had to pay for her course herself, and her husband was worried about money. To save, the family sold their second car and stopped going on holiday abroad. Rosa also found studying difficult at first, because it was twenty years since she had last been in a classroom. ''The other students were half my age,'' she remembers. ''In the first week I felt like their mother.''

Luckily, her classmates turned out to be very friendly. They helped her with the computer programs she was not familiar with, and in return she gave them advice on managing their time and money. She also found that her experience at work was useful. ''In an office you learn to stay calm when people are angry with you,'' she says. ''That is exactly what you need in a hospital.''

Three years later, Rosa finished her studies and now works in a children''s ward. She earns less than she did as an accountant, but she says she has never regretted her choice. ''Every morning I look forward to going to work,'' she says. ''I could not say that before.''

Her advice to others who are thinking about changing career? ''Do not wait too long. And do not be afraid of being the oldest person in the room.''', 'VALID'),
    ('cbadfec6-e812-58a9-9c75-438132ed2f24', 'PASSAGE', 'Transport | 2010 | 2020
Car | 55 | 48
Bus | 20 | 17
Bicycle | 5 | 14
Walking | 15 | 16
Train | 5 | 5', 'VALID');

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('8719d971-1497-5f91-9436-bd06d028802d', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('ffe566d4-4d23-5429-8ea6-d77c448f66e7', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('c1ff79c2-9624-5556-be86-ceac0b803d45', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('f76d49c7-6d87-5448-a296-3e105aac4c85', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('bf82c53c-f611-5751-9dc3-f877a493681f', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('ee3fb80c-c6c6-53ec-8efb-667d1f07f2fd', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('9ac03771-28b6-5ab8-8573-e0266eb13929', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('47128980-60e8-5fff-abc8-fa2a9acb5e8f', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('089f07d4-80db-57ea-942e-bef138f24c98', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('e2b46ec0-646a-571c-8951-ece5ac55723c', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('a404a3c9-c8d3-5f71-9aae-93c8879589b9', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('abfd9d97-704e-501a-a9fe-aef992326cb4', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('69091c78-0681-547b-99f5-74aecb3803ab', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('90c96707-1539-54ac-8dec-29267db7a162', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('bb719cdd-e60a-5dae-b783-12e19f0791f2', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('bfe9b78a-a71b-5892-bb47-3efda04d1c55', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('c78c7671-9d47-5c3a-b6fd-0a7d4c9d0083', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('cc49192a-6f5d-5bf8-9b22-2c1e97daa5e1', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('21f9327c-02dc-5ab1-9392-96e7c63b4372', 'FILL_IN_BLANK', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('189ea45d-7961-5dc0-b5b0-730d7bbad0f1', 'MULTIPLE_CHOICE', 'LISTENING', 'PLACEMENT', 'PUBLISHED'),
    ('1b1ce419-1725-52cb-bf6d-4089622d3a63', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('d6251629-571a-5709-b345-715dbe24bc27', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('8c33876b-b54f-573a-8b69-e31a2070a696', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('54762296-8fd1-5786-bc49-766a1f9d3642', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('f2a68fad-e560-5a90-865a-3c1422afaed8', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('16632083-fa7b-5108-8091-3c654b79c951', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('0e628d04-0d5c-5c74-82f4-622c8130a13c', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('4f21d949-3b90-5f02-ab7e-9b6f32955977', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('5597fff7-745e-5cc2-9e85-bba26e778b06', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('80003822-c9cb-57ef-92d9-90606f66d459', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('db71ae37-f084-5b65-9960-585ce3ea182e', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('64ac8968-4e2c-570e-91fd-7f8770ed839d', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('df5283c9-28c4-550e-9e5e-373e0bebdb33', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('ff85f07e-2ce7-5856-8c49-b6b343786545', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('4291712a-8673-5e5c-9a21-3c473a278fe1', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('976b515f-39c3-54f8-bf15-95075526b123', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('2e276957-d228-5e96-b8e9-7658252f89fc', 'MULTIPLE_CHOICE', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('be2105d1-9876-54fc-bf53-f6c93105caa0', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('4dad3146-81c5-583d-8684-7ce74be711c5', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('0e5bbe1e-16dc-5085-bc3a-c526ad2eefa0', 'FILL_IN_BLANK', 'READING', 'PLACEMENT', 'PUBLISHED'),
    ('ed821333-1042-5c11-9c1b-ca87df389a98', 'ESSAY', 'WRITING', 'PLACEMENT', 'PUBLISHED'),
    ('bb70a755-a65c-50ed-9f4d-3699d8de1c05', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED'),
    ('be326968-2698-5439-a628-1de8e15b37eb', 'SPEAKING', 'SPEAKING', 'PLACEMENT', 'PUBLISHED');

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('fcacc7f6-99dc-53b3-a689-70e60142fea8', '8719d971-1497-5f91-9436-bd06d028802d', 1, 'Standard membership: £______ a month', NULL,
     '{"type":"FILL","accepted":["30","thirty"]}'::jsonb, 1, 'Standard membership is thirty pounds a month.', 'PUBLISHED'),
    ('6a7ca451-8159-5c99-bae0-852f0c46102d', 'ffe566d4-4d23-5429-8ea6-d77c448f66e7', 1, 'Premium membership: £______ a month', NULL,
     '{"type":"FILL","accepted":["45","forty-five","forty five"]}'::jsonb, 1, 'Premium is forty-five pounds.', 'PUBLISHED'),
    ('eff70b72-a165-5840-8587-7090ed044a51', 'c1ff79c2-9624-5556-be86-ceac0b803d45', 1, 'What does Premium include that Standard does not?', '[{"optionKey":"A","content":"The swimming pool","sortOrder":1},{"optionKey":"B","content":"Fitness classes","sortOrder":2},{"optionKey":"C","content":"A personal trainer","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Premium includes fitness classes as well.', 'PUBLISHED'),
    ('00935f32-d5a4-50b6-9a93-ee8eb573e084', 'f76d49c7-6d87-5448-a296-3e105aac4c85', 1, 'Yoga class is on ______.', NULL,
     '{"type":"FILL","accepted":["monday","mondays"]}'::jsonb, 1, 'There''s yoga on Mondays.', 'PUBLISHED'),
    ('4c89f768-4bad-55b7-991b-c71b0fc485d9', 'bf82c53c-f611-5751-9dc3-f877a493681f', 1, 'Dance class starts at ______ in the evening.', NULL,
     '{"type":"FILL","accepted":["6.30","6:30","18:30","half past six","half past 6"]}'::jsonb, 1, 'It starts at half past six.', 'PUBLISHED'),
    ('c918fba7-02df-53bc-9d20-8977bfa6cf8f', 'ee3fb80c-c6c6-53ec-8efb-667d1f07f2fd', 1, 'The dance class lasts ______ hour.', NULL,
     '{"type":"FILL","accepted":["1","one"]}'::jsonb, 1, 'It lasts one hour.', 'PUBLISHED'),
    ('2a09e031-e17f-5eea-baab-abf95932e709', '9ac03771-28b6-5ab8-8573-e0266eb13929', 1, 'Students get ______ per cent off with a student card.', NULL,
     '{"type":"FILL","accepted":["20","twenty"]}'::jsonb, 1, 'Students get twenty per cent off.', 'PUBLISHED'),
    ('dd4a5110-1e68-5c3c-a9da-ddff0377ba44', '47128980-60e8-5fff-abc8-fa2a9acb5e8f', 1, 'What is the joining fee this month?', '[{"optionKey":"A","content":"£10","sortOrder":1},{"optionKey":"B","content":"Nothing","sortOrder":2},{"optionKey":"C","content":"£20","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Normally ten pounds, but it''s free this month.', 'PUBLISHED'),
    ('1dda5894-42f3-59b1-a43e-b85b08408609', '089f07d4-80db-57ea-942e-bef138f24c98', 1, 'The student chooses a contract of ______ months.', NULL,
     '{"type":"FILL","accepted":["3","three"]}'::jsonb, 1, 'She says she''ll take three months for now.', 'PUBLISHED'),
    ('593adf82-cb9a-58f7-af77-961c0176f92e', 'e2b46ec0-646a-571c-8951-ece5ac55723c', 1, 'What must she bring when she signs up?', '[{"optionKey":"A","content":"A student card and a photo","sortOrder":1},{"optionKey":"B","content":"Her passport","sortOrder":2},{"optionKey":"C","content":"A letter from a doctor","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Please bring your student card and a photo.', 'PUBLISHED'),
    ('542f2585-8034-51f5-bc36-440cc1636614', 'a404a3c9-c8d3-5f71-9aae-93c8879589b9', 1, 'How many bikes will there be?', '[{"optionKey":"A","content":"Forty","sortOrder":1},{"optionKey":"B","content":"Four hundred","sortOrder":2},{"optionKey":"C","content":"Four thousand","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'There will be four hundred bikes.', 'PUBLISHED'),
    ('6ffe8703-29ef-531c-ad78-8007056a226e', 'abfd9d97-704e-501a-a9fe-aef992326cb4', 1, 'There will be ______ stations across the city.', NULL,
     '{"type":"FILL","accepted":["40","forty"]}'::jsonb, 1, 'Four hundred bikes at forty stations.', 'PUBLISHED'),
    ('c0de38b0-03fb-525d-8fc0-f3f32fe02aab', '69091c78-0681-547b-99f5-74aecb3803ab', 1, 'The first ______ minutes of every journey are free.', NULL,
     '{"type":"FILL","accepted":["30","thirty"]}'::jsonb, 1, 'The first thirty minutes are free.', 'PUBLISHED'),
    ('2fc8392d-80dc-5fa5-8a43-acbf54dda4bc', '90c96707-1539-54ac-8dec-29267db7a162', 1, 'Each extra half hour costs £______.', NULL,
     '{"type":"FILL","accepted":["1","one"]}'::jsonb, 1, 'It costs one pound for each extra half hour.', 'PUBLISHED'),
    ('2484a295-1979-55ca-a3db-2f5ee7f85a80', 'bb719cdd-e60a-5dae-b783-12e19f0791f2', 1, 'To register, people must use ...', '[{"optionKey":"A","content":"the website or the phone app","sortOrder":1},{"optionKey":"B","content":"the council office","sortOrder":2},{"optionKey":"C","content":"a letter","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'You must register on the website or on the phone app.', 'PUBLISHED'),
    ('7ffe2b9b-2c0e-54e6-9b29-2460a15af58a', 'bfe9b78a-a71b-5892-bb47-3efda04d1c55', 1, 'The deposit is £______.', NULL,
     '{"type":"FILL","accepted":["50","fifty"]}'::jsonb, 1, 'You pay a deposit of fifty pounds.', 'PUBLISHED'),
    ('a1600f80-ae73-5784-829f-d3d68c8b0e25', 'c78c7671-9d47-5c3a-b6fd-0a7d4c9d0083', 1, 'One reason for the scheme is that ...', '[{"optionKey":"A","content":"the roads in the centre are crowded","sortOrder":1},{"optionKey":"B","content":"bikes are cheap to make","sortOrder":2},{"optionKey":"C","content":"tourists asked for it","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'The centre is very crowded and the roads are often blocked.', 'PUBLISHED'),
    ('508248e6-90ff-5dc5-ad5d-1908280d1cb4', 'cc49192a-6f5d-5bf8-9b22-2c1e97daa5e1', 1, 'Who must wear a helmet?', '[{"optionKey":"A","content":"Everyone","sortOrder":1},{"optionKey":"B","content":"Riders under sixteen","sortOrder":2},{"optionKey":"C","content":"Nobody","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Riders under sixteen must wear a helmet.', 'PUBLISHED'),
    ('f95f41ee-f513-57a9-9295-c6485a921337', '21f9327c-02dc-5ab1-9392-96e7c63b4372', 1, 'The first stations will open near the railway station, the university and the main ______ street.', NULL,
     '{"type":"FILL","accepted":["shopping"]}'::jsonb, 1, 'The main shopping street.', 'PUBLISHED'),
    ('071a0a50-65c7-56b4-9923-6f82e155c7e4', '189ea45d-7961-5dc0-b5b0-730d7bbad0f1', 1, 'If the scheme is a success, the council hopes to ...', '[{"optionKey":"A","content":"double the number of bikes","sortOrder":1},{"optionKey":"B","content":"close some car parks","sortOrder":2},{"optionKey":"C","content":"build new roads","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'It hopes to double the number of bikes within two years.', 'PUBLISHED'),
    ('aa68e1ab-1dcf-51de-8322-0cb094900a75', '1b1ce419-1725-52cb-bf6d-4089622d3a63', 1, 'A podcast can be played at any time the listener chooses.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'It can be played whenever the listener likes.', 'PUBLISHED'),
    ('4e6b8c09-6d6e-5685-b5c1-6974b60b32c4', 'd6251629-571a-5709-b345-715dbe24bc27', 1, 'Podcasters need a licence from the government.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Unlike radio stations, podcasters do not need a licence.', 'PUBLISHED'),
    ('bf5ca6c1-2c66-5aef-a9e8-38ff30c97f86', '8c33876b-b54f-573a-8b69-e31a2070a696', 1, 'Podcasts about football are the most popular.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'The passage does not say which subject is most popular.', 'PUBLISHED'),
    ('07c118a8-3017-52c0-979a-120abcba74ac', '54762296-8fd1-5786-bc49-766a1f9d3642', 1, 'Most podcasters earn enough to work full-time.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Only a small number earn enough to become full-time podcasters.', 'PUBLISHED'),
    ('29edaa35-dc73-51e8-9317-46a9bbf56d80', 'f2a68fad-e560-5a90-865a-3c1422afaed8', 1, 'What is needed to make a podcast?', '[{"optionKey":"A","content":"Expensive equipment","sortOrder":1},{"optionKey":"B","content":"A microphone, a computer and free software","sortOrder":2},{"optionKey":"C","content":"A television studio","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'A microphone, a computer and some free software are enough.', 'PUBLISHED'),
    ('ea1a9fa7-f0dd-5420-b503-ec16ddb61e0c', '16632083-fa7b-5108-8091-3c654b79c951', 1, 'What does the survey say about driving?', '[{"optionKey":"A","content":"One third of listeners hear their favourite shows while driving","sortOrder":1},{"optionKey":"B","content":"Most listeners never drive","sortOrder":2},{"optionKey":"C","content":"Nobody listens while driving","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'One third of listeners hear their favourite shows while driving.', 'PUBLISHED'),
    ('f3b0a72d-4759-5297-bb2b-85c3a29548b2', '0e628d04-0d5c-5c74-82f4-622c8130a13c', 1, 'Which group of listeners is now the largest?', '[{"optionKey":"A","content":"Young people","sortOrder":1},{"optionKey":"B","content":"Older people","sortOrder":2},{"optionKey":"C","content":"Teachers","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Young people are now the largest group.', 'PUBLISHED'),
    ('5775b597-d2d1-5ce7-bb4c-e4a57c7c04a6', '4f21d949-3b90-5f02-ab7e-9b6f32955977', 1, 'Podcasts can be about subjects such as cooking, history, football and ______.', NULL,
     '{"type":"FILL","accepted":["mental health"]}'::jsonb, 1, 'The subjects include mental health.', 'PUBLISHED'),
    ('16c5dfa5-7954-55cc-b9e3-69152a82991a', '5597fff7-745e-5cc2-9e85-bba26e778b06', 1, 'The ______ of podcasts varies greatly.', NULL,
     '{"type":"FILL","accepted":["quality"]}'::jsonb, 1, 'The quality varies greatly.', 'PUBLISHED'),
    ('6d4e9fe9-49f4-503e-b506-566f0ee61dcd', '80003822-c9cb-57ef-92d9-90606f66d459', 1, 'A small number of podcasters earn enough from ______ to work full-time.', NULL,
     '{"type":"FILL","accepted":["advertising"]}'::jsonb, 1, 'Only a small number earn enough from advertising.', 'PUBLISHED'),
    ('fab64a43-677f-5fa0-80a0-17fd0b0d391c', 'db71ae37-f084-5b65-9960-585ce3ea182e', 1, 'Rosa worked as an accountant for sixteen years.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'She had worked in an office for sixteen years.', 'PUBLISHED'),
    ('3ce8fc0e-9eec-5138-9d03-061e9bdf4037', '64ac8968-4e2c-570e-91fd-7f8770ed839d', 1, 'Rosa''s husband was happy about her decision from the start.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Her husband was worried about money.', 'PUBLISHED'),
    ('77845327-a7e8-56cf-9710-03b90a55b310', 'df5283c9-28c4-550e-9e5e-373e0bebdb33', 1, 'Rosa has two children.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'Her children are not mentioned.', 'PUBLISHED'),
    ('3fe5ef98-86cc-575f-b97e-6ce9a3ce59cb', 'ff85f07e-2ce7-5856-8c49-b6b343786545', 1, 'Rosa now earns more money than she did before.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'She earns less than she did as an accountant.', 'PUBLISHED'),
    ('bfac16ea-de84-5189-b9ad-1fb1eba8d36c', '4291712a-8673-5e5c-9a21-3c473a278fe1', 1, 'How did the family save money?', '[{"optionKey":"A","content":"They moved to a smaller house","sortOrder":1},{"optionKey":"B","content":"They sold their second car","sortOrder":2},{"optionKey":"C","content":"They stopped buying new clothes","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'The family sold their second car.', 'PUBLISHED'),
    ('edcd766f-b47b-5d18-bacb-454f1a8b7f0c', '976b515f-39c3-54f8-bf15-95075526b123', 1, 'How did Rosa feel in her first week as a student?', '[{"optionKey":"A","content":"Like the mother of the other students","sortOrder":1},{"optionKey":"B","content":"Very confident","sortOrder":2},{"optionKey":"C","content":"Bored","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'In the first week she felt like their mother.', 'PUBLISHED'),
    ('9bc1f1a4-fdab-53cd-b948-a4402589db2e', '2e276957-d228-5e96-b8e9-7658252f89fc', 1, 'How did her classmates help her?', '[{"optionKey":"A","content":"With computer programs","sortOrder":1},{"optionKey":"B","content":"By paying for her course","sortOrder":2},{"optionKey":"C","content":"By cooking for her family","sortOrder":3}]'::jsonb,
     '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'They helped her with the computer programs.', 'PUBLISHED'),
    ('e5cfab15-66d9-57c8-a505-363d79816d7d', 'be2105d1-9876-54fc-bf53-f6c93105caa0', 1, 'In her office job, Rosa learned to stay ______ when people were angry.', NULL,
     '{"type":"FILL","accepted":["calm"]}'::jsonb, 1, 'In an office you learn to stay calm.', 'PUBLISHED'),
    ('9610824a-afb1-5f8d-825b-ddc05a48d658', '4dad3146-81c5-583d-8684-7ce74be711c5', 1, 'Rosa now works in a children''s ______.', NULL,
     '{"type":"FILL","accepted":["ward"]}'::jsonb, 1, 'She works in a children''s ward.', 'PUBLISHED'),
    ('38ad8cb9-351a-53a1-963a-598e66ac47e9', '0e5bbe1e-16dc-5085-bc3a-c526ad2eefa0', 1, 'Her advice is: ''Do not ______ too long.''', NULL,
     '{"type":"FILL","accepted":["wait"]}'::jsonb, 1, 'Do not wait too long.', 'PUBLISHED'),
    ('0818e796-8cf7-5c37-9099-de3fd788098a', 'ed821333-1042-5c11-9c1b-ca87df389a98', 1, 'The table below shows the percentage of people who used five types of transport to travel to work in one town in 2010 and in 2020. Summarise the information by selecting and reporting the main features, and make comparisons where relevant. Write at least 100 words.', NULL,
     '{"type":"ESSAY","task":"TASK_1","minWords":100,"passBand":4.5,"chartFacts":"Percentage of people using five types of transport to travel to work in one town. Car: 55 in 2010, 48 in 2020. Bus: 20, 17. Bicycle: 5, 14. Walking: 15, 16. Train: 5, 5. Key features: the car was the most common way to travel in both years but fell from 55% to 48%; the bicycle showed the biggest increase, almost tripling from 5% to 14%; bus use fell from 20% to 17%; walking rose slightly from 15% to 16%; train use stayed the same at 5%."}'::jsonb, 1, 'Graded by the placement essay grader.', 'PUBLISHED'),
    ('035b067b-70fe-5eab-b7c2-9465444e915a', 'bb70a755-a65c-50ed-9f4d-3699d8de1c05', 1, 'Let''s talk about your work or studies. What do you do? What do you like most about it?', NULL,
     '{"type":"SPEAKING","durationSeconds":45}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED'),
    ('635f54ff-5168-576f-b77f-9061c61312fa', 'be326968-2698-5439-a628-1de8e15b37eb', 1, 'Describe a time when you helped someone. You should say who you helped, what the problem was, how you helped, and how you felt afterwards. Speak for one to two minutes.', NULL,
     '{"type":"SPEAKING","durationSeconds":90}'::jsonb, 1, 'Recorded answer; counted only when a placement speaking band is configured.', 'PUBLISHED');

UPDATE questions q SET current_published_version_id = qv.id
FROM question_versions qv
WHERE qv.question_id = q.id AND qv.id IN (
    'fcacc7f6-99dc-53b3-a689-70e60142fea8',
    '6a7ca451-8159-5c99-bae0-852f0c46102d',
    'eff70b72-a165-5840-8587-7090ed044a51',
    '00935f32-d5a4-50b6-9a93-ee8eb573e084',
    '4c89f768-4bad-55b7-991b-c71b0fc485d9',
    'c918fba7-02df-53bc-9d20-8977bfa6cf8f',
    '2a09e031-e17f-5eea-baab-abf95932e709',
    'dd4a5110-1e68-5c3c-a9da-ddff0377ba44',
    '1dda5894-42f3-59b1-a43e-b85b08408609',
    '593adf82-cb9a-58f7-af77-961c0176f92e',
    '542f2585-8034-51f5-bc36-440cc1636614',
    '6ffe8703-29ef-531c-ad78-8007056a226e',
    'c0de38b0-03fb-525d-8fc0-f3f32fe02aab',
    '2fc8392d-80dc-5fa5-8a43-acbf54dda4bc',
    '2484a295-1979-55ca-a3db-2f5ee7f85a80',
    '7ffe2b9b-2c0e-54e6-9b29-2460a15af58a',
    'a1600f80-ae73-5784-829f-d3d68c8b0e25',
    '508248e6-90ff-5dc5-ad5d-1908280d1cb4',
    'f95f41ee-f513-57a9-9295-c6485a921337',
    '071a0a50-65c7-56b4-9923-6f82e155c7e4',
    'aa68e1ab-1dcf-51de-8322-0cb094900a75',
    '4e6b8c09-6d6e-5685-b5c1-6974b60b32c4',
    'bf5ca6c1-2c66-5aef-a9e8-38ff30c97f86',
    '07c118a8-3017-52c0-979a-120abcba74ac',
    '29edaa35-dc73-51e8-9317-46a9bbf56d80',
    'ea1a9fa7-f0dd-5420-b503-ec16ddb61e0c',
    'f3b0a72d-4759-5297-bb2b-85c3a29548b2',
    '5775b597-d2d1-5ce7-bb4c-e4a57c7c04a6',
    '16c5dfa5-7954-55cc-b9e3-69152a82991a',
    '6d4e9fe9-49f4-503e-b506-566f0ee61dcd',
    'fab64a43-677f-5fa0-80a0-17fd0b0d391c',
    '3ce8fc0e-9eec-5138-9d03-061e9bdf4037',
    '77845327-a7e8-56cf-9710-03b90a55b310',
    '3fe5ef98-86cc-575f-b97e-6ce9a3ce59cb',
    'bfac16ea-de84-5189-b9ad-1fb1eba8d36c',
    'edcd766f-b47b-5d18-bacb-454f1a8b7f0c',
    '9bc1f1a4-fdab-53cd-b948-a4402589db2e',
    'e5cfab15-66d9-57c8-a505-363d79816d7d',
    '9610824a-afb1-5f8d-825b-ddc05a48d658',
    '38ad8cb9-351a-53a1-963a-598e66ac47e9',
    '0818e796-8cf7-5c37-9099-de3fd788098a',
    '035b067b-70fe-5eab-b7c2-9465444e915a',
    '635f54ff-5168-576f-b77f-9061c61312fa');

INSERT INTO content_packages (id, code, title, package_type, topic_id, lesson_id, course_id, status, required_feature_key, placement_pool)
VALUES ('7dd68c00-3db2-569c-be64-74021255d206', 'PLACEMENT-B35-B', 'IELTS placement test, band 3-5 (B)', 'PLACEMENT_TEST', NULL, NULL, NULL, 'PUBLISHED', NULL, 'BAND_3_5');

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at)
VALUES ('a1e621fe-5d25-5d98-bca5-21eeaa9aece6', '7dd68c00-3db2-569c-be64-74021255d206', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP);

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions) VALUES
    ('52fb2297-7fca-5f23-a107-ed27d8d18af6', 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', 'Listening Part 1: joining a fitness centre', 'LISTENING', 1, NULL, 'Questions 1–10. Complete the notes. Write ONE WORD AND/OR A NUMBER for each answer. For questions 3, 8 and 10, choose A, B or C.'),
    ('ebf9057b-3580-5196-8f3d-44951e32b6d4', 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', 'Listening Part 2: a new city bike scheme', 'LISTENING', 2, NULL, 'Questions 11–20. Choose the correct letter, A, B or C, or write ONE WORD AND/OR A NUMBER where you see a line.'),
    ('ab8d06d0-b74e-5c88-850b-e695de712946', 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', 'Reading Passage 1: the rise of the podcast', 'READING', 3, 600, 'Questions 21–24: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 25–27: Choose the correct letter, A, B or C. Questions 28–30: Complete the sentences. Choose NO MORE THAN TWO WORDS from the passage.'),
    ('4ea096c0-17f5-58ff-8d47-b592b7b41f51', 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', 'Reading Passage 2: starting again at forty', 'READING', 4, 600, 'Questions 31–34: Do the statements agree with the information in the passage? Choose TRUE, FALSE or NOT GIVEN. Questions 35–37: Choose the correct letter, A, B or C. Questions 38–40: Complete the sentences. Choose ONE WORD ONLY from the passage.'),
    ('39cc0add-5422-5bff-895e-8eed1cd52e77', 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', 'Writing: travelling to work', 'WRITING', 5, 1200, 'Spend about 20 minutes on this task. Write at least 100 words.'),
    ('d62523c0-32d6-5fd6-8353-58b154ff031d', 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', 'Speaking Part 1: work or study', 'SPEAKING', 6, NULL, 'Answer the questions in a few sentences.'),
    ('37140f15-5b4f-56b7-968e-4287bd2343e7', 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', 'Speaking Part 2: helping someone', 'SPEAKING', 7, NULL, 'You have one minute to prepare, then speak for one to two minutes.');

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('52571b5e-a61e-5c1f-922a-6397d1cea254', '1e38b7eb-9a61-5595-9eb1-077a48c7c461', '52fb2297-7fca-5f23-a107-ed27d8d18af6', 0),
    ('cf30b8b2-bc3c-5eed-8fe3-eef47500ab28', 'c930da51-b8de-57d7-a933-e53dca801ca4', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', 0),
    ('849fe443-0093-5cce-bf62-205be59b6858', '8c7ba390-fa8c-5b37-add6-bd4657e114b6', 'ab8d06d0-b74e-5c88-850b-e695de712946', 0),
    ('93539d61-f099-5a96-9af8-b6a7b6d7f691', '4c5788cb-19f7-5407-ac1b-7eab99cfcc81', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', 0),
    ('0fba7039-d742-5427-9fbb-03ba690087f5', 'cbadfec6-e812-58a9-9c75-438132ed2f24', '39cc0add-5422-5bff-895e-8eed1cd52e77', 0);

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score) VALUES
    ('5fbcf902-837d-57cc-b501-8f4cdb52e040', '52fb2297-7fca-5f23-a107-ed27d8d18af6', 'fcacc7f6-99dc-53b3-a689-70e60142fea8', 1, 1.0),
    ('87cf4635-d41f-5bcd-bb4c-e235abd4d4e1', '52fb2297-7fca-5f23-a107-ed27d8d18af6', '6a7ca451-8159-5c99-bae0-852f0c46102d', 2, 1.0),
    ('7d281ce4-4673-5ae8-9e58-d2788b36b478', '52fb2297-7fca-5f23-a107-ed27d8d18af6', 'eff70b72-a165-5840-8587-7090ed044a51', 3, 1.0),
    ('b716974d-4ca8-5e32-b007-95fee6960a88', '52fb2297-7fca-5f23-a107-ed27d8d18af6', '00935f32-d5a4-50b6-9a93-ee8eb573e084', 4, 1.0),
    ('b59fcc34-675a-5680-b82f-47a110fde141', '52fb2297-7fca-5f23-a107-ed27d8d18af6', '4c89f768-4bad-55b7-991b-c71b0fc485d9', 5, 1.0),
    ('9996d3a9-214a-5731-8e9f-62dc78344252', '52fb2297-7fca-5f23-a107-ed27d8d18af6', 'c918fba7-02df-53bc-9d20-8977bfa6cf8f', 6, 1.0),
    ('f19d5c12-19dd-5f9d-a1fa-51f81d0e387c', '52fb2297-7fca-5f23-a107-ed27d8d18af6', '2a09e031-e17f-5eea-baab-abf95932e709', 7, 1.0),
    ('4ee561db-2dc4-5a83-8519-ce0498240c88', '52fb2297-7fca-5f23-a107-ed27d8d18af6', 'dd4a5110-1e68-5c3c-a9da-ddff0377ba44', 8, 1.0),
    ('a25283ca-f7b2-5a48-a010-98674e84c3b7', '52fb2297-7fca-5f23-a107-ed27d8d18af6', '1dda5894-42f3-59b1-a43e-b85b08408609', 9, 1.0),
    ('79a951c6-3787-5022-8d45-9824226799d8', '52fb2297-7fca-5f23-a107-ed27d8d18af6', '593adf82-cb9a-58f7-af77-961c0176f92e', 10, 1.0),
    ('177c9def-8f2e-574c-a083-07fd540ac9ee', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', '542f2585-8034-51f5-bc36-440cc1636614', 1, 1.0),
    ('2e6c51e4-3c82-5aa5-9d21-d99446ed0615', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', '6ffe8703-29ef-531c-ad78-8007056a226e', 2, 1.0),
    ('fb57de22-978f-5695-a833-2bcf1850c3b3', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', 'c0de38b0-03fb-525d-8fc0-f3f32fe02aab', 3, 1.0),
    ('f76486ed-3615-5420-bd1a-5cfdb1cd8433', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', '2fc8392d-80dc-5fa5-8a43-acbf54dda4bc', 4, 1.0),
    ('87e6286a-da88-5052-8824-200e17b3f216', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', '2484a295-1979-55ca-a3db-2f5ee7f85a80', 5, 1.0),
    ('eccafaa4-b742-579f-aafc-902c19fd247a', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', '7ffe2b9b-2c0e-54e6-9b29-2460a15af58a', 6, 1.0),
    ('98e94dd1-23c2-5470-b6ed-fac4c1421328', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', 'a1600f80-ae73-5784-829f-d3d68c8b0e25', 7, 1.0),
    ('3e8c4128-285d-55dd-97ad-0e7e987cf06e', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', '508248e6-90ff-5dc5-ad5d-1908280d1cb4', 8, 1.0),
    ('c6b57e5a-0cf9-5eb5-b7f3-b2e7de1420a5', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', 'f95f41ee-f513-57a9-9295-c6485a921337', 9, 1.0),
    ('89a36d03-63f2-5db5-99be-b19057ab0d9a', 'ebf9057b-3580-5196-8f3d-44951e32b6d4', '071a0a50-65c7-56b4-9923-6f82e155c7e4', 10, 1.0),
    ('c5ddf988-948b-5b6c-9d95-8a2aaf9c9ccb', 'ab8d06d0-b74e-5c88-850b-e695de712946', 'aa68e1ab-1dcf-51de-8322-0cb094900a75', 1, 1.0),
    ('6712a368-b0d1-5837-ac5f-54b1d9bba083', 'ab8d06d0-b74e-5c88-850b-e695de712946', '4e6b8c09-6d6e-5685-b5c1-6974b60b32c4', 2, 1.0),
    ('29db602e-9936-5989-8575-8fed66ba7712', 'ab8d06d0-b74e-5c88-850b-e695de712946', 'bf5ca6c1-2c66-5aef-a9e8-38ff30c97f86', 3, 1.0),
    ('cd4d32c6-fd48-5328-b809-aea8168d3c82', 'ab8d06d0-b74e-5c88-850b-e695de712946', '07c118a8-3017-52c0-979a-120abcba74ac', 4, 1.0),
    ('c2729216-c094-5fa2-aa5d-4b5c04713f34', 'ab8d06d0-b74e-5c88-850b-e695de712946', '29edaa35-dc73-51e8-9317-46a9bbf56d80', 5, 1.0),
    ('4c5deb8e-bb0c-5821-a54c-761e87b5e1db', 'ab8d06d0-b74e-5c88-850b-e695de712946', 'ea1a9fa7-f0dd-5420-b503-ec16ddb61e0c', 6, 1.0),
    ('3a9a6382-4eb4-50bb-a7f8-5510663d95e4', 'ab8d06d0-b74e-5c88-850b-e695de712946', 'f3b0a72d-4759-5297-bb2b-85c3a29548b2', 7, 1.0),
    ('09f9d388-ce79-5a88-86ad-2a869f52ac5e', 'ab8d06d0-b74e-5c88-850b-e695de712946', '5775b597-d2d1-5ce7-bb4c-e4a57c7c04a6', 8, 1.0),
    ('01070b78-51d1-59bb-b2de-bbaf2a5ef1e5', 'ab8d06d0-b74e-5c88-850b-e695de712946', '16c5dfa5-7954-55cc-b9e3-69152a82991a', 9, 1.0),
    ('060869af-1a37-5ec6-9caa-fb610c0a7ed5', 'ab8d06d0-b74e-5c88-850b-e695de712946', '6d4e9fe9-49f4-503e-b506-566f0ee61dcd', 10, 1.0),
    ('f514ac7b-0e68-5ca5-a87c-66164a71983e', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', 'fab64a43-677f-5fa0-80a0-17fd0b0d391c', 1, 1.0),
    ('affc2320-7eed-54f8-9541-e36c2618da4f', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', '3ce8fc0e-9eec-5138-9d03-061e9bdf4037', 2, 1.0),
    ('f786e090-7b5c-5d81-8286-2c482ed111db', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', '77845327-a7e8-56cf-9710-03b90a55b310', 3, 1.0),
    ('32fd53f9-57b4-5ef1-b9de-766505f07ea6', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', '3fe5ef98-86cc-575f-b97e-6ce9a3ce59cb', 4, 1.0),
    ('18726712-3eec-5882-93f7-7fdabc385483', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', 'bfac16ea-de84-5189-b9ad-1fb1eba8d36c', 5, 1.0),
    ('25126b0d-8d14-53c6-bc1f-235f641607cb', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', 'edcd766f-b47b-5d18-bacb-454f1a8b7f0c', 6, 1.0),
    ('3b6eaab8-d4b8-5776-bf2d-ad5f5f161fb5', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', '9bc1f1a4-fdab-53cd-b948-a4402589db2e', 7, 1.0),
    ('229e9927-406e-58fb-b6f6-b53795c958eb', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', 'e5cfab15-66d9-57c8-a505-363d79816d7d', 8, 1.0),
    ('11f80080-71e2-552b-8994-5c7a5ef3633f', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', '9610824a-afb1-5f8d-825b-ddc05a48d658', 9, 1.0),
    ('d061d64c-18ef-52ef-bc48-df54a3ded9d4', '4ea096c0-17f5-58ff-8d47-b592b7b41f51', '38ad8cb9-351a-53a1-963a-598e66ac47e9', 10, 1.0),
    ('89f1e370-d23c-5a8f-8219-40c88b0b60ca', '39cc0add-5422-5bff-895e-8eed1cd52e77', '0818e796-8cf7-5c37-9099-de3fd788098a', 1, 1.0),
    ('25904489-b660-5da8-84f7-4439bafb34c4', 'd62523c0-32d6-5fd6-8353-58b154ff031d', '035b067b-70fe-5eab-b7c2-9465444e915a', 1, 1.0),
    ('72323d11-db7d-590e-b7e7-8a7d8951cf4a', '37140f15-5b4f-56b7-968e-4287bd2343e7', '635f54ff-5168-576f-b77f-9061c61312fa', 1, 1.0);

UPDATE content_packages SET current_published_version_id = 'a1e621fe-5d25-5d98-bca5-21eeaa9aece6', updated_at = CURRENT_TIMESTAMP
WHERE id = '7dd68c00-3db2-569c-be64-74021255d206';
