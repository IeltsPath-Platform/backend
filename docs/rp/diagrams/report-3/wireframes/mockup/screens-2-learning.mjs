// Learning path screens, drawn with the built-in mock of the frontend (dev server on port 5198).
import { signedIn } from './common.mjs'
export const common = signedIn('CUSTOMER')
// The demo tools of the mock are not part of the product.
const noDemo = `let el = [...document.querySelectorAll('button')].find((b) => b.textContent.includes('Đặt lại demo')); while (el && el.parentElement && !el.textContent.includes('Dữ liệu giả lập')) el = el.parentElement; if (el) el.remove()`
const state = (extra = {}) => ({ 'ieltspath.learning-path.mock.v3': { version: 3, seq: 5, completedLessons: [], reviews: [], topicTests: {}, courseTests: {}, assignments: {}, attempts: {}, processedRequests: {}, ...extra } })
const learn = (file, url, extra = {}) => ({ file, url, port: 5198, wait: 5000, ...extra, script: [noDemo, ...[extra.script ?? []].flat()] })

export const screens = [
  learn('learning-path-course-list', '/learn'),
  learn('learning-path-topic-list', '/learn/courses/mock-course', { storage: state({ completedLessons: ['lesson-l1'] }) }),
  learn('learning-path-knowledge-tree', '/learn/courses/mock-course/tree', { script: `document.querySelectorAll('.react-flow__background, .react-flow__attribution').forEach((el) => el.remove())`, targets: [undefined, undefined, '.react-flow', 'text=Builds on', 'text=Common mistakes'] }),
  learn('learning-path-topic', '/learn/topics/topic-demo-reading', { storage: state({ completedLessons: ['lesson-l1', 'lesson-l2'] }), targets: ['h1'] }),
  learn('learning-path-lesson', '/learn/lessons/lesson-l1'),
  learn('learning-path-practice-set', '/learn/lessons/lesson-l2?view=practice', { storage: state({ completedLessons: ['lesson-l1'] }) }),
  learn('learning-path-review-basket', '/learn/review-basket', { targets: ['h1', undefined, 'text=False and Not Given', undefined, 'main a[href="/learn"]'] }),
]