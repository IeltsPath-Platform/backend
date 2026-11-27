// Staff screens: examiner, administrator and key sales. All data is invented.
import { signedIn, page, uid, person } from './common.mjs'
export const common = signedIn('ADMIN')
export const patterns = [
  ['Còn (\\d+) ngày', '$1 days left'], ['Còn (\\d+) giờ', '$1 hours left'],
  ['Quá hạn (\\d+) giờ', '$1 h overdue'], ['Quá hạn (\\d+) ngày', '$1 days overdue'],
  ['chấm lúc ', 'graded on '], ['^Bài học · ', 'Lesson · '], ['\\. Chấm sớm kẻo học viên đổi giáo viên\\.', '. Grade soon or the Learner may switch.'],
  [' là đến hạn chấm\\.', ' until the deadline.'], ['Bạn đang chấm dở và đã lưu nháp · còn 1 ngày\\.', 'Started, draft saved · 1 day left.'], ['^Còn$', 'Another'], ['^Kiểm tra lúc ', 'Checked at '], ['^hạn dùng ', 'expires '], ['^hạn ', 'expires '], ['\\bTrang\\b', 'Page'], ['1\\.000', '1,000'], ['^(\\d+) từ$', '$1 words'],
]

// A made-up id whose first six characters give a readable essay code.
const jid = (head) => `${head}0000-1111-4111-8111-111111111111`
const hoursAgo = (h) => new Date(Date.now() - h * 3600000).toISOString()
const daysAgo = (d) => new Date(Date.now() - d * 86400000).toISOString()

// ---------- examiner ----------
const job = (head, task, status, hours, extra = {}) => ({
  id: jid(head), attemptId: null, attemptType: null, source: head < 'c' ? 'LESSON' : 'PRACTICE', skill: 'WRITING', task, status,
  claimedAt: status === 'PROCESSING' ? hoursAgo(2) : null, createdAt: hoursAgo(hours), waitSeconds: hours * 3600, overdue: hours > 48, hasDraft: false, ...extra,
})
const queued = [job('a1f3', 'TASK_2', 'QUEUED', 52), job('b7c2', 'TASK_1', 'QUEUED', 39), job('c4d8', 'TASK_2', 'QUEUED', 11)]
const processing = [job('d9e5', 'TASK_2', 'PROCESSING', 20, { hasDraft: true })]
const jobPage = (content) => ({ content, page: 0, size: 100, totalElements: content.length })
const done = [['e2a1', 'TASK_2', 6.5, 0], ['f5b3', 'TASK_1', 6, 1], ['a8c4', 'TASK_2', 7, 1], ['b3d6', 'TASK_2', 5.5, 3], ['c6e7', 'TASK_1', 6.5, 5]]
  .map(([head, task, band, days], i) => ({ jobId: jid(head), source: i % 2 ? 'PRACTICE' : 'LESSON', skill: 'WRITING', task, band, completedAt: daysAgo(days) }))
