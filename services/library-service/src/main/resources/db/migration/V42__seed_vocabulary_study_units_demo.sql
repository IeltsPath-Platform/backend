-- Demo curriculum uses existing catalog senses; requires content-author review before production.
-- Eight words produce eight CHOICE, two MATCH groups, and eight FILL steps.
DO $$
DECLARE
    lesson RECORD;
    word RECORD;
    prepared_lesson JSONB;
    prepared JSONB;
    topic UUID;
    topic_slug TEXT;
    base_order INT;
    word_index INT;
    sense_ids UUID[];
    meanings TEXT[];
    meaning TEXT;
    valid_topic BOOLEAN;
    seed_author CONSTANT UUID := '00000000-0000-4000-8000-000000000001';
BEGIN
    FOREACH topic_slug IN ARRAY ARRAY['education','environment']
    LOOP
        SELECT t.id INTO topic FROM vocabulary_topics t WHERE t.slug=topic_slug AND t.status='ACTIVE';
        IF NOT FOUND THEN
            RAISE NOTICE 'Skipping vocabulary demo topic %: topic is unavailable',topic_slug;
            CONTINUE;
        END IF;
        prepared:='[]'::jsonb;
        valid_topic:=TRUE;
        FOR lesson IN SELECT * FROM (VALUES
                ('d9000000-0000-4000-8000-000000000101'::uuid,'education','Đồ dùng và không gian lớp học','LEARN',1,ARRAY['school','classroom','book','notebook','pen','pencil','dictionary','library']),
                ('d9000000-0000-4000-8000-000000000102'::uuid,'education','Bài học, bài tập và kiểm tra','LEARN',2,ARRAY['lesson','homework','assignment','question','answer','test','exam','grade']),
                ('d9000000-0000-4000-8000-000000000103'::uuid,'education','Học đại học và tài chính','LEARN',3,ARRAY['university','campus','degree','diploma','tuition','scholarship','semester','professor']),
                ('d9000000-0000-4000-8000-000000000104'::uuid,'education','Ôn tập lớp học và đại học','REVIEW',4,ARRAY['book','notebook','homework','assignment','university','campus','tuition','scholarship']),
                ('d9000000-0000-4000-8000-000000000201'::uuid,'environment','Thời tiết và khí hậu','LEARN',1,ARRAY['weather','climate','temperature','rain','wind','cloud','storm','snow']),
                ('d9000000-0000-4000-8000-000000000202'::uuid,'environment','Thiên nhiên và môi trường sống','LEARN',2,ARRAY['forest','river','ocean','coast','habitat','species','biodiversity','ecosystem']),
                ('d9000000-0000-4000-8000-000000000203'::uuid,'environment','Ô nhiễm và giải pháp bền vững','LEARN',3,ARRAY['pollution','waste','recycle','renewable','solar','conservation','protect','sustainable']),
                ('d9000000-0000-4000-8000-000000000204'::uuid,'environment','Ôn tập khí hậu và bảo vệ thiên nhiên','REVIEW',4,ARRAY['weather','climate','forest','habitat','pollution','recycle','renewable','sustainable'])
            ) AS lessons(id,slug,title,kind,sort_order,lemmas) WHERE slug=topic_slug
        LOOP
            sense_ids:=ARRAY[]::uuid[];meanings:=ARRAY[]::text[];
            FOREACH meaning IN ARRAY lesson.lemmas
            LOOP
                SELECT s.id,s.vietnamese_meaning,s.example_sentence,i.lemma INTO word
                  FROM vocabulary_senses s JOIN vocabulary_items i ON i.id=s.vocabulary_item_id
                  WHERE s.topic_id=topic AND s.status='ACTIVE' AND i.status='ACTIVE' AND i.normalized_lemma=meaning
                  ORDER BY s.sort_order,s.id LIMIT 1;
                IF NOT FOUND OR NULLIF(btrim(word.vietnamese_meaning),'') IS NULL
                    OR word.example_sentence IS NULL
                    OR word.example_sentence !~* ('(^|[^[:alnum:]])'||meaning||'($|[^[:alnum:]])') THEN
                    valid_topic:=FALSE;EXIT;
                END IF;
                meaning:=lower(btrim(regexp_replace(normalize(word.vietnamese_meaning,NFC),'[[:space:]  ]+',' ','g')));
                IF meaning='' OR meaning=ANY(meanings) THEN valid_topic:=FALSE;EXIT;END IF;
                meanings:=array_append(meanings,meaning);sense_ids:=array_append(sense_ids,word.id);
            END LOOP;
            IF NOT valid_topic THEN EXIT;END IF;
            prepared:=prepared||jsonb_build_array(jsonb_build_object('id',lesson.id,'title',lesson.title,
                'kind',lesson.kind,'sortOrder',lesson.sort_order,'senses',to_jsonb(sense_ids)));
        END LOOP;
        -- Existing catalog edits are authoritative; a demo migration must never reactivate them or block startup.
        IF NOT valid_topic THEN
            RAISE NOTICE 'Skipping vocabulary demo topic %: curated content is unavailable or invalid',topic_slug;
            CONTINUE;
        END IF;
        SELECT coalesce(max(sort_order),0) INTO base_order FROM vocabulary_study_units WHERE topic_id=topic;
        FOR prepared_lesson IN SELECT value FROM jsonb_array_elements(prepared)
        LOOP
            INSERT INTO vocabulary_study_units(id,topic_id,sort_order,kind,published_revision)
              VALUES((prepared_lesson->>'id')::uuid,topic,base_order+(prepared_lesson->>'sortOrder')::int,prepared_lesson->>'kind',1);
            INSERT INTO vocabulary_study_unit_revisions(unit_id,revision,title,introduction_vi,kind,
                exercise_types,graded_step_count,published_at,created_by)
              VALUES((prepared_lesson->>'id')::uuid,1,prepared_lesson->>'title',
                CASE WHEN prepared_lesson->>'kind'='REVIEW' THEN 'Ôn lại các từ đã học qua ví dụ và bài tập.'
                     ELSE 'Học nghĩa, phát âm và cách dùng các từ trong nhóm này.' END,
                prepared_lesson->>'kind',ARRAY['CHOICE','MATCH','FILL'],18,CURRENT_TIMESTAMP,seed_author);
            word_index:=0;
            FOR meaning IN SELECT value FROM jsonb_array_elements_text(prepared_lesson->'senses')
            LOOP
                word_index:=word_index+1;
                INSERT INTO vocabulary_study_unit_words(unit_id,revision,sense_id,sort_order)
                  VALUES((prepared_lesson->>'id')::uuid,1,meaning::uuid,word_index);
            END LOOP;
        END LOOP;
        INSERT INTO vocabulary_study_topic_versions(topic_id,curriculum_version)
          VALUES(topic,1) ON CONFLICT(topic_id) DO UPDATE
            SET curriculum_version=vocabulary_study_topic_versions.curriculum_version+1,updated_at=CURRENT_TIMESTAMP;
    END LOOP;
END $$;

