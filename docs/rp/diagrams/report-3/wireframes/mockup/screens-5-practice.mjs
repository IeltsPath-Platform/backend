// Practice tests and grading screens, and the learning path screens that need exercises, drawn on the real API mode (port 5199).
import { signedIn, page, uid } from './common.mjs'

export const common = [
  ...signedIn('CUSTOMER'),
  [/(POST|PUT|GET) \/api\/(users\/me\/study-sessions|learning-support).*/, {}],
]
export const patterns = [['^Bộ (\\d+)/(\\d+) · ', 'Set $1/$2 · '], ['Tim kế tiếp sẽ về sau (\\d+) giờ (\\d+) phút\\.', 'The next heart returns in $1 h $2 min.'], ['Mỗi câu sai tốn một tim\\.', 'Each wrong answer costs one heart.'], ['Bài ôn vẫn làm được\\.', 'Reviews still work.'], ['^Bài (\\d+) · ', 'Lesson $1 · '], ['^Bài tập (\\d+)$', 'Exercise $1'], ['^Còn (\\d+) câu chưa trả lời$', '$1 questions unanswered'], ['Đã ghi âm · (\\d\\d:\\d\\d)', 'Answered · $1'], ['(\\d+) bản ghi · tổng (\\d\\d:\\d\\d)', '$1 recordings · total $2'], ['^(Task \\d) · Luyện tập tự do$', '$1 · Free practice'], ['^Nộp (\\d\\d/\\d\\d/\\d{4})$', 'Submitted $1'], ['^Còn (\\d+:\\d+)', 'Time left $1'], ['(\\d+) câu · (\\d+) phút', '$1 questions · $2 min']]
export const screens = []
// The extractor draws the content of a closed <details> (the account menu of the header), so it is removed first.
const noClosed = `document.querySelectorAll('details:not([open]) > :not(summary)').forEach((el) => { el.style.display = 'none' })`
const add = (screen) => screens.push({ wait: 10000, ...screen, script: [...[screen.script ?? []].flat(), noClosed] })
const ago = (minutes) => new Date(Date.now() - minutes * 60000).toISOString()
// A link stretched over a whole row (::after) would hide the row texts from the extractor.
const noStretch = `document.head.insertAdjacentHTML('beforeend', '<style>*::after, *::before { pointer-events: none !important }</style>')`
const picture = 'data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22320%22 height=%22180%22/%3E'

// ---------- Practice Test Catalog ----------
const card = (n, title, formats, questionCount = 13) => ({
  packageId: uid(500 + n), packageVersionId: uid(600 + n), scope: 'PART', title: `Passage 1: ${title}`, coverUrl: picture, source: null,
  formats, questionCount, timeLimitSeconds: 1200, requiredFeatureKey: n === 3 ? 'PREMIUM_TESTS' : null,
  parts: [{ sectionId: uid(700 + n), partNo: 1, title, questionCount, formats }],
})
const catalogCards = [
  card(1, 'Keeping cities cool', ['TRUE_FALSE_NOT_GIVEN', 'MULTIPLE_CHOICE']),
  card(2, 'The history of tea', ['MATCHING_HEADINGS', 'SENTENCE_COMPLETION']),
  card(3, 'Sleep and memory', ['YES_NO_NOT_GIVEN', 'SUMMARY_COMPLETION'], 14),
]
add({
  file: 'practice-tests-and-grading-practice-test-catalog',
  url: '/practice-tests?skill=reading',
  fixtures: [
    [/GET \/api\/content\/practice-tests\/formats/, ['TRUE_FALSE_NOT_GIVEN', 'MULTIPLE_CHOICE', 'MATCHING_HEADINGS', 'SENTENCE_COMPLETION'].map((code) => ({ code, sectionCount: 4 }))],
    [/GET \/api\/assessments\/practice\/progress/, { completed: [] }],
    [/POST \/api\/content\/practice-tests\/search/, { totalElements: 21, page: 0, size: 9, items: catalogCards.slice(1) }],
  ],
  // The "ready" window is shown without its dimmed backdrop, so that the catalog behind it stays readable.
  script: [
    `for (let i = 0; i < 40 && !document.querySelector('.practice-visual-card'); i++) await __wait(300); document.querySelector('.practice-visual-card').click(); for (let i = 0; i < 20 && !document.querySelector('[role=dialog]'); i++) await __wait(300)`,
    `const dialog = document.querySelector('[role=dialog]'); [...document.body.querySelectorAll('div')].filter((el) => el !== dialog && !el.contains(dialog) && !dialog.contains(el) && getComputedStyle(el).position === 'fixed').forEach((el) => { el.style.display = 'none' }); dialog.style.top = (dialog.offsetHeight / 2 + 690) + 'px'; dialog.style.left = '672px'; dialog.style.boxShadow = '0 0 0 1px #888'`,
  ],
  height: 1330,
  targets: ['.sidebar-skills-block', '.practice-pill-search', '.practice-segmented-tabs', '.practice-visual-card', '[role=dialog]', '.reading-pagination'],
})

