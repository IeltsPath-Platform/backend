-- Demo Reading practice tests for the practice test library: three published PRACTICE_TEST packages from three sources
-- (IELTS_SPACE_PRO 3 parts, C10_C20 1 part, ACTUAL_TESTS 2 parts, the last one PREMIUM_CONTENT). Passages and questions are
-- original text written for this project. Every question has purpose EXAM and a CHOICE or FILL answer spec. Cover images
-- are small inline SVG data URIs. Every statement is idempotent (ON CONFLICT DO NOTHING).

INSERT INTO content_assets (id, asset_type, text_content, media_reference, validation_status) VALUES
    ('2d000000-0000-4000-8000-010000000001', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiODIzMmMiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', 'VALID'),
    ('2d000000-0000-4000-8000-020000000001', 'PASSAGE', 'A. Twenty years ago, keeping bees on a city rooftop was a curiosity. Today, thousands of apartment dwellers tend hives between air vents and satellite dishes, and several hotels sell honey from their own roofs.

B. Cities turn out to suit bees better than farmland does. Gardens and parks flower from early spring to late autumn, while large fields of a single crop bloom for only a few weeks. Urban bees often produce more honey per hive than colonies in the countryside.

C. The trend has critics. Some ecologists warn that crowding too many managed hives into one district leaves wild bees with less to eat. They argue that a city should plant more flowers before it adds more hives.

D. Beekeepers reply that most newcomers join training courses and keep records of their colonies. Several councils now ask for a simple registration, which gives researchers a clearer picture of how many hives a neighbourhood can support.', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000000002', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiODIzMmMiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', 'VALID'),
    ('2d000000-0000-4000-8000-020000000002', 'PASSAGE', 'A. For most of the twentieth century, cities judged noise by complaint. If few residents phoned the council, a street was assumed to be quiet enough. This method missed the many people who never complained.

B. Sensors changed the picture. Small microphones fixed to lamp posts now record sound levels every minute and send the figures to a central map. Planners can see at a glance which streets are loudest after midnight and which are noisiest at the morning rush.

C. The data produced a surprise. The loudest places were often not main roads but narrow streets where sound bounces between tall buildings. Planting hedges and fitting road surfaces that absorb sound cut the levels there by several decibels.

D. Residents remain divided. Some welcome the map, while others worry that the microphones might record conversations. Engineers insist that the devices store only volume, never speech.', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000000003', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiNiODIzMmMiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAzPC90ZXh0Pjwvc3ZnPg==', 'VALID'),
    ('2d000000-0000-4000-8000-020000000003', 'PASSAGE', 'A. Two centuries ago, oyster reefs lined many coasts, filtering water and sheltering fish. Overfishing and pollution removed almost all of them, and by the 1990s a once common shellfish had become rare.

B. Restoration began with an unexpected material: old shells. Volunteers collect discarded shells from restaurants, let them dry in the sun for a year, and place them on the seabed. Young oysters prefer to settle on shell rather than on bare sand.

C. Results have been slow but encouraging. After five years, one restored reef supported twice as many fish species as a nearby patch of sand, and the water above it was visibly clearer.

D. Scientists caution that reefs alone cannot solve coastal pollution. Without reducing the waste that enters rivers, new oysters may struggle to survive, however carefully they are planted.', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000000004', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiMwZjZiOGYiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', 'VALID'),
    ('2d000000-0000-4000-8000-020000000004', 'PASSAGE', 'A. In several towns, children now walk to school in supervised lines known as walking buses. A volunteer adult leads at the front, another follows at the back, and the group collects pupils from agreed stops along a fixed route.

B. The scheme began as a response to congestion. Head teachers noticed that parents driving short distances caused queues outside the gates every morning, which made the road less safe for those on foot.

C. Early results suggest benefits beyond traffic. Teachers report that pupils who walk arrive more alert, and parents say that the journey gives children time to talk with friends.

D. Not everyone is convinced. Some parents worry about bad weather and about relying on volunteers, who may be unavailable at short notice. Organisers respond by keeping a reserve list of helpers.', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000000005', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM3YTRiMDAiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAxPC90ZXh0Pjwvc3ZnPg==', 'VALID'),
    ('2d000000-0000-4000-8000-020000000005', 'PASSAGE', 'A. Sourdough bread is raised not by packaged yeast but by a living mixture of flour, water and wild microbes called a starter. Bakers feed the starter regularly, and a healthy one can be kept going for decades.

B. The microbes in a starter do two jobs. Yeasts produce gas that makes the dough rise, while bacteria create the acids that give the loaf its sharp taste.

C. Temperature matters greatly. In a warm kitchen the acids build up quickly and the bread tastes sour; in a cool room fermentation is slower and the flavour is milder.

D. Researchers who sampled starters from hundreds of homes found that the mix of microbes depended less on the region than on the baker''s habits, such as how often the starter was fed.', NULL, 'VALID'),
    ('2d000000-0000-4000-8000-010000000006', 'IMAGE', NULL, 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMjAiIGhlaWdodD0iMjAwIiB2aWV3Qm94PSIwIDAgMzIwIDIwMCI+PHJlY3Qgd2lkdGg9IjMyMCIgaGVpZ2h0PSIyMDAiIGZpbGw9IiM3YTRiMDAiLz48dGV4dCB4PSIxNjAiIHk9IjExOCIgZm9udC1mYW1pbHk9Ikdlb3JnaWEsc2VyaWYiIGZvbnQtc2l6ZT0iNzIiIGZpbGw9IiNmZmZmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlAyPC90ZXh0Pjwvc3ZnPg==', 'VALID'),
    ('2d000000-0000-4000-8000-020000000006', 'PASSAGE', 'A. Each year a tree adds a ring of new wood to its trunk. In a wet year the ring is wide, and in a dry year it is narrow, so a cross-section of a trunk works like a record of past weather.

B. By matching the pattern of rings in living trees with those in older timber, researchers have built records that stretch back thousands of years. A beam from an old house can sometimes be dated to the very year its tree was cut.

C. The method has limits. Trees growing in places with steady rainfall show little variation from year to year, so their rings reveal much less about climate than those of trees at the edge of a desert.

D. Even so, ring records have helped historians understand why certain settlements failed. A run of narrow rings often matches a long drought that appears in written accounts of famine.', NULL, 'VALID')
ON CONFLICT DO NOTHING;

INSERT INTO questions (id, question_type, skill, purpose, status) VALUES
    ('2d000000-0000-4000-8000-030000000001', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000002', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000003', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000004', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000005', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000006', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000007', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000008', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000009', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000010', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000011', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000012', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000013', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000014', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000015', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000016', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000017', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000018', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000019', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000020', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000021', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000022', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000023', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000024', 'TRUE_FALSE_NOT_GIVEN', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000025', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000026', 'FILL_IN_BLANK', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000027', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000028', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000029', 'MULTIPLE_CHOICE', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000030', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-030000000031', 'MATCHING', 'READING', 'EXAM', 'PUBLISHED')
ON CONFLICT DO NOTHING;

INSERT INTO question_versions (id, question_id, version_number, stem, options, answer_spec, schema_version, explanation, status) VALUES
    ('2d000000-0000-4000-8000-040000000001', '2d000000-0000-4000-8000-030000000001', 1, 'Some hotels sell honey produced on their own roofs.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Paragraph A: several hotels sell honey from their own roofs.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000002', '2d000000-0000-4000-8000-030000000002', 1, 'Bees in cities always make less honey than bees in the countryside.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Paragraph B: urban bees often produce more honey per hive.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000003', '2d000000-0000-4000-8000-030000000003', 1, 'Most new beekeepers keep bees in order to make a profit.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'The passage does not say why newcomers keep bees.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000004', '2d000000-0000-4000-8000-030000000004', 1, 'Gardens and parks in cities flower from early spring to late ______.', NULL, '{"type":"FILL","accepted":["autumn"]}'::jsonb, 1, 'Paragraph B: from early spring to late autumn.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000005', '2d000000-0000-4000-8000-030000000005', 1, 'Some councils now ask beekeepers for a simple ______.', NULL, '{"type":"FILL","accepted":["registration"]}'::jsonb, 1, 'Paragraph D: several councils now ask for a simple registration.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000006', '2d000000-0000-4000-8000-030000000006', 1, 'How did cities mainly judge noise in the past?', '[{"optionKey":"A","content":"By counting complaints","sortOrder":1},{"optionKey":"B","content":"By measuring with sensors","sortOrder":2},{"optionKey":"C","content":"By asking engineers","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph A: cities judged noise by complaint.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000007', '2d000000-0000-4000-8000-030000000007', 1, 'What did the sensor data show about the loudest places?', '[{"optionKey":"A","content":"They were main roads","sortOrder":1},{"optionKey":"B","content":"They were narrow streets between tall buildings","sortOrder":2},{"optionKey":"C","content":"They were parks","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph C: narrow streets where sound bounces between tall buildings.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000008', '2d000000-0000-4000-8000-030000000008', 1, 'What do engineers say about the devices?', '[{"optionKey":"A","content":"They record speech clearly","sortOrder":1},{"optionKey":"B","content":"They are too expensive","sortOrder":2},{"optionKey":"C","content":"They store only volume","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Paragraph D: the devices store only volume, never speech.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000009', '2d000000-0000-4000-8000-030000000009', 1, 'a reason why some people distrust the sensors', '[{"optionKey":"A","content":"Paragraph A","sortOrder":1},{"optionKey":"B","content":"Paragraph B","sortOrder":2},{"optionKey":"C","content":"Paragraph C","sortOrder":3},{"optionKey":"D","content":"Paragraph D","sortOrder":4}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'Paragraph D: some residents worry the microphones might record conversations.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000010', '2d000000-0000-4000-8000-030000000010', 1, 'a way of reducing sound in a narrow street', '[{"optionKey":"A","content":"Paragraph A","sortOrder":1},{"optionKey":"B","content":"Paragraph B","sortOrder":2},{"optionKey":"C","content":"Paragraph C","sortOrder":3},{"optionKey":"D","content":"Paragraph D","sortOrder":4}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Paragraph C: hedges and sound-absorbing road surfaces cut the levels.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000011', '2d000000-0000-4000-8000-030000000011', 1, 'The writer believes that shells from restaurants are a useful material for restoring reefs.', '[{"optionKey":"YES","content":"YES","sortOrder":1},{"optionKey":"NO","content":"NO","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"YES"}'::jsonb, 1, 'Paragraph B: restoration began with discarded shells and young oysters prefer them.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000012', '2d000000-0000-4000-8000-030000000012', 1, 'The writer thinks oyster reefs will solve coastal pollution on their own.', '[{"optionKey":"YES","content":"YES","sortOrder":1},{"optionKey":"NO","content":"NO","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NO"}'::jsonb, 1, 'Paragraph D: reefs alone cannot solve coastal pollution.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000013', '2d000000-0000-4000-8000-030000000013', 1, 'The writer states that oysters are more profitable than other shellfish.', '[{"optionKey":"YES","content":"YES","sortOrder":1},{"optionKey":"NO","content":"NO","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'Profit is not discussed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000014', '2d000000-0000-4000-8000-030000000014', 1, 'Old ______ from restaurants are used to start new reefs.', NULL, '{"type":"FILL","accepted":["shells","shell"]}'::jsonb, 1, 'Paragraph B: volunteers collect discarded shells from restaurants.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000015', '2d000000-0000-4000-8000-030000000015', 1, 'After five years, the water above one restored reef was visibly ______.', NULL, '{"type":"FILL","accepted":["clearer"]}'::jsonb, 1, 'Paragraph C: the water above it was visibly clearer.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000016', '2d000000-0000-4000-8000-030000000016', 1, 'A volunteer walks at the back of each group.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Paragraph A: another volunteer follows at the back.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000017', '2d000000-0000-4000-8000-030000000017', 1, 'Walking buses follow a different route every day.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Paragraph A: a fixed route.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000018', '2d000000-0000-4000-8000-030000000018', 1, 'Most pupils prefer walking to cycling.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'Cycling is not mentioned.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000019', '2d000000-0000-4000-8000-030000000019', 1, 'What problem did head teachers first notice?', '[{"optionKey":"A","content":"Queues of cars outside the gates","sortOrder":1},{"optionKey":"B","content":"Too few teachers","sortOrder":2},{"optionKey":"C","content":"Late buses","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph B: parents driving short distances caused queues.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000020', '2d000000-0000-4000-8000-030000000020', 1, 'What do organisers do about missing volunteers?', '[{"optionKey":"A","content":"Cancel the route","sortOrder":1},{"optionKey":"B","content":"Keep a reserve list of helpers","sortOrder":2},{"optionKey":"C","content":"Ask pupils to walk alone","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph D: a reserve list of helpers.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000021', '2d000000-0000-4000-8000-030000000021', 1, 'a doubt that some parents have', '[{"optionKey":"A","content":"Paragraph A","sortOrder":1},{"optionKey":"B","content":"Paragraph B","sortOrder":2},{"optionKey":"C","content":"Paragraph C","sortOrder":3},{"optionKey":"D","content":"Paragraph D","sortOrder":4}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'Paragraph D: worries about weather and reliance on volunteers.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000022', '2d000000-0000-4000-8000-030000000022', 1, 'A starter can stay alive for many years.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"TRUE"}'::jsonb, 1, 'Paragraph A: a healthy one can be kept going for decades.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000023', '2d000000-0000-4000-8000-030000000023', 1, 'Yeasts are responsible for the sharp taste of the bread.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"FALSE"}'::jsonb, 1, 'Paragraph B: bacteria create the acids that give the sharp taste.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000024', '2d000000-0000-4000-8000-030000000024', 1, 'Sourdough is healthier than bread made with packaged yeast.', '[{"optionKey":"TRUE","content":"TRUE","sortOrder":1},{"optionKey":"FALSE","content":"FALSE","sortOrder":2},{"optionKey":"NOT_GIVEN","content":"NOT GIVEN","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"NOT_GIVEN"}'::jsonb, 1, 'Health is not discussed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000025', '2d000000-0000-4000-8000-030000000025', 1, 'In a ______ room, fermentation is slower and the flavour is milder.', NULL, '{"type":"FILL","accepted":["cool"]}'::jsonb, 1, 'Paragraph C: in a cool room fermentation is slower.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000026', '2d000000-0000-4000-8000-030000000026', 1, 'The mix of microbes depended mainly on how often the baker ______ the starter.', NULL, '{"type":"FILL","accepted":["fed","feeds"]}'::jsonb, 1, 'Paragraph D: the baker''s habits, such as how often the starter was fed.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000027', '2d000000-0000-4000-8000-030000000027', 1, 'What does a wide ring show?', '[{"optionKey":"A","content":"A dry year","sortOrder":1},{"optionKey":"B","content":"A wet year","sortOrder":2},{"optionKey":"C","content":"A cold winter","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"B"}'::jsonb, 1, 'Paragraph A: in a wet year the ring is wide.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000028', '2d000000-0000-4000-8000-030000000028', 1, 'What can a beam from an old house sometimes reveal?', '[{"optionKey":"A","content":"The year its tree was cut","sortOrder":1},{"optionKey":"B","content":"The builder''s name","sortOrder":2},{"optionKey":"C","content":"The cost of the house","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph B: it can be dated to the year its tree was cut.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000029', '2d000000-0000-4000-8000-030000000029', 1, 'Which trees give the least information about climate?', '[{"optionKey":"A","content":"Trees where rainfall is steady","sortOrder":1},{"optionKey":"B","content":"Trees at the edge of a desert","sortOrder":2},{"optionKey":"C","content":"Trees in old houses","sortOrder":3}]'::jsonb, '{"type":"CHOICE","correct":"A"}'::jsonb, 1, 'Paragraph C: steady rainfall means little variation between rings.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000030', '2d000000-0000-4000-8000-030000000030', 1, 'an example of ring records helping to explain a historical event', '[{"optionKey":"A","content":"Paragraph A","sortOrder":1},{"optionKey":"B","content":"Paragraph B","sortOrder":2},{"optionKey":"C","content":"Paragraph C","sortOrder":3},{"optionKey":"D","content":"Paragraph D","sortOrder":4}]'::jsonb, '{"type":"CHOICE","correct":"D"}'::jsonb, 1, 'Paragraph D: narrow rings match a drought in accounts of famine.', 'PUBLISHED'),
    ('2d000000-0000-4000-8000-040000000031', '2d000000-0000-4000-8000-030000000031', 1, 'a reason why the method cannot be used everywhere', '[{"optionKey":"A","content":"Paragraph A","sortOrder":1},{"optionKey":"B","content":"Paragraph B","sortOrder":2},{"optionKey":"C","content":"Paragraph C","sortOrder":3},{"optionKey":"D","content":"Paragraph D","sortOrder":4}]'::jsonb, '{"type":"CHOICE","correct":"C"}'::jsonb, 1, 'Paragraph C: the method has limits where rainfall is steady.', 'PUBLISHED')
ON CONFLICT DO NOTHING;

UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000001' WHERE id = '2d000000-0000-4000-8000-030000000001';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000002' WHERE id = '2d000000-0000-4000-8000-030000000002';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000003' WHERE id = '2d000000-0000-4000-8000-030000000003';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000004' WHERE id = '2d000000-0000-4000-8000-030000000004';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000005' WHERE id = '2d000000-0000-4000-8000-030000000005';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000006' WHERE id = '2d000000-0000-4000-8000-030000000006';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000007' WHERE id = '2d000000-0000-4000-8000-030000000007';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000008' WHERE id = '2d000000-0000-4000-8000-030000000008';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000009' WHERE id = '2d000000-0000-4000-8000-030000000009';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000010' WHERE id = '2d000000-0000-4000-8000-030000000010';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000011' WHERE id = '2d000000-0000-4000-8000-030000000011';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000012' WHERE id = '2d000000-0000-4000-8000-030000000012';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000013' WHERE id = '2d000000-0000-4000-8000-030000000013';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000014' WHERE id = '2d000000-0000-4000-8000-030000000014';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000015' WHERE id = '2d000000-0000-4000-8000-030000000015';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000016' WHERE id = '2d000000-0000-4000-8000-030000000016';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000017' WHERE id = '2d000000-0000-4000-8000-030000000017';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000018' WHERE id = '2d000000-0000-4000-8000-030000000018';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000019' WHERE id = '2d000000-0000-4000-8000-030000000019';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000020' WHERE id = '2d000000-0000-4000-8000-030000000020';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000021' WHERE id = '2d000000-0000-4000-8000-030000000021';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000022' WHERE id = '2d000000-0000-4000-8000-030000000022';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000023' WHERE id = '2d000000-0000-4000-8000-030000000023';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000024' WHERE id = '2d000000-0000-4000-8000-030000000024';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000025' WHERE id = '2d000000-0000-4000-8000-030000000025';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000026' WHERE id = '2d000000-0000-4000-8000-030000000026';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000027' WHERE id = '2d000000-0000-4000-8000-030000000027';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000028' WHERE id = '2d000000-0000-4000-8000-030000000028';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000029' WHERE id = '2d000000-0000-4000-8000-030000000029';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000030' WHERE id = '2d000000-0000-4000-8000-030000000030';
UPDATE questions SET current_published_version_id = '2d000000-0000-4000-8000-040000000031' WHERE id = '2d000000-0000-4000-8000-030000000031';

INSERT INTO content_packages (id, code, title, package_type, status, required_feature_key, source_code) VALUES
    ('2d000000-0000-4000-8000-050000000001', 'PT-READING-ISP-01', 'Reading Practice Test 1', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'IELTS_SPACE_PRO'),
    ('2d000000-0000-4000-8000-050000000002', 'PT-READING-C10-01', 'Reading Practice Test 2', 'PRACTICE_TEST', 'PUBLISHED', NULL, 'C10_C20'),
    ('2d000000-0000-4000-8000-050000000003', 'PT-READING-ACT-01', 'Reading Practice Test 3', 'PRACTICE_TEST', 'PUBLISHED', 'PREMIUM_CONTENT', 'ACTUAL_TESTS')
ON CONFLICT DO NOTHING;

INSERT INTO content_package_versions (id, package_id, version_number, status, rules, schema_version, published_at) VALUES
    ('2d000000-0000-4000-8000-060000000001', '2d000000-0000-4000-8000-050000000001', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000000002', '2d000000-0000-4000-8000-050000000002', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP),
    ('2d000000-0000-4000-8000-060000000003', '2d000000-0000-4000-8000-050000000003', 1, 'PUBLISHED', '{}', 1, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000000001' WHERE id = '2d000000-0000-4000-8000-050000000001';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000000002' WHERE id = '2d000000-0000-4000-8000-050000000002';
UPDATE content_packages SET current_published_version_id = '2d000000-0000-4000-8000-060000000003' WHERE id = '2d000000-0000-4000-8000-050000000003';

INSERT INTO content_sections (id, package_version_id, title, skill, sort_order, time_limit_seconds, instructions, part_no, cover_asset_id) VALUES
    ('2d000000-0000-4000-8000-070000000001', '2d000000-0000-4000-8000-060000000001', 'Passage 1: The urban beekeeper', 'READING', 1, 1200, 'You should spend about 20 minutes on Questions 1-5, which are based on the reading passage below.', 1, '2d000000-0000-4000-8000-010000000001'),
    ('2d000000-0000-4000-8000-070000000002', '2d000000-0000-4000-8000-060000000001', 'Passage 2: Measuring city noise', 'READING', 2, 1200, 'You should spend about 20 minutes on Questions 1-5, which are based on the reading passage below.', 2, '2d000000-0000-4000-8000-010000000002'),
    ('2d000000-0000-4000-8000-070000000003', '2d000000-0000-4000-8000-060000000001', 'Passage 3: Bringing back native oysters', 'READING', 3, 1200, 'You should spend about 20 minutes on Questions 1-5, which are based on the reading passage below.', 3, '2d000000-0000-4000-8000-010000000003'),
    ('2d000000-0000-4000-8000-070000000004', '2d000000-0000-4000-8000-060000000002', 'Passage 1: Walking school buses', 'READING', 1, 1200, 'You should spend about 20 minutes on Questions 1-6, which are based on the reading passage below.', 1, '2d000000-0000-4000-8000-010000000004'),
    ('2d000000-0000-4000-8000-070000000005', '2d000000-0000-4000-8000-060000000003', 'Passage 1: The science of sourdough', 'READING', 1, 1200, 'You should spend about 20 minutes on Questions 1-5, which are based on the reading passage below.', 1, '2d000000-0000-4000-8000-010000000005'),
    ('2d000000-0000-4000-8000-070000000006', '2d000000-0000-4000-8000-060000000003', 'Passage 2: Reading the rings of old trees', 'READING', 2, 1200, 'You should spend about 20 minutes on Questions 1-5, which are based on the reading passage below.', 2, '2d000000-0000-4000-8000-010000000006')
ON CONFLICT DO NOTHING;

INSERT INTO section_question_groups (id, section_id, sort_order, format_code, instructions) VALUES
    ('2d000000-0000-4000-8000-090000000001', '2d000000-0000-4000-8000-070000000001', 1, 'TRUE_FALSE_NOT_GIVEN', 'Questions 1-3. Do the following statements agree with the information given in the passage? Choose TRUE if the statement agrees with the information, FALSE if it contradicts the information, or NOT GIVEN if there is no information on this.'),
    ('2d000000-0000-4000-8000-090000000002', '2d000000-0000-4000-8000-070000000001', 2, 'GAP_FILLING', 'Questions 4-5. Complete the sentences. Write NO MORE THAN TWO WORDS from the passage for each answer.'),
    ('2d000000-0000-4000-8000-090000000003', '2d000000-0000-4000-8000-070000000002', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 1-3. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000000004', '2d000000-0000-4000-8000-070000000002', 2, 'MATCHING_INFORMATION', 'Questions 4-5. The passage has four paragraphs, A-D. Which paragraph contains the following information? Write the correct letter, A-D.'),
    ('2d000000-0000-4000-8000-090000000005', '2d000000-0000-4000-8000-070000000003', 1, 'YES_NO_NOT_GIVEN', 'Questions 1-3. Do the following statements agree with the views of the writer? Choose YES if the statement agrees with the writer, NO if it contradicts the writer, or NOT GIVEN if it is impossible to say what the writer thinks.'),
    ('2d000000-0000-4000-8000-090000000006', '2d000000-0000-4000-8000-070000000003', 2, 'SENTENCE_COMPLETION', 'Questions 4-5. Complete the sentences. Write NO MORE THAN TWO WORDS from the passage for each answer.'),
    ('2d000000-0000-4000-8000-090000000007', '2d000000-0000-4000-8000-070000000004', 1, 'TRUE_FALSE_NOT_GIVEN', 'Questions 1-3. Do the following statements agree with the information given in the passage? Choose TRUE if the statement agrees with the information, FALSE if it contradicts the information, or NOT GIVEN if there is no information on this.'),
    ('2d000000-0000-4000-8000-090000000008', '2d000000-0000-4000-8000-070000000004', 2, 'MULTIPLE_CHOICE_ONE', 'Questions 4-5. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000000009', '2d000000-0000-4000-8000-070000000004', 3, 'MATCHING_INFORMATION', 'Questions 6-6. The passage has four paragraphs, A-D. Which paragraph contains the following information? Write the correct letter, A-D.'),
    ('2d000000-0000-4000-8000-090000000010', '2d000000-0000-4000-8000-070000000005', 1, 'TRUE_FALSE_NOT_GIVEN', 'Questions 1-3. Do the following statements agree with the information given in the passage? Choose TRUE if the statement agrees with the information, FALSE if it contradicts the information, or NOT GIVEN if there is no information on this.'),
    ('2d000000-0000-4000-8000-090000000011', '2d000000-0000-4000-8000-070000000005', 2, 'GAP_FILLING', 'Questions 4-5. Complete the sentences. Write NO MORE THAN TWO WORDS from the passage for each answer.'),
    ('2d000000-0000-4000-8000-090000000012', '2d000000-0000-4000-8000-070000000006', 1, 'MULTIPLE_CHOICE_ONE', 'Questions 1-3. Choose the correct letter, A, B or C.'),
    ('2d000000-0000-4000-8000-090000000013', '2d000000-0000-4000-8000-070000000006', 2, 'MATCHING_INFORMATION', 'Questions 4-5. The passage has four paragraphs, A-D. Which paragraph contains the following information? Write the correct letter, A-D.')
ON CONFLICT DO NOTHING;

INSERT INTO section_questions (id, section_id, question_version_id, sort_order, max_score, group_id) VALUES
    ('2d000000-0000-4000-8000-080000000001', '2d000000-0000-4000-8000-070000000001', '2d000000-0000-4000-8000-040000000001', 1, 1.0, '2d000000-0000-4000-8000-090000000001'),
    ('2d000000-0000-4000-8000-080000000002', '2d000000-0000-4000-8000-070000000001', '2d000000-0000-4000-8000-040000000002', 2, 1.0, '2d000000-0000-4000-8000-090000000001'),
    ('2d000000-0000-4000-8000-080000000003', '2d000000-0000-4000-8000-070000000001', '2d000000-0000-4000-8000-040000000003', 3, 1.0, '2d000000-0000-4000-8000-090000000001'),
    ('2d000000-0000-4000-8000-080000000004', '2d000000-0000-4000-8000-070000000001', '2d000000-0000-4000-8000-040000000004', 4, 1.0, '2d000000-0000-4000-8000-090000000002'),
    ('2d000000-0000-4000-8000-080000000005', '2d000000-0000-4000-8000-070000000001', '2d000000-0000-4000-8000-040000000005', 5, 1.0, '2d000000-0000-4000-8000-090000000002'),
    ('2d000000-0000-4000-8000-080000000006', '2d000000-0000-4000-8000-070000000002', '2d000000-0000-4000-8000-040000000006', 1, 1.0, '2d000000-0000-4000-8000-090000000003'),
    ('2d000000-0000-4000-8000-080000000007', '2d000000-0000-4000-8000-070000000002', '2d000000-0000-4000-8000-040000000007', 2, 1.0, '2d000000-0000-4000-8000-090000000003'),
    ('2d000000-0000-4000-8000-080000000008', '2d000000-0000-4000-8000-070000000002', '2d000000-0000-4000-8000-040000000008', 3, 1.0, '2d000000-0000-4000-8000-090000000003'),
    ('2d000000-0000-4000-8000-080000000009', '2d000000-0000-4000-8000-070000000002', '2d000000-0000-4000-8000-040000000009', 4, 1.0, '2d000000-0000-4000-8000-090000000004'),
    ('2d000000-0000-4000-8000-080000000010', '2d000000-0000-4000-8000-070000000002', '2d000000-0000-4000-8000-040000000010', 5, 1.0, '2d000000-0000-4000-8000-090000000004'),
    ('2d000000-0000-4000-8000-080000000011', '2d000000-0000-4000-8000-070000000003', '2d000000-0000-4000-8000-040000000011', 1, 1.0, '2d000000-0000-4000-8000-090000000005'),
    ('2d000000-0000-4000-8000-080000000012', '2d000000-0000-4000-8000-070000000003', '2d000000-0000-4000-8000-040000000012', 2, 1.0, '2d000000-0000-4000-8000-090000000005'),
    ('2d000000-0000-4000-8000-080000000013', '2d000000-0000-4000-8000-070000000003', '2d000000-0000-4000-8000-040000000013', 3, 1.0, '2d000000-0000-4000-8000-090000000005'),
    ('2d000000-0000-4000-8000-080000000014', '2d000000-0000-4000-8000-070000000003', '2d000000-0000-4000-8000-040000000014', 4, 1.0, '2d000000-0000-4000-8000-090000000006'),
    ('2d000000-0000-4000-8000-080000000015', '2d000000-0000-4000-8000-070000000003', '2d000000-0000-4000-8000-040000000015', 5, 1.0, '2d000000-0000-4000-8000-090000000006'),
    ('2d000000-0000-4000-8000-080000000016', '2d000000-0000-4000-8000-070000000004', '2d000000-0000-4000-8000-040000000016', 1, 1.0, '2d000000-0000-4000-8000-090000000007'),
    ('2d000000-0000-4000-8000-080000000017', '2d000000-0000-4000-8000-070000000004', '2d000000-0000-4000-8000-040000000017', 2, 1.0, '2d000000-0000-4000-8000-090000000007'),
    ('2d000000-0000-4000-8000-080000000018', '2d000000-0000-4000-8000-070000000004', '2d000000-0000-4000-8000-040000000018', 3, 1.0, '2d000000-0000-4000-8000-090000000007'),
    ('2d000000-0000-4000-8000-080000000019', '2d000000-0000-4000-8000-070000000004', '2d000000-0000-4000-8000-040000000019', 4, 1.0, '2d000000-0000-4000-8000-090000000008'),
    ('2d000000-0000-4000-8000-080000000020', '2d000000-0000-4000-8000-070000000004', '2d000000-0000-4000-8000-040000000020', 5, 1.0, '2d000000-0000-4000-8000-090000000008'),
    ('2d000000-0000-4000-8000-080000000021', '2d000000-0000-4000-8000-070000000004', '2d000000-0000-4000-8000-040000000021', 6, 1.0, '2d000000-0000-4000-8000-090000000009'),
    ('2d000000-0000-4000-8000-080000000022', '2d000000-0000-4000-8000-070000000005', '2d000000-0000-4000-8000-040000000022', 1, 1.0, '2d000000-0000-4000-8000-090000000010'),
    ('2d000000-0000-4000-8000-080000000023', '2d000000-0000-4000-8000-070000000005', '2d000000-0000-4000-8000-040000000023', 2, 1.0, '2d000000-0000-4000-8000-090000000010'),
    ('2d000000-0000-4000-8000-080000000024', '2d000000-0000-4000-8000-070000000005', '2d000000-0000-4000-8000-040000000024', 3, 1.0, '2d000000-0000-4000-8000-090000000010'),
    ('2d000000-0000-4000-8000-080000000025', '2d000000-0000-4000-8000-070000000005', '2d000000-0000-4000-8000-040000000025', 4, 1.0, '2d000000-0000-4000-8000-090000000011'),
    ('2d000000-0000-4000-8000-080000000026', '2d000000-0000-4000-8000-070000000005', '2d000000-0000-4000-8000-040000000026', 5, 1.0, '2d000000-0000-4000-8000-090000000011'),
    ('2d000000-0000-4000-8000-080000000027', '2d000000-0000-4000-8000-070000000006', '2d000000-0000-4000-8000-040000000027', 1, 1.0, '2d000000-0000-4000-8000-090000000012'),
    ('2d000000-0000-4000-8000-080000000028', '2d000000-0000-4000-8000-070000000006', '2d000000-0000-4000-8000-040000000028', 2, 1.0, '2d000000-0000-4000-8000-090000000012'),
    ('2d000000-0000-4000-8000-080000000029', '2d000000-0000-4000-8000-070000000006', '2d000000-0000-4000-8000-040000000029', 3, 1.0, '2d000000-0000-4000-8000-090000000012'),
    ('2d000000-0000-4000-8000-080000000030', '2d000000-0000-4000-8000-070000000006', '2d000000-0000-4000-8000-040000000030', 4, 1.0, '2d000000-0000-4000-8000-090000000013'),
    ('2d000000-0000-4000-8000-080000000031', '2d000000-0000-4000-8000-070000000006', '2d000000-0000-4000-8000-040000000031', 5, 1.0, '2d000000-0000-4000-8000-090000000013')
ON CONFLICT DO NOTHING;

INSERT INTO content_asset_links (id, asset_id, section_id, sort_order) VALUES
    ('2d000000-0000-4000-8000-0a0000000001', '2d000000-0000-4000-8000-020000000001', '2d000000-0000-4000-8000-070000000001', 0),
    ('2d000000-0000-4000-8000-0a0000000002', '2d000000-0000-4000-8000-020000000002', '2d000000-0000-4000-8000-070000000002', 0),
    ('2d000000-0000-4000-8000-0a0000000003', '2d000000-0000-4000-8000-020000000003', '2d000000-0000-4000-8000-070000000003', 0),
    ('2d000000-0000-4000-8000-0a0000000004', '2d000000-0000-4000-8000-020000000004', '2d000000-0000-4000-8000-070000000004', 0),
    ('2d000000-0000-4000-8000-0a0000000005', '2d000000-0000-4000-8000-020000000005', '2d000000-0000-4000-8000-070000000005', 0),
    ('2d000000-0000-4000-8000-0a0000000006', '2d000000-0000-4000-8000-020000000006', '2d000000-0000-4000-8000-070000000006', 0)
ON CONFLICT DO NOTHING;
