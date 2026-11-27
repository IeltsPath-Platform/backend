// Games and community screens. All data is invented; no call reaches a real backend.
import { signedIn, me, uid } from './common.mjs'
export const common = signedIn('CUSTOMER')
export const patterns = [['^(\\d+) từ$', '$1 words'], ['^(\\d+) giây / từ$', '$1 seconds / word'], ['· Mọi chủ đề', '· All topics']]

const catalog = {
  games: [
    { gameType: 'WORDLY', name: 'Wordly', description: 'Guess the English word from its meaning, letter by letter.', status: 'AVAILABLE' },
    { gameType: 'ROCKET', name: 'Rocket', description: 'Fly your rocket into the planet that carries the right word.', status: 'AVAILABLE' },
  ],
}
const topics = [['education', 'Education'], ['environment', 'Environment'], ['technology', 'Technology'], ['health', 'Health'],
  ['work-business', 'Work'], ['society', 'Society'], ['daily-life', 'Daily life']]
  .map(([slug, name], i) => ({ id: uid(300 + i), slug, name, nameVi: name }))
const filters = {
  topics,
  availability: [null, 'A1', 'A2', 'B1', 'B2', 'C1', 'C2'].flatMap((cefrLevel) => [null, ...topics.map((t) => t.id)]
    .map((topicId) => ({ cefrLevel, topicId, words: 60 }))),
}
const wordFilters = [/GET \/api\/games\/word-filters/, filters]
// The closed account menu of the navbar is invisible in the page but would still be drawn.
const noMenu = `document.head.insertAdjacentHTML('beforeend', '<style>.site-user-tier-dropdown [class*=_tierCard_]{display:none!important}</style>')`

export const screens = [
  {
    file: 'games-game-catalog',
    url: '/games',
    wait: 8000,
    fixtures: [[/GET \/api\/games\/catalog/, catalog]],
    script: noMenu,
    targets: ['.game-catalog-highlight', '.game-catalog-card', '.game-join-panel h2', '#game-room-code', null],
  },
  {
    file: 'games-wordly-setup',
    url: '/games/wordly/setup?mode=SOLO',
    wait: 8000,
    fixtures: [wordFilters],
    script: noMenu,
    targets: ['.game-howto', '.game-settings-fields fieldset:nth-of-type(1) .game-radio-options', '.game-settings-fields fieldset:nth-of-type(2) .game-radio-options', '#game-lives', '.game-word-difficulty', 'form button[type=submit]'],
  },
  {
    file: 'games-rocket-setup',
    url: '/games/rocket',
    fixtures: [wordFilters],
    wait: 8000,
    height: 1000,
    targets: ['#rocket-setup-title', '.rocket-count-options', '.rocket-rules li:nth-child(2)', '.rocket-rules li:nth-child(1)', 'text=Level', '.rocket-setup .rocket-primary'],
  },
]

