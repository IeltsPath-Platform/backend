// Answers that every signed-in page asks for, and small builders for made-up people. All data is invented.
export const uid = (n) => `${String(n).padStart(8, '0')}-1111-4111-8111-111111111111`
export const person = (n, fullName, email, role, status = 'ACTIVE') => ({
  id: uid(n), email, fullName, phoneNumber: null, status, emailVerified: true, hasPassword: true,
  roles: [{ id: uid(900 + n), name: role }], createdAt: '2026-09-01T08:00:00Z', updatedAt: '2026-11-01T08:00:00Z',
})
export const me = (role) => person(1, 'An Nguyen', 'an.nguyen@example.com', role)
export const page = (content) => ({ content, page: 0, size: 25, totalElements: content.length, totalPages: content.length ? 1 : 0 })
export const signedIn = (role) => [
  [/POST \/auth\/refresh/, { accessToken: 'mock.token.value', expiresIn: 3600 }],
  [/GET \/api\/users\/me$/, me(role)],
  [/GET \/api\/access\/me\/points$/, { userId: uid(1), balance: 120, totalCredited: 200, totalDebited: 80, updatedAt: '2026-11-01T08:00:00Z' }],
  [/GET \/api\/users\/me\/profile/, { displayName: 'An', avatarReference: null, bio: '', selfReportedBand: 5.5, timezone: 'Asia/Ho_Chi_Minh', visibility: 'PUBLIC' }],
  [/GET \/api\/notifications\/unread-count/, { count: 2 }],
  [/(POST|PUT) \/api\/users\/me\/study-sessions.*/, {}],
]