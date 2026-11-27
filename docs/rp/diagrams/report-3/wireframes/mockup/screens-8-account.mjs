// Home, public examiner pages, overview and account screens. Targets follow the element order of wf_index.json.
import { signedIn, me, uid } from './common.mjs'

export const common = signedIn('CUSTOMER')
export const patterns = [
  ['· tim kế tiếp sau (\\d+) phút', '· next heart in $1 minutes'],
  ['Hoạt động gần nhất:', 'Last active:'],
  ['^(\\d+) đang học · (\\d+) cần ôn$', '$1 learning · $2 to review'],
  ['^Bắt đầu: (\\d+)/(\\d+)/(\\d+)$', 'Started: $1/$2/$3'],
  ['trung bình mỗi ngày học so với 7 ngày trước', 'per study day vs the previous 7 days'],
  ['so với 7 ngày trước', 'vs the previous 7 days'],
  ['^Tuần này có (\\d+)/7 ngày học được ghi nhận, tuần trước (\\d+)/7\\.$', 'This week $1/7 days recorded, last week $2/7.'],
  ['^(\\d+)/(\\d+) đang bật$', '$1/$2 on'],
  ['^(\\d+) chứng chỉ$', '$1 certificates'],
  ['^(\\d+) bài$', '$1 lessons'],
  ['^(\\d+)/(\\d+) bài$', '$1/$2 lessons'],
]

// Invented examiners. The avatar points at the dev server so that no picture is fetched from outside.
const examiner = (n, displayName, ieltsBand, acceptingSubmissions, certificates, bio) => ({
  id: uid(n), displayName, ieltsBand, acceptingSubmissions, certificates, bio, avatarUrl: 'http://localhost:5199/favicon.ico',
})
const cert = (name, issuer, year, score) => ({ name, issuer, year, score })
const examiners = [
  examiner(31, 'Daniel Carter', 8.5, true, [cert('IELTS Academic', 'IDP Education', 2025, '8.5 Overall'), cert('CELTA', 'Cambridge English', 2021, 'Pass A')], 'Writing coach who focuses on clear arguments and precise word choice.'),
  examiner(32, 'Emma Whitfield', 8.0, true, [cert('IELTS Academic', 'British Council', 2024, '8.0 Overall'), cert('TESOL Certificate', 'Trinity College', 2020, 'Distinction')], 'Speaking and Writing examiner with a calm, step by step style.'),
  examiner(33, 'Oliver Bennett', 8.0, false, [cert('IELTS Academic', 'IDP Education', 2024, '8.0 Overall')], 'Helps learners organise essays and remove repeated grammar mistakes.'),
]

const guest = [[/GET \/api\/users\/examiners/, examiners]]
const preference = (eventType, enabled = true) => ({ eventType, enabled })

