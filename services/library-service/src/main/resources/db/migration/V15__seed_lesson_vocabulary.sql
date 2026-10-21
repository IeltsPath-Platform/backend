-- Vocabulary senses for the DEMO_READING lessons L1-L4.
-- Stable UUIDs and normalized-lemma checks make the seed safe to reapply.

INSERT INTO vocabulary_items (id, lemma, normalized_lemma, ipa, pronunciation_audio_reference, status)
SELECT item.id::uuid, item.lemma, item.normalized_lemma, NULL, NULL, 'ACTIVE'
FROM (VALUES
    ('7b000000-0000-4000-8000-000000000001', 'city planner', 'city planner'),
    ('7b000000-0000-4000-8000-000000000002', 'cope', 'cope'),
    ('7b000000-0000-4000-8000-000000000003', 'hotter', 'hotter'),
    ('7b000000-0000-4000-8000-000000000004', 'green roof', 'green roof'),
    ('7b000000-0000-4000-8000-000000000005', 'layer', 'layer'),
    ('7b000000-0000-4000-8000-000000000006', 'soil', 'soil'),
    ('7b000000-0000-4000-8000-000000000007', 'plant', 'plant'),
    ('7b000000-0000-4000-8000-000000000008', 'waterproof membrane', 'waterproof membrane'),
    ('7b000000-0000-4000-8000-000000000009', 'conventional', 'conventional'),
    ('7b000000-0000-4000-8000-000000000010', 'black roof', 'black roof'),
    ('7b000000-0000-4000-8000-000000000011', 'reach', 'reach'),
    ('7b000000-0000-4000-8000-000000000012', 'planted roof', 'planted roof'),
    ('7b000000-0000-4000-8000-000000000013', 'rarely', 'rarely'),
    ('7b000000-0000-4000-8000-000000000014', 'nearby', 'nearby'),
    ('7b000000-0000-4000-8000-000000000015', 'air conditioning', 'air conditioning'),
    ('7b000000-0000-4000-8000-000000000016', 'manage rainwater', 'manage rainwater'),
    ('7b000000-0000-4000-8000-000000000017', 'soak up', 'soak up'),
    ('7b000000-0000-4000-8000-000000000018', 'heavy shower', 'heavy shower'),
    ('7b000000-0000-4000-8000-000000000019', 'release', 'release'),
    ('7b000000-0000-4000-8000-000000000020', 'slowly', 'slowly'),
    ('7b000000-0000-4000-8000-000000000021', 'pressure', 'pressure'),
    ('7b000000-0000-4000-8000-000000000022', 'city drain', 'city drain'),
    ('7b000000-0000-4000-8000-000000000023', 'Copenhagen', 'copenhagen'),
    ('7b000000-0000-4000-8000-000000000024', 'convinced', 'convinced'),
    ('7b000000-0000-4000-8000-000000000025', 'critic', 'critic'),
    ('7b000000-0000-4000-8000-000000000026', 'install', 'install'),
    ('7b000000-0000-4000-8000-000000000027', 'regular care', 'regular care'),
    ('7b000000-0000-4000-8000-000000000028', 'older building', 'older building'),
    ('7b000000-0000-4000-8000-000000000029', 'carry', 'carry'),
    ('7b000000-0000-4000-8000-000000000030', 'extra weight', 'extra weight')
) AS item(id, lemma, normalized_lemma)
WHERE NOT EXISTS (
    SELECT 1 FROM vocabulary_items existing WHERE existing.normalized_lemma = item.normalized_lemma
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabulary_senses (
    id, vocabulary_item_id, part_of_speech, english_definition, vietnamese_meaning,
    example_sentence, image_url, sort_order, status
)
SELECT sense.id::uuid, item.id, sense.part_of_speech, sense.english_definition,
       sense.vietnamese_meaning, sense.example_sentence, NULL, 0, 'ACTIVE'
FROM (VALUES
    ('7a000000-0000-4000-8000-000000000001', 'city planner', 'NOUN', 'A person who plans how towns and cities should develop.', 'nhà quy hoạch đô thị', 'City planners are turning to green roofs to cope with hotter summers.'),
    ('7a000000-0000-4000-8000-000000000002', 'cope', 'VERB', 'To deal successfully with a difficult situation.', 'đối phó, ứng phó', 'Cities are turning to green roofs to cope with hotter summers.'),
    ('7a000000-0000-4000-8000-000000000003', 'hotter', 'ADJECTIVE', 'More warm than something else or than before.', 'nóng hơn', 'Green roofs can help cities cope with hotter summers.'),
    ('7a000000-0000-4000-8000-000000000004', 'green roof', 'NOUN', 'A roof covered partly or completely with soil and plants.', 'mái xanh, mái nhà phủ cây', 'A green roof has a layer of soil and plants.'),
    ('7a000000-0000-4000-8000-000000000005', 'layer', 'NOUN', 'A quantity of material covering a surface or lying between surfaces.', 'lớp', 'A green roof is a layer of soil and plants.'),
    ('7a000000-0000-4000-8000-000000000006', 'soil', 'NOUN', 'The upper layer of earth in which plants grow.', 'đất trồng, đất', 'The plants grow in a layer of soil on the roof.'),
    ('7a000000-0000-4000-8000-000000000007', 'plant', 'NOUN', 'A living thing that grows in soil and usually has leaves or flowers.', 'cây, thực vật', 'Plants grow over a waterproof membrane on the roof.'),
    ('7a000000-0000-4000-8000-000000000008', 'waterproof membrane', 'NOUN', 'A thin layer that prevents water from passing through.', 'màng chống thấm', 'The soil and plants sit over a waterproof membrane.'),
    ('7a000000-0000-4000-8000-000000000009', 'benefit', 'NOUN', 'A helpful or positive effect.', 'lợi ích', 'The most important benefit of green roofs is that they keep buildings cool.'),
    ('7a000000-0000-4000-8000-000000000010', 'conventional', 'ADJECTIVE', 'Based on what is usual or traditional.', 'thông thường, truyền thống', 'A conventional black roof can become very hot on a summer afternoon.'),
    ('7a000000-0000-4000-8000-000000000011', 'black roof', 'NOUN', 'A roof with a black outer surface.', 'mái nhà màu đen', 'A conventional black roof can reach 80°C.'),
    ('7a000000-0000-4000-8000-000000000012', 'reach', 'VERB', 'To rise to or arrive at a particular level.', 'đạt tới', 'A conventional black roof can reach 80°C.'),
    ('7a000000-0000-4000-8000-000000000013', 'planted roof', 'NOUN', 'A roof that has plants growing on it.', 'mái nhà trồng cây', 'A planted roof nearby rarely rises above 30°C.'),
    ('7a000000-0000-4000-8000-000000000014', 'rarely', 'ADVERB', 'Not often.', 'hiếm khi', 'A planted roof nearby rarely rises above 30°C.'),
    ('7a000000-0000-4000-8000-000000000015', 'nearby', 'ADVERB', 'In a place close to the one being discussed.', 'ở gần đó', 'A planted roof nearby stays cooler than a conventional roof.'),
    ('7a000000-0000-4000-8000-000000000016', 'air conditioning', 'NOUN', 'A system that cools and controls the air inside a building.', 'hệ thống điều hòa không khí', 'Cooler roofs mean the floors below need far less air conditioning.'),
    ('7a000000-0000-4000-8000-000000000017', 'manage rainwater', 'VERB', 'To control how rainwater is collected, absorbed, or released.', 'điều tiết nước mưa', 'Green roofs also help cities manage rainwater.'),
    ('7a000000-0000-4000-8000-000000000018', 'soak up', 'PHRASAL_VERB', 'To absorb a liquid.', 'thấm hút', 'The soil soaks up much of a heavy shower.'),
    ('7a000000-0000-4000-8000-000000000019', 'heavy shower', 'NOUN', 'A short period of heavy rain.', 'trận mưa rào lớn', 'The soil soaks up much of a heavy shower.'),
    ('7a000000-0000-4000-8000-000000000020', 'release', 'VERB', 'To allow something to flow or move out.', 'giải phóng, xả ra', 'The soil releases the water slowly.'),
    ('7a000000-0000-4000-8000-000000000021', 'slowly', 'ADVERB', 'At a low speed or over a long period of time.', 'chậm, từ từ', 'The soil releases rainwater slowly.'),
    ('7a000000-0000-4000-8000-000000000022', 'pressure', 'NOUN', 'A force or demand placed on something.', 'áp lực', 'Absorbing rain takes pressure off city drains.'),
    ('7a000000-0000-4000-8000-000000000023', 'city drain', 'NOUN', 'A pipe or channel that carries wastewater or rainwater away from a city.', 'cống thoát nước đô thị', 'Green roofs take pressure off city drains.'),
    ('7a000000-0000-4000-8000-000000000024', 'Copenhagen', 'NOUN', 'The capital city of Denmark.', 'Copenhagen, thủ đô Đan Mạch', 'In Copenhagen, new flat roofs must be planted for this reason.'),
    ('7a000000-0000-4000-8000-000000000025', 'convinced', 'ADJECTIVE', 'Certain that something is true or right.', 'tin chắc, bị thuyết phục', 'Not everyone is convinced that green roofs are a good idea.'),
    ('7a000000-0000-4000-8000-000000000026', 'critic', 'NOUN', 'A person who expresses disapproval of someone or something.', 'người chỉ trích', 'Critics point out that green roofs can be expensive to install.'),
    ('7a000000-0000-4000-8000-000000000027', 'install', 'VERB', 'To put equipment or a system in place so it can be used.', 'lắp đặt', 'Green roofs can be expensive to install.'),
    ('7a000000-0000-4000-8000-000000000028', 'regular care', 'NOUN', 'Attention or work done repeatedly to keep something in good condition.', 'sự chăm sóc thường xuyên', 'Green roofs need regular care.'),
    ('7a000000-0000-4000-8000-000000000029', 'older building', 'NOUN', 'A building that was built a long time ago.', 'tòa nhà cũ', 'Many older buildings are not strong enough to carry the extra weight.'),
    ('7a000000-0000-4000-8000-000000000030', 'carry', 'VERB', 'To support or hold the weight of something.', 'chịu, đỡ', 'Some older buildings cannot carry the extra weight.'),
    ('7a000000-0000-4000-8000-000000000031', 'extra weight', 'NOUN', 'An additional amount of weight.', 'trọng lượng tăng thêm', 'Older buildings may not be strong enough to carry the extra weight.'),
    ('7a000000-0000-4000-8000-000000000032', 'drawback', 'NOUN', 'A disadvantage or negative feature.', 'nhược điểm', 'The cost and maintenance of green roofs are possible drawbacks.')
) AS sense(id, normalized_lemma, part_of_speech, english_definition, vietnamese_meaning, example_sentence)
JOIN LATERAL (
    SELECT vocabulary_item.id
    FROM vocabulary_items vocabulary_item
    WHERE vocabulary_item.normalized_lemma = sense.normalized_lemma
    ORDER BY vocabulary_item.id
    LIMIT 1
) item ON TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM vocabulary_senses existing WHERE existing.id = sense.id::uuid
)
ON CONFLICT (id) DO NOTHING;
