// Content authoring screens (/content-author/*). Targets follow the order of the elements in wf_index.json.
import { signedIn, page, uid } from './common.mjs'

export const common = [
  ...signedIn('CONTENT_AUTHOR'),
  [/GET \/api\/notifications/, page([])],
]

export const patterns = []

// The dev server can be slow: wait for the page title and for the loading lines to go before anything else.
const ready = `for (let i = 0; i < 80 && !(document.querySelector('#admin-main h1') && !document.querySelector('#admin-main [aria-busy=true]') && !/Đang tải/.test(document.querySelector('#admin-main').textContent)); i += 1) await __wait(500); await __wait(1200)`

// The sticky table header covers the first row inside its scroll box; draw it in the normal flow.
const flatHead = `const flat = document.createElement('style'); flat.textContent = '.adm-table thead th{position:static!important}'; document.head.append(flat)`
// Folded <details> would still be drawn by the extractor, so they are shown open.
const openDetails = `document.querySelectorAll('details').forEach((el) => { el.open = true })`

// Waits until a button or link with this text is there (the dev server can be slow), then clicks it.
const press = (text, nth = 0) => `for (let i = 0; i < 60; i += 1) { try { __click('${text}', ${nth}); break } catch (error) { if (i === 59) throw error; await __wait(500) } }`

const at = '2026-11-20T08:00:00Z'

// A form drawer is shown beside the page (not over it), so the elements of the page and of the form can both be numbered.
const beside = (width = 540) => `const style = document.createElement('style'); style.textContent = '.adm-layer__overlay{background:transparent!important;backdrop-filter:none!important;pointer-events:none!important} body{pointer-events:auto!important} #root{padding-right:${width}px} .adm-drawer{width:${width}px!important;max-width:${width}px!important;animation:none!important}'; document.head.append(style)`

const course = (n, code, name, bandLevel, extra = {}) => ({ id: uid(700 + n), code, name, bandLevel, coverImageUrl: null, bannerImageUrl: null, description: 'A made-up course for the demo.', status: 'ACTIVE', free: false, skillUnlockPoints: { READING: 40, LISTENING: 40 }, comboUnlockPoints: 70, createdAt: at, updatedAt: at, ...extra })
const topic = (n, code, name, skill, extra = {}) => ({ id: uid(800 + n), parentTopicId: null, code, name, description: null, sortOrder: n, status: 'ACTIVE', bandMin: 5, bandMax: 6, skill, courseId: uid(701), createdAt: at, updatedAt: at, children: [], ...extra })
const courses = [course(1, 'IELTS-B55', 'IELTS Band 5.5', 5.5, { free: true }), course(2, 'IELTS-B65', 'IELTS Band 6.5', 6.5, { status: 'INACTIVE' })]
const topics = [topic(1, 'READ-FND', 'Reading foundations', 'READING'), topic(2, 'READ-TFNG', 'True, False, Not Given', 'READING'), topic(3, 'LIST-FORM', 'Form completion', 'LISTENING', { status: 'INACTIVE' })]
const courseFixtures = [[/GET \/api\/content\/courses/, courses], [/GET \/api\/content\/topics/, topics], [/GET \/api\/content\/admin\/code-settings|GET \/api\/content\/admin\/codes/, {}]]

const cell = (skill, cefrLevel, total, none, one = 0) => ({ skill, cefrLevel, activeKnowledgePoints: total, withoutPublishedQuestion: none, withOnePublishedQuestion: one })
const work = (n, kind, status, subject, detail = null, extra = {}) => ({ id: uid(300 + n), queue: 'CONTENT_AUTHOR', kind, status, subjectRef: uid(310 + n), subject, detail, createdAt: at, updatedAt: at, ...extra })
const kpSubject = (n, name, occurrences) => ({ type: 'KNOWLEDGE_POINT', id: uid(400 + n), code: `READ-B1-00${n}`, name, occurrences })
const workItems = [
  work(1, 'CONTENT_GAP', 'OPEN', kpSubject(1, 'Skimming for the main idea', 4)),
  work(2, 'QUESTION_TOO_HARD', 'IN_PROGRESS', { type: 'QUESTION', questionId: uid(501), stemPreview: 'The author claims that city gardens...' }, JSON.stringify({ correctRate: 0.18, attempts: 64, difficulty: 'MEDIUM' })),
  work(3, 'EDGE_SUSPECT', 'OPEN', { type: 'EDGE', from: { id: uid(401), name: 'Paraphrase' }, to: { id: uid(402), name: 'Scanning' } }, JSON.stringify({ probes: 40, passRate: 0.91, rootRate: 0.04 })),
]