// Game room, matches and result.
const roomId = uid(400)
const matchId = uid(401)
const at = (ms) => new Date(Date.now() + ms).toISOString()
const member = (n, displayName, status, slot) => ({ userId: uid(n), role: n === 1 ? 'HOST' : 'PLAYER', status, joinedAt: at(-60000), slotNumber: slot, displayName })
const settings = { initialLives: 4, wordTimeLimitSeconds: 180, playMode: 'FINITE', wordLimit: 10, timeEnabled: true, cefrLevel: 'B1', topicId: null }
const room = (extra = {}) => ({
  id: roomId, roomCode: 'AB7K2Q', hostUserId: uid(1), gameType: 'WORDLY', learningDomain: 'IELTS', mode: 'MULTIPLAYER', maxPlayers: 4,
  status: 'WAITING', createdAt: at(-120000), members: [member(1, 'An Nguyen', 'READY', 1), member(2, 'Binh Tran', 'READY', 2), member(3, 'Chi Le', 'JOINED', 3)],
  items: [], playerMode: 'MULTIPLAYER', settings, canStart: false, activeMatchId: null, message: null, ...extra,
})
// The room socket cannot be answered by fixtures: a stand-in reports "connected". The first tries fail on a bad
// subprotocol name, so nothing is ever opened against a backend, and the next retry uses the stand-in.
const fakeSocket = `window.WebSocket = class { constructor() { setTimeout(() => this.onmessage && this.onmessage({ data: '{"type":"CONNECTED"}' }), 100) } send() {} close() {} }; await __wait(10000)`
const ranked = (n, displayName, rank, totalScore, wordsSolved, lives, ms) => ({ userId: uid(n), displayName, slotNumber: n, rank, status: 'FINISHED', livesRemaining: lives, totalScore, totalSolveTimeMs: ms, wordsProcessed: 10, wordsSolved, currentStreak: 0 })
const players = [ranked(2, 'Binh Tran', 1, 980, 9, 3, 214000), ranked(1, 'An Nguyen', 2, 860, 8, 2, 236500), ranked(3, 'Chi Le', 3, 540, 6, 0, 301200)]
const wordPlayer = { status: 'ACTIVE', livesRemaining: 3, totalScore: 320, currentStreak: 2, bestStreak: 2 }
const option = (i, word) => ({ optionId: uid(500 + i), word })

screens.push(
  {
    file: 'games-game-room',
    url: `/games/rooms/${roomId}`,
    wait: 8000,
    fixtures: [
      [/GET \/api\/games\/rooms\/[^/]+$/, room()],
      [/POST .*websocket-ticket/, { ticket: 'mock', expiresAt: at(600000), subprotocols: ['not a protocol'] }],
      wordFilters,
    ],
    script: [noMenu, fakeSocket],
    targets: ['.game-room-code', '.game-player-grid', '.game-player-card .game-readiness', 'text=Edit settings', '.game-lobby-actions .game-actions', 'text=Leave room'],
  },
  {
    file: 'games-wordly-play',
    url: `/games/matches/${matchId}/play`,
    wait: 8000,
    fixtures: [
      [/GET \/api\/games\/matches\/[^/]+\/words\/current/, {
        playerWordId: uid(402), wordNumber: 4, wordLimit: 10, meaning: 'good for the environment', columns: 5, maxAttempts: 6, attemptsUsed: 2,
        timeEnabled: true, deadlineAt: at(150000), wordStatus: 'ACTIVE', player: wordPlayer,
        guesses: [
          { attemptNumber: 1, guess: 'PLANT', letters: ['ABSENT', 'ABSENT', 'ABSENT', 'PRESENT', 'ABSENT'] },
          { attemptNumber: 2, guess: 'GRAIN', letters: ['CORRECT', 'CORRECT', 'ABSENT', 'ABSENT', 'PRESENT'] },
        ],
        keyboard: { P: 'ABSENT', L: 'ABSENT', A: 'ABSENT', N: 'PRESENT', T: 'ABSENT', G: 'CORRECT', R: 'CORRECT', I: 'ABSENT' },
        hint: null,
      }],
      [/GET \/api\/games\/matches\/[^/]+$/, { matchId, roomId, sessionId: null, status: 'IN_PROGRESS', startedAt: at(-90000), players: [], playerMode: 'SOLO', settings, ownPlayer: null, ranking: null }],
      [/GET \/api\/games\/rooms\/[^/]+$/, room({ status: 'IN_MATCH', playerMode: 'SOLO', activeMatchId: matchId })],
    ],
    script: noMenu,
    targets: ['.wordly-arena h2', '.wordly-board', '.wordly-keyboard', '.wordly-hud', 'text=Hint', 'text=Exit game'],
  },
  {
    file: 'games-rocket-play',
    url: `/games/rocket/${matchId}`,
    wait: 10000,
    height: 720,
    fixtures: [
      [/GET \/api\/games\/rocket\/matches\/[^/]+\/rounds\/current/, {
        matchId, settings: { questionCount: 10, initialLives: 4, secondsPerQuestion: 15 }, matchFinished: false, lastRoundResult: null, serverTime: at(0),
        round: { roundId: uid(403), number: 3, total: 10, meaning: 'good for the environment', options: [option(1, 'green'), option(2, 'crowded'), option(3, 'ancient')],
          opensAt: at(-3000), deadlineAt: at(600000), serverTime: at(0), me: { answered: false, optionId: null }, player: { livesRemaining: 3, totalScore: 260, status: 'ACTIVE' } },
      }],
      [/GET \/api\/games\/matches\/[^/]+$/, { matchId, roomId, status: 'IN_PROGRESS', playerMode: 'SOLO', ownPlayer: null, ranking: null, players: [] }],
    ],
    targets: ['.rocket-mission', '.rocket-planet-label', null, null, '.rocket-hud-left .rocket-icon-button'],
  },
  {
    file: 'games-game-result',
    url: `/games/matches/${matchId}/result`,
    wait: 8000,
    fixtures: [
      [/GET \/api\/games\/matches\/[^/]+\/result/, {
        matchId, gameType: 'WORDLY', playerMode: 'MULTIPLAYER', players, status: 'COMPLETED', endReason: 'WORD_LIMIT_REACHED', settings,
        player: { userId: uid(1), displayName: 'An Nguyen', status: 'FINISHED', livesRemaining: 2, wordsProcessed: 10, wordsSolved: 8, wordsFailed: 1, wordsTimedOut: 1, bestStreak: 5, baseScore: 710, bonusScore: 150, totalScore: 860, totalSolveTimeMs: 236500 },
        startedAt: at(-400000), endedAt: at(-60000), durationMs: 340000,
      }],
      [/GET \/api\/games\/matches\/[^/]+$/, { matchId, roomId, status: 'COMPLETED' }],
      [/GET \/api\/games\/rooms\/[^/]+$/, room()],
    ],
    script: noMenu,
    targets: ['.games-eyebrow', '.games-result-stats', '.game-ranking table', 'text=Choose a game'],
  },
)