// ---------- Reading and Listening workspaces, results, review, history ----------
const attemptId = uid(222)
const wav = (() => {
  const data = 8000
  const head = Buffer.alloc(44)
  head.write('RIFF', 0); head.writeUInt32LE(36 + data, 4); head.write('WAVEfmt ', 8); head.writeUInt32LE(16, 16)
  head.writeUInt16LE(1, 20); head.writeUInt16LE(1, 22); head.writeUInt32LE(8000, 24); head.writeUInt32LE(8000, 28)
  head.writeUInt16LE(1, 32); head.writeUInt16LE(8, 34); head.write('data', 36); head.writeUInt32LE(data, 40)
  return 'data:audio/wav;base64,' + Buffer.concat([head, Buffer.alloc(data, 128)]).toString('base64')
})()
const passage = [
  'Cities have always been warmer than the countryside around them. Dark roofs and roads soak up sunlight during the day and give the heat back at night.',
  'Planting trees is one of the cheapest ways to cool a street. Shade keeps pavements from heating up, and water leaving the leaves lowers the air temperature.',
  'Some cities now paint roofs white. Early results suggest that the rooms underneath need far less power for cooling in the hottest months.',
].join('\n\n')
const statements = [
  'City surfaces release stored heat after sunset.',
  'Planting trees is more expensive than painting roofs.',
  'White roofs reduce the energy needed for cooling.',
  'Planners usually apply a single measure to a whole city.',
  'Water from leaves affects the temperature of the air.',
]
const keys = ['TRUE', 'FALSE', 'TRUE', 'FALSE', 'TRUE']
const structureOf = (skill, extra) => ({
  sections: [{
    id: uid(101), contentSectionId: uid(201), sortOrder: 1, partNo: 1, startedAt: ago(8), completedAt: null,
    snapshot: {
      title: skill === 'READING' ? 'Keeping cities cool' : 'Booking a community hall', skill, partNo: 1, instructions: 'Answer Questions 1-5.',
      groups: [{ groupId: 'g1', sortOrder: 1, formatCode: 'TRUE_FALSE_NOT_GIVEN', instructions: 'Do the following statements agree with the information?' }],
      ...extra,
    },
    items: statements.map((stem, i) => ({
      id: uid(1000 + i), questionVersionId: uid(2000 + i), sortOrder: i + 1,
      questionSnapshot: JSON.stringify({ stem, groupId: 'g1', questionType: 'MULTIPLE_CHOICE', options: ['TRUE', 'FALSE', 'NOT GIVEN'].map((v) => ({ optionKey: v, content: v })) }),
    })),
  }],
})
const reading = structureOf('READING', { passage })
const listening = structureOf('LISTENING', { audio: { url: wav, durationSeconds: 372 } })
const responses = ['TRUE', 'TRUE', 'TRUE', 'FALSE'].map((answer, i) => ({ id: uid(3000 + i), attemptItemId: uid(1000 + i), payload: JSON.stringify({ answer }), schemaVersion: 1, revision: 1 }))
const attempt = (status) => ({ id: attemptId, packageVersionId: uid(600), scope: 'PART', mode: 'TIMED', status, startedAt: ago(17), submittedAt: status === 'SUBMITTED' ? ago(1) : null, expiresAt: new Date(Date.now() + 12 * 60000).toISOString() })
const attemptFixtures = (structure, status, answers = responses) => [
  [/GET \/api\/assessments\/attempts\/[^/]+$/, attempt(status)],
  [/GET \/api\/assessments\/attempts\/[^/]+\/structure/, structure],
  [/GET \/api\/assessments\/attempts\/[^/]+\/responses/, answers],
  [/(POST|PUT) \/api\/assessments\/attempts\/.*/, {}],
]
add({
  file: 'practice-tests-and-grading-reading-practice', url: `/practice/reading/attempts/${attemptId}`, root: '.rw-root', height: 700,
  fixtures: attemptFixtures(reading, 'IN_PROGRESS', responses.slice(0, 2)),
  targets: ['.rw-topbar', '.rw-passage', 'text=Questions 1', '.rw-qnav', '.rw-arrow-next', '.rw-footer-actions'],
})
add({
  file: 'practice-tests-and-grading-listening-practice', url: `/practice/listening/attempts/${attemptId}`, root: '.rw-root', height: 700,
  fixtures: attemptFixtures(listening, 'IN_PROGRESS', responses.slice(0, 2)),
  // Headless Edge refuses autoplay; the player is told that playback works.
  script: `HTMLMediaElement.prototype.play = function () { this.dispatchEvent(new Event('play')); this.dispatchEvent(new Event('playing')); return Promise.resolve() }; if (document.querySelector('.lw-exam-audio-start')) document.querySelector('.lw-exam-audio-start').click()`,
  targets: ['.rw-topbar', '.lw-exam-audio', 'text=Questions 1', '.rw-arrow-next', '.rw-footer-actions'],
})
const result = {
  id: uid(310), attemptId, resultVersion: 1, status: 'COMPLETED', completedAt: ago(1), score: 3, maxScore: 5, percent: 60,
  items: [true, false, true, true, null].map((correct, i) => ({ attemptItemId: uid(1000 + i), questionVersionId: uid(2000 + i), correct: correct ?? false })),
  solutions: keys.map((correctAnswer, i) => ({ attemptItemId: uid(1000 + i), questionVersionId: uid(2000 + i), correctAnswer, explanation: 'The first paragraph says that roads give the heat back at night.' })),
  practice: { scope: 'PART', skill: 'READING', mode: 'TIMED', startedAt: ago(17), submittedAt: ago(1), timeLimitSeconds: 1200, band: null,
    sections: [{ attemptSectionId: uid(101), partNo: 1, title: 'Keeping cities cool', questionCount: 5, timeSpentSeconds: 960, timeLimitSeconds: 1200 }] },
}
const resultFixtures = [[/GET \/api\/assessments\/attempts\/[^/]+\/result/, result], ...attemptFixtures(reading, 'SUBMITTED')]
add({
  file: 'practice-tests-and-grading-reading-listening-result', url: `/practice/reading/attempts/${attemptId}/result`, fixtures: resultFixtures,
  targets: ['.pb-nav', '.rr-top', null, '.rr-summary-actions .rr-primary'],
})
add({
  file: 'practice-tests-and-grading-answer-review', url: `/practice/reading/attempts/${attemptId}/review`, root: '.rv-root', height: 700, fixtures: resultFixtures,
  script: `document.querySelector('.rv-explain-button').click()`,
  targets: ['.rv-root .rw-topbar', '.rv-root .rw-passage', '.rv-root .rw-questions'],
})
const historyItem = (n, daysAgo, percent) => ({ attemptId: uid(4000 + n), packageId: uid(500 + n), packageVersionId: uid(600 + n), skill: 'READING', scope: 'PART', partNos: [1], status: 'SUBMITTED', submittedAt: ago(daysAgo * 1440), percent })
add({
  file: 'practice-tests-and-grading-reading-history', url: '/practice/reading/history',
  fixtures: [
    [/GET \/api\/assessments\/practice\/history/, { totalElements: 12, page: 0, size: 10, items: [historyItem(1, 1, 77), historyItem(2, 4, 62), historyItem(3, 9, 54)] }],
    [/POST \/api\/content\/practice-tests\/search/, { totalElements: 3, page: 0, size: 50, items: catalogCards }],
  ],
  targets: ['main h1', '.reading-history-list'],
})