const question = (n, code, questionType, skill, purpose, status) => ({ id: uid(500 + n), code, questionType, skill, purpose, status, accessLevel: 'FREE', currentPublishedVersionId: status === 'PUBLISHED' ? uid(601) : null, createdAt: at, updatedAt: at })
const questions = [
  question(1, 'Q-READ-0001', 'MULTIPLE_CHOICE', 'READING', 'LEARNING', 'PUBLISHED'),
  question(2, 'Q-LIST-0002', 'FILL_IN_BLANK', 'LISTENING', 'EXAM', 'DRAFT'),
  question(3, 'Q-WRIT-0003', 'ESSAY', 'WRITING', 'PLACEMENT', 'ARCHIVED'),
]
const version = (n, status, stem) => ({ id: uid(600 + n), versionNumber: n, stem, status, difficulty: 'MEDIUM', explanation: 'The first sentence states it.', hint: 'Read the first sentence.', options: [{ optionKey: 'A', content: 'City gardens' }, { optionKey: 'B', content: 'Public transport' }], answerSpecJson: JSON.stringify({ type: 'CHOICE', correct: 'A' }), knowledgePoints: [] })

const catalogItem = (n, code, title, packageType, extra = {}) => ({ id: uid(1000 + n), code, title, packageType, accessLevel: 'FREE', status: 'PUBLISHED', currentPublishedVersionId: uid(1100 + n), lessonId: null, topicId: null, courseId: null, sourceCode: null, placementPool: null, createdAt: at, updatedAt: at, latestVersionNumber: 1, hasDraft: false, skill: 'READING', partNos: [1], sectionCount: 2, questionCount: 20, ...extra })
const catalog = (rows) => ({ content: rows, page: 0, size: 100, totalElements: rows.length, totalPages: 1 })
const stats = (rows) => rows.map((row, i) => ({ packageId: row.id, started: 40 - i * 7, submitted: 30 - i * 6, averageBand: 6 - i * 0.5 }))
const packages = [
  catalogItem(1, 'SET-READ-01', 'Reading practice: main ideas', 'PRACTICE_SET'),
  catalogItem(2, 'QUIZ-LIST-02', 'Listening quiz: forms', 'QUIZ', { status: 'DRAFT', latestVersionNumber: 2, hasDraft: true, currentPublishedVersionId: null }),
  catalogItem(3, 'MOCK-03', 'Mock test March sample', 'MOCK_TEST', { accessLevel: 'PREMIUM', status: 'ARCHIVED' }),
]
const placements = [
  catalogItem(11, 'PLACE-30', 'Placement test: starter', 'PLACEMENT_TEST', { placementPool: 'BAND_0_3' }),
  catalogItem(12, 'PLACE-50', 'Placement test: intermediate', 'PLACEMENT_TEST', { placementPool: 'BAND_3_5' }),
  catalogItem(13, 'PLACE-90', 'Placement test: advanced', 'PLACEMENT_TEST', { placementPool: 'BAND_5_9', status: 'DRAFT', currentPublishedVersionId: null }),
]
const practice = [
  catalogItem(21, 'PRAC-READ-1', 'Reading test: city gardens', 'PRACTICE_TEST', { sourceCode: 'SAMPLE' }),
  catalogItem(22, 'PRAC-LIST-1', 'Listening test: library tour', 'PRACTICE_TEST', { skill: 'LISTENING', accessLevel: 'PREMIUM', sourceCode: 'SAMPLE' }),
  catalogItem(23, 'PRAC-WRIT-1', 'Writing test: line graph', 'PRACTICE_TEST', { skill: 'WRITING', status: 'DRAFT', sourceCode: null, currentPublishedVersionId: null }),
]
const catalogFixtures = (rows) => [
  [/GET \/api\/content\/admin\/packages\?/, catalog(rows)],
  [/GET \/api\/assessments\/admin\/package-stats/, stats(rows)],
  [/GET \/api\/content\/practice-tests\/sources/, [{ code: 'SAMPLE', name: 'Sample book 1' }]],
  ...courseFixtures,
]
const sectionOf = (n, title, partNo, skill = 'READING') => ({ id: uid(1200 + n), title, skill, sortOrder: n, timeLimitSeconds: 1200, partNo, coverAssetId: null, instructions: 'Read the passage and answer.', passage: 'City gardens give people a place to rest.', audio: null,
  questions: [{ id: uid(1300 + n), questionVersionId: uid(600 + n), questionId: uid(500 + n), stem: 'Which option states the main idea?', status: 'PUBLISHED', purpose: 'LEARNING', sortOrder: 1, maxScore: 1 }] })