// Community.
const authors = { 2: 'Binh Tran', 3: 'Chi Le', 4: 'Dung Pham' }
const post = (n, authorId, extra) => ({
  id: uid(600 + n), authorId: uid(authorId), category: 'GENERAL', title: null, body: '', createdAt: '2026-11-20T09:30:00Z', imageUrls: [], videoUrl: null, targetBand: null, background: null,
  author: { id: uid(authorId), displayName: authors[authorId] }, reactionCounts: {}, myReaction: null, commentCount: 0, bookmarkedByMe: false, followingAuthor: false, ...extra,
})
const posts = [
  post(1, 2, { category: 'QUESTION', title: 'How do you plan Writing Task 2?', body: 'I often run out of time before the conclusion. How many minutes do you spend on the outline?', targetBand: 7, reactionCounts: { LIKE: 8, HELPFUL: 3 }, commentCount: 2 }),
  post(2, 3, { category: 'SPEAKING_PARTNER', title: 'Looking for a Speaking partner', body: 'Free on weekday evenings. I would like to practise Part 2 and Part 3 twice a week.', reactionCounts: { LOVE: 4 }, commentCount: 1, followingAuthor: true }),
]
const cursorPage = (content) => ({ content, nextCursor: null, hasNext: false, size: 10 })
const feed = [/GET \/api\/community\/posts\?pagination/, cursorPage(posts)]
const comment = (n, authorId, body, replyCount = 0) => ({ id: uid(700 + n), authorId: uid(authorId), parentCommentId: null, body, replyCount, author: { displayName: authorId === 1 ? 'An Nguyen' : authors[authorId] } })