// ---------- Writing workspace and result ----------
const task = {
  packageId: uid(510), packageVersionId: uid(610), sectionId: uid(710), questionVersionId: uid(2100), packageTitle: 'Public transport', title: 'Task 2: Public transport', partNo: 2, task: 'TASK_2', minWords: 250, timeLimitSeconds: 2400,
  instructions: 'Write at least 250 words.', prompt: 'Some people think that city governments should make public transport free. To what extent do you agree or disagree?', guide: null, sampleAnswer: null, images: [],
}
const essayText = 'Many cities are crowded with private cars. I strongly agree that free public transport would help to solve this problem.\n\nFirstly, people is more likely to leave their cars at home when a bus ride costs nothing. This reduce traffic and pollution.\n\nSecondly, low income families would save money every month and could reach more jobs.\n\nIn conclusion, the benefits of free transport are larger than its cost.'
add({
  file: 'practice-tests-and-grading-writing-workspace', url: `/practice/writing/tasks/${task.packageVersionId}/${task.sectionId}?mode=exam`, root: '.rw-root', height: 700,
  fixtures: [
    [/GET \/api\/content\/practice-tests\/[^/]+\/sections\/[^/]+\/writing-task/, task],
    [/GET \/api\/learning\/free-writing\/grading-terms/, { pointCost: 3, minWords: 50, maxWords: 600 }],
  ],
  script: `__fill('.ww-exam-answer textarea', ${JSON.stringify(essayText)})`,
  targets: ['.rw-root header', '.ww-exam-task', '.ww-exam-answer', '.rw-exam-arrows', '.rw-footer-actions'],
})
const criterion = (code, band, strength, improve, quote) => ({ code, band, strengths: [strength], improvements: [improve], evidence: [quote], descriptor: 'Addresses the task with a generally clear position.' })
const excerpt = (text, suggestion, category) => ({ excerpt: text, suggestion, category, start: essayText.indexOf(text), end: essayText.indexOf(text) + text.length, reason: 'Subject and verb must agree.', certainty: 'ERROR' })
const graded = {
  submissionId: uid(800), status: 'GRADED', task: 'TASK_2', wordCount: 262, overallBand: 6, passed: true, pointsCharged: 10, essayText,
  criteria: [
    criterion('TR', 6, 'The position is clear.', 'Develop the second idea.', 'I strongly agree'),
    criterion('CC', 6.5, 'Paragraphs follow a clear order.', 'Vary the linking words.', 'Firstly'),
    criterion('LR', 6, 'Topic words are used well.', 'Avoid repeating the same noun.', 'public transport'),
    criterion('GRA', 5.5, 'Simple sentences are accurate.', 'Check subject and verb agreement.', 'This reduce traffic'),
  ],
  corrections: [excerpt('people is', 'people are', 'GRAMMAR'), excerpt('This reduce', 'This reduces', 'GRAMMAR')],
  summary: 'A clear answer. Work on grammar accuracy to reach the next band.', sampleAnswer: 'Free public transport is often proposed as a way to reduce traffic...',
}
add({
  file: 'practice-tests-and-grading-writing-result', url: `/practice/writing/submissions/${graded.submissionId}/result`,
  fixtures: [
    [/GET \/api\/learning\/writing-submissions\/[^/]+$/, graded],
    [/GET \/api\/learning\/free-writing\/grading-terms/, { pointCost: 3, minWords: 50, maxWords: 600 }],
  ],
  targets: ['.wr-hero', '.wr-criteria', '.wr-essay', '.wr-metrics', '.wr-actions .wr-secondary', 'details.wr-sample > summary', null],
})

