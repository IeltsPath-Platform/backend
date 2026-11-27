// Vocabulary, flashcards and video dictation screens. All data is invented.
import { signedIn, page, uid } from './common.mjs'

export const common = [
  ...signedIn('CUSTOMER'),
]

export const patterns = []

const sense = (n, definition, example, extra = {}) => ({ id: uid(3000 + n), vietnameseMeaning: null, englishDefinition: definition, exampleSentence: example, partOfSpeech: 'adjective', cefrLevel: 'B2', ...extra })
const word = (n, lemma, ipa, s) => ({ id: uid(2000 + n), lemma, ipa, ipaUk: ipa, ipaUs: ipa, pronunciationAudioReference: null, academicWordList: n % 2 === 0, senses: [s] })
const words = [
  word(1, 'environment', '/ɪnˈvaɪrənmənt/', sense(1, 'the natural world where people, animals and plants live', 'We must protect the environment for future generations.', { partOfSpeech: 'noun', cefrLevel: 'B1' })),
  word(2, 'environmental', '/ɪnˌvaɪrənˈmentl/', sense(2, 'relating to the natural world and how people affect it', 'The factory caused serious environmental damage.')),
  word(3, 'environmentalist', '/ɪnˌvaɪrənˈmentəlɪst/', sense(3, 'a person who works to protect nature', 'Environmentalists oppose the new motorway.', { partOfSpeech: 'noun', cefrLevel: 'C1' })),
  word(4, 'environmentally', '/ɪnˌvaɪrənˈmentəli/', sense(4, 'in a way that is connected with the natural world', 'The packaging is environmentally friendly.', { partOfSpeech: 'adverb' })),
]
const topic = (n, name, count) => ({ id: uid(4000 + n), slug: name.toLowerCase(), name, nameVi: name, coverKey: null, coverUrl: null, sortOrder: n, wordCount: count })
const topics = [topic(1, 'Environment', 120), topic(2, 'Education', 96), topic(3, 'Technology', 110)]
const set = (n, title, description, band, count, kind = 'BAND') => ({ id: uid(5000 + n), title, description, kind, cefrLevel: 'B2', bandRange: band, coverKey: null, coverUrl: null, wordCount: count, sortOrder: n, status: 'ACTIVE' })
const sets = [
  set(1, 'Environment - Band 6.5', 'Core words about climate, pollution and conservation.', '6.0–6.5', 40),
  set(2, 'Education - Band 6.0', 'Schools, universities and learning habits.', '5.5–6.0', 36),
  set(3, 'Technology - Band 7.0', 'Innovation, automation and digital life.', '6.5–7.0', 45),
]
const setWords = [
  word(11, 'sustainable', '/səˈsteɪnəbl/', sense(11, 'able to continue without harming nature', 'The city needs a sustainable transport system.')),
  word(12, 'pollution', '/pəˈluːʃn/', sense(12, 'harmful substances in air, water or soil', 'Air pollution is a major problem in large cities.', { partOfSpeech: 'noun' })),
  word(13, 'habitat', '/ˈhæbɪtæt/', sense(13, 'the natural home of an animal or plant', 'Forest clearing destroys the habitat of many birds.', { partOfSpeech: 'noun' })),
  word(14, 'conserve', '/kənˈsɜːv/', sense(14, 'to protect something from loss or damage', 'We should conserve water during the dry season.', { partOfSpeech: 'verb' })),
]
const setId = uid(5001)
const setDetail = { ...sets[0], topicSummaries: [{ topicId: uid(4001), name: 'Climate change', count: 16 }, { topicId: uid(4002), name: 'Pollution', count: 14 }, { topicId: uid(4003), name: 'Wildlife', count: 10 }] }
const wordId = uid(2011)