// The content of a closed <details> still has layout boxes, which the extractor would draw.
const closeMenu = `document.querySelectorAll('details:not([open]) > :not(summary), .profile-cover-particle').forEach((el) => { el.style.display = 'none' })`
const day = (back) => new Date(Date.now() - back * 86400000).toISOString().slice(0, 10)
const at = (back) => `${day(back)}T08:00:00Z`
const SKILLS = ['LISTENING', 'READING', 'WRITING', 'SPEAKING']
const learnerProfile = [/GET \/api\/users\/me\/profile$/, { userId: uid(1), displayName: 'An', avatarReference: null, bio: 'Preparing for IELTS to study abroad next year.', selfReportedBand: 5.5, timezone: 'Asia/Ho_Chi_Minh', visibility: 'PUBLIC', createdAt: '2026-09-01T08:00:00Z' }]
const report = [/GET \/api\/learning\/report/, {
  skills: SKILLS.map((skill, i) => ({ skill, mastery: [0.72, 0.66, 0.41, 0.58][i], tierCounts: { PRIORITY: i === 2 ? 3 : 0 }, kpCount: 8, band: [6.5, 6.0, 5.5, 6.0][i] })),
  knowledgePoints: SKILLS.map((skill, i) => ({ skill, band: 6, score: { mastery: [0.72, 0.66, 0.41, 0.58][i], tier: 'CONSOLIDATE', evidenceCount: 5 } })),
  estimatedBand: { value: 6.0, estimated: true }, target: { targetBand: 7.0, gap: 1, examDate: null }, placementBand: 5.5,
}]
const course = (n, name, bandLevel, completedLessons, totalLessons) => ({ courseId: uid(n), name, bandLevel, completedLessons, totalLessons, percent: Math.round(completedLessons / totalLessons * 100), startedAt: at(50), lastStudiedAt: at(n - 70), activityProvenance: 'TRACKED', resumeTarget: { type: 'COURSE', courseId: uid(n) } })
const progress = [/GET \/api\/learning\/statistics\/progress/, {
  availability: 'AVAILABLE', generatedAt: at(0), dataFrom: at(90), scope: 'ALL', curriculumRevision: '1', page: 0, size: 20, totalCourses: 2, hasNext: false,
  courses: [course(71, 'IELTS Foundation 6.0', 6, 14, 32), course(72, 'IELTS Intensive 7.0', 7, 3, 28)],
  skills: SKILLS.map((skill, i) => ({ skill, completedLessons: [6, 5, 3, 3][i], totalLessons: 15, percent: [40, 33, 20, 20][i] })),
}]
const streak = [/GET \/api\/learning\/me\/streak/, { currentDays: 5, longestDays: 12, freezeAvailable: false, nextFreezeCost: 10, activeToday: true, lostStreakDays: 0, frozenDays: [] }]
const studyTime = [/GET \/api\/users\/me\/statistics\/study-time/, {
  availability: 'AVAILABLE', generatedAt: at(0), dataFrom: at(89), timezonePolicy: 'PROFILE', scope: 'ALL',
  days: Array.from({ length: 30 }, (_, i) => ({ date: day(i), timezone: 'Asia/Ho_Chi_Minh', actualSeconds: [1800, 2700, 900, 0, 3600, 2400, 1500][i % 7], plannedSeconds: null, status: 'COMPLETE' })),
}]
const lifetime = [/GET \/api\/learning\/statistics\/overview/, { generatedAt: at(0), dataFrom: at(89), totalStudySeconds: 61200, completedLessons: 17 }]
const placement = [/GET \/api\/learning\/placement-test\/options/, { pools: [], limitPer24h: 3, retakesLeft: 2, nextRetakeAt: null, hasPlacement: true, currentBand: 5.5, openAssignment: null }]
const overviewFixtures = [learnerProfile, report, progress, streak, studyTime, lifetime, placement]

const bandPoint = (n, back, band, gradingSource) => ({ attemptId: uid(n), resultVersion: 1, submittedAt: at(back), band, pendingRegrade: false, gradingSource, measurementQuality: 'FULL', scoringVersion: 'v1' })
const bands = [
  [/GET \/api\/assessments\/me\/statistics\/bands\/availability/, { options: ['LISTENING', 'READING', 'WRITING', 'SPEAKING'].map((skill, i) => ({ seriesKind: 'OFFICIAL_PRACTICE', skill, latestSubmittedAt: at(3 + i) })) }],
  [/GET \/api\/assessments\/me\/statistics\/bands\?.*PLACEMENT/, { availability: 'AVAILABLE', generatedAt: at(0), dataFrom: at(90), seriesKind: 'PLACEMENT', skill: 'LISTENING', eligibilityReason: null, points: [], nextCursor: null, baseline: 5.5, latest: 5.5, delta: null }],
  [/GET \/api\/assessments\/me\/statistics\/bands\?/, { availability: 'AVAILABLE', generatedAt: at(0), dataFrom: at(90), seriesKind: 'OFFICIAL_PRACTICE', skill: 'LISTENING', eligibilityReason: null, nextCursor: null, baseline: 5.5, latest: 6.5, delta: 1.0,
    points: [bandPoint(81, 60, 5.5, 'AUTO'), bandPoint(82, 45, 6.0, 'AUTO'), bandPoint(83, 24, 6.0, 'AUTO'), bandPoint(84, 3, 6.5, 'AUTO')] }],
]
const vocabulary = [/vocabulary-map/, { currentBand: 'B2', suggestedBand: 'B1', bands: [['4.0-5.0', 'B1', 900, 410], ['5.5-6.5', 'B2', 1200, 260], ['7.0-8.0', 'C1', 800, 40]].map(([band, cefr, total, learned]) => ({ band, cefr, total, learned, learning: Math.round(learned / 4), needsReview: 12, notLearned: total - learned })) }]
const writing = [/GET \/api\/learning\/writing\/progress/, { tasks: [{ task: 'TASK_2', insufficient: false, recurringErrors: [], fixedErrors: [],
  points: [5.5, 6.0, 6.0].map((overallBand, i) => ({ submissionId: uid(90 + i), source: 'PRACTICE', gradedAt: at(40 - i * 15), overallBand, criteria: { TR: overallBand, CC: overallBand, LR: overallBand, GRA: overallBand - 0.5 } })) }, { task: 'TASK_1', insufficient: true, points: [], recurringErrors: [], fixedErrors: [] }] }]