// ---------- Grading job, examiner reviews, submission history ----------
const reviewItem = (n, preview, days) => ({ submissionId: uid(800 + n), reviewSubmissionId: uid(850 + n), source: 'FREE_WRITING', task: 'TASK_2', promptPreview: preview, wordCount: 255 + n * 6, requestedAt: ago(days * 1440) })
const job = (n, state, extra = {}) => ({ id: uid(900 + n), reviewSubmissionId: uid(850 + n), skill: 'WRITING', source: 'FREE_WRITING', state, examinerId: uid(77), requestedAt: ago(2000), deadline: new Date(Date.now() + 86400000).toISOString(), canChoose: false, canCancel: false, changesLeft: 2, createdAt: ago(2000), band: state === 'COMPLETED' ? 6.5 : null, completedAt: state === 'COMPLETED' ? ago(300) : null, ...extra })
const jobs = [job(1, 'COMPLETED'), job(2, 'WAITING')]
const reviews = { items: [reviewItem(1, 'Some people think that public transport should be free. To what extent do you agree?', 3), reviewItem(2, 'Many young people move to large cities. Is this a positive or negative development?', 1)], page: 0, size: 10, hasNext: false }
const reviewFixtures = [
  [/GET \/api\/users\/examiners\/[^/?]+$/, { id: uid(77), displayName: 'Minh Tran', avatarUrl: null, bio: 'IELTS writing examiner.', ieltsBand: 8, certificates: [], acceptingSubmissions: true }],
  [/GET \/api\/assessments\/grading-jobs\/mine/, jobs],
  [/GET \/api\/learning\/examiner-reviews\?/, reviews],
]
add({
  file: 'practice-tests-and-grading-grading-job', url: `/grading-jobs/${uid(901)}`, fixtures: reviewFixtures,
  targets: ['.we-waiting section[class=wr-card]:last-of-type', null, '.wr-actions .wr-primary', null],
})
add({
  file: 'practice-tests-and-grading-examiner-reviews', url: '/writing-reviews', fixtures: reviewFixtures, script: noStretch,
  targets: ['.wl-title', null, '.sh-tabs', '.wl-rows'],
})
const past = (n, preview, days, band) => ({ kind: 'FREE_WRITING', id: uid(800 + n), submittedAt: ago(days * 1440), skill: 'WRITING', task: n % 2 ? 'TASK_2' : 'TASK_1', promptPreview: preview, band, status: 'GRADED', revisionNo: 0 })
add({
  file: 'practice-tests-and-grading-submission-history', url: '/submission-history?tab=practice&skill=writing', script: noStretch,
  fixtures: [
    [/GET \/api\/learning\/submission-history\?/, { items: [past(1, 'Public transport should be free. Do you agree?', 1, 6), past(2, 'The chart shows household water use in three countries.', 5, 6.5), past(3, 'Some people prefer to work from home. Discuss both views.', 12, 5.5)], page: 0, size: 10, hasNext: false }],
  ],
  targets: ['h1', '.sh-skills', '.sh-tabs', '#sh-panel ol, #sh-panel ul, #sh-panel table'],
})

