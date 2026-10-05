"""Build the SRS entity summary from repository migrations, without running SQL."""
from pathlib import Path
import re, json, hashlib

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent

def split_sql(text):
    # Split only outside strings and parentheses; enough for repository DDL.
    result, start, depth, quoted, i = [], 0, 0, False, 0
    while i < len(text):
        c = text[i]
        if c == "'":
            if quoted and i + 1 < len(text) and text[i + 1] == "'":
                i += 2
                continue
            quoted = not quoted
        if not quoted:
            depth += (c == '(') - (c == ')')
            if c == ',' and depth == 0:
                result.append(text[start:i].strip()); start = i + 1
        i += 1
    result.append(text[start:].strip())
    return result

def parse_schema():
    schemas, sources = {}, []
    for folder in sorted((ROOT / 'services').glob('*/src/main/resources/db/migration')):
        service = folder.parts[-6].replace('-service','')
        tables = {}
        for path in sorted(folder.glob('V*.sql'), key=lambda p: int(re.match(r'V(\d+)',p.name)[1])):
            raw = path.read_text(encoding='utf-8-sig')
            sql = re.sub(r'--[^\n]*','',raw)
            sources.append({'path':path.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
            for m in re.finditer(r'CREATE TABLE\s+(\w+)\s*\(', sql, re.I):
                name, start = m[1], m.end()
                depth, i, quoted = 1, start, False
                while depth:
                    c=sql[i]
                    if c=="'":
                        if quoted and sql[i+1:i+2]=="'": i+=2; continue
                        quoted=not quoted
                    if not quoted: depth += (c=='(')-(c==')')
                    i+=1
                t={'name':name,'service':service,'cols':{},'pk':[],'unique':[],'fks':[],'source':path.relative_to(ROOT).as_posix()}
                for part in split_sql(sql[start:i-1]):
                    col=re.match(r'(\w+)\s+(UUID|VARCHAR|TEXT|INT\b|INTEGER|BIGINT|BOOLEAN|DATE\b|TIMESTAMP|TIMESTAMPTZ|NUMERIC|DOUBLE|JSONB)',part,re.I)
                    if col:
                        c=col[1]; t['cols'][c]={'nullable':not bool(re.search(r'NOT NULL|PRIMARY KEY',part,re.I))}
                        if re.search(r'PRIMARY KEY',part,re.I):t['pk'].append(c)
                        if re.search(r'\bUNIQUE\b',part,re.I):t['unique'].append([c])
                        fk=re.search(r'REFERENCES\s+(\w+)\s*\(([^)]+)\)',part,re.I)
                        if fk:t['fks'].append({'cols':[c],'target':fk[1],'target_cols':fk[2].replace(' ','').split(',')})
                    else:
                        pk=re.search(r'PRIMARY KEY\s*\(([^)]+)\)',part,re.I)
                        if pk:t['pk']=pk[1].replace(' ','').split(',')
                        uq=re.search(r'UNIQUE\s*\(([^)]+)\)',part,re.I)
                        if uq:t['unique'].append(uq[1].replace(' ','').split(','))
                        fk=re.search(r'FOREIGN KEY\s*\(([^)]+)\)\s*REFERENCES\s+(\w+)\s*\(([^)]+)\)',part,re.I)
                        if fk:t['fks'].append({'cols':fk[1].replace(' ','').split(','),'target':fk[2],'target_cols':fk[3].replace(' ','').split(',')})
                tables[name]=t
            for m in re.finditer(r'ALTER TABLE\s+(\w+)\s+(.+?);',sql,re.I|re.S):
                if m[1] not in tables:continue
                t=tables[m[1]]
                for part in split_sql(m[2]):
                    c=re.match(r'ADD COLUMN(?: IF NOT EXISTS)?\s+(\w+)\s+(.+)',part,re.I|re.S)
                    if c:
                        t['cols'][c[1]]={'nullable':not bool(re.search(r'NOT NULL',c[2],re.I))}
                        fk=re.search(r'REFERENCES\s+(\w+)\s*\(([^)]+)\)',c[2],re.I)
                        if fk:t['fks'].append({'cols':[c[1]],'target':fk[1],'target_cols':fk[2].replace(' ','').split(',')})
                    fk=re.search(r'FOREIGN KEY\s*\(([^)]+)\)\s*REFERENCES\s+(\w+)\s*\(([^)]+)\)',part,re.I)
                    if fk:t['fks'].append({'cols':fk[1].replace(' ','').split(','),'target':fk[2],'target_cols':fk[3].replace(' ','').split(',')})
                    drop=re.search(r'DROP COLUMN(?: IF EXISTS)?\s+(\w+)',part,re.I)
                    if drop:t['cols'].pop(drop[1],None)
            for m in re.finditer(r'DROP TABLE(?: IF EXISTS)?\s+(\w+)',sql,re.I):tables.pop(m[1],None)
        schemas[service]=tables
    return schemas,sources

# Display names and descriptions summarize stored business records, not new features.
DESCRIPTIONS = {
'users':('User','Represents an account that owns learning records or performs staff actions.'),
'roles':('Role','Defines a system access role assigned to users.'),
'learner_profiles':('Learner Profile','Stores the learning profile and visibility preferences of one user.'),
'learning_goals':('Learning Goal','Records a user\'s target band, study availability and goal lifecycle.'),
'learning_activities':('Learning Activity','Records a user\'s learning activity and its source reference.'),
'streaks':('Streak','Stores a user\'s current and longest qualified-day streak.'),
'plans':('Plan','Defines an access offering available through subscriptions.'),
'plan_features':('Plan Feature','Defines a feature entitlement and optional limit within a plan.'),
'subscriptions':('Subscription','Records a user\'s access to a plan for a period.'),
'key_products':('Key Product','Defines the points or plan benefits granted by an activation key.'),
'activation_keys':('Activation Key','Represents a redeemable code issued for a key product.'),
'key_activations':('Key Activation','Records the user and benefits of a key redemption.'),
'point_wallets':('Point Wallet','Stores a user\'s point balance and credited/debited totals.'),
'point_ledger_entries':('Point Ledger Entry','Records an individual point balance change and its source.'),
'topics':('Topic','Organizes curriculum content and ordered lessons, with an optional parent topic.'),
'knowledge_points':('Knowledge Point','Defines a unit of knowledge associated with a topic.'),
'lessons':('Lesson','Defines an ordered learning unit inside a topic.'),
'lesson_blocks':('Lesson Block','Defines an ordered text, asset, vocabulary or exercise block of a lesson.'),
'content_packages':('Content Package','Groups versioned content for practice, tests, quizzes or legacy lesson packages.'),
'content_package_versions':('Package Version','Preserves one numbered version of a content package.'),
'content_sections':('Content Section','Groups ordered questions within a package version.'),
'questions':('Question','Identifies a reusable question independently of its versions.'),
'question_versions':('Question Version','Preserves the prompt, answer specification and explanation of a question version.'),
'content_assets':('Content Asset','Stores text or a media reference used by learning content.'),
'vocabulary_items':('Vocabulary Item','Defines a vocabulary headword and its pronunciation information.'),
'vocabulary_senses':('Vocabulary Sense','Defines a meaning and example for a vocabulary item.'),
'learning_videos':('Learning Video','Describes a catalog video and its optional topic reference.'),
'video_segments':('Video Segment','Defines a timed transcript segment of a learning video.'),
'video_segment_lexical_entries':('Segment Lexical Entry','Marks a text span in a segment and its optional vocabulary sense.'),
'video_learning_progress':('Video Progress','Records one user\'s viewing progress for one video.'),
'saved_video_segments':('Saved Segment','Records a user\'s saved video segment and transcript snapshot.'),
'notes':('Note','Stores a user\'s personal note with an optional source reference.'),
'flashcard_decks':('Flashcard Deck','Groups a user\'s flashcards into a named deck.'),
'flashcards':('Flashcard','Stores a user\'s study card and optional vocabulary or source reference.'),
'topic_progress':('Topic Progress','Records one user\'s sequence position and completion for a topic.'),
'lesson_progress':('Lesson Progress','Records one user\'s passed blocks and completion for a lesson.'),
'lesson_exercise_submissions':('Exercise Submission','Records a user\'s answers and result for a lesson block submission.'),
'lesson_writing_submissions':('Writing Submission','Records an essay, its grading outcome and point-debit reference.'),
'practice_attempts':('Practice Attempt','Records a user\'s attempt at a lesson practice package.'),
'lesson_practice_passes':('Practice Pass','Records that one user has satisfied a lesson\'s practice gate.'),
'review_items':('Review Item','Tracks a user\'s remediation for a knowledge point and lesson.'),
'review_sets':('Review Set','Records a package version assigned as a set within a review item.'),
'review_theory_checks':('Review Theory Check','Records the answers and result of a review theory quick-check.'),
'topic_test_assignments':('Topic Test Assignment','Records a topic test package version assigned to a user and its consumed attempt.'),
'kp_evidence':('KP Evidence','Records a user\'s knowledge-point outcome with its learning or assessment source.'),
'assessment_attempts':('Assessment Attempt','Records one user\'s sitting of a content package version.'),
'attempt_sections':('Attempt Section','Preserves a content section snapshot within an assessment attempt.'),
'attempt_items':('Attempt Item','Preserves a question version snapshot within an attempt section.'),
'attempt_responses':('Attempt Response','Stores the current response for an attempt item.'),
'assessment_results':('Assessment Result','Represents a numbered result version for an assessment attempt.'),
'skill_scores':('Skill Score','Records a skill score and grading source within an assessment result.'),
'item_results':('Item Result','Records the score and feedback for an attempt item in a result version.'),
'error_analysis_items':('Error Analysis Item','Describes an error in an item result and its optional knowledge point.'),
'learner_submissions':('Learner Submission','Stores submitted text or an audio reference, optionally linked to an attempt item.'),
'grading_jobs':('Grading Job','Tracks grading requested for a learner submission and its charging references.'),
'human_reviews':('Human Review','Tracks examiner assignment and review for a grading job.'),
'grading_point_costs':('Grading Point Cost','Defines a time-bounded point cost for a skill and grading mode.'),
'video_practice_attempts':('Video Practice Attempt','Records a user\'s dictation or shadowing attempt for a video segment.'),
'game_sessions':('Game Session','Records a user\'s game play and optional multiplayer participant link.'),
'game_answers':('Game Answer','Records an answer to a snapshot item within a game session.'),
'game_rooms':('Game Room','Represents a hosted multiplayer room and its configuration.'),
'game_room_members':('Room Member','Records a user\'s membership and readiness in a game room.'),
'game_matches':('Game Match','Represents a multiplayer match with its content snapshot.'),
'game_match_players':('Match Player','Records one user\'s participation, score and rank in a match.'),
'game_events':('Game Event','Records a sequenced match event with an optional player reference.'),
'quiz_events':('Quiz Event','Defines a scheduled quiz using a topic and content package version.'),
'quiz_participations':('Quiz Participation','Records a user\'s assessment attempt in a quiz event.'),
'leaderboard_periods':('Leaderboard Period','Defines the activity and time window of a leaderboard.'),
'leaderboard_entries':('Leaderboard Entry','Records a user\'s score and rank for a leaderboard period.'),
'posts':('Post','Stores a user-authored community post and its moderation status.'),
'comments':('Comment','Stores a reply to a post, optionally nested below another comment.'),
'post_reactions':('Post Reaction','Records a user\'s reaction type on a post.'),
}

GROUPS=[
('user','Accounts and learning profile','users roles learner_profiles learning_goals learning_activities streaks'),
('access','Plans and activation','plans plan_features subscriptions key_products activation_keys key_activations'),
('access','Points','point_wallets point_ledger_entries'),
('content','Curriculum','topics knowledge_points lessons lesson_blocks content_assets'),
('content','Packages and questions','content_packages content_package_versions content_sections questions question_versions'),
('library','Vocabulary and video catalog','vocabulary_items vocabulary_senses learning_videos video_segments video_segment_lexical_entries'),
('library','Personal library','video_learning_progress saved_video_segments notes flashcard_decks flashcards'),
('learning','Lesson progress and practice','topic_progress lesson_progress lesson_exercise_submissions lesson_writing_submissions practice_attempts lesson_practice_passes'),
('learning','Review and evidence','review_items review_sets review_theory_checks topic_test_assignments kp_evidence'),
('assessment','Attempts and results','assessment_attempts attempt_sections attempt_items attempt_responses assessment_results skill_scores item_results error_analysis_items'),
('assessment','Submissions and grading','learner_submissions grading_jobs human_reviews grading_point_costs video_practice_attempts'),
('game','Game play','game_rooms game_room_members game_matches game_match_players game_sessions game_answers game_events'),
('game','Quizzes and leaderboards','quiz_events quiz_participations leaderboard_periods leaderboard_entries'),
('community','Community','posts comments post_reactions')]

# These associations are explicitly identified by stored reference columns.
# They are not foreign keys and never imply cross-database referential enforcement.
REFS={
'user_id':('user','users'), 'author_id':('user','users'), 'host_user_id':('user','users'), 'grader_user_id':('user','users'),
'topic_id':('content','topics'),'lesson_id':('content','lessons'),'block_id':('content','lesson_blocks'),
'knowledge_point_id':('content','knowledge_points'),'kp_id':('content','knowledge_points'),
'package_id':('content','content_packages'),'package_version_id':('content','content_package_versions'),
'question_version_id':('content','question_versions'),'content_section_id':('content','content_sections'),
'vocabulary_sense_id':('library','vocabulary_senses'),'video_id':('library','learning_videos'),'segment_id':('library','video_segments'),
'learning_goal_id':('user','learning_goals'),'attempt_id':('assessment','assessment_attempts'),
'consumed_attempt_id':('assessment','assessment_attempts'),'point_ledger_entry_id':('access','point_ledger_entries'),
'debit_ledger_entry_id':('access','point_ledger_entries'),'premium_subscription_id':('access','subscriptions')}

def main():
    schemas,sources=parse_schema()
    nodes={}
    for service, title, names in GROUPS:
        for name in names.split():
            t=schemas[service][name]
            t.update(label=DESCRIPTIONS[name][0],purpose=DESCRIPTIONS[name][1],key=f'{service}.{name}')
            nodes[t['key']]=t
    edges=[]
    for key,t in nodes.items():
        foreign_cols=set()
        for fk in t['fks']:
            foreign_cols.update(fk['cols'])
            target=f"{t['service']}.{fk['target']}"
            if target not in nodes:continue
            edges.append({'parent':target,'child':key,'cols':fk['cols'],'kind':'FK',
                'optional':any(t['cols'][c]['nullable'] for c in fk['cols']),
                'unique':fk['cols'] in t['unique'] or fk['cols']==t['pk']})
        for col,(svc,target) in REFS.items():
            if col not in t['cols'] or col in foreign_cols:continue
            parent=f'{svc}.{target}'
            if parent not in nodes or parent==key:continue
            edges.append({'parent':parent,'child':key,'cols':[col],'kind':'Ref','optional':t['cols'][col]['nullable'],
                'unique':[col] in t['unique'] or [col]==t['pk']})
    bridges=[]
    for svc,names in [('user',['user_roles']),('library',['flashcard_deck_items']),('content',['lesson_knowledge_points','lesson_block_questions','lesson_block_knowledge_points','question_knowledge_points','section_questions'])]:
        for name in names:
            t=schemas[svc][name]; a,b=t['fks'][:2]
            bridges.append({'parent':f"{svc}.{a['target']}",'child':f"{svc}.{b['target']}",'cols':a['cols']+b['cols'],'kind':'M:N','via':name})
    bridges.append({'parent':'content.lesson_blocks','child':'library.vocabulary_senses','cols':['block_id','vocabulary_sense_id'],'kind':'M:N Ref','via':'lesson_block_vocabulary'})
    for owner,col in [('content_sections','section_id'),('question_versions','question_version_id')]:
        bridges.append({'parent':f'content.{owner}','child':'content.content_assets','cols':[col,'asset_id'],'kind':'M:N','via':'content_asset_links'})
    model={'nodes':nodes,'edges':edges,'bridges':bridges,'groups':GROUPS,'sources':sources}
    (OUT/'erd-model.json').write_text(json.dumps(model,indent=2),encoding='utf-8')
    print(json.dumps({'entities':len(nodes),'fk':sum(e['kind']=='FK' for e in edges),'references':sum(e['kind']=='Ref' for e in edges),'associations':len(bridges),'groups':len(GROUPS)}))

if __name__=='__main__':main()