export const screens = [
  {
    file: 'vocabulary-and-flashcards-vocabulary', url: '/vocabulary?q=environ', guest: true, wait: 14000,
    fixtures: [
      [/GET \/api\/content\/vocabulary\/topics/, topics],
      [/GET \/api\/content\/vocabulary\/sets\?kind=BAND/, { items: sets, page: 0, size: 12, total: 3 }],
      [/GET \/api\/content\/vocabulary\/sets\?kind=TOPIC/, { items: [], page: 0, size: 12, total: 0 }],
      [/GET \/api\/content\/vocabulary\?/, { items: words, page: 0, size: 12, total: 4 }],
    ],
    targets: ['h1', '.dict-search__field', '.dict-search__lang', '.dict-results', '.dict-explore', '.dict-guest'],
  },
  {
    file: 'vocabulary-and-flashcards-word-detail', words: { 'Chủ đề': 'Topic' }, url: `/vocabulary/${wordId}`, wait: 14000,
    fixtures: [
      [new RegExp(`GET /api/content/vocabulary/${wordId}`), {
        ...setWords[0], cefrLevel: 'B2', frequencyRank: 2450, academicWordList: true, topic: 'Environment',
        definitions: [{ definition: 'able to continue without harming nature' }],
        collocations: [{ phrase: 'sustainable development', meaning: 'growth that protects resources' }],
        thesaurus: [{ word: 'renewable', relation: 'SYNONYM' }], commonMistakes: [], examples: [{ sentence: 'Solar power is a sustainable source of energy.' }],
      }],
      [/GET \/api\/learning-support\/flashcards\/notebook\/saved/, []],
    ],
    targets: ['nav[aria-label="Breadcrumb"]', 'h1', '.vocab-container > div > div:first-child > div:last-child', 'aside > div:first-child', 'aside h4', 'aside textarea', 'aside > div:last-child'],
  },
  {
    file: 'vocabulary-and-flashcards-vocabulary-set', words: { 'Đúng': 'Correct' }, url: `/vocabulary/sets/${setId}`, wait: 14000,
    fixtures: [
      [new RegExp(`GET /api/content/vocabulary/sets/${setId}/words`), { items: setWords, page: 0, size: 50, total: 4 }],
      [new RegExp(`GET /api/content/vocabulary/sets/${setId}$`), setDetail],
      [/GET .*notebook\/sets\/[^/]+\/progress/, { setId, total: 40, learned: 12, learning: 8, needsReview: 5, notLearned: 15, percent: 30 }],
      [/GET .*notebook\/recent-result/, { sessionId: uid(1), topic: 'Pollution', correct: 8, total: 10, percent: 80, finishedAt: '2026-11-20T08:00:00Z' }],
      [/GET .*notebook\/achievements/, { items: [{ code: 'STREAK_7', achieved: true, progress: 7, target: 7 }, { code: 'MASTER_300', achieved: false, progress: 124, target: 300 }] }],
      [/GET .*notebook\/saved/, []],
      [/GET .*notebook\/weekly-goal/, { target: 40, progress: 18, remaining: 22, days: ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'].map((weekday, i) => ({ date: `2026-11-${23 + i}`, weekday, achieved: i < 3 })) }],
    ],
    targets: [null, 'h1', undefined, '[role=tablist]', 'main [role=tablist] + div > div:first-child', 'main ol'],
  },
  {
    file: 'vocabulary-and-flashcards-vocabulary-map', url: '/vocabulary/map', wait: 14000,
    fixtures: [
      [/GET \/api\/learning\/report/, { estimatedBand: { value: 5.5, estimated: true }, placementCompleted: true }],
      [/GET .*notebook\/vocabulary-map/, {
        currentBand: 'B2', suggestedBand: 'C1',
        bands: [['A1', 500, 420, 40, 20], ['A2', 700, 480, 90, 40], ['B1', 1000, 430, 210, 80], ['B2', 1200, 260, 240, 90], ['C1', 900, 60, 80, 30], ['C2', 600, 8, 14, 6]]
          .map(([cefr, total, learned, learning, needsReview]) => ({ band: cefr, cefr, total, learned, learning, needsReview, notLearned: total - learned - learning - needsReview })),
      }],
    ],
    targets: ['h1', 'main > div:nth-child(2) > div:first-child > div:first-child', 'main > div:nth-child(2) > div:nth-child(2)', 'main > div:nth-child(2) > div:first-child > div:nth-child(2)'],
  },
]

// ---- Notebook, decks and study modes ----
const list = (items, total = items.length) => ({ items, page: 0, size: 50, total })
const entry = (n, w, level, box) => ({
  flashcardId: uid(6000 + n), vocabularySenseId: w.senses[0].id, vocabularyItemId: w.id, lemma: w.lemma, ipa: w.ipa, pronunciationAudioReference: null,
  partOfSpeech: w.senses[0].partOfSpeech, vietnameseMeaning: null, englishDefinition: w.senses[0].englishDefinition, exampleSentence: w.senses[0].exampleSentence,
  imageUrl: null, level, leitnerBox: box, reviewCount: box, lastReviewedAt: '2026-11-20T08:00:00Z', nextReviewAt: '2026-11-30T08:00:00Z', createdAt: '2026-11-18T08:00:00Z', note: null, cefrLevel: w.senses[0].cefrLevel, topicName: 'Environment',
})
const entries = [entry(1, setWords[0], 'LEARNING', 3), entry(2, setWords[1], 'MASTERED', 4), entry(3, setWords[2], 'FAMILIAR', 2), entry(4, setWords[3], 'UNKNOWN', 1)]
const studyWords = [0, 1, 2].flatMap((r) => setWords.map((w, i) => ({ ...w, id: uid(2100 + r * 10 + i) })))
const dueCard = (n, w) => ({ id: uid(6100 + n), userId: uid(1), sourceType: 'VOCABULARY_SENSE', vocabularySenseId: w.senses[0].id, front: w.lemma, back: w.senses[0].englishDefinition, leitnerBox: 1, reviewCount: 1, nextReviewAt: '2026-11-26T08:00:00Z', exampleSentence: w.senses[0].exampleSentence, imageUrl: null, ipa: w.ipa, pronunciationAudioReference: null, status: 'ACTIVE' })
const due = [0, 1, 2].flatMap((r) => setWords.map((w, i) => dueCard(r * 10 + i, w)))
const deckId = uid(7001)
const deck = { id: deckId, name: 'Environment words', description: 'Words for Writing Task 2 essays about nature.', status: 'ACTIVE', createdAt: '2026-11-01T08:00:00Z', updatedAt: '2026-11-20T08:00:00Z', coverImageKey: null, coverImageUrl: null, cardCount: 4, dueCount: 3 }
const deckItems = setWords.map((w, i) => ({ deckId, flashcardId: uid(6100 + i), sortOrder: i, addedAt: '2026-11-18T08:00:00Z', front: w.lemma, back: w.senses[0].englishDefinition, sourceType: 'MANUAL', status: 'ACTIVE', imageUrl: null, exampleSentence: w.senses[0].exampleSentence, ipa: w.ipa.replaceAll('/', ''), pronunciationAudioReference: null, leitnerBox: i % 2 + 1 }))
const deckFixtures = [
  [new RegExp(`GET /api/learning-support/decks/${deckId}/items`), list(deckItems)],
  [new RegExp(`GET /api/learning-support/decks/${deckId}$`), deck],
  ...deckItems.map((item) => [new RegExp(`GET /api/learning-support/flashcards/${item.flashcardId}$`), { ...item, id: item.flashcardId, vocabularySenseId: null, sourceReferenceId: null, highlightedText: null, createdAt: item.addedAt, updatedAt: item.addedAt, nextReviewAt: null, lastReviewedAt: null, reviewCount: 0 }]),
]
const setFixtures = [
  [new RegExp(`GET /api/content/vocabulary/sets/${setId}/words`), list(studyWords)],
  [new RegExp(`GET /api/content/vocabulary/sets/${setId}$`), setDetail],
]

screens.push(
  {
    file: 'vocabulary-and-flashcards-set-study', url: `/vocabulary/sets/${setId}`, wait: 14000, fixtures: setFixtures,
    // Questions are picked at random: answer until a fill-in question is shown. The session is opened from the set
    // page with speech switched off first, because a dictation question would speak on its own.
    script: `window.speechSynthesis.speak = () => {}; document.querySelector('a[href$="/study"]').click(); await __wait(4000); for (let i = 0; i < 12 && !document.querySelector('[aria-label="Điền từ vào câu ví dụ"]'); i++) { __fill('.review-input', 'answer'); await __wait(200); document.querySelector('.review-submit').click(); await __wait(500); document.querySelector('.review-submit').click(); await __wait(900) }`,
    targets: ['button[aria-label="Thoát phiên học"]', null, null, '[aria-label="Điền từ vào câu ví dụ"]', null, null],
  },
  {
    file: 'vocabulary-and-flashcards-notebook', url: '/vocabulary/notebook', wait: 14000,
    fixtures: [
      [/GET .*notebook\/summary/, { total: 48, byLevel: { UNKNOWN: 9, FAMILIAR: 11, LEARNING: 14, MASTERED: 14 }, dueNow: 7, addedLast7Days: 6, reviewedToday: 5, nextDueAt: null }],
      [/GET .*notebook\/entries/, list(entries, 4)],
      [/GET \/api\/learning-support\/flashcards\/due/, list([due[0]], 7)],
      [/GET \/api\/learning-support\/decks\?/, list([deck], 3)],
      [/GET \/api\/learning-support\/flashcards\?/, list([], 52)],
    ],
    targets: ['h1', '[role=tablist]', '.nb-stats', '.nb-cta', '.vocab-toolbar__filters'],
  },
  {
    file: 'vocabulary-and-flashcards-notebook-review', url: '/vocabulary/notebook/review', wait: 14000,
    fixtures: [[/GET \/api\/learning-support\/flashcards\/due/, list(due)], [/POST .*\/reviews/, {}]],
    script: `for (let i = 0; i < 11 && !document.querySelector('.review-flip'); i++) { document.querySelector('.vocab-choice-btn').click(); await __wait(1500) }`,
    targets: ['button[aria-label="Thoát phiên ôn"]', '.vocab-quiz-card', null],
  },
  {
    file: 'vocabulary-and-flashcards-deck-detail', url: `/flashcards/decks/${deckId}`, wait: 14000, fixtures: deckFixtures,
    targets: ['.flashcards-back', 'h1', '.deck-hub-modes', '#deck-cards'],
  },
  {
    file: 'vocabulary-and-flashcards-deck-editor', url: `/flashcards/decks/${deckId}/edit`, wait: 14000, fixtures: deckFixtures,
    script: `__click('Nhập')`,
    targets: ['.deck-editor-title', '.deck-editor-description', '.deck-editor-meta > :last-child', '.deck-editor-rows', '.deck-editor-import', '.deck-editor-actions'],
  },
  {
    file: 'vocabulary-and-flashcards-deck-learn-mode', url: `/flashcards/decks/${deckId}/learn`, wait: 14000, fixtures: deckFixtures,
    // Choose a wrong meaning so that the result of an answer is on screen.
    script: `const right = ${JSON.stringify(Object.fromEntries(deckItems.map((i) => [i.front, i.back])))}; const prompt = document.querySelector('.study-question-prompt').textContent.trim(); const wrong = [...document.querySelectorAll('.study-choices button')].find((b) => !b.textContent.includes(right[prompt])); wrong.click(); await __wait(900); const bubble = document.createTreeWalker(document.querySelector('.study-layout-with-coach'), NodeFilter.SHOW_TEXT); for (let node = bubble.nextNode(); node; node = bubble.nextNode()) { if (!node.parentElement.closest('.study-main-col') && node.textContent.trim().length > 15) node.textContent = 'Close one. Compare the two words.' }`,
    targets: ['h1', '.study-progress', '.study-question', '.study-feedback', null],
  },
  {
    file: 'vocabulary-and-flashcards-deck-test-mode', words: { 'Sai': 'False' }, url: `/flashcards/decks/${deckId}/test`, wait: 18000, fixtures: deckFixtures,
    script: `__click('Bắt đầu làm bài')`,
    targets: ['h1', null, null, '.test-questions', '.test-submit'],
  },
  {
    file: 'vocabulary-and-flashcards-flashcard-study', url: '/flashcards/study', wait: 14000,
    fixtures: [[/GET \/api\/learning-support\/flashcards\/due/, list(due.slice(0, 8))]],
    script: `__click('Lật thẻ')`,
    targets: ['.flashcards-back', 'section[aria-label="Thẻ đang ôn"]', '.flashcards-study-actions', null],
  },
)

// ---- Video dictation ----
const video = (n, title, level, total, passed, seconds) => ({ id: uid(8000 + n), youtubeVideoId: `mock-video-${n}`, title, thumbnailUrl: null, durationSeconds: seconds, level, totalSegments: total, passedSegments: passed, lastPracticedAt: passed ? '2026-11-25T08:00:00Z' : null })
const videos = [video(1, 'A day at the library', 'B1', 18, 6, 192), video(2, 'Ordering food in a cafe', 'B1', 14, 0, 156), video(3, 'Booking a train ticket', 'B1', 16, 0, 171), video(4, 'How cities save water', 'B2', 22, 0, 268), video(5, 'The history of paper', 'B2', 25, 0, 301)]
const videoId = videos[0].id
const lines = ['The library opens at nine in the morning.', 'Students can borrow up to five books.', 'Please keep quiet in the reading room.', 'You can return books at the front desk.']
const segments = lines.map((transcript, i) => ({ id: uid(8100 + i), sequenceNo: i + 1, startMs: i * 4000, endMs: i * 4000 + 3500, transcript, translationVi: null, ipaWords: transcript.split(' ').map(() => null) }))
const videoFixtures = [
  [new RegExp(`GET /api/learning-support/video-dictation/videos/${videoId}/progress`), { videoId, totalSegments: 4, passedSegments: 0, nextSegmentId: segments[0].id, nextSequenceNo: 1, segments: segments.map((s) => ({ segmentId: s.id, sequenceNo: s.sequenceNo, attempts: 0, bestAccuracyPercent: null, passed: false })) }],
  [new RegExp(`GET /api/learning-support/video-dictation/videos/${videoId}$`), { ...videos[0], youtubeUrl: 'about:blank', description: null, segments }],
  [/GET \/api\/learning-support\/video-dictation\/videos$/, videos],
]
// A stand-in for the YouTube player API, set before the player page opens, so that nothing is loaded from outside.
const fakePlayer = `window.YT = { PlayerState: { ENDED: 0, PLAYING: 1 }, Player: function (mount, options) { const screen = document.createElement('canvas'); screen.style.cssText = 'width:100%;height:100%;display:block'; mount.style.cssText = 'width:100%;height:100%'; mount.appendChild(screen); Object.assign(this, { seekTo() {}, playVideo() { setTimeout(() => options.events.onStateChange({ data: 1 }), 10) }, pauseVideo() {}, getCurrentTime: () => 1e6, getPlayerState: () => 2, setPlaybackRate() {}, unloadModule() {}, setOption() {}, destroy() {} }); setTimeout(() => options.events.onReady(), 100) } }`

screens.push(
  { file: 'video-dictation-dictation-library', url: '/video-dictation', wait: 14000, fixtures: videoFixtures, words: { 'câu': 'sentences', 'Câu': 'Sentence', 'video': 'videos', 'Đang học': 'In progress' }, targets: ['h1', '.vd-search', '.vd-levels', '.vd-group'] },
  {
    file: 'video-dictation-dictation-player', url: '/video-dictation', wait: 18000, height: 760, words: { 'Kiểm tra': 'Check', 'câu': 'sentences', 'Câu': 'Sentence', 'Đang học': 'In progress' }, fixtures: videoFixtures,
    script: [
      `${fakePlayer}; document.querySelector('a[href="/video-dictation/${videoId}"]').click(); await __wait(5000)`,
      `__click('Bắt đầu nghe câu'); await __wait(1200); __fill('#vdp-answer', 'The library opens at nine')`,
    ],
    targets: ['.vdp-bar', '.vdp-stage__video', '[aria-labelledby="vdp-card-title"]', 'text=Check', '.vdp-controls__replay'],
  },
)

// Run first on every screen: the content of a closed <details> (account menu) is not visible but would be drawn, and
// the link stretched over a dictionary card would make the extractor treat the card text as covered.
for (const screen of screens) {
  const tidy = `document.querySelectorAll('details:not([open]) > :not(summary)').forEach((el) => { el.style.display = 'none' }); document.querySelectorAll('.dict-card__link').forEach((el) => { el.style.pointerEvents = 'none' }); document.querySelectorAll('.review-flip:not(.is-flipped) .is-back, .flashcard-face:not(.is-flipped) .flashcard-face-back').forEach((el) => { el.style.visibility = 'hidden' })`
  screen.script = [...[screen.script ?? []].flat(), tidy]
}