// ---------- Speaking workspace and review ----------
const speakingTask = {
  packageId: uid(520), packageVersionId: uid(620), sectionId: uid(720), packageTitle: 'Daily life', title: 'Part 1: Daily routine', partNo: 1, timeLimitSeconds: 300, instructions: 'Answer each question in two or three sentences.',
  questions: ['What time do you usually get up?', 'What do you like to do after work or school?'].map((prompt, i) => ({ questionVersionId: uid(2200 + i), sortOrder: i + 1, prompt, durationSeconds: 30 })),
}
const speakingFixtures = [
  [/GET \/api\/content\/practice-tests\/[^/]+\/sections\/[^/]+\/speaking-task/, speakingTask],
  [/POST \/api\/learning\/practice-sessions/, {}],
  [/GET \/api\/learning\/speaking-practice\/terms/, { enabled: true, pointCost: 5, maxRecordingBytes: 5000000, retentionDays: 90 }],
]
const speakingUrl = `/practice/speaking/tasks/${speakingTask.packageVersionId}/${speakingTask.sectionId}`
add({
  file: 'practice-tests-and-grading-speaking-workspace', url: speakingUrl, height: 700, fixtures: speakingFixtures,
  targets: ['.skill-page-topbar', '.sq-card', '.sr-card', '.sq-next'],
})
// Headless Edge has no microphone: a stand-in recorder returns a short clip, so that the answers can be reviewed.
const fakeMic = `const bytes = Uint8Array.from(atob(${JSON.stringify(wav.split(',')[1])}), (c) => c.charCodeAt(0));
  navigator.mediaDevices.getUserMedia = async () => new AudioContext().createMediaStreamDestination().stream;
  AnalyserNode.prototype.getByteFrequencyData = (a) => a.fill(150);
  window.MediaRecorder = class { constructor() { this.state = 'inactive'; this.mimeType = 'audio/wav' } static isTypeSupported() { return true }
    start() { this.state = 'recording' } stop() { this.state = 'inactive'; this.ondataavailable?.({ data: new Blob([bytes], { type: 'audio/wav' }) }); setTimeout(() => this.onstop?.(), 50) } requestData() {} }`
const record = `const orb = () => document.querySelector('.sr-orb'); orb().click(); await __wait(1500); for (let i = 0; i < 40 && orb().disabled; i++) await __wait(1000); orb().click(); await __wait(2500)`
add({
  file: 'practice-tests-and-grading-speaking-review', url: speakingUrl, height: 1200, fixtures: speakingFixtures,
  script: [
    fakeMic, record, `document.querySelector('.sq-next').click()`, record,
    `for (let i = 0; i < 30 && !document.querySelector('.sq-finish:not([disabled])'); i++) await __wait(1000); document.querySelector('.sq-finish').click()`,
    `await __wait(1000); document.querySelector('.sv-continue button').click(); for (let i = 0; i < 20 && !document.querySelector('[role=dialog]'); i++) await __wait(300); await __wait(600)`,
    // The confirm window is moved under the list and drawn without its dimmed backdrop.
    `const dialog = document.querySelector('[role=dialog]'); [...document.body.children].filter((el) => el !== dialog && el.id !== 'root' && !el.contains(dialog) && getComputedStyle(el).position === 'fixed').forEach((el) => { el.style.display = 'none' }); const stage = document.querySelector('.sv-stage'); stage.style.alignContent = 'start'; stage.style.alignItems = 'flex-start'; stage.style.justifyContent = 'flex-start'; stage.style.flexDirection = 'column'; stage.style.alignItems = 'center'; document.querySelector('.sv-card').style.margin = '0 auto'; await __wait(300); dialog.style.top = (document.querySelector('.sv-card').getBoundingClientRect().bottom + 20 + dialog.offsetHeight / 2) + 'px'; const box = dialog.querySelector('input[type=checkbox], [role=checkbox]'); if (box) box.closest('label')?.setAttribute('data-consent', '')`,
  ],
  targets: ['.sv-head', '.sv-scroll', 'label[data-consent]', '[role=dialog] button:last-of-type', '.sv-back'],
})

// ---------- Speaking result ----------
const speakingCriterion = (code, name, band, comment) => ({ code, name, band, comment, strengths: ['Answers stay on the topic.'], weaknesses: ['Pauses while looking for words.'], evidence: [{ quote: 'I usually get up at, um, six', note: 'Hesitation before the time.' }] })
add({
  file: 'practice-tests-and-grading-speaking-result', url: `/practice/speaking/submissions/${uid(830)}/result`, height: 2300,
  fixtures: [[/GET \/api\/learning\/speaking-practice\/submissions\/[^/]+$/, {
    id: uid(830), packageVersionId: uid(620), scope: 'PART', status: 'GRADED', pointCost: 5, failureCode: null, createdAt: ago(60), recordings: [],
    result: {
      overallBand: 6,
      criteria: [
        speakingCriterion('FC', 'Fluency', 5.5, 'Frequent short pauses break the flow.'), speakingCriterion('LR', 'Vocabulary', 6, 'Enough words for familiar topics.'),
        speakingCriterion('GRA', 'Grammar', 6, 'Simple forms are mostly correct.'), speakingCriterion('P', 'Pronunciation', 6.5, 'Easy to understand.'),
      ],
      summary: 'A clear answer on familiar topics.', focus: ['Speak in longer turns without stopping.'], studyPlan: ['Record a one-minute answer each day.'],
      personalFeedback: { note: 'Fluency keeps you at this band.', studyPlan: ['Record a one-minute answer each day.'] },
      partNotes: [{ partNo: 1, note: 'Good points: direct answers. To improve: add a reason, for example after "I usually get up at six".' }],
      transcripts: [{ partNo: 1, questionNo: 1, question: 'What time do you usually get up?', transcript: 'I usually get up at, um, six because my class start early.', words: [] }],
      errors: [{ name: 'Subject and verb agreement', partNo: 1, excerpt: 'my class start early', suggestion: 'my class starts early' }],
      pronunciation: { accuracy: 78, fluency: 70, pronunciation: 75, weakWords: [{ word: 'usually', accuracy: 52, sounds: [], tip: 'Say three syllables.' }] },
      rhythm: { wordsPerMinute: 96, longPauses: 3 },
    },
  }]],
  targets: ['.sres-dash', '.sres-next', '[aria-label="Nhận xét theo part"]', '.sres-transcripts', '.sres-reference', '.sres-back'],
})