const versionOf = (n, status, sections) => ({ id: uid(1100 + n), versionNumber: n, status, rulesJson: '{}', publishedAt: status === 'PUBLISHED' ? '2026-10-23T08:00:00Z' : null, publishAt: null, lastPublishError: null, sections })
const editorPackage = { ...catalogItem(1, 'SET-READ-01', 'Reading practice: main ideas', 'PRACTICE_SET'), versions: [versionOf(1, 'PUBLISHED', []), versionOf(2, 'DRAFT', [sectionOf(1, 'Passage 1: City gardens', 1), sectionOf(2, 'Passage 2: Night buses', 2)])] }

const sense = (n, partOfSpeech, englishDefinition) => ({ id: uid(1400 + n), vietnameseMeaning: 'Sample meaning', englishDefinition, exampleSentence: 'They had to abandon the old plan.', partOfSpeech, cefrLevel: 'B2' })
const words3 = [['abandon', 'verb'], ['policy', 'noun'], ['significant', 'adjective']].map(([lemma, pos], i) => ({ id: uid(1450 + i), lemma, ipa: '/sample/', pronunciationAudioReference: null, senses: [sense(i, pos, 'A made-up definition for the demo.')] }))
const band = (value) => ({ band: value, descriptor: 'A sample descriptor.' })
const rubric = (version, status) => ({ code: 'WRITING-T2', task: 'TASK_2', version, status, sourceNote: 'Sample source note', systemPrompt: 'Grade the essay.', criteria: ['TR', 'CC', 'LR', 'GRA'].map((code, i) => ({ code, name: ['Task Response', 'Coherence and Cohesion', 'Lexical Resource', 'Grammar'][i], description: 'Sample criterion.', sortOrder: i + 1, bandDescriptors: [band(5), band(6)], knowledgePointId: null })), examples: [] })
const errorType = (n, code, name, category, severity, reviewStatus) => ({ id: uid(1500 + n), code, name, category, severity, knowledgePointId: null, fixHint: 'Check the verb form.', status: 'ACTIVE', reviewStatus })
const node = (n, code, name, extra = {}) => ({ id: uid(400 + n), code, name, skill: 'READING', kind: 'SKILL', status: 'ACTIVE', cefrLevel: 'B1', canDo: 'Can do it.', courseId: uid(701), band: 5.5, ...extra })
const tree = { nodes: [node(1, 'READ-B1-001', 'Main idea'), node(2, 'READ-B1-002', 'Skimming'), node(3, 'READ-B1-003', 'Paraphrase'), node(4, 'READ-B1-004', 'Inference', { canDo: null })], edges: [{ from: uid(402), to: uid(401) }, { from: uid(403), to: uid(401) }] }
const setting = (kind, pattern) => ({ kind, applied: true, pattern, startNumber: 1, editable: true })

const importSummary = (n, filename, status, courseCode, courseName) => ({ id: uid(1600 + n), filename, status, importedCourseId: status === 'IMPORTED' ? uid(701) : null, courseCode, courseName, updatedAt: at })
const issue = (sheet, row, column, code, message) => ({ sheet, row, column, code, message })
const outline = { course: { cells: {}, row: 3, code: 'IELTS-B60', name: 'IELTS Band 6.0', bandLevel: '6.0' },
  topics: [{ cells: {}, row: 3, code: 'READ-A', name: 'Reading skills', skill: 'READING', sortOrder: '1', tests: [], lessons: [{ cells: {}, row: 3, code: 'L1', title: 'Finding the main idea', knowledgePointCodes: ['READ-B1-001'], blocks: [], practiceSets: [] }] }],
  courseTests: [], milestones: [], questions: [] }