const badge = (code, name, description, awarded, current, target) => ({ code, name, description, rewardPoints: 10, awarded, awardedAt: awarded ? at(10) : null, progress: awarded ? null : { current, target } })
const achievements = [
  learnerProfile,
  [/GET \/api\/learning\/me\/progress/, { totalXp: 1240, level: 6, nextLevelAt: 1500, xpToNextLevel: 260, xpToday: 40, dailyCap: 100 }],
  [/GET \/api\/learning\/me\/hearts/, { hearts: 4, maxHearts: 5, nextHeartAt: new Date(Date.now() + 3600000).toISOString(), refillCost: 20 }],
  streak,
  [/GET \/api\/learning\/me\/quests/, { date: day(0), completed: 1, quests: [
    { type: 'PRACTICE', required: 10, progress: 10, completed: true, xpReward: 20, pointsReward: 0, target: null },
    { type: 'PRACTICE_PLUS', required: 20, progress: 10, completed: false, xpReward: 30, pointsReward: 5, target: null },
    { type: 'STREAK', required: 1, progress: 0, completed: false, xpReward: 10, pointsReward: 0, target: null },
  ] }],
  [/GET \/api\/learning\/me\/badges/, [badge('FIRST_LESSON', 'First step', 'Finish your first lesson.', true), badge('STREAK_7', 'One week', 'Study seven days in a row.', false, 5, 7), badge('XP_1000', 'Collector', 'Earn 1000 XP.', true), badge('LESSONS_50', 'Dedicated', 'Finish 50 lessons.', false, 17, 50)]],
  [/GET \/api\/learning\/me\/league(\?|$)/, { tier: 'SILVER', weekStart: day(4), closesAt: new Date(Date.now() + 3 * 86400000).toISOString(), joined: true, groupSize: 5, promoteTop: 2, demoteFrom: 5,
    table: [['Maya Brooks', 320], ['Leo Harper', 290], [null, 210], ['Nora Quinn', 180], ['Sam Ellis', 90]].map(([displayName, weeklyXp], i) => ({ rank: i + 1, displayName, weeklyXp, you: i === 2, zone: i < 2 ? 'PROMOTION' : i === 4 ? 'DEMOTION' : 'SAFE' })) }],
]

