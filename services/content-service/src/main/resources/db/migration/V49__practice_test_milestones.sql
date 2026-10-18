CREATE TABLE course_practice_test_milestones (
    course_id UUID NOT NULL REFERENCES courses(id),
    package_id UUID NOT NULL UNIQUE REFERENCES content_packages(id),
    after_topic_sequence INTEGER NOT NULL CHECK (after_topic_sequence > 0),
    PRIMARY KEY (course_id, package_id)
);

-- Original short Reading/Writing practice tests. These are course checkpoints, not full IELTS mock exams.
-- Separate question IDs preserve the ownership of every existing lesson, review and test question.
DO $$
DECLARE
    test JSONB;
    item JSONB;
    course UUID;
    package UUID;
    version UUID;
    section UUID;
    question UUID;
    question_version UUID;
    asset UUID;
    kp UUID;
    position INTEGER;
    midpoint INTEGER;
BEGIN
    FOR test IN SELECT value FROM jsonb_array_elements($tests$[
      {"course":"IELTS_5_5","code":"COURSE55-PRACTICE-1","title":"Community projects: short Reading and Writing checkpoint",
       "difficulty":"EASY","band":5.5,
       "passage":"A. The fictional town of Fernwick opened a tool library in April. Residents could borrow drills and garden tools rather than buy equipment they used only occasionally. The council provided an unused room beside the railway station. Volunteers collected donated tools and checked that each item worked before putting it on a shelf.\n\nB. Membership costs twelve pounds per year, and a member can borrow two tools at a time. A loan normally lasts seven days. Large machines are excluded because the room has neither enough space nor a safe way to move them. An online catalogue lists the tools, but members collect them in person.\n\nC. At the end of the first month, 48 residents had joined. The volunteers did not interpret this figure as proof that the project had reduced household spending: they had not measured members' previous purchases. They planned a survey for the autumn. Their immediate priority was to recruit another volunteer so that the library could open on Saturday mornings.",
       "items":[
         {"stem":"Why are large machines excluded?","options":["They are too expensive to repair.","The room lacks space and a safe way to move them.","Residents never ask to borrow them."],"correct":"B","kp":"TR_R_SCANNING","explanation":"Paragraph B gives space and moving safety as the reasons; demand and repair cost are not stated."},
         {"stem":"Complete the sentence with ONE WORD from the passage: The volunteers planned a _____ for the autumn.","accepted":["survey"],"kp":"PR_SENTENCE_COMPLETION","explanation":"Paragraph C explicitly says they planned a survey for the autumn."},
         {"stem":"What is the main purpose of paragraph C?","options":["To describe initial membership and what still needs to be investigated.","To prove that borrowing tools saves every household money.","To explain how to repair the donated tools."],"correct":"A","kp":"DR_TOPIC_SENTENCE","explanation":"It reports membership, cautions against inferring savings, and identifies next steps."},
         {"stem":"Some people think local councils should fund facilities where residents can share equipment. Others believe residents should pay the full cost themselves. To what extent do you agree that councils should contribute? Give reasons and relevant examples. Write at least 250 words.","essay":true,"kp":"TR_W_OPINION","explanation":"State a clear position, develop reasons, address funding trade-offs and support your argument with relevant examples. There is no single correct opinion."}
       ]},
      {"course":"IELTS_6_5","code":"COURSE65-PRACTICE-1","title":"Public research: short Reading and Writing checkpoint",
       "difficulty":"MEDIUM","band":6.5,
       "passage":"A. A fictional university tested whether sending reminders would increase attendance at public science talks. During the spring term, people who booked a seat for one of six talks were randomly allocated either to receive a reminder the day before or to receive no further message. Both groups received the same booking confirmation.\n\nB. Attendance was higher among those sent reminders. However, the researchers stressed that the experiment concerned people who had already booked. It did not show whether messages could persuade people with no initial interest to attend. Nor did it establish how frequent reminders would affect participation over a whole year.\n\nC. Some organisers wanted to replace all printed publicity with electronic messages. The researchers considered that proposal premature: the trial had not compared methods of attracting new visitors. They recommended retaining printed publicity while repeating the reminder experiment in autumn, when the programme would include different subjects. Attendance records alone also gave no indication of how much visitors learned. A short voluntary questionnaire was therefore proposed, although the researchers acknowledged that the visitors who chose to respond might differ from those who did not.",
       "items":[
         {"stem":"Which conclusion is supported by the experiment?","options":["Reminders increased attendance among people who had booked.","Electronic publicity attracts more new visitors than printed publicity.","People learn more when reminded to attend."],"correct":"A","kp":"R65_EVIDENCE_INFERENCE","explanation":"The comparison concerns randomly allocated existing bookers; recruitment methods and learning were not measured."},
         {"stem":"Which phrase best preserves the meaning of 'considered that proposal premature'?","options":["Believed the change was already overdue.","Thought the change should happen immediately.","Judged that there was not yet enough evidence for the change."],"correct":"C","kp":"R65_PARAPHRASE","explanation":"The proposal is premature because the trial has not compared ways to attract new visitors."},
         {"stem":"Why might questionnaire results fail to represent all visitors?","options":["Only visitors receiving reminders can answer.","Respondents may differ from visitors who do not respond.","All visitors must complete the questionnaire."],"correct":"B","kp":"R65_EVIDENCE_INFERENCE","explanation":"Paragraph C explicitly acknowledges possible differences between volunteers and non-respondents."},
         {"stem":"Some people believe universities should spend more on public science events. Others think their resources should focus on teaching enrolled students. Discuss both views and give your own opinion. Give reasons and relevant examples. Write at least 250 words.","essay":true,"kp":"W65_DISCUSS_BOTH_VIEWS","explanation":"Develop both views, make your own position explicit and compare the benefits and opportunity costs. A reasoned position can support either funding choice."}
       ]}
    ]$tests$::jsonb) LOOP
        SELECT id INTO STRICT course FROM courses WHERE code=test->>'course';
        package := md5('practice-milestone-package:' || (test->>'code'))::uuid;
        version := md5('practice-milestone-version:' || (test->>'code'))::uuid;
        INSERT INTO content_packages(id,code,title,package_type,status,course_id)
        VALUES(package,test->>'code',test->>'title','PRACTICE_TEST','PUBLISHED',course);
        INSERT INTO content_package_versions(id,package_id,version_number,status,rules,published_at)
        VALUES(version,package,1,'PUBLISHED','{"shortCheckpoint":true,"skills":["READING","WRITING"]}',CURRENT_TIMESTAMP);
        UPDATE content_packages SET current_published_version_id=version WHERE id=package;
        FOR position IN 1..2 LOOP
            section := md5('practice-milestone-section:' || (test->>'code') || ':' || position)::uuid;
            INSERT INTO content_sections(id,package_version_id,title,skill,sort_order,time_limit_seconds,instructions)
            VALUES(section,version,CASE WHEN position=1 THEN 'Reading' ELSE 'Writing' END,
                CASE WHEN position=1 THEN 'READING' ELSE 'WRITING' END,position,
                CASE WHEN position=1 THEN 600 ELSE 2400 END,'Complete all questions in this short checkpoint.');
            IF position=1 THEN
                asset := md5('practice-milestone-passage:' || (test->>'code'))::uuid;
                INSERT INTO content_assets(id,asset_type,text_content,validation_status)
                VALUES(asset,'PASSAGE',test->>'passage','VALID');
                INSERT INTO content_asset_links(asset_id,section_id,sort_order) VALUES(asset,section,1);
            END IF;
        END LOOP;
        position := 0;
        FOR item IN SELECT value FROM jsonb_array_elements(test->'items') LOOP
            position := position+1;
            question := md5('practice-milestone-question:' || (test->>'code') || ':' || position)::uuid;
            question_version := md5('practice-milestone-question-version:' || (test->>'code') || ':' || position)::uuid;
            section := md5('practice-milestone-section:' || (test->>'code') || ':' || CASE WHEN position=4 THEN 2 ELSE 1 END)::uuid;
            INSERT INTO questions(id,question_type,skill,purpose,status)
            VALUES(question,CASE WHEN item ? 'essay' THEN 'ESSAY' WHEN item ? 'accepted' THEN 'FILL_IN_BLANK' ELSE 'MULTIPLE_CHOICE' END,
                CASE WHEN position=4 THEN 'WRITING' ELSE 'READING' END,'EXAM','PUBLISHED');
            INSERT INTO question_versions(id,question_id,version_number,stem,options,answer_spec,explanation,difficulty,status)
            VALUES(question_version,question,1,item->>'stem',
                (SELECT jsonb_agg(jsonb_build_object('optionKey',chr(64+o.n::integer),'content',o.value,'sortOrder',o.n))
                 FROM jsonb_array_elements_text(item->'options') WITH ORDINALITY o(value,n)),
                CASE WHEN item ? 'essay' THEN jsonb_build_object('type','ESSAY','task','TASK_2','minWords',250,'passBand',test->'band')
                     WHEN item ? 'accepted' THEN jsonb_build_object('type','FILL','accepted',item->'accepted')
                     ELSE jsonb_build_object('type','CHOICE','correct',item->>'correct') END,
                item->>'explanation',test->>'difficulty','PUBLISHED');
            UPDATE questions SET current_published_version_id=question_version WHERE id=question;
            INSERT INTO section_questions(section_id,question_version_id,sort_order,max_score)
            VALUES(section,question_version,CASE WHEN position=4 THEN 1 ELSE position END,1);
            SELECT id INTO STRICT kp FROM knowledge_points WHERE code=item->>'kp';
            INSERT INTO question_knowledge_points(question_version_id,knowledge_point_id,weight) VALUES(question_version,kp,1);
        END LOOP;
        SELECT GREATEST(1,CEIL(count(*)/2.0)::integer) INTO midpoint FROM topics t
        WHERE t.course_id=course AND t.status='ACTIVE'
          AND EXISTS (SELECT 1 FROM lessons l WHERE l.topic_id=t.id AND l.status='PUBLISHED');
        INSERT INTO course_practice_test_milestones(course_id,package_id,after_topic_sequence) VALUES(course,package,midpoint);
    END LOOP;
END;
$$;