const summary = (extra = {}) => ({ queued: 3, processing: 1, dueSoon: 1, overdue: 1, completedToday: 1, completedThisWeek: 5, receivedToday: 2, dailyLimit: 5, overdueLimit: 3, ...extra })
const examinerProfile = {
  id: uid(1), displayName: 'An Nguyen', avatarUrl: null, bio: '', ieltsBand: 8, acceptingSubmissions: true,
  certificates: [{ name: 'IELTS Academic', issuer: 'Sample Test Centre', year: 2025, score: '8.0' }, { name: 'TESOL', issuer: 'City Teaching Institute', year: 2023, score: 'Pass' }],
}
const examiner = (extraSummary) => [
  [/GET \/api\/assessments\/examiners\/me\/summary/, summary(extraSummary)],
  [/GET \/api\/assessments\/grading-jobs\?.*status=QUEUED/, jobPage(queued)],
  [/GET \/api\/assessments\/grading-jobs\?.*status=PROCESSING/, jobPage(processing)],
  [/GET \/api\/assessments\/examiners\/me\/completed\?/, { content: done, page: 0, size: 100, totalElements: done.length }],
  [/GET \/api\/users\/examiners\//, examinerProfile],
  [/GET \/api\/notifications\?/, page([])],
  ...signedIn('EXAMINER'),
]

const prompt = 'Some people think that cities should build more cycle lanes instead of new roads. To what extent do you agree or disagree?'
const essay = 'Many cities are crowded with cars today. In my opinion, building more cycle lanes is a sensible idea, but it cannot fully replace new roads.\n\nFirstly, cycle lanes helps people to travel cheaply. When lanes are safe, more residents choose a bicycle for short trips, which reduce traffic and air pollution.\n\nHowever, roads are still necessary for buses, ambulances and goods. A city who only builds cycle lanes may make delivery slower.\n\nIn conclusion, governments should give more space to cyclists while keeping important roads in good condition.'
const mark = (excerpt, errorCode, suggestion) => ({ excerpt, start: essay.indexOf(excerpt), end: essay.indexOf(excerpt) + excerpt.length, errorCode, suggestion })
const marks = [mark('cycle lanes helps', 'SUBJECT_VERB', 'cycle lanes help'), mark('A city who', 'RELATIVE_PRONOUN', 'A city that')]
const errorType = (n, code, name, category) => ({ id: uid(300 + n), code, name, category, severity: 2, knowledgePointId: null, fixHint: 'Check the form.', status: 'ACTIVE', reviewStatus: 'APPROVED' })
const errorTypes = [errorType(1, 'SUBJECT_VERB', 'Subject-verb agreement', 'GRAMMAR'), errorType(2, 'RELATIVE_PRONOUN', 'Relative pronoun', 'GRAMMAR')]
const criteria = [
  ['taskResponse', 6.5, 'Clear position with two relevant ideas; develop the second one further.'],
  ['coherence', 7, 'Logical paragraphs and clear linking words.'],
  ['lexical', 6, 'Good topic words; some repetition of "cycle lanes".'],
  ['grammar', 6, 'Check subject-verb agreement and relative pronouns.'],
].map(([criterion, band, comment]) => ({ criterion, band, comment }))
const feedback = 'A clear and well organised answer. Fix agreement errors first, then practise giving one concrete example in each body paragraph.'
const chart = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="420" height="160"><rect width="420" height="160" fill="#eee"/></svg>')

const staff = (file, url, fixtures, extra = {}) => ({ file, url, fixtures, wait: 15000, ...extra })

const examinerScreens = [
  staff('grading-by-examiners-examiner-dashboard', '/examiner', examiner({ receivedToday: 5 }),
    { targets: ['.ex-dashboard', 'text=Do now', '.ov-panels', '.ex-note--warn'] }),
  staff('grading-by-examiners-writing-queue', '/examiner/grading/writing', examiner(),
    { words: { 'Bài': 'Essay', 'bài': 'essays' }, targets: ['h1', '[aria-label="Choose the list"], [aria-label="Chọn danh sách"]', '.adm-eg-search', '.adm-eg-filter', '.adm-table'] }),
  staff('grading-by-examiners-writing-grading', `/examiner/grading/writing/${jid('d9e5')}`, [
    [/POST \/api\/assessments\/grading-jobs\/[^/]+\/claim/, processing[0]],
    [/GET \/api\/assessments\/grading-jobs\/[^/]+\/essay/, { jobId: jid('d9e5'), attemptId: null, prompt, images: [], essay, task: 'TASK_2', source: 'PRACTICE' }],
    [/GET \/api\/assessments\/grading-jobs\/[^/]+\/human-review\/draft/, { payload: { criteria, feedback, annotations: marks }, savedAt: hoursAgo(1) }],
    [/GET \/api\/content\/error-types/, errorTypes],
    ...examiner(),
  ], { targets: ['.adm-titlebar__actions a', 'h1', '.eg-pane--left', '.eg-criterion:nth-of-type(4)', '.eg-criterion--overall', '.eg-actionbar .adm-btn--primary'], height: 1100, script: `const el = document.querySelector('.eg-criterion:nth-of-type(4)'); const pane = el.closest('.eg-pane'); pane.scrollTop += el.getBoundingClientRect().top - pane.getBoundingClientRect().top` }),
  staff('grading-by-examiners-graded-review', `/examiner/grading/writing/${jid('e2a1')}/review`, [
    [/GET \/api\/assessments\/examiners\/me\/completed\/[^?]+$/, { jobId: jid('e2a1'), source: 'LESSON', skill: 'WRITING', task: 'TASK_2', prompt, images: [{ url: chart, altText: 'Sample chart' }], essay, band: 6.5, criteria, errorCodes: marks.map((m) => m.errorCode), feedback, annotations: marks, completedAt: daysAgo(0) }],
    ...examiner(),
  ], { targets: ['.adm-titlebar__actions a', 'h1', '.eg-pane--left', '.eg-result', '.eg-split-tools'] }),
  staff('grading-by-examiners-speaking-grading', '/examiner/grading/speaking', examiner(), { targets: ['h1', null, null, null] }),
  staff('grading-by-examiners-examiner-profile', '/examiner/profile', examiner(),
    { targets: ['.ex-form__actions a', 'h1', '.ex-form select', '.ex-switch', '.ex-cert', '.ex-form__actions button'] }),
]

// ---------- administrator ----------
const account = (n, name, mail, role, status) => person(n, name, mail, role, status)
const users = [
  account(1, 'An Nguyen', 'an.nguyen@example.com', 'ADMIN'),
  account(2, 'Sam Writer', 'sam.writer@example.com', 'CONTENT_AUTHOR'),
  account(3, 'Erin Grader', 'erin.grader@example.com', 'EXAMINER'),
  account(4, 'Max Manager', 'max.manager@example.com', 'STAFF_MANAGER'),
  account(5, 'Sally Seller', 'sally.seller@example.com', 'SALES_STAFF'),
  account(6, 'Leo Learner', 'leo.learner@example.com', 'CUSTOMER', 'LOCKED'),
]
const instance = (name, ms) => ({ instanceId: `${name}-1`, uri: `http://${name}.example.internal:8000`, status: 'UP', responseMs: ms })
const service = (id, ms) => ({ serviceId: id, status: 'UP', instances: [instance(id.toLowerCase().replace(/-service$/, ''), ms)] })
const snapshot = { checkedAt: new Date().toISOString(), services: [service('USER-SERVICE', 18), service('CONTENT-SERVICE', 24), service('LEARNING-SERVICE', 31), service('ASSESSMENT-SERVICE', 27), service('ACCESS-SERVICE', 22)] }
const day = (back) => new Date(Date.now() - back * 86400000).toISOString().slice(0, 10)
const activity = {
  from: day(29), to: day(0),
  daily: Array.from({ length: 30 }, (_, i) => ({ day: day(29 - i), dau: 12 + ((i * 7) % 19), wau: 60 + i, mau: 140 + i * 2 })),
  retention: [[70, 40, 0.62, 0.5, 0.38, 0.25], [56, 35, 0.6, 0.46, 0.31, null], [28, 48, 0.65, 0.52, null, null], [14, 52, 0.58, null, null, null]]
    .map(([back, size, week1, week2, week4, week8]) => ({ weekStart: day(back), size, week1, week2, week4, week8 })),
}
const userPage = { content: users, page: 0, size: 25, totalElements: users.length, totalPages: 1 }
const admin = [
  [/GET \/api\/users\/admin\/all/, users],
  [/GET \/api\/users\?/, userPage],
  [/GET \/api\/users\/[^/]+\/login-history/, { items: [
    [1, true, null], [2, false, 'BAD_CREDENTIALS'], [26, true, null], [50, true, null], [75, false, 'ACCOUNT_LOCKED'],
  ].map(([hours, success, failureReason], i) => ({ id: uid(500 + i), success, failureReason, ipAddress: `192.0.2.${10 + i}`, userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0 Safari/537.36', createdAt: hoursAgo(hours) })), page: 0, size: 20, total: 5 }],
  [/GET \/api\/admin\/system\/services/, snapshot],
  [/GET \/api\/learning\/admin\/activity-stats/, activity],
  [/GET \/api\/notifications\?/, page([])],
  ...signedIn('ADMIN'),
]
// The "all rows shown" footer wraps one letter per line in the real page, so it is hidden.
const noFooterNote = `const el = [...document.querySelectorAll('body *')].filter((e) => e.textContent.trim().startsWith('Đã hiện tất cả')).pop(); if (el) el.style.display = 'none'`
const rowMenu = (item) => [noFooterNote, `__click('Hành động của sam.writer'); await __wait(500); __click('${item}')`]
const modal = (file, url, fixtures, extra) => staff(file, url, fixtures, { root: '[role=dialog]', backdrop: true, height: 800, ...extra })

const adminScreens = [
  staff('administration-administrator-home', '/admin', admin, { targets: ['h1', '.sys-summary-grid'] }),
  staff('administration-users', '/admin/users', admin, { script: noFooterNote, targets: ['h1', '.adm-titlebar__actions button', '.um-filters', '.um-table'] }),
  modal('administration-user-actions-window', '/admin/users', admin, { script: rowMenu('Chỉnh sửa'), targets: ['.um-editor .um-form-grid', null, '.um-editor select', null, '.um-editor .adm-btn--danger'] }),
  modal('administration-login-history-window', '/admin/users', admin, { script: [...rowMenu('Lịch sử đăng nhập'), `document.querySelectorAll('[role=dialog] th').forEach((th) => { th.style.position = 'static' }); document.querySelectorAll('[role=dialog] *').forEach((e) => { e.scrollTop = 0 })`], targets: ['[role=dialog] h2', '[role=dialog] table', '[role=dialog] button[aria-label="Đóng"], [role=dialog] button[aria-label="Close"], [role=dialog] button'] }),
  staff('administration-system-overview', '/admin/system', admin, { targets: ['h1', '.sys-summary-grid', null] }),
  staff('administration-service-status', '/admin/services', admin, { targets: ['h1', '.adm-titlebar__actions button', '.adm-table'] }),
  staff('administration-activity-log', '/admin/activity', admin, { words: { 'Hoạt động': 'Activity' }, targets: ['h1', '.sys-date-range input', '.sys-chart-wrap', '[aria-label="Retention theo cohort"], .sys-panel + .sys-panel'] }),
]

// ---------- key sales ----------
const products = [
  ['AI Points 100', 'POINTS', 100], ['AI Points 500', 'POINTS', 500], ['Premium 30 days', 'PREMIUM', null, 30, 4], ['Premium 90 days', 'PREMIUM', null, 90, 12],
].map(([name, keyType, pointsAmount, premiumDays = null, humanGradingCredits = null], i) => ({ id: uid(600 + i), name, keyType, pointsAmount, premiumDays, humanGradingCredits, status: 'ACTIVE' }))
const states = (READY, ISSUED, ACTIVATED, REVOKED = 0, EXPIRED = 0) => ({ READY, ISSUED, ACTIVATED, REVOKED, EXPIRED })
const batch = (code, label, productIndex, byState, pool, back) => ({
  code, label, productId: products[productIndex].id, productName: products[productIndex].name,
  quantity: Object.values(byState).reduce((a, b) => a + b, 0), expiresOn: day(-90 + back), createdAt: hoursAgo(back * 24), byState, pool,
})
const batches = [
  batch('KB-001', 'Autumn sale', 0, states(120, 50, 30), 80, 25), batch('KB-002', 'Class sale', 1, states(60, 25, 15), 40, 18),
  batch('KB-003', 'Trial offer', 2, states(40, 12, 28), 20, 10), batch('KB-004', 'Winter sale', 3, states(30, 8, 12), 30, 3),
]
const sales = [['Sally Seller', 90, 55, 30], ['Sid Sample', 60, 25, 15]].map(([displayName, held, issued, activated], i) => ({ id: uid(700 + i), displayName, active: true, held, issued, activated }))
const keyRow = (n, state, saleId, product = 0) => ({
  id: uid(800 + n), batchCode: 'KB-001', seq: n, productId: products[product].id, productName: products[product].name,
  maskedCode: `IP-****-****-DM${String(n).padStart(2, '0')}`, state, saleId,
  issuedAt: state === 'READY' ? null : hoursAgo(30 + n), activatedAt: state === 'ACTIVATED' ? hoursAgo(20 + n) : null,
  revokedAt: null, expiresAt: day(-65), note: null, codeStored: true,
})
const batchKeys = [keyRow(1, 'ACTIVATED', sales[0].id), keyRow(2, 'ISSUED', sales[0].id), keyRow(3, 'READY', sales[1].id), keyRow(4, 'READY', null), keyRow(5, 'READY', null)]
const paged = (items, total = items.length) => ({ items, total, page: 0, size: 10 })
const catalog = [/GET \/api\/access\/key-products/, products]
const manager = [
  catalog,
  [/GET \/api\/access\/manager\/sales/, sales],
  [/GET \/api\/access\/manager\/batches\?/, paged(batches)],
  [/GET \/api\/access\/manager\/batches\/[^/?]+\/keys/, paged(batchKeys, 200)],
  [/GET \/api\/access\/manager\/batches\/[^/?]+$/, batches[0]],
  [/GET \/api\/notifications\?/, page([])],
  ...signedIn('STAFF_MANAGER'),
]
const counts = (key, label, byState) => ({ key, label, byState })
const seller = [
  catalog,
  [/GET \/api\/access\/sale\/stock/, {
    byProduct: products.map((p, i) => counts(p.id, p.name, states([12, 6, 4, 0][i], 5, 8))),
    byBatch: batches.slice(0, 3).map((b, i) => counts(b.code, b.productName, states([12, 6, 4][i], 5, 8))),
  }],
  [/GET \/api\/access\/sale\/issued/, paged([keyRow(11, 'ISSUED', uid(1)), keyRow(12, 'ACTIVATED', uid(1)), keyRow(13, 'ISSUED', uid(1)), keyRow(14, 'ACTIVATED', uid(1)), keyRow(15, 'EXPIRED', uid(1))], 13)],
  [/GET \/api\/notifications\?/, page([])],
  ...signedIn('SALES_STAFF'),
]
const mgr = '/point-management/sale-manager'
// The confirm window of "create batch" is drawn under the form (not over it), so that both can be numbered.
const confirmBelow = `__click('Xem lại'); await __wait(900); const dlg = document.querySelector('[role=dialog]'); const overlay = [...document.querySelectorAll('body *')].find((e) => e !== dlg && !e.contains(dlg) && getComputedStyle(e).position === 'fixed' && e.getBoundingClientRect().width >= innerWidth && e.getBoundingClientRect().height >= innerHeight); if (overlay) overlay.remove(); dlg.querySelector('input[type=checkbox]').click(); await __wait(300); dlg.style.cssText += ';position:absolute;top:930px;left:50%;transform:translateX(-50%);animation:none'`

// The closed "view as table" part of the charts is hidden in the browser, but the drawing tool would still draw it.
const closedTable = `document.querySelectorAll('details:not([open]) > :not(summary)').forEach((e) => { e.style.display = 'none' })`
const salesScreens = [
  staff('key-sales-key-overview', `${mgr}/overview`, manager, { script: closedTable, width: 1280, targets: ['h1', '.adm-titlebar__actions a', '.pm-filterbar', '.pm-chart', '.pm-split .adm-table-wrap'] }),
  staff('key-sales-key-batches', `${mgr}/batches`, manager, { words: { 'đợt': 'batches' }, targets: ['h1', '.adm-titlebar__actions a', '.pm-filterbar', '.pm-table'] }),
  staff('key-sales-create-batch', `${mgr}/batches/new`, manager, { script: confirmBelow, height: 1330, targets: ['h1', '.pm-step', '.pm-step + .pm-step', '.pm-step + .pm-step + .pm-step', '.pm-summary', '[role=dialog] .pm-check', '[role=dialog] .adm-btn--primary'] }),
  staff('key-sales-batch-created', `${mgr}/batches/KB-001/created`, manager, { targets: ['.pm-success', '.pm-table', '.pm-alert', '.pm-success a'] }),
  staff('key-sales-batch-detail', `${mgr}/batches/KB-001`, manager, { script: `document.querySelectorAll('.pm-table tbody input[type=checkbox]')[2].click()`, targets: ['[aria-label="Tình trạng của đợt"], .adm-titlebar + .adm-card', '.adm-titlebar__actions button', '.adm-titlebar__actions button:nth-of-type(2)', '.adm-titlebar__actions button:nth-of-type(3)', '.pm-table'] }),
  staff('key-sales-issue-key-directly', `${mgr}/issue`, manager, { targets: ['h1', '.pm-packs', '.pm-pack button'] }),
  staff('key-sales-issue-key', '/point-management/sale/issue', seller, { script: closedTable, words: { 'học viên.': 'Learner.' }, targets: ['h1', '.pm-chart', '.pm-packs'] }),
  staff('key-sales-issued-keys', '/point-management/sale/issued', seller, { targets: ['h1', '.pm-filterbar', '.pm-table'] }),
]

export const screens = [...examinerScreens, ...adminScreens, ...salesScreens]