export const screens = [
  {
    file: 'home-and-public-pages-home', url: '/home', guest: true, fixtures: guest, wait: 9000, height: 2600,
    script: `document.querySelectorAll('#home-main > section:not(#home-hero):not(#home-pricing):not(#home-consultation)').forEach((el) => { el.style.display = 'none' }); document.querySelectorAll('[data-home-reveal]').forEach((el) => el.classList.add('is-visible')); window.scrollTo(0, 0)`,
    targets: ['#home-hero-title', '.home-hero-primary', '.home-hero-points', '.home-hero-journey', '#home-pricing-title', '#home-consultation-title'],
  },
  {
    // Opened from a Premium card of the practice catalogue: that route gives the window a working activation client.
    file: 'home-and-public-pages-activate-key-window', url: '/practice-tests?skill=reading', wait: 12000, height: 700,
    fixtures: [
      [/GET \/api\/content\/practice-tests\/formats/, []],
      [/POST \/api\/content\/practice-tests\/search/, { totalElements: 1, page: 0, size: 12, items: [{ packageId: uid(61), packageVersionId: uid(62), scope: 'FULL', title: 'City Gardens', coverUrl: null, source: null, formats: [], questionCount: 40, timeLimitSeconds: 3600, requiredFeatureKey: 'PREMIUM_TESTS', parts: [{ sectionId: uid(63), partNo: 1, title: 'Part 1', questionCount: 40 }] }] }],
      [/GET \/api\/assessments\/practice\/(reading|progress|attempts)/, { completed: [], items: [] }],
    ],
    script: `const card = document.querySelector('.practice-visual-card[role=button]'); if (!card) throw new Error('DBG ' + document.body.innerText.replace(/\\s+/g,' ').slice(0,500)); card.click(); await __wait(900); __click('Kích hoạt bằng Mã Key'); await __wait(900)`,
    root: '[role=dialog]', backdrop: true,
    targets: ['#activation-key-help', '#activation-key', '[role=dialog] button[type=submit]', '[role=dialog] button[aria-label="Đóng"]'],
  },
  {
    file: 'home-and-public-pages-examiner-directory', url: '/mentors', guest: true, fixtures: guest, wait: 9000, height: 2750,
    targets: ['#exp-hero-title', '.exd-stats-strip', '.exp-search-input', '.exp-skill-chips', '.exp-sort-group', '.exp-card-item', '#exp-workflow-title'],
  },
  {
    file: 'home-and-public-pages-examiner-detail', url: `/mentors/${uid(31)}`, guest: true, wait: 9000,
    fixtures: [[/GET \/api\/users\/examiners\/0/, examiners[0]], ...guest],
    targets: ['.exd-breadcrumb', '.exd-pill--verified', '.exd-stats-strip', '#exd-certs-title', '#exd-method-title', '.exd-hero-actions .exd-btn-primary'],
  },
  {
    file: 'account-and-overview-notification-preferences', url: '/notifications/preferences', wait: 18000, script: closeMenu,
    fixtures: [
      [/GET \/api\/notifications\/preferences/, [
        'community.post-commented.v1', 'community.comment-replied.v1', 'community.comment-mentioned.v1', 'community.post-reacted.v1', 'community.user-followed.v1',
        'access.subscription-granted.v1', 'access.subscription-expiry-notice.v1', 'assessment.result-ready.v1', 'library.vocabulary-review-due.v1',
        'assessment.grading-requested.v1', 'assessment.grading-due-soon.v1', 'assessment.grading-overdue.v1', 'assessment.grading-completed.v1',
      ].map((type) => preference(type))],
      [/GET \/api\/notifications\/mail-preferences/, { learning: true }],
    ],
    targets: ['.notification-preferences-back', '.notification-preferences-hero h1', '[aria-labelledby=notification-group-learner] li', 'text=Premium activated', '#notification-group-examiner'],
  },
  {
    file: 'account-and-overview-overview', url: '/overview', wait: 18000, height: 1850, fixtures: overviewFixtures, script: closeMenu,
    targets: ['.overview-sidebar-nav', '.overview-hero-welcome', '.overview-course-info', '.overview-week-activity', '#overview-skills-title', '.pl-retake-entry'],
  },
  {
    file: 'account-and-overview-detailed-statistics', url: '/overview?tab=detailed-stats', wait: 18000, height: 3100, script: closeMenu,
    fixtures: [...bands, vocabulary, writing, report, progress, learnerProfile],
    targets: ['.detailed-hero h1', '.detailed-periods', '.detailed-band-tabs', '.detailed-band-plot', '.detailed-vocabulary h2'],
  },
  {
    file: 'account-and-overview-achievements', url: '/overview?tab=achievements', wait: 18000, height: 1900, script: closeMenu, fixtures: achievements,
    targets: ['.achievement-page-head h1', '.achievement-level', '.achievement-badges', '.achievement-league', '.achievement-quests', '.achievement-continue'],
  },
  {
    file: 'account-and-overview-profile-and-security', url: '/overview?tab=profile', wait: 18000, height: 2380,
    script: [closeMenu, `__click('Chỉnh sửa'); await __wait(900); __fill('#field-newPass', 'Sunrise2026'); await __wait(300)`],
    fixtures: [
      [/GET \/api\/users\/me$/, { ...me('CUSTOMER'), phoneNumber: '0912345678', emailVerified: false }],
      // Chromium lists no 'Asia/Ho_Chi_Minh' among the supported zones, so the select would stay empty with it.
      [/GET \/api\/users\/me\/profile$/, { ...learnerProfile[1], timezone: 'Asia/Bangkok' }], streak,
      [/GET \/api\/users\/me\/learning-goals\/active/, { message: 'none' }, 404],
      [/GET \/api\/users\/me\/learning-goals/, []],
      [/GET \/api\/users\/me\/oauth/, [{ id: uid(95), userId: uid(1), provider: 'google', providerSubject: 'demo', linkedAt: '2026-09-02T08:00:00Z' }]],
      [/GET \/api\/users\/me\/sessions/, [{ sessionId: uid(96), startedAt: at(2), lastUsedAt: at(0), ipAddress: '203.0.113.20', userAgent: 'Mozilla/5.0 (Windows NT 10.0) Chrome/140.0', current: true }]],
      [/GET \/api\/users\/me\/security-events/, { items: [] }],
    ],
    targets: ['.profile-avatar-dropzone', '#personal-info-title', '#field-displayName', '#field-fullName', '#field-phoneNumber', '#field-timezone', '#field-bio', '[aria-label="Xác minh email"]', '.profile-switch', '#security-title', '#field-currentPass', '#field-newPass', '#field-confirmPass', '.profile-change-pass-btn', '.profile-unlink-btn'],
  },
]