const importDraftDetail = { id: uid(1601), filename: 'band6-course.xlsx', status: 'DRAFT', importedCourseId: null, updatedAt: at, sheets: {},
  report: { valid: false, errors: [issue('Lessons', 4, 'knowledge_point', 'UNKNOWN_KNOWLEDGE_POINT', 'Unknown knowledge point')], warnings: [issue('Questions', 7, 'explanation', 'MISSING_EXPLANATION', 'No explanation')], outline, summary: { topics: 3, lessons: 12, questions: 60, packages: 4 } } }

const item = (n, stem) => ({ itemId: uid(1700 + n), stem, explanation: 'The first sentence states it.', difficulty: 'MEDIUM', options: [{ optionKey: 'A', content: 'City gardens', sortOrder: 0 }, { optionKey: 'B', content: 'Night buses', sortOrder: 1 }], answerSpec: { type: 'CHOICE', correct: 'A', distractors: { B: 'WORD_CHOICE' } } })
const aiDraft = (n, status, extra = {}) => ({ id: uid(1800 + n), kind: 'QUESTIONS', status, request: { knowledgePointId: uid(401), questionType: 'MULTIPLE_CHOICE', count: 5 },
  result: status === 'QUEUED' ? null : { revision: 0, items: [item(1, 'What is the main idea of paragraph A?'), item(2, 'Which sentence best sums up paragraph B?')], rejected: [{ index: 2, reason: 'DUPLICATE_STEM' }] },
  errorCode: null, model: 'sample-model', promptVersion: 'question-v1', createdAt: at, finishedAt: at, reviewedAt: null, reviewNote: null, origin: { type: 'KNOWLEDGE_POINT', title: 'Questions on main idea ' + n, knowledgePointName: 'Main idea', exists: true }, itemCount: 2, ...extra })

const goldEssay = (n, task, examinerBand, source, essayPreview) => ({ id: uid(1900 + n), task, examinerBand, source, essayPreview, createdAt: at })
const evalRun = { id: uid(1950), task: 'TASK_2', model: 'sample-model', promptVersion: 'writing-v3', status: 'DONE', n: 12, withinHalfBand: 0.83, mae: 0.38, qwk: 0.76, passed: true, startedAt: at, finishedAt: at }
const criterion = (withinHalfBand, mae, qwk) => ({ n: 12, withinHalfBand, mae, qwk })
const evalDetail = { ...evalRun, thresholds: { withinHalfBand: 0.8, qwk: 0.7 }, perCriterion: { TR: criterion(0.83, 0.4, 0.74), CC: criterion(0.92, 0.3, 0.8), LR: criterion(0.75, 0.45, 0.71), GRA: criterion(0.83, 0.38, 0.77) }, errorCodeFalsePositive: 0.1, failureCode: null,
  items: [1, 2, 3].map((n) => ({ goldId: uid(1900 + n), llmBand: n === 3 ? 6.5 : 5.5 + n * 0.5, llmCriteria: {}, llmErrorCodes: [], diff: { examinerBand: 6 + (n - 1) * 0.5, absoluteBandDifference: n === 3 ? 0.5 : 0 } })) }
const queued = (n, task, source, waitSeconds, status = 'QUEUED') => ({ id: uid(2000 + n), attemptType: source, skill: 'WRITING', task, status, createdAt: at, waitSeconds, overdue: waitSeconds > 172800 })