// ---------- Learning path: lesson, no hearts, practice set ----------
const courseId = uid(5000)
const topicId = uid(5001)
const lessonId = uid(5002)
const course = { courseId, code: 'B55', name: 'Band 5.5 course', bandLevel: 5.5 }
const topic = { topicId, code: 'R1', name: 'Reading foundations', description: 'Core reading skills.', sequenceOrder: 1, status: 'IN_PROGRESS', completedLessonCount: 0, totalLessonCount: 2, skill: 'READING', course }
const summary = (n, id, title, status, practiceStatus) => ({ lessonId: id, code: `R1-${n}`, title, sortOrder: n, status, practiceStatus, estimatedMinutes: 15 })
const choice = (n, stem) => ({ questionVersionId: uid(5100 + n), sortOrder: n, stem, options: ['TRUE', 'FALSE', 'NOT GIVEN'].map((v, i) => ({ optionKey: v, content: v, sortOrder: i })) })
const questions = [choice(1, 'Planting trees can lower the temperature of a street.'), choice(2, 'Painting roofs white costs more than planting trees.')]
const heart = (hearts) => ({ hearts, maxHearts: 5, nextHeartAt: hearts < 5 ? new Date(Date.now() + 3 * 3600000 + 25 * 60000).toISOString() : null, refillCost: 5 })
const lessonFixtures = (lessonStatus, practiceStatus, hearts = 5, reviewsWaiting = []) => [
  [/GET \/api\/learning\/topics$/, [topic]],
  [/GET \/api\/learning\/topics\/[^/]+\/lessons/, { topicId, lessons: [summary(1, lessonId, 'Recognising paraphrase', lessonStatus, practiceStatus), summary(2, uid(5003), 'Scanning for details', 'LOCKED', null)], testStatus: 'LOCKED' }],
  [/GET \/api\/learning\/lessons\/[^/]+\/resources/, { lessonId, slides: [], summaryMarkdown: '## Paraphrase\nThe question says the same idea with other words.', documents: [] }],
  [/GET \/api\/learning\/lessons\/[^/]+\/practice-sets/, { lessonId, skill: 'READING', lessonCompleted: false, practiceStatus, practicePassReason: null, items: [{ packageId: uid(5200), code: 'PS-01', title: 'Paraphrase practice: city trees', questionCount: 2, accessLevel: 'FREE', status: 'AVAILABLE', bestPercent: null, lastAttemptId: null, revealed: false, skills: ['READING'] }] }],
  [/GET \/api\/learning\/lessons\/[^/]+$/, { lessonId, topicId, code: 'R1-1', title: 'Recognising paraphrase', sortOrder: 1, status: lessonStatus, blocks: [
    { blockId: uid(5301), blockType: 'ASSET', sortOrder: 1, asset: { assetType: 'PASSAGE', textContent: passage } },
    { blockId: uid(5302), blockType: 'EXERCISE', blockKind: 'EXERCISE', sortOrder: 2, questions },
  ] }],
  [/GET \/api\/learning\/reviews\?/, reviewsWaiting],
  [/GET \/api\/learning\/me\/hearts/, heart(hearts)],
  [/GET \/api\/learning\/review-basket\/summary/, { required: 0, due: 1 }],
]