screens.push(
  {
    file: 'community-community-feed',
    url: '/community',
    wait: 8000,
    fixtures: [feed],
    script: noMenu,
    targets: ['.community-heading .community-eyebrow', '#community-search-input', '.community-left nav', '.community-categories', '.community-heading .community-primary', '.community-post'],
  },
  {
    file: 'community-post-detail',
    url: `/community/posts/${uid(601)}`,
    wait: 8000,
    fixtures: [
      [/GET \/api\/community\/posts\/[^/?]+\/comments/, { content: [comment(1, 3, 'I spend about five minutes on the outline, then write without stopping.', 1), comment(2, 1, 'Writing the two topic sentences first helps me a lot.')] }],
      [/GET \/api\/community\/posts\/[^/?]+$/, posts[0]],
    ],
    script: [noMenu, `__click('2 Bình luận')`],
    targets: ['.community-post', '.community-comment-textarea', '.community-comment', '.community-report-btn'],
  },
  {
    file: 'community-learner-profile',
    url: `/community/users/${uid(2)}`,
    wait: 12000,
    fixtures: [
      [/GET \/api\/users\/[^/]+\/public-profile/, { id: uid(2), displayName: 'Binh Tran', avatarUrl: null, bio: 'Preparing for IELTS Academic. I like sharing Writing tips.', targetBand: 7, streak: 12 }],
      [/GET \/api\/community\/users\/[^/]+\/relationship/, { following: false, followerCount: 24, followingCount: 9 }],
      [/GET \/api\/community\/users\/[^/]+\/posts/, cursorPage([posts[0]])],
    ],
    // The page repeats the profile above the card; only the card belongs to the screen.
    script: [noMenu, `const card = document.querySelector('.community-public-profile'); let el = card; while (el && el.parentElement) { [...el.parentElement.children].forEach((other) => { if (other !== el && other.compareDocumentPosition(el) & Node.DOCUMENT_POSITION_FOLLOWING && other.textContent.includes('Binh Tran') && !other.closest('aside, nav, header')) other.style.display = 'none' }); if (el.parentElement.tagName === 'MAIN') break; el = el.parentElement }`],
    targets: ['.community-public-profile .community-profile-avatar', '.community-profile-stats', '.community-profile-tabs', null],
  },
  {
    file: 'community-write-post-window',
    url: '/community',
    wait: 8000,
    height: 800,
    root: '[role=dialog]',
    backdrop: true,
    fixtures: [feed],
    script: [`__click('Bài viết mới')`, `__click('Thêm tiêu đề bài viết')`, `__fill('.composer-textarea', 'Any tips for map questions?')`, `document.querySelector('.composer-select-pill select').style.pointerEvents = 'none'`],
    targets: ['.composer-select-pill', '.composer-title-input', '.composer-textarea', '.composer-tool-btn:nth-child(1)', '.composer-tool-btn:nth-child(2)', '.composer-submit-btn'],
  },
  {
    file: 'community-community-reports',
    words: { 'Bài viết': 'Post' },
    url: '/community/admin/reports',
    wait: 8000,
    fixtures: [
      [/GET \/api\/users\/me$/, me('ADMIN')],
      [/GET \/api\/community\/admin\/reports/, {
        content: [
          { id: uid(801), reporterId: uid(3), reporterDisplayName: 'Chi Le', contentType: 'POST', contentId: uid(601), contentAuthorId: uid(4), contentAuthorDisplayName: 'Dung Pham', contentPreview: 'Buy guaranteed band 8 answers, send me a message today.', reason: 'Spam advertising', status: 'PENDING', createdAt: '2026-11-21T10:15:00Z' },
          { id: uid(802), reporterId: uid(2), reporterDisplayName: 'Binh Tran', contentType: 'COMMENT', contentId: uid(701), contentAuthorId: uid(4), contentAuthorDisplayName: 'Dung Pham', contentPreview: 'This essay is useless, do not bother writing again.', reason: 'Insulting another learner', status: 'PENDING', createdAt: '2026-11-22T14:40:00Z' },
        ], page: 0, size: 20, totalElements: 2, totalPages: 1, sort: 'createdAt,desc',
      }],
    ],
    script: noMenu,
    targets: ['.community-report-admin h1', '.community-report-admin li', '.community-report-admin li a', '.community-report-actions button:nth-child(1)', '.community-report-actions button:nth-child(2)'],
  },
)