const list = [
  {
    file: 'content-authoring-content-author-home',
    url: '/content-author',
    wait: 7000,
    fixtures: [
      [/GET \/api\/content\/admin\/overview\/question-labels/, [
        { questionVersionId: uid(601), questionId: uid(501), label: 'The author claims that city gardens...' },
        { questionVersionId: uid(602), questionId: uid(502), label: 'Choose the correct heading for B...' },
      ]],
      [/GET \/api\/content\/admin\/overview/, {
        knowledgePoints: { total: 48, byStatus: { ACTIVE: 48 }, bySkill: { LISTENING: 12, READING: 18, WRITING: 9, SPEAKING: 6, ALL: 3 }, byCefr: { A1: 4, A2: 8, B1: 16, B2: 14, C1: 6 } },
        activeKnowledgePointsWithoutPublishedQuestion: 9, draftQuestionVersions: 5, lessons: { PUBLISHED: 36 }, staleDraftQuestionVersions: 2, staleDraftAfterDays: 14,
        coverage: [cell('LISTENING', 'A2', 4, 0, 1), cell('LISTENING', 'B1', 8, 2, 1), cell('READING', 'B1', 10, 1, 2), cell('READING', 'B2', 8, 1), cell('WRITING', 'B2', 6, 4), cell('WRITING', 'C1', 3, 0), cell('SPEAKING', 'B1', 6, 1), cell('ALL', 'A1', 3, 0)],
      }],
      [/GET \/api\/content\/knowledge-tree\/health/, { isolatedIds: [uid(401)], missingMetadataIds: [uid(402), uid(403)], levelViolations: [] }],
      [/GET \/api\/learning\/admin\/work-items/, page(workItems)],
      [/GET \/api\/learning\/admin\/question-stats/, { questions: [
        { questionVersionId: uid(601), attempts: 64, correct: 12, difficulty: 'MEDIUM' },
        { questionVersionId: uid(602), attempts: 52, correct: 46, difficulty: 'MEDIUM' },
      ] }],
    ],
    targets: ['h1', 'section[aria-label="Nội dung"]', 'section[aria-label="Việc cần làm của R&D"]'],
  },
  {
    file: 'content-authoring-courses',
    url: '/content-author/courses',
    width: 1500,
    height: 1150,
    wait: 7000,
    fixtures: courseFixtures,
    script: [press('Tạo khóa học'), beside()],
    targets: ['h1', '.adm-titlebar__actions button', 'section[aria-label="Khóa học"]', '.adm-drawer'],
  },
  {
    file: 'content-authoring-question-bank',
    url: '/content-author/questions',
    wait: 7000,
    fixtures: [[/GET \/api\/content\/admin\/questions\?/, page(questions)]],
    script: flatHead,
    targets: ['h1', '.adm-titlebar__actions button', '.adm-filters', '.adm-table-wrap'],
  },
  {
    file: 'content-authoring-question-detail',
    url: `/content-author/questions/${uid(501)}`,
    width: 1500,
    height: 1250,
    wait: 7000,
    fixtures: [
      [/GET \/api\/content\/admin\/questions\/[^/?]+\/usages/, []],
      [/GET \/api\/content\/admin\/questions\/versions\/[^/]+\/passages/, []],
      [/GET \/api\/content\/admin\/questions\/[^/?]+$/, { ...questions[0], versions: [version(2, 'DRAFT', 'What is the main idea of paragraph B?'), version(1, 'PUBLISHED', 'What is paragraph B mainly about?')] }],
      [/GET \/api\/content\/knowledge-tree/, { nodes: [], edges: [] }],
    ],
    script: [openDetails, press('Thêm phiên bản'), beside(620)],
    targets: ['.adm-titlebar__actions a', '.adm-titlebar__text', 'section[aria-label="Các phiên bản"]', '.adm-drawer'],
  },
  {
    file: 'content-authoring-test-packages',
    url: '/content-author/packages',
    wait: 7000,
    fixtures: catalogFixtures(packages),
    script: flatHead,
    words: { 'Trắc nghiệm': 'Quiz' },
    targets: ['h1', '.adm-titlebar__actions button', 'section.adm-package-form-grid', '.adm-table-wrap'],
  },
  {
    file: 'content-authoring-package-editor',
    url: `/content-author/packages/${editorPackage.id}`,
    wait: 7000,
    fixtures: [
      [/GET \/api\/content\/admin\/packages\/[^/?]+\/publication-schedules/, []],
      [/GET \/api\/content\/admin\/packages\/[^/?]+$/, editorPackage],
      [/GET .*section-drafts|GET \/api\/(content|ai)\/.*drafts/, []],
      ...courseFixtures,
    ],
    script: press('Mở tất cả'),
    targets: ['h1', 'section.adm-pe-bar', 'section.adm-pe-content h3', 'section.adm-pe-publish button', 'section.adm-pe-publish h3'],
  },
  {
    file: 'content-authoring-placement-tests',
    url: '/content-author/placement-tests',
    wait: 7000,
    fixtures: [...placements.map((row) => [new RegExp('GET /api/content/admin/packages\\?(?=.*status=PUBLISHED)(?=.*placementPool=' + row.placementPool + ')'), catalog(row.status === 'PUBLISHED' ? [row] : [])]), ...catalogFixtures(placements)],
    script: flatHead,
    targets: ['h1', '[role=tablist]', '.adm-table-wrap', '.adm-titlebar__actions button'],
  },
  {
    file: 'content-authoring-practice-library',
    url: '/content-author/practice-tests',
    wait: 7000,
    width: 1200,
    fixtures: catalogFixtures(practice),
    script: flatHead,
    targets: ['h1', '[role=tablist]', 'section.adm-filters', '.adm-table-wrap', '.adm-titlebar__actions button'],
  },
  {
    file: 'content-authoring-vocabulary-content',
    url: '/content-author/vocabulary',
    wait: 7000,
    fixtures: [
      [/GET \/api\/content\/vocabulary\/[0-9a-f-]{36}/, { ...words3[0], cefrLevel: 'B2', wordFamily: 'abandon, abandonment', definitions: [], collocations: [], thesaurus: [] }],
      [/GET \/api\/content\/vocabulary/, { items: words3, page: 0, size: 25, total: 3 }],
      [/GET \/api\/content\/admin\/vocabulary\/(topics|sets)/, []],
    ],
    script: `for (let i = 0; i < 40; i += 1) { const word = [...document.querySelectorAll('span')].find((el) => el.textContent.trim() === 'abandon'); if (word) { word.click(); word.parentElement.parentElement.parentElement.id = 'word-list'; break } await __wait(500) } await __wait(2000)`,
    targets: ['h1', 'text=Words and senses', 'input[placeholder^="Find a word"]', '#word-list'],
  },
  {
    file: 'content-authoring-grading-rubrics',
    url: '/content-author/rubrics?task=TASK_2',
    wait: 7000,
    fixtures: [
      [/GET \/api\/content\/rubrics\?/, [rubric(1, 'ACTIVE')]],
      [/GET \/api\/content\/admin\/rubrics\/versions/, [{ code: 'WRITING-T2', task: 'TASK_2', version: 1, status: 'ACTIVE' }, { code: 'WRITING-T2', task: 'TASK_2', version: 2, status: 'DRAFT' }]],
      [/GET \/api\/content\/admin\/rubrics\?/, [rubric(2, 'DRAFT')]],
    ],
    script: [flatHead, `document.querySelector('section[aria-label="Bản nháp"] h2').firstChild.textContent = 'Drafts'`],
    targets: ['h1', '.rb-seg', 'section[aria-label="Đang dùng"], section[aria-label^="Rubric đang dùng"]', 'section[aria-label="Bản nháp"]'],
  },
  {
    file: 'content-authoring-error-types',
    url: '/content-author/error-types',
    wait: 7000,
    width: 1200,
    fixtures: [
      [/GET \/api\/content\/error-types/, [errorType(1, 'SUBJECT_VERB', 'Subject and verb agreement', 'GRAMMAR', 2, 'APPROVED'), errorType(2, 'WORD_CHOICE', 'Wrong word choice', 'VOCABULARY', 1, 'DRAFT'), errorType(3, 'MISSING_LINK', 'Missing linking words', 'COHERENCE', 3, 'DRAFT')]],
      [/GET \/api\/content\/knowledge-tree/, tree],
    ],
    script: [flatHead, `document.querySelectorAll('.adm-error-actions button').forEach((b) => { if (b.textContent.trim() === 'Duyệt') b.textContent = 'Approve'; if (b.textContent.trim() === 'Ngừng dùng') b.textContent = 'Deactivate' })`],
    targets: ['h1', '.adm-titlebar__actions button', '.adm-filters', '.adm-table-wrap'],
  },
  {
    file: 'content-authoring-knowledge-graph',
    url: '/content-author/knowledge-graph',
    wait: 14000,
    fixtures: [
      [/GET \/api\/content\/knowledge-tree\/health/, { isolatedIds: [uid(404)], missingMetadataIds: [uid(404)], levelViolations: [] }],
      [/GET \/api\/content\/knowledge-tree/, tree],
      ...courseFixtures,
    ],
    targets: ['h1', '.adm-filters', '.kg-health', '.adm-titlebar__actions button.adm-btn--primary'],
  },
  {
    file: 'content-authoring-code-settings',
    url: '/content-author/settings',
    wait: 7000,
    fixtures: [[/GET \/api\/content\/admin\/code-settings/, [setting('COURSE', 'COURSE_{BAND}_{SEQ:3}'), setting('TOPIC', '{COURSE}_{SKILL}{SEQ:2}')]]],
    targets: ['h1', '.adm-code-settings__pattern', 'input[inputmode=numeric]', 'text=Auto-generated code set', 'form .adm-form-footer button'],
  },
  {
    file: 'content-authoring-course-import',
    url: '/content-author/course-import',
    wait: 5000,
    fixtures: [[/GET \/api\/content\/admin\/course-import\/drafts/, [importSummary(1, 'band6-course.xlsx', 'DRAFT', 'IELTS-B60', 'IELTS Band 6.0'), importSummary(2, 'band55-course.xlsx', 'IMPORTED', 'IELTS-B55', 'IELTS Band 5.5')]]],
    targets: ['h1', 'input[type=file]', '#check-button', 'text=Knowledge point codes', '.adm-table-wrap'],
    script: `[...document.querySelectorAll('button')].find((b) => b.textContent.trim() === 'Kiểm tra').id = 'check-button'`,
  },
  {
    file: 'content-authoring-import-draft',
    url: `/content-author/course-import/${importDraftDetail.id}`,
    wait: 5000,
    fixtures: [[/GET \/api\/content\/admin\/course-import\/drafts\/[0-9a-f-]{36}/, importDraftDetail], [/GET \/api\/content\/knowledge-tree/, tree], [/GET \/api\/content\/error-types/, []]],
    script: [flatHead, `[...document.querySelectorAll('h2, h3')].find((h) => h.textContent.trim().startsWith('Lỗi')).id = 'issue-list'`],
    targets: ['.adm-od__back', 'h1', '.adm-od__stats', '#issue-list', '.adm-od__actions .adm-btn--primary', '.adm-od__actions button'],
  },
  {
    file: 'content-authoring-ai-drafts',
    url: '/content-author/ai-drafts?status=ALL',
    wait: 5000,
    fixtures: [[/GET \/api\/content\/admin\/ai-drafts\?/, [aiDraft(1, 'READY'), aiDraft(2, 'APPROVED'), aiDraft(3, 'REJECTED')]]],
    targets: ['h1', '.adm-draft-actions select', '.adm-draft-list', null],
  },
  {
    file: 'content-authoring-ai-draft-detail',
    url: `/content-author/ai-drafts/${uid(1801)}`,
    wait: 6000,
    fixtures: [[/GET \/api\/content\/admin\/ai-drafts\/[0-9a-f-]{36}/, aiDraft(1, 'READY')], [/GET \/api\/content\/knowledge-tree/, tree], [/GET \/api\/content\/knowledge-points/, []], [/GET \/api\/content\/error-types/, []]],
    script: flatHead,
    targets: ['.adm-titlebar__actions a', 'h1', 'table.dr-items', null, 'section[aria-label="Từ chối bản nháp"]'],
  },
  {
    file: 'content-authoring-ai-evaluation',
    url: '/content-author/ai-evaluation',
    wait: 5000,
    fixtures: [
      [/GET \/api\/assessments\/admin\/ai-prompt-activations/, [{ task: 'TASK_2', promptVersion: 'writing-v2', runId: uid(1950), activatedAt: at }]],
      [/GET \/api\/assessments\/admin\/ai-gold-essays/, { content: [goldEssay(1, 'TASK_2', 6.5, 'MANUAL', 'Many cities now build small gardens on roofs...'), goldEssay(2, 'TASK_1', 6, 'COMPARISON', 'The line graph shows bus use over ten years...')], page: 0, size: 20, totalElements: 2 }],
      [/GET \/api\/assessments\/admin\/ai-evaluations/, { content: [evalRun], page: 0, size: 20, totalElements: 1 }],
    ],
    script: flatHead,
    targets: ['h1', 'section[aria-label="Bản chấm đang sử dụng"]', '.adm-evaluation-workspace select', null, '.adm-table-wrap'],
  },
  {
    file: 'content-authoring-evaluation-run',
    url: `/content-author/ai-evaluation/runs/${evalRun.id}`,
    wait: 5000,
    fixtures: [[/GET \/api\/assessments\/admin\/ai-evaluations\/[0-9a-f-]{36}/, evalDetail], [/GET \/api\/assessments\/admin\/ai-prompt-activations/, []], [/GET \/api\/assessments\/admin\/ai-gold-essays/, { content: [1, 2, 3].map((n) => goldEssay(n, 'TASK_2', 5.5 + n * 0.5, 'MANUAL', 'Sample essay ' + n + ' about city gardens...')), page: 0, size: 20, totalElements: 3 }]],
    script: flatHead,
    targets: ['.adm-titlebar__actions a', 'h1', '.adm-evaluation-workspace section:nth-of-type(2)', '.adm-evaluation-workspace section:nth-of-type(3)', 'text=Activate this version'],
  },
  {
    file: 'content-authoring-work-queue',
    url: '/content-author/work-items',
    wait: 5000,
    width: 1200,
    fixtures: [[/GET \/api\/learning\/admin\/work-items/, page(workItems)]],
    // The "all shown" line wraps into one narrow column of the table.
    script: [flatHead, `[...document.querySelectorAll('td, p, span, div')].filter((el) => el.childElementCount === 0 && el.textContent.trim().startsWith('Đã hiện tất cả')).forEach((el) => { el.style.whiteSpace = 'nowrap' })`],
    targets: ['h1', '.adm-filters', '.adm-table-wrap', null],
  },
  {
    file: 'content-authoring-examiner-grade-check',
    url: '/content-author/examiner-review',
    wait: 5000,
    fixtures: [
      [/GET \/api\/assessments\/qa-reviews\/queue/, { content: [queued(1, 'TASK_2', 'TOPIC_GATE', 7200), queued(2, 'TASK_1', 'PLACEMENT', 200000)], page: 0, size: 20, totalElements: 2 }],
      [/GET \/api\/assessments\/qa-reviews\/mine/, { content: [queued(3, 'TASK_2', 'MOCK', 3600, 'CLAIMED')], page: 0, size: 20, totalElements: 1 }],
      [/GET \/api\/assessments\/admin\/examiner-quality/, []],
    ],
    script: flatHead,
    words: { 'bài': 'essays' },
    targets: ['h1', '.adm-er-actions', '.adm-table-wrap', null],
  },
  {
    file: 'content-authoring-review-essay',
    url: `/content-author/examiner-review/${uid(2003)}`,
    wait: 5000,
    fixtures: [[/GET \/api\/assessments\/qa-reviews\/[0-9a-f-]{36}\/essay/, { jobId: uid(2003), task: 'TASK_2', prompt: 'Some people think cities need more public gardens. Discuss.', essay: 'Many cities now build small gardens on roofs. In my view this is useful because people can rest near their homes. However, gardens cost money to keep.', rubric: { code: 'WRITING-T2', version: 1 }, source: 'MOCK' }]],
    // The rubric is folded: its hidden text is removed so that only the summary line is drawn.
    script: `document.querySelectorAll('.adm-er-rubric').forEach((el) => el.remove())`,
    words: { 'Bài': 'Type:' },
    targets: ['.adm-titlebar__actions a', 'h1', '#admin-main section.adm-card', '#admin-main form', '#admin-main form button[type=submit]'],
  },
]

export const screens = list.map((screen) => ({ ...screen, script: [ready, ...[screen.script ?? []].flat()] }))