const practiceAttempt = { attemptId: uid(5500), packageId: uid(5200), packageVersionId: uid(5201), passage: passage, audio: { mediaUrl: wav, durationSeconds: 95, transcript: 'Today we talk about cooling cities.' }, questions }
const practiceFixtures = [
  [/POST \/api\/learning\/lessons\/[^/]+\/practice-attempts/, practiceAttempt],
]
add({
  file: 'learning-path-practice-set', url: `/learn/lessons/${lessonId}?view=practice`, width: 1360,
  fixtures: [...practiceFixtures, ...lessonFixtures('IN_PROGRESS', 'REQUIRED')],
  script: `for (let i = 0; i < 30 && !document.querySelector('.lp-practice-card__action'); i++) await __wait(300); document.querySelector('.lp-practice-card__action').click(); await __wait(1500)`,
  targets: ['.lp-trail', '.lp-practice-attempt-head', '.lp-practice .lp-split', '.lp-exercise__actions button[type=submit]', null, null],
})
// A review only becomes "waiting" after a practice submission, so the lesson is reached through its practice tab.
const answerAll = `for (let i = 0; i < 30 && !document.querySelector('.lp-practice-card__actions button'); i++) await __wait(300); document.querySelector('.lp-practice-card__actions button').click(); await __wait(1500); document.querySelectorAll('.lp-questions > li').forEach((li) => li.querySelector('[role=radio]').click()); await __wait(500); document.querySelector('.lp-exercise__actions button[type=submit]').click(); await __wait(1500)`
add({
  file: 'learning-path-lesson', url: `/learn/lessons/${lessonId}?view=practice`, width: 1360,
  fixtures: [
    ...practiceFixtures,
    [/POST \/api\/learning\/practice-attempts\/[^/]+\/submissions/, { attemptId: uid(5500), correct: 0, total: 2, percent: 0, passed: false, countedAsEvidence: true, results: questions.map((q) => ({ questionVersionId: q.questionVersionId, correct: false })), reviewsCreated: [{ reviewId: uid(5400), knowledgePointId: uid(5401), blocking: true }] }],
    ...lessonFixtures('COMPLETED', 'REQUIRED', 5, [{ reviewId: uid(5400), lessonId, knowledgePointId: uid(5401), blocking: true }]),
  ],
  script: [answerAll, `document.querySelector('.lp-res-nav a[href$="view=summary"]').click(); await __wait(1500)`, `document.querySelectorAll('.lp-lesson-page [role=status].lp-hint').forEach((el) => el.remove())`],
  targets: ['.lp-trail', '.lp-res-nav', '.lp-res-card', '.lp-done--review', '.lp-res-nav a[href$="view=exercises"]', '.lp-res-nav a[href$="view=practice"]', null],
})
add({
  file: 'learning-path-no-hearts-window', url: `/learn/lessons/${lessonId}?view=practice`, root: '[role=dialog]', backdrop: true, height: 600,
  fixtures: [[/POST \/api\/learning\/lessons\/[^/]+\/practice-attempts/, { code: 'HEARTS_DEPLETED', message: 'No hearts left', refillCost: 5 }, 409], ...lessonFixtures('IN_PROGRESS', 'REQUIRED', 0)],
  script: `for (let i = 0; i < 30 && !document.querySelector('.lp-practice-card__actions button'); i++) await __wait(300); document.querySelector('.lp-practice-card__actions button').click(); for (let i = 0; i < 20 && !document.querySelector('[role=dialog]'); i++) await __wait(300); await __wait(900)`,
  targets: ['[role=dialog] h2', '[role=dialog] h2 + p', '[role=dialog] .grid button', '[role=dialog] .grid button:last-child'],
})

// ---------- Topic test and its result ----------
const testSection = (n, title) => ({
  id: uid(5600 + n), contentSectionId: uid(5610 + n), sortOrder: n, partNo: n,
  snapshot: JSON.stringify({ title, instructions: 'Choose the correct answer.', skill: 'READING', passage }),
  items: questions.map((q, i) => ({ id: uid(5700 + n * 10 + i), questionVersionId: q.questionVersionId, sortOrder: i + 1, questionSnapshot: JSON.stringify({ stem: q.stem, options: q.options }) })),
})
add({
  file: 'learning-path-topic-test', url: `/learn/tests/${uid(5800)}?topic=${topicId}`, width: 1200,
  fixtures: [
    [/GET \/api\/assessments\/attempts\/[^/]+\/structure/, { sections: [testSection(1, 'City cooling'), testSection(2, 'Urban trees'), testSection(3, 'White roofs')] }],
    [/PUT \/api\/assessments\/attempts\/.*/, { id: uid(5801), attemptItemId: uid(5710), payload: '{}', schemaVersion: 1, revision: 1 }],
    ...lessonFixtures('IN_PROGRESS', 'REQUIRED'),
  ],
  // One answer is chosen so that the save message shows, and the second part is opened to show both buttons.
  script: [`document.querySelector('.lp-sections__tab:nth-child(2)').click(); await __wait(600); document.querySelector('.lp-questions [role=radio]').click(); await __wait(800)`],
  targets: ['.lp-test-head', '.lp-sections', '.lp-split--test', '.lp-exercise__actions', '.lp-test-bar button'],
})
const nextTopic = { ...topic, topicId: uid(5900), code: 'R2', name: 'True / False / Not Given skills', sequenceOrder: 2 }
add({
  file: 'learning-path-topic-test-result', url: `/learn/tests/${uid(5800)}/result?topic=${topicId}`,
  fixtures: [
    [/GET \/api\/assessments\/attempts\/[^/]+\/result/, { id: uid(5810), attemptId: uid(5800), status: 'COMPLETED', score: 5, maxScore: 6, percent: 83, items: [true, true, false, true, true, true].map((correct, i) => ({ attemptItemId: uid(5710 + i), questionVersionId: uid(5101 + i), correct })), solutions: [0, 1, 2, 3, 4, 5].map((i) => ({ questionVersionId: uid(5101 + i), correctAnswer: 'TRUE', explanation: 'The first paragraph states this.' })) }],
    [/GET \/api\/learning\/topics$/, [{ ...topic, status: 'PASSED', completedLessonCount: 2 }, nextTopic]],
    [/GET \/api\/learning\/courses/, []],
    ...lessonFixtures('COMPLETED', 'PASSED'),
  ],
  targets: [null, '.lp-score', '.lp-result-items', '.lp-score__actions a:first-child', '.lp-score__actions a:last-child'],
})

// ---------- Review and prerequisite check ----------
const reviewId = uid(5400)
const probeSession = uid(6000)
const reviewDetail = {
  reviewId, reviewStatus: 'PENDING', lessonId, theory: ['## Paraphrase\nThe question says the same idea with other words.'], stage: 'PRACTICE', failedSets: 2, maxFailedSets: 3, knowledgePointId: uid(5401), skill: 'READING', blocking: true,
  state: 'DIAGNOSING', diagnosticSessionId: probeSession,
  set: { reviewSetId: uid(6001), packageId: uid(6002), packageVersionId: uid(6003), passage, audio: { mediaUrl: wav, durationSeconds: 60 }, questions },
}
add({
  file: 'learning-path-review', url: `/learn/reviews/${reviewId}`, width: 1200,
  targets: ['.lp-review-head', '.lp-theory', '.lp-split', '.lp-exercise__actions button[type=submit]', '.lp-diagnosis'],
  fixtures: [
    [/GET \/api\/learning\/reviews\/[^/?]+$/, reviewDetail],
    [/GET \/api\/learning\/diagnostics\/[^/]+$/, { sessionId: probeSession, rootReviewId: reviewId, status: 'PROBING', probes: [{ probeId: uid(6004), knowledgePoint: { knowledgePointId: uid(6005), code: 'R-SYN', name: 'Recognising synonyms' }, depth: 1, status: 'OPEN' }], conclusion: [], waitingOnReviewIds: [] }],
    ...lessonFixtures('IN_PROGRESS', 'REQUIRED'),
  ],
})
add({
  file: 'learning-path-prerequisite-check', url: `/learn/reviews/${reviewId}/diagnostics/${probeSession}/probes/${uid(6004)}`, width: 1200,
  targets: ['.lp-review-head', '.lp-split', '.lp-exercise__actions button[type=submit]', null],
  fixtures: [
    [/GET \/api\/learning\/diagnostics\/[^/]+\/probes\/[^/]+$/, { sessionId: probeSession, probeId: uid(6004), knowledgePointId: uid(6005), status: 'OPEN', passage, audio: { mediaUrl: wav, durationSeconds: 60 }, questions }],
    ...lessonFixtures('IN_PROGRESS', 'REQUIRED'),
  ],
})

// ---------- Placement test ----------
const placementSection = (n, skill, title) => ({
  id: uid(6100 + n), contentSectionId: uid(6110 + n), sortOrder: n, partNo: 1, startedAt: null, completedAt: null,
  snapshot: JSON.stringify({ title, instructions: 'Answer the questions.', skill, partNo: 1, ...(skill === 'LISTENING' ? { audio: { url: wav, durationSeconds: 60 } } : { passage }) }),
  items: questions.map((q, i) => ({ id: uid(6200 + n * 10 + i), questionVersionId: q.questionVersionId, sortOrder: i + 1, questionSnapshot: JSON.stringify({ stem: q.stem, options: q.options }) })),
})
add({
  file: 'learning-path-placement-test', url: '/learn/placement', height: 700,
  fixtures: [
    [/GET \/api\/assessments\/attempts\/placement\/current/, { id: uid(6300), userId: uid(1), packageVersionId: uid(6301), attemptType: 'PLACEMENT', mode: 'STANDARD', channel: 'WEB', status: 'IN_PROGRESS', startedAt: ago(12), submittedAt: null, expiresAt: null, rowVersion: 1, createdAt: ago(12), updatedAt: ago(1) }],
    [/GET \/api\/assessments\/attempts\/[^/]+\/structure/, { sections: [placementSection(1, 'LISTENING', 'Part 1: Booking a hall'), placementSection(2, 'READING', 'Passage 1: Keeping cities cool'), placementSection(3, 'WRITING', 'Task 1: A chart'), placementSection(4, 'SPEAKING', 'Part 1: Daily life')] }],
    [/GET \/api\/assessments\/attempts\/[^/]+\/responses/, []],
    [/(POST|PUT) \/api\/assessments\/attempts\/.*/, {}],
    [/GET \/api\/learning\/placement-test\/options/, { pools: [], limitPer24h: 2, retakesLeft: 1, nextRetakeAt: null, hasPlacement: false, currentBand: null, openAssignment: null }],
    ...lessonFixtures('IN_PROGRESS', 'REQUIRED'),
  ],
  script: [
    `__click('Tiếp tục'); await __wait(2000)`,
    `const row = [...document.querySelectorAll('.pl-parts__row')][1]; row.querySelector('button').click(); await __wait(2000)`,
  ],
  targets: [null, null, '.pl-qbar', '.pl-exam__body', '.pl-qbar__submit', null],
})
