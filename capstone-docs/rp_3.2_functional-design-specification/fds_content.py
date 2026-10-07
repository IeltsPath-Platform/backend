"""Content of the IELTSPath FDS: document meta, API catalog, Part 1, conventions, background jobs.

Screens live in fds_screens_learner.py and fds_screens_staff.py. API ids are assigned in catalog order, so
screens refer to an endpoint by its key (for example "auth.login") and never hard-code an API number.
Facts were checked against the code at commit ecd2266 (no API change up to 6351f3a) and against SRS v0.9.15.
"""
from pathlib import Path

HERE = Path(__file__).resolve().parent
VERSION = "v0.4"
AUTHOR = "SonLH"
SCREENSHOT_DIR = HERE / "screenshots"
FRONTEND_REF = "IeltsPath Platform Frontend (React 19 + Vite 8), branch feat/ui-main-flow, commit ac25901"
TDS_REF = "IELTSPath_TDS v0.9.16 (Draft) — Report 4_TDS_IELTSPath_IeltsPath_v1.docx"
RTW_REF = "IELTSPath_RTW v1.0 (05/10/2026) — Report 3.1_RTW_IELTSPath_v1.xlsx"
# Endpoints that TDS Part 2.4 specifies in full (request body, status codes, response body).
TDS_24_KEYS = {"user.register", "auth.login", "auth.refresh", "learn.topics", "learn.lesson", "learn.submitBlock",
               "learn.submitEssay", "learn.startPractice", "learn.submitPractice", "learn.submitReview",
               "learn.assignTest", "as.start", "as.save", "as.submit", "as.result", "access.activate"}
FE_NOT_BUILT ="Not built in the frontend yet — this section is the design target for the frontend team."
# Short frontend labels used in the Screen Index.
FE_LABELS = {"api": "FE: built, real API", "mock": "FE: built on mock data", "partial": "FE: partly built",
             "placeholder": "FE: placeholder route", None: "FE: not built"}

DOC_INFO = [
    ("Document Title", "English Learning and IELTS Preparation Support System Based on Personalized Learning "
                       "Pathways for IELTS Space (IELTSPath)"),
    ("Version", VERSION),
    ("Date", "07/10/2026"),
    ("Status", "Draft"),
    ("Author(s)", AUTHOR),
    ("Reviewer(s)", "[Name / Role]"),
    ("SRS Reference", "IELTSPath_SRS v0.9.15 (05/10/2026) — Report 3.0_SRS_IELTSPath_IeltsPath_v1.docx"),
    ("Frontend Reference", FRONTEND_REF),
    ("TDS Reference", TDS_REF),
    ("RTW Reference", RTW_REF + " — Sheet 2 Use Case List (UC ids), Sheet 4 Permission Matrix (role policy), "
                      "Sheet 6 Business Rules (BR ids). The RTW has no screen or API sheet: Section 1.1 Screen Index, "
                      "Appendix A and Appendix D hold those lists."),
]

VERSION_HISTORY = [
    ("v0.1", "05/10/2026", AUTHOR,
     "Initial draft from SRS v0.9.6: Part 1 screen navigation flow, screen index and job schedule; Part 2 "
     "common conventions and 43 screen specifications covering FT-01 to FT-54; Part 3 background jobs JOB-01 "
     "to JOB-07; Appendix A API catalog; Appendix B open questions."),
    ("v0.2", "05/10/2026", AUTHOR,
     "Aligned with the frontend (" + FRONTEND_REF + "): real routes, guards, Vietnamese UI copy, API calls and "
     "error handling for the built screens; added Home, OAuth Callback, Overview, Practice Test Catalog, Practice "
     "Workspaces, Classroom and Classroom Lesson Workspace; every screen states its frontend status; screenshots "
     "embedded where the screen exists; Appendix C lists frontend–SRS gaps."),
    ("v0.3", "06/10/2026", AUTHOR,
     "Cross-checked with " + TDS_REF + ": TDS reference filled; backend session, error body, paging and "
     "correlation conventions added to Section 2.0; Appendix A marks the endpoints detailed in TDS 2.4, uses the "
     "admin vocabulary write paths and flags the internal access endpoints; JOB-01 payload uses the wire field "
     "names of the event contract; Lesson Player and Test Attempt cover PAYMENT_UNAVAILABLE, stale revision and "
     "ATTEMPT_EXPIRED; new open questions OQ-18 – OQ-20 and frontend gap GAP-13."),
    ("v0.4", "07/10/2026", AUTHOR,
     "Traced to " + RTW_REF + ": every screen names its use cases by RTW id (UC-01 – UC-42); use cases missing "
     "from screens were added (UC-05 Send reset link, UC-13 Grade essay, UC-14 Submit Speaking recording, UC-16 "
     "Request AI grading, UC-25 Request examiner grading); Plans & Key Products cites BR-08 for key product limits "
     "instead of BR-31; Test Result cites BR-31; Section 2.0 points to the RTW Permission Matrix for role policy; "
     "new Appendix D maps each use case and business rule to its screens and jobs. Full review against the "
     "backend controllers: six endpoints missing from Appendix A were added (admin delete learner profile, get and "
     "delete one video progress, get one flashcard, get one deck, expire an attempt) and the /api/content/admin "
     "path aliases are explained; User Detail can delete a learner profile; Media Assets also traces UC-40; Login "
     "cites GAP-11; each screen and job lists the open questions that affect it, and OQ-20 names the right "
     "screens. Aligned with SRS v0.9.15 (Part 6 NFRs): SRS reference updated; Login and Forgot Password add the "
     "sign-in and reset rate limits (NFR-SEC11) and the same-response rule for unknown emails (NFR-SEC12); Register "
     "and Reset Password cite the 72-byte password limit (NFR-SEC04); Lesson Player cites the 45-second grading "
     "limit (NFR-P03); Test Result and JOB-01 cite the 5-second result delivery (NFR-P05) and new frontend gap "
     "GAP-14; Section 2.0 and Appendix A cite NFR-P04, NFR-U01 and NFR-U02."),
]

# ------------------------------------------------------------------ API catalog
# key: (method, path, service, roles, FT, status). Paths are the paths seen through the API Gateway.
_L = "CUSTOMER"
_ANY = "Any signed-in role"
_AUTHOR = "ADMIN, CONTENT_AUTHOR"
_READERS = "ADMIN, CONTENT_AUTHOR, CUSTOMER, EXAMINER"
_TBC = "Draft — path [TBC]"
_INTERNAL = "ADMIN token (service-to-service)"
API_CATALOG = [
    # user-service
    ("user.register", "POST", "/api/users/register", "user", "Public", "FT-01", "Specified"),
    ("auth.login", "POST", "/auth/login", "user", "Public", "FT-02", "Specified"),
    ("auth.refresh", "POST", "/auth/refresh", "user", "Public (refresh token)", "FT-03", "Specified"),
    ("auth.logout", "POST", "/auth/logout", "user", "Public (refresh token)", "FT-03", "Specified"),
    ("auth.me", "GET", "/auth/me", "user", _ANY, "FT-02, FT-07", "Specified"),
    ("auth.forgot", "POST", "/auth/forgot-password", "user", "Public", "FT-04", "Specified"),
    ("auth.reset", "POST", "/auth/reset-password", "user", "Public", "FT-04", "Specified"),
    ("auth.oauthStart", "GET", "/auth/oauth/{provider}/authorize?redirect_uri=…", "user", "Public", "FT-05",
     "Draft — frontend already builds this URL; backend endpoint missing"),
    ("user.me", "GET", "/api/users/me", "user", _ANY, "FT-06", "Specified"),
    ("user.updateMe", "PUT", "/api/users/me", "user", _ANY, "FT-06", "Specified"),
    ("user.profile", "GET", "/api/users/me/profile", "user", _ANY, "FT-06", "Specified"),
    ("user.updateProfile", "PUT", "/api/users/me/profile", "user", _ANY, "FT-06", "Specified"),
    ("user.oauth", "GET", "/api/users/me/oauth", "user", _ANY, "FT-05", "Specified"),
    ("user.unlinkOauth", "DELETE", "/api/users/me/oauth/{provider}", "user", _ANY, "FT-05", "Specified"),
    ("goal.active", "GET", "/api/users/me/learning-goals/active", "user", _ANY, "FT-09", "Specified"),
    ("goal.list", "GET", "/api/users/me/learning-goals", "user", _ANY, "FT-09", "Specified"),
    ("goal.create", "POST", "/api/users/me/learning-goals", "user", _ANY, "FT-09", "Specified"),
    ("goal.status", "PUT", "/api/users/me/learning-goals/{goalId}/status", "user", _ANY, "FT-09", "Specified"),
    ("admin.users", "GET", "/api/users", "user", "ADMIN", "FT-08", "Specified (no paging yet)"),
    ("admin.createUser", "POST", "/api/users", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.user", "GET", "/api/users/{id}", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.updateUser", "PUT", "/api/users/{id}", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.roles", "PUT", "/api/users/{id}/roles", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.status", "PUT", "/api/users/{id}/status", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.deleteUser", "DELETE", "/api/users/{id}", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.profile", "GET", "/api/users/{id}/profile", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.updateProfile", "PUT", "/api/users/{id}/profile", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.deleteProfile", "DELETE", "/api/users/{id}/profile", "user", "ADMIN", "FT-08", "Specified"),
    ("admin.goals", "GET", "/api/users/{id}/learning-goals", "user", "ADMIN", "FT-08, FT-09", "Specified"),
    ("admin.goalStatus", "PUT", "/api/users/{id}/learning-goals/{goalId}/status", "user", "ADMIN", "FT-08",
     "Specified"),
    ("admin.oauth", "GET", "/api/users/{id}/oauth", "user", "ADMIN", "FT-05, FT-08", "Specified"),
    ("admin.unlinkOauth", "DELETE", "/api/users/{id}/oauth/{provider}", "user", "ADMIN", "FT-05, FT-08",
     "Specified"),
    ("admin.tokens", "GET", "/api/users/{id}/action-tokens", "user", "ADMIN", "FT-04, FT-08", "Specified"),
    ("admin.revokeTokens", "DELETE", "/api/users/{id}/action-tokens", "user", "ADMIN", "FT-04, FT-08",
     "Specified"),
    ("activity.create", "POST", "/api/learning-support/activities", "user", _ANY, "FT-44", "Specified"),
    ("activity.list", "GET", "/api/learning-support/activities", "user", _ANY, "FT-44", "Specified"),
    ("activity.delete", "DELETE", "/api/learning-support/activities/{id}", "user", _ANY, "FT-44", "Specified"),
    ("streak.get", "GET", "/api/learning-support/streak", "user", _ANY, "FT-45", "Specified"),
    ("streak.put", "PUT", "/api/learning-support/streak", "user", _ANY, "FT-45", "Specified"),
    # access-service
    ("access.plans", "GET", "/api/access/plans", "access", _ANY, "FT-10", "Specified"),
    ("access.products", "GET", "/api/access/key-products", "access", _ANY, "FT-10", "Specified"),
    ("access.mySub", "GET", "/api/access/me/subscription", "access", _ANY, "FT-10, FT-14", "Specified"),
    ("access.myPoints", "GET", "/api/access/me/points", "access", _ANY, "FT-12", "Specified"),
    ("access.myHistory", "GET", "/api/access/me/points/history", "access", _ANY, "FT-12", "Specified"),
    ("access.activate", "POST", "/api/access/me/keys/activate", "access", _ANY, "FT-11", "Specified"),
    ("access.generate", "POST", "/api/access/admin/keys/generate", "access", "ADMIN (SALES_STAFF: Draft)",
     "FT-11", "Specified"),
    ("access.revoke", "POST", "/api/access/admin/keys/{id}/revoke", "access", "ADMIN (SALES_STAFF: Draft)",
     "FT-11", "Specified"),
    ("access.keyList", "GET", "/api/access/admin/keys", "access", "ADMIN, SALES_STAFF", "FT-11", _TBC),
    ("access.createPlan", "POST", "/api/access/admin/plans", "access", "ADMIN", "FT-10", "Specified"),
    ("access.createProduct", "POST", "/api/access/admin/key-products", "access", "ADMIN", "FT-10, FT-14",
     "Specified"),
    ("access.adjust", "POST", "/api/access/admin/points/adjust", "access", "ADMIN", "FT-12", "Specified"),
    ("access.grant", "POST", "/api/access/admin/subscriptions/grant", "access", "ADMIN", "FT-10, FT-14",
     "Specified"),
    ("access.entitlement", "GET", "/api/access/users/{userId}/entitlement", "access", _INTERNAL, "FT-13",
     "Specified (no caller yet, TDS 2.5)"),
    ("access.refund", "POST", "/api/access/points/refund", "access", _INTERNAL, "FT-12",
     "Specified (no caller yet, TDS 2.5)"),
    ("access.consumeCredit", "POST", "/api/access/users/{userId}/consume-human-grading", "access",
     _INTERNAL, "FT-14", "Specified (no caller yet, TDS 2.5; learner flow: Draft)"),
    # content-service
    ("content.topics", "GET", "/api/content/topics", "content", _READERS, "FT-15", "Specified"),
    ("content.topic", "GET", "/api/content/topics/{id}", "content", _READERS, "FT-15", "Specified"),
    ("content.createTopic", "POST", "/api/content/topics", "content", _AUTHOR, "FT-15", "Specified"),
    ("content.updateTopic", "PUT", "/api/content/topics/{id}", "content", _AUTHOR, "FT-15", "Specified"),
    ("content.kps", "GET", "/api/content/knowledge-points", "content", _ANY, "FT-15", "Specified"),
    ("content.createKp", "POST", "/api/content/knowledge-points", "content", _AUTHOR, "FT-15", "Specified"),
    ("content.questions", "GET", "/api/content/questions", "content", _AUTHOR, "FT-16", "Specified"),
    ("content.question", "GET", "/api/content/questions/{id}", "content", _AUTHOR, "FT-16", "Specified"),
    ("content.createQuestion", "POST", "/api/content/questions", "content", _AUTHOR, "FT-16", "Specified"),
    ("content.addQuestionVersion", "POST", "/api/content/questions/{id}/versions", "content", _AUTHOR,
     "FT-16", "Specified"),
    ("content.archiveQuestion", "POST", "/api/content/questions/{id}/archive", "content", _AUTHOR, "FT-16",
     "Specified"),
    ("content.packages", "GET", "/api/content/packages", "content", _AUTHOR, "FT-17", "Specified"),
    ("content.package", "GET", "/api/content/packages/{id}", "content", _AUTHOR, "FT-17", "Specified"),
    ("content.createPackage", "POST", "/api/content/packages", "content", _AUTHOR, "FT-17", "Specified"),
    ("content.addPackageVersion", "POST", "/api/content/packages/{id}/versions", "content", _AUTHOR, "FT-17",
     "Specified"),
    ("content.addSection", "POST", "/api/content/packages/versions/{versionId}/sections", "content", _AUTHOR,
     "FT-17", "Specified"),
    ("content.publish", "POST", "/api/content/packages/{id}/publish", "content", _AUTHOR, "FT-17", "Specified"),
    ("content.asset", "GET", "/api/content/assets/{id}", "content", _AUTHOR, "FT-18", "Specified"),
    ("content.createAsset", "POST", "/api/content/assets", "content", _AUTHOR, "FT-18", "Specified"),
    ("content.linkAsset", "POST", "/api/content/assets/links", "content", _AUTHOR, "FT-18", "Specified"),
    ("content.upload", "POST", "/api/content/assets/uploads", "content", _AUTHOR, "FT-18", _TBC),
    ("content.reading", "GET", "/api/content/reading/sections/{sectionId}", "content", _READERS, "FT-42",
     "Specified"),
    ("content.lessons", "GET/POST/PUT", "/api/content/lessons…", "content", _AUTHOR, "FT-19", _TBC),
    # library-service (the Gateway routes these prefixes to library)
    ("vocab.search", "GET", "/api/content/vocabulary/search", "library", _READERS, "FT-36", "Specified"),
    ("vocab.get", "GET", "/api/content/vocabulary/{id}", "library", _READERS, "FT-36", "Specified"),
    ("vocab.create", "POST", "/api/content/admin/vocabulary", "library", _AUTHOR, "FT-36", "Specified"),
    ("vocab.addSense", "POST", "/api/content/admin/vocabulary/{id}/senses", "library", _AUTHOR, "FT-36", "Specified"),
    ("video.list", "GET", "/api/content/videos", "library", _READERS, "FT-37", "Specified"),
    ("video.get", "GET", "/api/content/videos/{id}", "library", _READERS, "FT-37", "Specified"),
    ("video.create", "POST", "/api/content/videos", "library", _AUTHOR, "FT-37", "Specified"),
    ("video.addSegment", "POST", "/api/content/videos/{id}/segments", "library", _AUTHOR, "FT-37", "Specified"),
    ("video.addLexical", "POST", "/api/content/videos/segments/{segmentId}/lexical-entries", "library", _AUTHOR,
     "FT-37", "Specified"),
    ("video.publish", "POST", "/api/content/videos/{id}/publish", "library", _AUTHOR, "FT-37", "Specified"),
    ("progress.put", "PUT", "/api/learning-support/video-progress", "library", _ANY, "FT-38", "Specified"),
    ("progress.list", "GET", "/api/learning-support/video-progress", "library", _ANY, "FT-38", "Specified"),
    ("progress.get", "GET", "/api/learning-support/video-progress/{id}", "library", _ANY, "FT-38", "Specified"),
    ("progress.delete", "DELETE", "/api/learning-support/video-progress/{id}", "library", _ANY, "FT-38",
     "Specified"),
    ("segment.save", "POST", "/api/learning-support/saved-segments", "library", _ANY, "FT-38", "Specified"),
    ("segment.list", "GET", "/api/learning-support/saved-segments", "library", _ANY, "FT-38", "Specified"),
    ("segment.delete", "DELETE", "/api/learning-support/saved-segments/{id}", "library", _ANY, "FT-38",
     "Specified"),
    ("note.create", "POST", "/api/learning-support/notes", "library", _ANY, "FT-42", "Specified"),
    ("note.list", "GET", "/api/learning-support/notes", "library", _ANY, "FT-42", "Specified"),
    ("note.get", "GET", "/api/learning-support/notes/{id}", "library", _ANY, "FT-42", "Specified"),
    ("note.update", "PUT", "/api/learning-support/notes/{id}", "library", _ANY, "FT-42", "Specified"),
    ("note.delete", "DELETE", "/api/learning-support/notes/{id}", "library", _ANY, "FT-42", "Specified"),
    ("card.create", "POST", "/api/learning-support/flashcards", "library", _ANY, "FT-43", "Specified"),
    ("card.list", "GET", "/api/learning-support/flashcards", "library", _ANY, "FT-43", "Specified"),
    ("card.get", "GET", "/api/learning-support/flashcards/{id}", "library", _ANY, "FT-43", "Specified"),
    ("card.update", "PUT", "/api/learning-support/flashcards/{id}", "library", _ANY, "FT-43", "Specified"),
    ("card.delete", "DELETE", "/api/learning-support/flashcards/{id}", "library", _ANY, "FT-43", "Specified"),
    ("deck.create", "POST", "/api/learning-support/decks", "library", _ANY, "FT-43", "Specified"),
    ("deck.list", "GET", "/api/learning-support/decks", "library", _ANY, "FT-43", "Specified"),
    ("deck.get", "GET", "/api/learning-support/decks/{id}", "library", _ANY, "FT-43", "Specified"),
    ("deck.update", "PUT", "/api/learning-support/decks/{id}", "library", _ANY, "FT-43", "Specified"),
    ("deck.delete", "DELETE", "/api/learning-support/decks/{id}", "library", _ANY, "FT-43", "Specified"),
    ("deck.addItem", "POST", "/api/learning-support/decks/{deckId}/items", "library", _ANY, "FT-43",
     "Specified"),
    ("deck.items", "GET", "/api/learning-support/decks/{deckId}/items", "library", _ANY, "FT-43", "Specified"),
    ("deck.removeItem", "DELETE", "/api/learning-support/decks/{deckId}/items/{flashcardId}", "library", _ANY,
     "FT-43", "Specified"),
    # learning-service
    ("learn.topics", "GET", "/api/learning/topics", "learning", _L, "FT-13, FT-20", "Specified"),
    ("learn.topicLessons", "GET", "/api/learning/topics/{id}/lessons", "learning", _L, "FT-20", "Specified"),
    ("learn.lesson", "GET", "/api/learning/lessons/{id}", "learning", _L, "FT-21, FT-23, FT-24", "Specified"),
    ("learn.submitBlock", "POST", "/api/learning/lessons/{id}/exercises/{blockId}/submissions", "learning", _L,
     "FT-22, FT-23", "Specified"),
    ("learn.complete", "POST", "/api/learning/lessons/{id}/complete", "learning", _L, "FT-21", "Specified"),
    ("learn.mastery", "GET", "/api/learning/mastery", "learning", _L, "FT-26", "Specified"),
    ("learn.practiceSets", "GET", "/api/learning/lessons/{id}/practice-sets", "learning", _L, "FT-27",
     "Specified"),
    ("learn.startPractice", "POST", "/api/learning/lessons/{id}/practice-attempts", "learning", _L, "FT-27",
     "Specified"),
    ("learn.practice", "GET", "/api/learning/practice-attempts/{id}", "learning", _L, "FT-27", "Specified"),
    ("learn.submitPractice", "POST", "/api/learning/practice-attempts/{id}/submissions", "learning", _L,
     "FT-27", "Specified"),
    ("learn.reviews", "GET", "/api/learning/reviews", "learning", _L, "FT-28", "Specified"),
    ("learn.review", "GET", "/api/learning/reviews/{reviewId}", "learning", _L, "FT-28", "Specified"),
    ("learn.submitReview", "POST", "/api/learning/reviews/{reviewId}/submissions", "learning", _L, "FT-28",
     "Specified"),
    ("learn.theoryCheck", "POST", "/api/learning/reviews/{reviewId}/theory-check", "learning", _L, "FT-28",
     "Specified"),
    ("learn.assignTest", "POST", "/api/learning/topics/{id}/test-assignments", "learning", _L, "FT-29",
     "Specified"),
    ("learn.submitEssay", "POST", "/api/learning/lessons/{lessonId}/essays/{blockId}/submissions", "learning",
     _L, "FT-25", "Specified"),
    ("learn.essay", "GET", "/api/learning/writing-submissions/{id}", "learning", _L, "FT-25", "Specified"),
    # assessment-service
    ("as.start", "POST", "/api/assessments/attempts", "assessment", _L, "FT-30, FT-34, FT-35", "Specified"),
    ("as.attempt", "GET", "/api/assessments/attempts/{id}", "assessment", "Attempt owner", "FT-30", "Specified"),
    ("as.structure", "GET", "/api/assessments/attempts/{id}/structure", "assessment", "Attempt owner",
     "FT-24, FT-30", "Specified"),
    ("as.save", "PUT", "/api/assessments/attempts/{id}/items/{itemId}/response", "assessment", "Attempt owner",
     "FT-30", "Specified"),
    ("as.expire", "POST", "/api/assessments/attempts/{id}/expire", "assessment", "Attempt owner", "FT-30",
     "Specified"),
    ("as.submit", "POST", "/api/assessments/attempts/{id}/submit", "assessment", "Attempt owner",
     "FT-30, FT-31", "Specified"),
    ("as.result", "GET", "/api/assessments/attempts/{attemptId}/result", "assessment", "Attempt owner",
     "FT-24, FT-31", "Specified"),
    ("as.mockList", "GET", "/api/assessments/catalog?type=MOCK|PLACEMENT", "assessment", _L, "FT-34, FT-35",
     _TBC),
    ("as.queue", "GET", "/api/assessments/grading/queue", "assessment", "ADMIN, EXAMINER", "FT-32", _TBC),
    ("as.openVersion", "POST", "/api/assessments/grading/attempts/{attemptId}/results", "assessment",
     "ADMIN, EXAMINER", "FT-32", "Specified"),
    ("as.saveDetails", "PUT", "/api/assessments/grading/results/{resultId}/details", "assessment",
     "ADMIN, EXAMINER", "FT-32", "Specified"),
    ("as.finalize", "POST", "/api/assessments/grading/results/{resultId}/finalize", "assessment",
     "ADMIN, EXAMINER", "FT-32", "Specified"),
    ("as.submission", "POST", "/api/assessments/submissions", "assessment", _L, "FT-33, FT-39", "Specified"),
    ("as.createJob", "POST", "/api/assessments/grading-jobs", "assessment", _L, "FT-33", "Specified"),
    ("as.job", "GET", "/api/assessments/grading-jobs/{id}", "assessment", "Job owner", "FT-33", "Specified"),
    ("as.videoPractice", "POST", "/api/assessments/video-practice", "assessment", _L, "FT-40, FT-41",
     "Specified"),
    ("as.recording", "POST", "/api/assessments/recordings", "assessment", _L, "FT-39, FT-40", _TBC),
    ("as.dictation", "POST", "/api/assessments/video-practice/{id}/dictation", "assessment", _L, "FT-41", _TBC),
    # game-service
    ("game.start", "POST", "/api/games/sessions", "game", _ANY, "FT-46", "Specified"),
    ("game.answer", "POST", "/api/games/sessions/{sessionId}/answers/{itemSequence}", "game", "Session owner",
     "FT-46", "Specified"),
    ("game.session", "GET", "/api/games/sessions/{sessionId}", "game", "Session owner", "FT-46", "Specified"),
    ("game.history", "GET", "/api/games/sessions", "game", _ANY, "FT-46", "Specified"),
    ("game.abandon", "POST", "/api/games/sessions/{sessionId}/abandon", "game", "Session owner", "FT-46",
     "Specified"),
    ("room.create", "POST", "/api/games/rooms", "game", _ANY, "FT-47", "Specified"),
    ("room.get", "GET", "/api/games/rooms/{roomId}", "game", "Room member", "FT-47", "Specified"),
    ("room.byCode", "GET", "/api/games/rooms/code/{roomCode}", "game", _ANY, "FT-47", "Specified"),
    ("room.join", "POST", "/api/games/rooms/{roomId}/join", "game", _ANY, "FT-47", "Specified"),
    ("room.joinByCode", "POST", "/api/games/rooms/code/{roomCode}/join", "game", _ANY, "FT-47", "Specified"),
    ("room.ready", "POST", "/api/games/rooms/{roomId}/ready", "game", "Room member", "FT-47", "Specified"),
    ("room.leave", "POST", "/api/games/rooms/{roomId}/leave", "game", "Room member", "FT-47", "Specified"),
    ("room.close", "POST", "/api/games/rooms/{roomId}/close", "game", "Room host", "FT-47", "Specified"),
    ("room.ticket", "POST", "/api/games/rooms/{roomId}/websocket-ticket", "game", "Room member", "FT-47",
     "Specified"),
    ("room.ws", "WS", "/ws/games (subprotocols game.v1, ticket.{ticket})", "game", "Ticket holder", "FT-47",
     "Specified"),
    ("game.match", "GET", "/api/games/matches/{matchId}", "game", "Match player", "FT-47", "Specified"),
    ("game.queue", "POST", "/api/games/matchmaking", "game", _ANY, "FT-48", _TBC),
    # community-service
    ("post.create", "POST", "/api/community/posts", "community", _ANY, "FT-49", "Specified"),
    ("post.list", "GET", "/api/community/posts", "community", _ANY, "FT-49", "Specified"),
    ("post.get", "GET", "/api/community/posts/{id}", "community", _ANY, "FT-49", "Specified"),
    ("post.edit", "PUT", "/api/community/posts/{id}", "community", "Post author", "FT-49", "Specified"),
    ("post.delete", "DELETE", "/api/community/posts/{id}", "community", "Post author", "FT-49", "Specified"),
    ("post.moderate", "PUT", "/api/community/posts/{id}/status", "community", "ADMIN", "FT-50", "Specified"),
    ("comment.create", "POST", "/api/community/posts/{postId}/comments", "community", _ANY, "FT-49", "Specified"),
    ("comment.list", "GET", "/api/community/posts/{postId}/comments", "community", _ANY, "FT-49", "Specified"),
    ("comment.edit", "PUT", "/api/community/comments/{id}", "community", "Comment author", "FT-49", "Specified"),
    ("comment.delete", "DELETE", "/api/community/comments/{id}", "community", "Comment author", "FT-49",
     "Specified"),
    ("comment.moderate", "PUT", "/api/community/comments/{id}/status", "community", "ADMIN", "FT-50",
     "Specified"),
    ("post.react", "PUT", "/api/community/posts/{postId}/reactions/{type}", "community", _ANY, "FT-49",
     "Specified"),
    ("post.unreact", "DELETE", "/api/community/posts/{postId}/reactions/{type}", "community", _ANY, "FT-49",
     "Specified"),
    # notification-service (skeleton only: reserved route, no API)
    ("notif.list", "GET", "/api/notifications", "notification", _L, "FT-54", _TBC),
    ("notif.read", "POST", "/api/notifications/{id}/read", "notification", _L, "FT-54", _TBC),
    ("notif.prefs", "GET", "/api/notifications/preferences", "notification", _L, "FT-51", _TBC),
    ("notif.savePrefs", "PUT", "/api/notifications/preferences/{channel}", "notification", _L, "FT-51", _TBC),
    ("notif.devices", "GET", "/api/notifications/devices", "notification", _L, "FT-51", _TBC),
    ("notif.registerDevice", "PUT", "/api/notifications/devices/{installationId}", "notification", _L, "FT-51",
     _TBC),
    ("notif.revokeDevice", "DELETE", "/api/notifications/devices/{installationId}", "notification", _L, "FT-51",
     _TBC),
]

_API_INDEX = {row[0]: (f"API-{i:02d}", row) for i, row in enumerate(API_CATALOG, 1)}
_API_USERS = {}
# Endpoints called by the app shell rather than one screen (Section 2.0).
_GLOBAL_USERS = {"auth.refresh": "All (2.0 session)", "auth.logout": "Top bar (all)"}


def api(key):
    """Returns (API id, 'METHOD path') for a catalog key; unknown keys fail the build."""
    if key not in _API_INDEX:
        raise KeyError(f"API key {key!r} is not in API_CATALOG")
    api_id, row = _API_INDEX[key]
    return api_id, f"{row[1]} {row[2]}"


def register_use(api_key, screen_number):
    _API_USERS.setdefault(api_key, []).append(screen_number)


def api_catalog_rows():
    rows = []
    for key, method, path, service, roles, ft, status in API_CATALOG:
        api_id, _ = _API_INDEX[key]
        used = ", ".join(sorted(set(_API_USERS.get(key, [])), key=lambda s: [int(x) for x in s.split(".")]))
        used = used or _GLOBAL_USERS.get(key, "— (not called by any screen)")
        if key in TDS_24_KEYS:
            status += " · detail: TDS 2.4"
        rows.append((api_id, f"`{method} {path}`", service, roles, used, ft, status))
    return rows


# ------------------------------------------------------------------ RTW traceability
# Copied from the RTW (Sheet 2 Use Case List: id → name, FT, status; Sheet 6 Business Rules: id → short title,
# status, owner). Screens name use cases as "UC: <name>; <name>"; the build turns each name into its RTW id and
# stops when a name is not in this list, so the FDS and the RTW cannot drift apart silently.
USE_CASES = {
    "UC-01": ("Register", "FT-01", "Specified"),
    "UC-02": ("Sign in", "FT-02, FT-03, FT-05", "Specified"),
    "UC-03": ("Sign in with OAuth", "FT-05", "Draft"),
    "UC-04": ("Reset password", "FT-04", "Specified"),
    "UC-05": ("Send reset link", "FT-04", "Draft"),
    "UC-06": ("Manage profile and learning goal", "FT-06, FT-09", "Specified"),
    "UC-07": ("Manage users and roles", "FT-08", "Specified"),
    "UC-08": ("View learning path", "FT-13, FT-20, FT-29", "Specified"),
    "UC-09": ("Study lesson", "FT-13, FT-21, FT-22, FT-23, FT-24", "Specified"),
    "UC-10": ("Do lesson practice", "FT-27", "Specified"),
    "UC-11": ("Complete review", "FT-28", "Specified"),
    "UC-12": ("Submit Writing task", "FT-25", "Specified"),
    "UC-13": ("Grade essay", "FT-25, FT-33", "Specified"),
    "UC-14": ("Submit Speaking recording", "FT-39", "Draft"),
    "UC-15": ("Assess speech", "FT-33, FT-39, FT-40", "Draft"),
    "UC-16": ("Request AI grading", "FT-33", "Draft"),
    "UC-17": ("Practise shadowing", "FT-40", "Draft"),
    "UC-18": ("View mastery", "FT-26", "Specified"),
    "UC-19": ("View activity and streak", "FT-44, FT-45", "Specified"),
    "UC-20": ("Take test attempt", "FT-24, FT-29, FT-30, FT-31, FT-34, FT-35", "Specified"),
    "UC-21": ("Take mock test", "FT-34", "Draft"),
    "UC-22": ("View result", "FT-31, FT-34, FT-35", "Specified"),
    "UC-23": ("Take placement test", "FT-35", "Draft"),
    "UC-24": ("Grade attempt manually and finalize result", "FT-32", "Specified"),
    "UC-25": ("Request examiner grading", "FT-14", "Draft"),
    "UC-26": ("Activate key", "FT-10, FT-11, FT-14", "Specified"),
    "UC-27": ("View points", "FT-12, FT-25", "Specified"),
    "UC-28": ("Generate and revoke activation keys", "FT-11", "Specified"),
    "UC-29": ("Manage plans and point adjustments", "FT-10, FT-12", "Specified"),
    "UC-30": ("View subscription", "FT-10", "Specified"),
    "UC-31": ("Receive study reminders and grading-done alerts", "FT-52, FT-53, FT-54", "Draft"),
    "UC-32": ("Manage notification preferences", "FT-51", "Draft"),
    "UC-33": ("Manage notes and flashcards", "FT-42, FT-43", "Specified"),
    "UC-34": ("Play game", "FT-46, FT-47, FT-48", "Specified"),
    "UC-35": ("Post and comment", "FT-49", "Specified"),
    "UC-36": ("Moderate community", "FT-50", "Specified"),
    "UC-37": ("Browse vocabulary and videos", "FT-36, FT-37", "Specified"),
    "UC-38": ("Track video progress and saved segments", "FT-38", "Specified"),
    "UC-39": ("Practise dictation", "FT-41", "Draft"),
    "UC-40": ("Manage topics, knowledge points, questions, packages, vocabulary and videos",
              "FT-15, FT-16, FT-17, FT-18, FT-36, FT-37", "Specified"),
    "UC-41": ("Upload media", "FT-18", "Draft"),
    "UC-42": ("Author lessons", "FT-19", "Draft"),
}
_UC_BY_NAME = {name: uc_id for uc_id, (name, _, _) in USE_CASES.items()}

BUSINESS_RULES = {
    "BR-01": ("Registration creates one CUSTOMER account per email", "Approved", "user-service"),
    "BR-02": ("Password length; BCrypt hash only", "Approved", "user-service"),
    "BR-03": ("Reset token hashed, single-use, 15 minutes", "Approved", "user-service"),
    "BR-04": ("Access token 3,600 s; refresh token 7 days, rotated", "Approved", "user-service"),
    "BR-05": ("At most one ACTIVE learning goal", "Approved", "user-service"),
    "BR-06": ("Five canonical roles; only ADMIN manages users", "Approved", "user-service, api-gateway"),
    "BR-07": ("Activation key redeemed once; 1–1,000 keys per request", "Approved", "access-service"),
    "BR-08": ("PREMIUM key extends subscription; POINTS key credits wallet", "Approved", "access-service"),
    "BR-09": ("Point balance ≥ 0; ledger entry per change", "Approved", "access-service"),
    "BR-10": ("AI Writing grading costs 3 points, charged after success", "Approved",
              "learning-service, access-service"),
    "BR-11": ("At most 10 AI Writing gradings per day", "Approved", "learning-service"),
    "BR-12": ("One skill per topic; locked once lessons are published", "Approved", "content-service"),
    "BR-13": ("One owner per question", "Approved", "content-service"),
    "BR-14": ("Question purpose LEARNING or EXAM fits the package", "Approved", "content-service"),
    "BR-15": ("Pass mark 70%", "Approved", "learning-service, assessment-service"),
    "BR-16": ("Topic order per skill; one IN_PROGRESS topic", "Approved", "learning-service"),
    "BR-17": ("Lesson gates; pending review blocks its own skill", "Approved", "learning-service"),
    "BR-18": ("Evidence only from first submissions", "Approved", "learning-service"),
    "BR-19": ("Answers, hints and transcripts hidden until earned", "Approved", "learning-service, content-service"),
    "BR-20": ("Revealed practice set never reused as review set", "Approved", "learning-service"),
    "BR-21": ("Review ladder: set → theory → set", "Approved", "learning-service"),
    "BR-22": ("Final test prerequisites; single-use test code", "Approved", "learning-service"),
    "BR-23": ("Mastery from latest 5 outcomes; review below 0.6", "Approved", "learning-service"),
    "BR-24": ("Test solutions only at ≥ 70%", "Approved", "assessment-service"),
    "BR-25": ("Each result version applied once; newer replaces older", "Approved",
              "learning-service, assessment-service"),
    "BR-26": ("Only EXAMINER and ADMIN grade and finalize", "Approved", "assessment-service"),
    "BR-27": ("Personal data owner-only; catalog writes by staff", "Approved", "All services owning personal data"),
    "BR-28": ("Own community content only; ADMIN moderates", "Approved", "community-service"),
    "BR-29": ("Game scoring, snapshot size and room rules", "Approved", "game-service"),
    "BR-30": ("Same requestId / idempotency key never applied twice", "Approved",
              "learning-service, access-service, assessment-service"),
    "BR-31": ("Examiner grading consumes one credit", "Draft", "access-service, assessment-service"),
    "BR-32": ("Notification preferences and reminder limits", "Draft", "notification-service"),
    "BR-33": ("Mock test band conversion", "Draft", "assessment-service"),
    "BR-34": ("Premium content needs an entitlement", "Draft", "learning-service, access-service"),
}


def uc_ids(uc_text):
    """RTW ids of the use cases a screen names ("UC: A; B"); an empty list for "—"."""
    if not uc_text.startswith("UC:"):
        return []
    ids = []
    for name in (n.strip() for n in uc_text[3:].split(";")):
        if name not in _UC_BY_NAME:
            raise SystemExit(f"Use case '{name}' is not in the RTW Use Case List (fds_content.USE_CASES)")
        ids.append(_UC_BY_NAME[name])
    return ids


def uc_label(uc_text):
    """'UC: Sign in; Register' → 'UC-02 Sign in; UC-01 Register'; any other text is kept as is."""
    ids = sorted(uc_ids(uc_text))
    return "; ".join(f"{i} {USE_CASES[i][0]}" for i in ids) if ids else uc_text


APPENDIX_INTRO = [
    "Every endpoint a screen calls, numbered in catalog order. Paths are those seen through the API Gateway "
    "(port 8080); the Gateway replaces the external access token with a short-lived internal token. Endpoints "
    "under /internal are service-to-service only and are listed in TDS 2.5, not here. \"Draft — path [TBC]\" "
    "marks an endpoint that the FDS needs but the current release does not provide; TDS 2.1 lists the feature as "
    "planned with no endpoint yet, so the path is proposed here and fixed when the TDS designs it. \"detail: TDS "
    "2.4\" marks an endpoint whose request body, status codes and response body are written out in TDS 2.4; the "
    "FDS describes only how a screen uses it.",
    "Path aliases: the topic, question and vocabulary controllers also answer under /api/content/admin/… "
    "(for example /api/content/admin/topics, /api/content/admin/questions, GET /api/content/admin/vocabulary/search) "
    "with the same roles and behaviour; the catalog lists one path per endpoint. Vocabulary writes use the "
    "/api/content/admin/vocabulary paths that the Gateway routes to library-service.",
    "Error bodies (TDS 2.4 API Conventions): every service except learning returns ErrorResponse {timestamp, "
    "status, error, message, path, details}, where details maps each invalid field to its message and assessment "
    "adds a code; learning-service returns {detail, code}, plus reviews or lessonIds for gate errors (for example "
    "403 REVIEW_REQUIRED). SRS NFR-U01 and NFR-U02 require the ErrorResponse format with a field list everywhere; learning-service is an open issue there. Screens map these codes to the messages in each 2.x.6 "
    "table and never show the raw code.",
]

# ------------------------------------------------------------------ Part 1
NAV_FLOW_PNG = HERE / "part1-diagrams" / "IELTSPath_ScreenNavigationFlow.png"
JOB_FLOW_PNG = HERE / "part1-diagrams" / "IELTSPath_JobSchedule.png"
NAV_FLOW_CAPTION = ("Figure 1.1 — Screen Navigation Flow, read top to bottom: ① public screens → sign-in → top bar "
                    "→ ② learning loop, ③ prototypes, ④ self-study & account, ⑤ staff by role. Colours and line "
                    "styles follow the legend. Source: part1-diagrams/IELTSPath_ScreenNavigationFlow.drawio")
JOB_FLOW_CAPTION = ("Figure 1.2 — Job Schedule & Dependencies: the running pipeline grade → outbox → JOB-01 → "
                    "RabbitMQ → JOB-02 with its retry and dead-letter queues, and the Draft jobs JOB-03 to JOB-07 "
                    "with their triggers and external systems. Colours follow the legend. "
                    "Source: part1-diagrams/IELTSPath_JobSchedule.drawio")

NAV_FLOW_TEXT = [
    "The web client is the IELTS Space frontend (" + FRONTEND_REF + "), a React single-page app in front of the API "
    "Gateway (VITE_API_BASE_URL, default http://localhost:8080). Its UI language is Vietnamese. \"/\" opens /overview "
    "for a signed-in user and /home otherwise. Public routes are /home, /vocabulary, /login, /register, "
    "/forgot-password, /reset-password and /auth/oauth/callback; every other route is wrapped in RequireAuth.",
    "The top bar (SiteNavbar) shows Trang chủ (/home) and Từ điển (/vocabulary) to guests, and adds Tổng quát "
    "(/overview), Lớp học (/classroom), Lộ trình (/learn), Thực hành (/practice), Luyện đề (/practice-tests) and "
    "Học liệu (/materials) after sign-in, plus the user dropdown with the point balance and Đăng xuất. After sign-in "
    "the app returns to the route the user came from, otherwise to /learn; it does not route by role yet.",
    "Learners move along the learning loop /learn → /learn/topics/:topicId → /learn/lessons/:lessonId → "
    "/learn/lessons/:lessonId/practice → /learn/reviews/:reviewId → /learn/tests/:attemptId → "
    "/learn/tests/:attemptId/result. These screens call the real backend. Overview, Classroom, Luyện đề (practice "
    "tests) and Từ điển are built on mock data. /practice, /materials, /classes/:classCode/join, /mentors/:mentorSlug, "
    "/terms, /privacy and /copyright show a placeholder page (RouteStatusPage); unknown routes show NotFoundPage.",
    "Screens marked \"FE: not built\" in the index — the account, library, games, community, notification and every "
    "staff screen — are specified here as the design target. Figure 1.1 shows the navigation in five areas: blue "
    "screens call the real API, orange screens run on mock data and grey dashed screens are not built yet. One arrow "
    "from the top bar leads into each signed-in area, and staff areas open by role once GAP-02 is closed. Placeholder "
    "routes are not drawn.",
]

JOB_FLOW_TEXT = [
    "Two jobs run in the current release: JOB-01 relays assessment outbox rows to RabbitMQ every 2 seconds and "
    "JOB-02 consumes AssessmentCompleted.v2 in the learning service, with a 30-second retry queue and a dead-letter "
    "queue. JOB-03 to JOB-07 are required by Draft features (AI grading, notifications, matchmaking) and are "
    "specified here so the TDS can design them; their schedules and limits marked [TBC] need a team decision.",
    "No other service runs a scheduler. access-service (PointCredited, PointDebited, SubscriptionGranted, "
    "SubscriptionChanged) and game-service (GameRoomCreated, GameMatchCompleted) write outbox rows but have no "
    "relay, so those events reach no consumer (TDS 2.5); this is a known gap, not a job.",
]

JOB_SCHEDULE_ASCII = [
    "┌──────────────────────────────────────────────────────────────────────────┐",
    "│  JOB-01  Assessment outbox relay       ←── every 2 s (fixed delay)       │",
    "│  JOB-02  AssessmentCompleted consumer  ←── RabbitMQ event (retry 30 s)   │",
    "│  JOB-03  AI grading worker             ←── grading job created  [Draft]  │",
    "│  JOB-04  Grading-done alert dispatcher ←── RabbitMQ event        [Draft]  │",
    "│  JOB-05  Study reminder scheduler      ←── every [TBC] min       [Draft]  │",
    "│  JOB-06  Notification delivery retry   ←── every [TBC] s         [Draft]  │",
    "│  JOB-07  Matchmaking queue sweeper     ←── every [TBC] s         [Draft]  │",
    "└──────────────────────────────────────────────────────────────────────────┘",
    "Dependencies: JOB-01 → (assessment.events / assessment.completed.v2) → JOB-02, JOB-04;",
    "              JOB-03 → finalize result → outbox → JOB-01;  JOB-04, JOB-05 → JOB-06 → FCM / Email.",
]


def screen_index_rows():
    from fds_screens import SCREENS as screens
    return [(f"2.{n}", s["name"], s["route"], s["roles"], f"{s['status']}\n{FE_LABELS[s.get('fe')]}")
            for n, s in enumerate(screens, 1)]


# ------------------------------------------------------------------ Part 2 shared content
PART2_INTRO = [
    "One section per screen, in the order of the Screen Index (Section 1.1). Component codes follow "
    "[ScreenKey]-[TYPE]-[NN]; types are TXT (text input), TXA (text area), NUM (number), DAT (date), SEL "
    "(select), CHK (checkbox / toggle), RAD (radio group), BTN (button), LNK (link), TBL (table / list), CRD "
    "(card), BDG (badge), TAB (tab bar), MDL (modal / dialog), PNL (panel), AUD (audio player), VID (video player), "
    "PRG (progress indicator).",
    "Every General Information table has a Frontend row. For a built screen it names the React page, and the "
    "route, components, UI copy, API calls and messages describe what the code does today; UI copy is the exact "
    "Vietnamese text of the app. For a screen that is not built, the section is the design target and its UI copy "
    "is an English proposal to be translated when the screen is built. API ids refer to Appendix A; gaps between "
    "the frontend and the SRS are listed in Appendix C.",
]

CONVENTIONS_TEXT = [
    "Session (src/lib/httpClient.ts, features/auth/authSession.ts): the access token is kept in memory only; the "
    "refresh token stays in the HttpOnly cookie set by the backend, and every request is sent with credentials. "
    "On start-up AuthProvider calls auth.refresh with an empty body and then user.me while showing \"Đang khôi phục "
    "phiên đăng nhập…\". On an HTTP 401 the client refreshes once (single-flight: parallel requests share one "
    "refresh) and repeats the request; if the refresh fails the session is cleared and RequireAuth sends the user "
    "to /login, keeping the original route in router state (GG-01).",
    "Requests time out after 30 s (60 s for Writing grading). Buttons that submit are disabled and show a "
    "spinner while the request runs, and an in-flight guard stops double clicks. Exercise, practice, review and "
    "Writing submissions send a new requestId (UUID) per submission (lib/requestId.ts); key activation keeps the "
    "same idempotencyKey while the same key is retried (BR-30).",
    "Feedback is shown inline (role=status / role=alert text next to the action), not as toasts. Learning pages "
    "use shared states from components/PageState.tsx: a loading line, an error panel with \"Thử lại\", a not-found "
    "panel and lock redirects (GLB table). A pending review is shown on every /learn page by ReviewGateBanner.",
    "Backend contract the screens rely on (TDS 2.2 and 2.4): the access token lives 1 hour and the refresh token 7 "
    "days; a refresh consumes the refresh token and returns a new pair (BR-04), and an access token stays valid "
    "until it expires because there is no blacklist. Sign-in and refresh also set HttpOnly cookies access_token "
    "(path /) and refresh_token (path /auth), SameSite=Lax, but the Gateway reads the access token only from the "
    "Authorization: Bearer header, which is why the client keeps it in memory and sends it on every call. The "
    "Gateway adds X-Correlation-Id when the client sends none and returns it in the response; support asks the "
    "user for it when a request fails. Lists are paged with ?page=0&size=20 (size 1–100) or ?limit=20 (NFR-P04); times are "
    "ISO 8601 and are shown in the browser's local time.",
    "Roles: the Role(s) row of each screen and the role column of Appendix A follow the RTW Permission Matrix "
    "(" + RTW_REF + ", Sheet 4), which states the required policy per role and notes where the current release "
    "differs. Learner self-service endpoints check only sign-in and ownership today, so any signed-in role can use "
    "them on its own data; those screens say \"Any signed-in role\".",
    "Scope: this FDS covers the web client only. TDS 1.2 also lists an Expo / React Native mobile app that calls the "
    "same APIs; its screens are not specified here (OQ-18).",
]

GLOBAL_MESSAGES = [
    ("GLB-01", "Redirect", "Session expired and the silent refresh failed (401), or no session on a protected route",
     "Login page; httpClient error \"Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.\"",
     "Immediate; original route kept in router state"),
    ("GLB-02", "Inline Error", "HTTP 403 without a specific code",
     "Server message; Home access calls use \"Tài khoản của bạn chưa được phép thực hiện thao tác này.\"",
     "Until the next action"),
    ("GLB-03", "Not Found Panel", "HTTP 404 / NOT_FOUND for the main resource, or an unknown /learn route",
     "Title \"Không tìm thấy trang\" · \"Nội dung này không tồn tại hoặc đã bị gỡ.\" · Button \"Về lộ trình\"",
     "Persistent"),
    ("GLB-04", "Error Panel", "HTTP 500 / 502 on page load",
     "Title \"Chưa tải được nội dung\" · \"Máy chủ đang gặp sự cố hoặc mất kết nối. Hãy thử lại.\" · Button \"Thử lại\"",
     "Persistent; Thử lại reloads"),
    ("GLB-05", "Error Panel", "HTTP 503 (a dependent service is unavailable)", "Same panel as GLB-04",
     "Persistent; Thử lại reloads"),
    ("GLB-06", "Inline Error", "HTTP 400 with field errors", "Field message from the server (details), else the "
     "screen's own message", "Below the form, role=alert"),
    ("GLB-07", "Confirm Modal", "Leaving a screen with unsaved edits",
     "Design target, not built: Title \"Bỏ thay đổi?\" · Body \"Các thay đổi chưa được lưu.\"",
     "Buttons \"Bỏ thay đổi\" (Danger) + \"Tiếp tục sửa\""),
    ("GLB-08", "Inline Error", "Network error or timeout (status 0)",
     "\"Không kết nối được máy chủ. Kiểm tra mạng hoặc địa chỉ API.\" (fallback \"Có lỗi xảy ra, vui lòng thử lại.\")",
     "Until the next action"),
    ("GLB-09", "Redirect + Notice", "403 TOPIC_LOCKED / LESSON_LOCKED / TEST_LOCKED when a learning page loads",
     "→ /learn \"Topic đó chưa mở. Hãy hoàn thành chặng trước.\" · → topic \"Bài học đó chưa mở. Hãy học theo thứ tự.\" "
     "· → topic \"Bài kiểm tra chưa mở. Hoàn thành mọi bài học trước.\"", "Notice banner on the target page"),
    ("GLB-10", "Review Panel / Banner", "403 REVIEW_REQUIRED on load, or pending reviews exist",
     "Panel \"Cần ôn lại trước khi học tiếp\" · \"Hoàn thành bài ôn \"{KP}\" để mở nội dung này.\" · Banner \"Bạn còn {n} bài "
     "ôn bắt buộc. Bắt đầu với \"{KP}\" để mở bài kế tiếp.\" · Button \"Làm bài ôn\"", "Persistent until the review ends"),
]

GLOBAL_GUARDS = [
    ("GG-01", "Route is wrapped in RequireAuth and no session exists", "Continue rendering",
     "Redirect → /login (state.from = current location); after sign-in return to it"),
    ("GG-02", "Route requires a staff role", "Continue rendering",
     "Design target: redirect → 403 page. Not built: the frontend has no role-based routes yet (Appendix C)"),
    ("GG-03", "Signed-in user opens /login, /register, /forgot-password, /reset-password or /auth/oauth/callback "
     "(GuestOnly)", "Continue rendering (guest)", "Redirect → /learn"),
]
# ------------------------------------------------------------------ Part 3
PART3_INTRO = [
    "One section per background job. JOB-01 and JOB-02 describe the current release (values from code and "
    "config-repo). JOB-03 to JOB-07 are Draft: they specify required behaviour for Draft features, and every "
    "value marked [TBC] is an open decision listed in Appendix B. The platform has no metrics stack yet; alert "
    "rules name the condition to monitor once one is chosen.",
]

_STD_LOG_FORMAT = "[TIMESTAMP] [LEVEL] [service] [logger] message k=v … (correlationId when a request started it)"

JOBS = [
    dict(
        code="JOB-01", name="Assessment outbox relay",
        bean="com.group01.assessment.infrastructure.messaging.OutboxRelayScheduler → OutboxRelay "
             "(assessment-service)",
        purpose="Publishes the AssessmentCompleted.v2 events that grading writes to the outbox table, so a "
                "result reaches the learning service only after its database transaction has committed.",
        trigger="@Scheduled fixed delay 2 s (assessment.outbox.relay.poll-interval = PT2S); enabled by "
                "ASSESSMENT_OUTBOX_RELAY_ENABLED (default true)",
        refs="FT-29, FT-31, FT-32 · BR-25, BR-26, BR-30",
        status="Specified (implemented)",
        sla_short="< 2 s for a batch of 50 events under normal load; alert when an event stays unpublished > 60 s.",
        context="When a test result becomes final — at submit for auto-graded attempts (FT-31) or when an "
                "examiner finalizes it (FT-32) — the assessment service writes the result and an outbox row in "
                "one transaction. This job moves committed rows to RabbitMQ (exchange assessment.events, routing "
                "key assessment.completed.v2). Without it, no topic is ever passed by a final test, no "
                "assessment evidence is recorded and no review opens after a test; learners stay stuck on the "
                "topic although their result shows a pass.",
        flow=[
            "STEP 1: Claim a batch (one transaction)",
            "  - SELECT * FROM outbox_events WHERE published_at IS NULL AND retry_count < 20",
            "    ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED",
            "  - Row locks replace a job lock: two instances never claim the same row.",
            "  - Empty result → end the run quietly.",
            "",
            "STEP 2: Publish each claimed row",
            "  FOR EACH event (in created_at order):",
            "    2a: publish to exchange assessment.events, routing key assessment.completed.v2,",
            "        persistent, messageId = event id, with publisher confirm and mandatory routing",
            "    2b: confirmed → UPDATE outbox_events SET published_at = now()",
            "    2c: not confirmed / not routed / broker down → retry_count + 1, last_error = message",
            "        (the row stays unpublished and is claimed again on a later run)",
            "",
            "STEP 3: Commit the batch transaction (releases the row locks)",
            "",
            "STEP 4: Errors outside a single event (database down) are caught by the scheduler,",
            "        logged at WARN and retried on the next tick; the scheduler thread never stops.",
        ],
        inputs=[
            ("Table `outbox_events`", "`id, event_type, payload, created_at, retry_count`",
             "`published_at IS NULL AND retry_count < max-attempts (20)`", "Batch size 50; FOR UPDATE SKIP LOCKED"),
        ],
        outputs=[
            ("Event Publish", "Exchange `assessment.events` → queue `learning.assessment-completed.v2`",
             "`AssessmentCompleted.v2` envelope {event_id, event_type, occurred_at, source, data}; data {user_id, "
             "package_version_id, attempt_id, result_id, result_version, assessment_type, status, completed_at, "
             "overall_band, item_results[]} (docs/contracts/assessment-completed-v2.md, TDS 2.5)",
             "After the business transaction committed"),
            ("DB Update", "Table `outbox_events`", "`SET published_at = now()`", "Broker confirmed the message"),
            ("DB Update", "Table `outbox_events`", "`SET retry_count = retry_count + 1, last_error = …`",
             "Publish failed"),
        ],
        idempotency=[
            "Pattern B (status guard) + consumer deduplication:",
            "  - Only rows with published_at IS NULL are claimed; a published row is never sent again.",
            "  - Delivery is at-least-once: a crash after the broker confirm but before the commit",
            "    republishes the same event id. JOB-02 ignores a result version it already applied",
            "    (BR-25), so the duplicate has no effect.",
        ],
        verification="Stop the database commit after the confirm (test double) and run the relay twice: the "
                     "broker receives the same messageId twice and the learning data changes once · L2 ref: "
                     "[test-id TBC].",
        rules=[
            ("BR-25", "Each result version is applied once", "Step 2 (messageId = event id)",
             "Consumer ignores the duplicate"),
            ("BR-26", "Finalizing twice has no further effect (no second outbox row)", "Upstream of Step 1",
             "No second event exists to relay"),
        ],
        errors=[
            ("Transient", "Broker unreachable, no confirm within timeout, message returned as unroutable",
             "Log WARN with event id and type; retry_count + 1; retried on a later run", "Yes", "20"),
            ("Permanent", "Event reached retry_count = 20", "Stays unpublished; no longer claimed; needs manual "
             "replay after the cause is fixed", "No", "—"),
            ("Critical", "Database unavailable or batch transaction fails",
             "Log WARN \"Outbox relay batch failed\"; whole batch retried on the next tick", "Yes", "Unbounded"),
        ],
        retry="Max 20 publish attempts per event · retried every poll (2 s fixed delay); no exponential backoff",
        dlq="None for the relay; events that exhausted 20 attempts stay in outbox_events with last_error and "
            "must be replayed manually [TBC: replay procedure]",
        sla=[
            ("Runtime per execution (normal)", "< 2 s", "> 5 s", "Log timing [TBC: tracing not set up]"),
            ("Runtime per execution (peak)", "< 5 s", "> 10 s", "Load test"),
            ("Records per execution", "≤ 50 (batch size)", "—", "Count of published events"),
            ("Event age before publish", "< 5 s end to end for 95% of results (NFR-P05)", "> 60 s",
             "now() − created_at of unpublished rows"),
        ],
        index="idx_outbox_unpublished ON outbox_events(created_at) WHERE published_at IS NULL (assessment migrations); "
              "uq_outbox_assessment_completed_result keeps one AssessmentCompleted row per result version",
        batch="50 records per run · ordered by created_at, claimed with SKIP LOCKED (no offset)",
        logs=[
            ("Batch failed (database / transaction)", "WARN", "`message` (no payload)"),
            ("Event not published", "WARN", "`eventId eventType error`"),
            ("Batch completed", "DEBUG [TBC]", "`published claimed durationMs`"),
        ],
        log_format=_STD_LOG_FORMAT + "; event payloads are never logged",
        alerts=[
            ("`OutboxRelayLag`", "oldest unpublished event older than 60 s", "Warning"),
            ("`OutboxRelayExhausted`", "any row with retry_count ≥ 20", "Critical"),
            ("`OutboxRelayNotRunning`", "no successful run within 2 × poll interval while rows are pending",
             "Critical"),
        ],
    ),
    dict(
        code="JOB-02", name="AssessmentCompleted.v2 consumer",
        bean="com.group01.learning.infrastructure.messaging.AssessmentCompletedListener → "
             "ApplyAssessmentResultUseCase (learning-service)",
        purpose="Applies each final assessment result to the learner's progress: knowledge-point evidence, topic "
                "test assignment, topic pass and reviews for weak knowledge points.",
        trigger="@RabbitListener on queue learning.assessment-completed.v2 (manual ack); "
                "learning.messaging.consumer-enabled (default true)",
        refs="FT-26, FT-29, FT-34, FT-35 · BR-22, BR-23, BR-25",
        status="Specified (implemented)",
        sla_short="< 1 s per message under normal load; alert on any message in the DLQ.",
        context="The topic final test is graded in the assessment module, but topic progress lives in the learning "
                "module, which never calls assessment over HTTP. This consumer turns each AssessmentCompleted.v2 "
                "event into evidence, consumes the learner's test code, passes the topic at 70% or more and "
                "opens reviews for knowledge points with mastery below 0.6. If it does not run, learners who "
                "passed a final test stay IN_PROGRESS, their mastery ignores test results and no review follows "
                "a failed test.",
        flow=[
            "STEP 1: Parse the message",
            "  - Validate against the AssessmentCompleted.v2 contract (docs/contracts).",
            "  - Contract violation → copy to the DLQ with header x-learning-failure, ack, stop.",
            "",
            "STEP 2: Apply the result (one transaction)",
            "  2a: SELECT pg_advisory_xact_lock(hashtext(userId))   -- serialise per learner",
            "  2b: applied version ≥ resultVersion for this attempt → log INFO, commit, ack (no change)",
            "  2c: an older version was applied → delete its assessment evidence (BR-25)",
            "  2d: record (userId, attemptId, resultVersion) as applied",
            "  2e: PLACEMENT → stop here (version only)",
            "  2f: append one evidence row per judged knowledge point of every item",
            "  2g: TOPIC_GATE → find the latest unused assignment for the package version issued",
            "      before completedAt; consume it; percent ≥ 70 → topic PASSED, next topic opens",
            "  2h: TOPIC_GATE / MOCK → re-evaluate reviews: open one for each KP with mastery < 0.6",
            "",
            "STEP 3: Acknowledge",
            "  - Commit succeeded → basicAck.",
            "  - Exception → attempt = previous rejections + 1;",
            "      attempt < 5 → basicNack (no requeue) → retry queue (TTL 30 s) → main queue",
            "      attempt ≥ 5 → copy to DLQ with x-learning-failure, then ack",
        ],
        inputs=[
            ("Queue `learning.assessment-completed.v2`", "`AssessmentCompleted.v2`",
             "Bound to `assessment.events` with key `assessment.completed.v2`",
             "Parsed by AssessmentCompletedParser; payload never logged"),
            ("Table `assessment_result_versions`", "`user_id, attempt_id, result_version`",
             "`user_id = ? AND attempt_id = ?`", "Idempotency check"),
            ("Table `topic_test_assignments`", "open assignment", "`user, package version, issued before completedAt`",
             "Row locked FOR UPDATE"),
        ],
        outputs=[
            ("DB Insert", "Table `kp_evidence`", "one row per judged knowledge point (source ASSESSMENT)",
             "Not PLACEMENT"),
            ("DB Update", "Table `topic_test_assignments`", "`consumed_attempt_id, score`", "TOPIC_GATE with an open "
             "assignment"),
            ("DB Update", "Table `topic_progress`", "topic PASSED; next topic IN_PROGRESS", "TOPIC_GATE and percent ≥ 70"),
            ("DB Insert", "Table `review_items`", "status PENDING per weak knowledge point", "TOPIC_GATE or MOCK, "
             "mastery < 0.6"),
            ("Event Publish", "Exchange `learning.assessment-completed.dlx` → `…v2.dlq`", "original message + "
             "`x-learning-failure`", "Contract violation or 5th failure"),
        ],
        idempotency=[
            "Pattern A (idempotency record) + per-learner lock:",
            "  - The applied (userId, attemptId, resultVersion) is stored in the same transaction as",
            "    the evidence. A redelivery or a JOB-01 duplicate finds version ≥ and changes nothing.",
            "  - A newer version (examiner re-grade) replaces the older version's evidence (BR-25).",
            "  - The advisory lock serialises two deliveries for the same learner.",
        ],
        verification="Deliver the same event twice and a lower version after a higher one: evidence count and topic "
                     "status are identical after each delivery (FT-29/AC-04, FT-26/NAC-03) · L2 ref: [test-id TBC].",
        rules=[
            ("BR-22", "Test code single-use, consumed even on failure", "Step 2g", "No open assignment → log INFO, "
             "evidence still recorded"),
            ("BR-15", "Topic pass mark 70%", "Step 2g", "Below 70% → topic stays IN_PROGRESS"),
            ("BR-23", "Reviews open for mastery < 0.6", "Step 2h", "Mastery ≥ 0.6 → no review"),
            ("BR-25", "Result version applied once; newer replaces older", "Step 2b–2d", "Equal/older ignored"),
        ],
        errors=[
            ("Transient", "Database unavailable, deadlock, lock timeout", "Log WARN eventId, attempt, errorType; nack → "
             "retry queue (30 s)", "Yes", "5 deliveries"),
            ("Permanent", "Payload breaks the v2 contract", "Log WARN messageId, reason; park in DLQ at once", "No", "—"),
            ("Critical", "5th failed delivery, or DLQ publish not confirmed in 5 s",
             "Park in DLQ with reason; if parking fails → nack and retry", "No", "—"),
        ],
        retry="Max 5 deliveries (learning.messaging.max-delivery-attempts) · fixed 30 s delay via the TTL of queue "
              "learning.assessment-completed.v2.retry (learning.messaging.retry-delay-ms = 30000)",
        dlq="Queue learning.assessment-completed.v2.dlq (exchange learning.assessment-completed.dlx) · TTL none "
            "[TBC: retention] · Alert when depth > 0; replay by moving the message back after the fix",
        sla=[
            ("Runtime per message (normal)", "< 1 s", "> 3 s", "Log timing [TBC]"),
            ("Runtime per message (peak)", "< 3 s", "> 10 s", "Load test"),
            ("Records per execution", "1 message", "—", "One delivery per invocation"),
            ("Queue depth", "0 at steady state", "> 100 for 5 min", "RabbitMQ management UI"),
        ],
        index="kp_evidence(user_id, kp_id, recorded order); topic_test_assignments(user_id, package_version_id, "
              "consumed_attempt_id) — see learning migrations V1–V5",
        batch="1 message per delivery · prefetch per Spring AMQP default [TBC]",
        logs=[
            ("Result already applied", "INFO", "`eventId attemptId version`"),
            ("Topic gate without package version / open assignment", "INFO", "`eventId`"),
            ("Contract violation", "WARN", "`messageId reason`"),
            ("Event failed", "WARN", "`eventId attempt errorType`"),
            ("Could not park event", "ERROR", "`messageId errorType`"),
        ],
        log_format=_STD_LOG_FORMAT + "; payloads and answers are never logged",
        alerts=[
            ("`AssessmentConsumerDlqDepth`", "dlq_messages > 0", "Warning"),
            ("`AssessmentConsumerRetrying`", "retry queue depth > 0 for 5 min", "Warning"),
            ("`AssessmentConsumerDown`", "no consumer on learning.assessment-completed.v2", "Critical"),
        ],
    ),
    dict(
        code="JOB-03", name="AI grading worker",
        draft="Status Draft (FT-33, FT-39): grading jobs and submissions are stored today, but no worker grades "
              "them and no points are charged. Lesson essays (FT-25) are graded synchronously and do not use "
              "this job.",
        bean="[TBC] com.group01.assessment.infrastructure.scheduler.GradingWorker (assessment-service)",
        purpose="Grades a learner's Writing or Speaking response that was sent for AI grading inside a test "
                "attempt, and finalizes the result so it feeds learning progress.",
        trigger="Polls grading_jobs with status QUEUED every [TBC] s (or consumes an internal queue [TBC])",
        refs="FT-33, FT-34, FT-39 · BR-09, BR-30",
        status="Draft",
        sla_short="Grade within [TBC] s of job creation; alert when a job stays QUEUED > [TBC] min.",
        context="AI grading of a test response takes 20–45 seconds and must not hold the learner's request open. "
                "The learner creates a grading job (FT-33) and the worker grades it later. Without the worker, "
                "Writing and Speaking sections of mock tests never get a score and the mock result never reaches "
                "learning.",
        flow=[
            "STEP 1: Claim jobs  -- SELECT … FROM grading_jobs WHERE status = 'QUEUED'",
            "        ORDER BY created_at LIMIT [TBC] FOR UPDATE SKIP LOCKED; set status RUNNING",
            "",
            "STEP 2: FOR EACH job:",
            "  2a: guard — job already GRADED or FAILED → skip",
            "  2b: call LLM provider (Writing) or Speech Assessment Provider (Speaking),",
            "      outside any DB transaction, timeout [TBC] s",
            "  2c: valid grade → open result version, save details, finalize (outbox row, JOB-01)",
            "  2d: debit points via /internal/access/points/debit with idempotencyKey = job id",
            "      (BR-30); 402 → job FAILED_PAYMENT [TBC]",
            "  2e: provider failure / invalid grade → status FAILED, nothing charged",
            "",
            "STEP 3: Log processed / failed / durationMs",
        ],
        inputs=[("Table `grading_jobs`", "`id, submission_id, skill, grading_mode, point_cost_snapshot`",
                 "`status = 'QUEUED'`", "Batch [TBC]"),
                ("Table `learner_submissions`", "`text_payload, audio_reference, prompt_snapshot`", "`id = job.submission_id`",
                 "Essay text never logged")],
        outputs=[("DB Update", "Table `grading_jobs`", "`status GRADED / FAILED, point_ledger_entry_id`", "Each job"),
                 ("DB Insert", "Tables `assessment_results`, `item_results`", "finalized result version", "Grade valid"),
                 ("Event Publish", "Outbox → JOB-01", "`AssessmentCompleted.v2`", "Result finalized")],
        idempotency=["Pattern B (status guard) + idempotency key:",
                     "  - Only QUEUED jobs are claimed; GRADED and FAILED are final.",
                     "  - The point debit uses the job id as idempotencyKey, so a retry never charges twice."],
        verification="Run the worker twice on the same job with a stub provider: one result version, one ledger "
                     "entry · L2 ref: [TBC].",
        rules=[("BR-09", "Balance never below 0; ledger entry per change", "Step 2d", "402 → job failed, no grade shown"),
               ("BR-30", "Same idempotency key never charges twice", "Step 2d", "Replay returns first ledger entry")],
        errors=[("Transient", "Provider timeout, 5xx, network error", "Log WARN jobId errorType; back to QUEUED", "Yes",
                 "[TBC]"),
                ("Permanent", "Invalid grade after one retry, unsupported audio", "Status FAILED; learner can resend",
                 "No", "—"),
                ("Critical", "Unhandled exception", "Log ERROR (no essay text); status FAILED; alert", "No", "—")],
        retry="Max [TBC] attempts · backoff [TBC]",
        dlq="Not event-driven [TBC]; FAILED jobs are visible to the learner (FT-33)",
        sla=[("Runtime per job (normal)", "< 45 s", "> 60 s", "Log timing"),
             ("Runtime per job (peak)", "[TBC]", "[TBC]", "Load test"),
             ("Records per execution", "≤ [TBC]", "—", "Log output"),
             ("Queue wait", "< [TBC] min", "> [TBC] min", "now() − created_at of QUEUED jobs")],
        index="grading_jobs(status, created_at)",
        batch="[TBC] jobs per run · SKIP LOCKED claim",
        logs=[("Job graded", "INFO", "`jobId skill durationMs`"), ("Job failed", "WARN", "`jobId errorType attempt`"),
              ("Provider unavailable", "ERROR", "`provider errorType`")],
        log_format=_STD_LOG_FORMAT + "; essays, prompts, transcripts and LLM output are never logged",
        alerts=[("`GradingWorkerBacklog`", "QUEUED jobs older than [TBC] min", "Warning"),
                ("`GradingWorkerFailures`", "FAILED/processed > [TBC]% in 15 min", "Critical")],
    ),
    dict(
        code="JOB-04", name="Grading-done alert dispatcher",
        draft="Status Draft (FT-52): notification-service is a skeleton; no alert is sent in the current release.",
        bean="[TBC] com.group01.notification.infrastructure.messaging.GradingDoneListener (notification-service)",
        purpose="Creates one grading-done notification when an examiner finalizes a result and hands it to the "
                "delivery channels the learner enabled.",
        trigger="RabbitMQ: own queue bound to assessment.events / assessment.completed.v2 [TBC: queue name]",
        refs="FT-51, FT-52 · BR-27, BR-32",
        status="Draft",
        sla_short="Notification created < [TBC] s after the event; delivered within [TBC] minutes.",
        context="Results that need an examiner can take days. The learner must learn that grading is done without "
                "polling the result screen. If the job does not run, the learner sees the result only on the next "
                "visit to Test Result.",
        flow=["STEP 1: Parse AssessmentCompleted.v2; skip unless the result was finalized by an examiner [TBC: field]",
              "STEP 2: In one transaction: INSERT notification (type GRADING_DONE, dedup key = attemptId:version)",
              "        ON CONFLICT DO NOTHING; one delivery row per enabled channel (FT-51)",
              "STEP 3: Ack; deliveries are sent by JOB-06",
              "        (push to each registered device; email when no device is registered)"],
        inputs=[("Queue `[TBC]`", "`AssessmentCompleted.v2`", "Examiner-finalized results", "Same contract as JOB-02"),
                ("Table `notification_preferences`", "`channel, enabled`", "`user_id = ?`", "")],
        outputs=[("DB Insert", "Table `notifications`", "GRADING_DONE naming the test, never the score", "Not a duplicate"),
                 ("DB Insert", "Table `notification_deliveries`", "one PENDING row per enabled channel", "Channel enabled")],
        idempotency=["Pattern D (upsert): unique (user_id, dedup_key); a redelivered event inserts nothing (FT-52/NAC-02)."],
        verification="Deliver the same event twice: one notification, one delivery per channel · L2 ref: [TBC].",
        rules=[("BR-32", "One preference per channel; dedup key per notification", "Step 2", "Duplicate ignored"),
               ("BR-27", "Notifications visible only to their owner", "Step 2", "—")],
        errors=[("Transient", "Database unavailable", "Nack → retry [TBC]", "Yes", "[TBC]"),
                ("Permanent", "Contract violation", "Park in DLQ", "No", "—"),
                ("Critical", "Unhandled exception", "Log ERROR; DLQ; alert", "No", "—")],
        retry="[TBC] (same pattern as JOB-02 recommended: 30 s retry queue, 5 deliveries)",
        dlq="[TBC] notification.grading-done.dlq · alert when depth > 0",
        sla=[("Runtime per message (normal)", "< 500 ms", "> 2 s", "Log timing"),
             ("Runtime per message (peak)", "[TBC]", "[TBC]", "Load test"),
             ("Records per execution", "1 message", "—", ""),
             ("DB query time", "< 50 ms", "> 200 ms", "Slow query log")],
        index="notifications(user_id, dedup_key) unique",
        batch="1 message per delivery",
        logs=[("Notification created", "INFO", "`notificationId userId type`"),
              ("Duplicate skipped", "DEBUG", "`dedupKey`"), ("Event failed", "WARN", "`eventId attempt errorType`")],
        log_format=_STD_LOG_FORMAT,
        alerts=[("`GradingAlertDlqDepth`", "dlq_messages > 0", "Warning"),
                ("`GradingAlertConsumerDown`", "no consumer on the queue", "Critical")],
    ),
    dict(
        code="JOB-05", name="Study reminder scheduler",
        draft="Status Draft (FT-53): no reminder is scheduled or sent in the current release; learning activity is "
              "reported by the client (FT-44).",
        bean="[TBC] com.group01.notification.infrastructure.scheduler.StudyReminderScheduler (notification-service)",
        purpose="Creates at most one study reminder per learner per day, at the learner's preferred local time, on "
                "days without a qualifying study activity.",
        trigger="@Scheduled every [TBC] minutes; ShedLock or SKIP LOCKED to run on one instance [TBC]",
        refs="FT-44, FT-51, FT-53 · BR-32",
        status="Draft",
        sla_short="< [TBC] s per run for [TBC] learners.",
        context="Regular study keeps a learner's streak (FT-45). A reminder at the time the learner chose nudges "
                "them back on days they have not studied. Without the job, learners receive no reminder; with a "
                "faulty job they might receive several, which BR-32 forbids.",
        flow=["STEP 1: Find learners whose preferred reminder time (in their timezone) fell inside the last interval",
              "        and whose reminder channel is enabled",
              "STEP 2: FOR EACH learner:",
              "  2a: skip if a qualifying activity exists today in the learner's timezone [TBC: data source —",
              "      user-service activity API or an activity event]",
              "  2b: INSERT notification with dedup key reminder:{userId}:{localDate} ON CONFLICT DO NOTHING",
              "  2c: create delivery rows for enabled channels (sent by JOB-06)",
              "STEP 3: Log created / skipped / durationMs"],
        inputs=[("Table `notification_preferences`", "`user_id, channel, preferred_time, timezone`",
                 "`enabled AND preferred time in window`", "Batch [TBC]")],
        outputs=[("DB Insert", "Table `notifications`", "STUDY_REMINDER", "No activity today, no reminder yet"),
                 ("DB Insert", "Table `notification_deliveries`", "PENDING per channel", "Channel enabled")],
        idempotency=["Pattern D (upsert): unique dedup key per learner and local date — at most one reminder a day."],
        verification="Run the job twice in the same window: one reminder per learner (FT-53/NAC-01) · L2 ref: [TBC].",
        rules=[("BR-32", "At most one study reminder per learner per day", "Step 2b", "Insert ignored")],
        errors=[("Transient", "Activity source unavailable", "Skip learner this run; retried next run", "Yes", "Next run"),
                ("Permanent", "Invalid timezone", "Skip learner; log WARN", "No", "—"),
                ("Critical", "Unhandled exception", "Log ERROR; alert", "No", "—")],
        retry="Next scheduled run",
        dlq="Not event-driven",
        sla=[("Runtime per execution (normal)", "< [TBC] s", "> [TBC] s", "Log timing"),
             ("Runtime per execution (peak)", "[TBC]", "[TBC]", "Load test"),
             ("Records per execution", "≤ [TBC]", "—", "Log output"),
             ("DB query time", "< 100 ms", "> 500 ms", "Slow query log")],
        index="notification_preferences(channel, preferred_time); notifications(user_id, dedup_key) unique",
        batch="[TBC] learners per run · cursor by user_id",
        logs=[("Job started", "INFO", "`runId window`"), ("Job completed", "INFO", "`created skipped failed durationMs`"),
              ("Learner skipped", "DEBUG", "`userId reason`")],
        log_format=_STD_LOG_FORMAT,
        alerts=[("`StudyReminderNotRunning`", "no run within 2 × interval", "Critical"),
                ("`StudyReminderSlow`", "runtime > [TBC] s", "Warning")],
    ),
    dict(
        code="JOB-06", name="Notification delivery retry",
        draft="Status Draft (FT-54): no delivery record exists in the current release.",
        bean="[TBC] com.group01.notification.infrastructure.scheduler.DeliveryDispatcher (notification-service)",
        purpose="Sends pending notification deliveries to Firebase Cloud Messaging or the Email Service and retries "
                "failed ones without creating a second notification.",
        trigger="@Scheduled every [TBC] s",
        refs="FT-52, FT-53, FT-54 · BR-32",
        status="Draft",
        sla_short="Pending delivery sent within [TBC] s.",
        context="External channels fail transiently. Every attempt is recorded with the provider's result and "
                "message id so the learner receives each notification at most once per channel and support can "
                "see why one was not delivered.",
        flow=["STEP 1: Claim deliveries WHERE status IN ('PENDING','RETRY') AND next_attempt_at <= now()",
              "        FOR UPDATE SKIP LOCKED LIMIT [TBC]",
              "STEP 2: FOR EACH delivery:",
              "  2a: skip if the device was revoked or the channel disabled since creation",
              "  2b: send (FCM: per device token; Email: learner address)",
              "  2c: success → SENT, store provider message id",
              "  2d: failure → attempt + 1; RETRY with next_attempt_at = now() + backoff; FAILED after [TBC]",
              "      (an invalid FCM token revokes the device [TBC])"],
        inputs=[("Table `notification_deliveries`", "`id, notification_id, channel, target, attempts`",
                 "`status IN (PENDING, RETRY) AND next_attempt_at <= now()`", "Batch [TBC]")],
        outputs=[("DB Update", "Table `notification_deliveries`", "`status, attempts, provider_message_id, last_error`",
                  "Each attempt"),
                 ("Push / Email", "Firebase Cloud Messaging / Email Service", "title + body, deep link", "Channel enabled")],
        idempotency=["Pattern B: only PENDING/RETRY rows are sent; SENT and FAILED are final; the notification row is",
                     "never re-created by a retry (FT-54/AC-02)."],
        verification="Stub FCM to time out once then succeed: one notification, two attempts recorded · L2 ref: [TBC].",
        rules=[("BR-32", "No duplicate notifications", "Step 2", "Retry updates the delivery only")],
        errors=[("Transient", "Provider timeout, 5xx, rate limit", "RETRY with backoff", "Yes", "[TBC]"),
                ("Permanent", "Invalid token / address", "FAILED; device revoked [TBC]", "No", "—"),
                ("Critical", "Unhandled exception", "Log ERROR; alert", "No", "—")],
        retry="Max [TBC] attempts · backoff [TBC] ms initial, ×2 multiplier, max [TBC] ms",
        dlq="Not event-driven; FAILED rows stay for audit [TBC: retention]",
        sla=[("Runtime per execution (normal)", "< [TBC] s", "> [TBC] s", "Log timing"),
             ("Runtime per execution (peak)", "[TBC]", "[TBC]", "Load test"),
             ("Records per execution", "≤ [TBC]", "—", "Log output"),
             ("Delivery delay", "< [TBC] s", "> [TBC] min", "sent_at − created_at")],
        index="notification_deliveries(status, next_attempt_at)",
        batch="[TBC] deliveries per run · SKIP LOCKED claim",
        logs=[("Delivery sent", "INFO", "`deliveryId channel providerMessageId`"),
              ("Delivery failed", "WARN", "`deliveryId channel attempt errorType`")],
        log_format=_STD_LOG_FORMAT + "; push tokens and email addresses are masked",
        alerts=[("`NotificationDeliveryFailures`", "FAILED/sent > [TBC]% in 15 min", "Warning"),
                ("`NotificationDispatcherNotRunning`", "no run within 2 × interval", "Critical")],
    ),
    dict(
        code="JOB-07", name="Matchmaking queue sweeper",
        draft="Status Draft (FT-48): matchmaking is not built; rooms are joined by id or code only.",
        bean="[TBC] com.group01.game.infrastructure.scheduler.MatchmakingSweeper (game-service)",
        purpose="Groups waiting learners of the same game type into a room and starts the match, and removes "
                "learners who waited too long.",
        trigger="@Scheduled every [TBC] s",
        refs="FT-47, FT-48 · BR-29",
        status="Draft",
        sla_short="< 1 s per run.",
        context="Learners who do not have a room code still want to play against others. The sweeper forms rooms "
                "from the waiting queue; without it, queued learners wait forever.",
        flow=["STEP 1: Lock the queue per game type (SKIP LOCKED)",
              "STEP 2: Remove entries older than [TBC] s → notify learner \"No match found\"",
              "STEP 3: While ≥ [TBC] learners wait for the same game type and domain:",
              "        create a room (FT-47), add them as members, start the match",
              "STEP 4: Log matched / expired / durationMs"],
        inputs=[("Table `[TBC] matchmaking_queue`", "`user_id, game_type, learning_domain, joined_at`", "`all`", "")],
        outputs=[("DB Insert", "Tables `game_rooms`, `room_members`, `game_matches`", "new room IN_MATCH", "Enough players"),
                 ("WebSocket", "/ws/games", "MATCH_FOUND [TBC]", "Matched")],
        idempotency=["Pattern B: a learner leaves the queue in the same transaction that adds them to a room."],
        verification="Two sweeper runs in parallel create one room for the same learners · L2 ref: [TBC].",
        rules=[("BR-29", "Room holds 2–32 players", "Step 3", "Group size capped")],
        errors=[("Transient", "Database contention", "Next run", "Yes", "Next run"),
                ("Permanent", "Content snapshot unavailable", "Return learners to the queue; alert", "No", "—"),
                ("Critical", "Unhandled exception", "Log ERROR; alert", "No", "—")],
        retry="Next scheduled run",
        dlq="Not event-driven",
        sla=[("Runtime per execution (normal)", "< 1 s", "> 3 s", "Log timing"),
             ("Runtime per execution (peak)", "[TBC]", "[TBC]", "Load test"),
             ("Records per execution", "≤ [TBC]", "—", "Log output"),
             ("Queue wait", "< [TBC] s", "> [TBC] s", "now() − joined_at")],
        index="[TBC] matchmaking_queue(game_type, learning_domain, joined_at)",
        batch="All waiting entries per game type",
        logs=[("Match formed", "INFO", "`roomId players gameType`"), ("Entry expired", "INFO", "`userId waitedSeconds`")],
        log_format=_STD_LOG_FORMAT,
        alerts=[("`MatchmakingNotRunning`", "no run within 2 × interval", "Critical"),
                ("`MatchmakingLongWait`", "p95 wait > [TBC] s", "Warning")],
    ),
]

OPEN_QUESTIONS = [
    ("OQ-01", "Route names and copy of screens not built yet are proposals; the frontend team decides the final "
              "ones.", "Screens marked FE: not built"),
    ("OQ-02", "OAuth providers, first-sign-in behaviour (create vs link by email) and the authorize endpoint the "
              "frontend already calls.", "Login, OAuth Callback, FT-05"),
    ("OQ-03", "Email Service integration: today the reset token is read from backend logs and pasted by hand.",
     "Forgot Password, Reset Password, JOB-06"),
    ("OQ-04", "Premium enforcement and the learner entitlement read; the frontend disables PREMIUM practice sets.",
     "Learning Path, Practice Set, FT-13"),
    ("OQ-05", "Mock test band conversion table; who grades Writing/Speaking sections of mock tests.",
     "Mock & Placement Tests, JOB-03"),
    ("OQ-06", "Placement test: one per learner? How the result sets the starting band/topic.",
     "Mock & Placement Tests, FT-35"),
    ("OQ-07", "AI grading price for test responses; grading worker schedule, timeouts and retries.",
     "Test Result, JOB-03"),
    ("OQ-08", "Recording upload size/duration/format and speech provider timeout.",
     "Video Player, Practice Workspaces, FT-39, FT-40"),
    ("OQ-09", "Dictation scoring rule (percentage of reference words).", "Video Player, FT-41"),
    ("OQ-10", "Notification channels, alert delay, reminder schedule and retry limits.",
     "Notification Centre, JOB-04 – JOB-06"),
    ("OQ-11", "Matchmaking group size, waiting limit and sweep interval.", "Game Lobby, JOB-07"),
    ("OQ-12", "Examiner work queue and an API for examiners to read learner responses.", "Grading Workspace, FT-32"),
    ("OQ-13", "Lesson authoring API and media upload limits.", "Media Assets, Lesson Builder, FT-18, FT-19"),
    ("OQ-14", "Activation key list for staff and SALES_STAFF access.", "Activation Keys, FT-11"),
    ("OQ-15", "Outbox relay replay procedure and DLQ retention; metrics/alerting stack.", "JOB-01, JOB-02"),
    ("OQ-16", "User list paging and search (GET /api/users returns all users today).", "User Management, FT-08"),
    ("OQ-17", "Overview, Classroom, Luyện đề and Từ điển run on mock data: which backend features they map to "
              "(mentor classes and timed practice tests are not in the SRS).",
     "Overview, Classroom, Practice Test Catalog, Practice Workspaces, Vocabulary"),
    ("OQ-18", "Mobile app (Expo, TDS 1.2): which screens it has and whether this FDS should cover them.",
     "All screens"),
    ("OQ-19", "TDS 2.2 leaves the client token storage [TBC]; the web client already keeps the access token in "
              "memory and the refresh token in the HttpOnly cookie (Section 2.0). The TDS should record this "
              "choice, and the Secure cookie flag must be on when deployed.", "Section 2.0, Login"),
    ("OQ-20", "Date-times: user-service returns local date-times without an offset, other services UTC (TDS 2.4, "
              "target UTC everywhere [TBC]). Until then screens that show user-service times may be off by the "
              "server's time zone.", "Profile & Account, User Management, User Detail"),
]


def open_questions_for(name):
    """Ids of the open questions whose Affects column names this screen or job ("Practice Workspaces" also
    matches "Practice Workspaces (Reading, …)"); questions about all screens stay in Appendix B only."""
    ids = []
    for oq_id, _, affects in OPEN_QUESTIONS:
        parts = [p.strip() for p in affects.replace(";", ",").split(",")]
        if any(p == name or name.startswith(p + " (") for p in parts):
            ids.append(oq_id)
    return ", ".join(ids)

# Frontend vs SRS / FDS differences found when reading the frontend code (Appendix C).
FE_GAPS = [
    ("GAP-01", "The learning path is one list sorted by sequenceOrder under the heading \"Lộ trình Reading\"; it is "
               "not grouped by skill.", "Learning Path", "FT-20, BR-16", "Add skill tabs and group topics by skill"),
    ("GAP-02", "No role-based home and no staff screens; GuestOnly and sign-in always lead to /learn.",
     "Login, all staff screens", "FT-07, FT-08", "Read roles from user.me and add the staff area"),
    ("GAP-03", "/home renders HomePage without an access client, so the key dialog always shows \"Phiên đăng nhập "
               "demo chưa hỗ trợ kích hoạt Key\" and nothing is redeemed.", "Home", "FT-11",
     "Pass an access client built from the session token"),
    ("GAP-04", "The Premium/Free badge in the user menu is a demo toggle (setDemoTier), not read from the "
               "subscription.", "Subscription & Points", "FT-10, FT-13", "Read access.mySub after sign-in"),
    ("GAP-05", "PREMIUM practice sets are always disabled (\"Premium — chưa mở trong giai đoạn này.\").",
     "Practice Set", "FT-13, BR-34", "Use the entitlement once FT-13 is enforced"),
    ("GAP-06", "Reset password uses a code typed by hand (from backend logs); there is no email link yet.",
     "Forgot Password, Reset Password", "FT-04", "Keep ?token= support; add email delivery"),
    ("GAP-07", "No review list, mastery, mock/placement, account, library, games, community or notification "
               "screens; pending reviews appear only in the review banner.", "Several",
     "FT-06, FT-09, FT-26, FT-28, FT-34 – FT-54", "Build from the specifications in Part 2"),
    ("GAP-08", "Writing feedback is shown inline in the essay block; writing-submissions/{id} is never called, so "
               "a FAILED or still-grading essay is not shown again after leaving the page.",
     "Lesson Player, Writing Feedback", "FT-25", "Read the latest submission on load"),
    ("GAP-09", "The topic test saves a choice answer when it is picked but a text answer only at submit; there is "
               "no timer or expiry handling.", "Test Attempt", "FT-30", "Save text answers on blur"),
    ("GAP-10", "Vocabulary, flashcards and notes of the practice area live in the browser (localStorage / page "
               "state), not in library-service.", "Vocabulary, Practice Workspaces", "FT-36, FT-42, FT-43",
     "Call the library APIs"),
    ("GAP-11", "The frontend reads the current user from /api/users/me; /auth/me is unused.", "Login", "FT-02",
     "None needed (both return the user)"),
    ("GAP-12", "When a review has no quick-check question the frontend sends an empty theory check, which the "
               "backend rejects with 422 INVALID_ANSWERS.", "Review Session", "FT-28 (NAC-02)",
     "Agree on how a review without quick-check moves to PRACTICE"),
    ("GAP-13", "Every essay try sends a new requestId. After 503 PAYMENT_UNAVAILABLE (graded, but the point debit "
               "failed) TDS 2.4 says to resend the same requestId; a new one grades the essay again with the LLM "
               "and counts toward the daily limit.", "Lesson Player", "FT-25, BR-30",
     "Keep the requestId of a PAYMENT_UNAVAILABLE try and resend it"),
    ("GAP-14", "After a passed test the result page re-reads the topics 4 times, 400 ms apart (about 1.6 s), but the "
               "relay polls every 2 s and NFR-P05 allows up to 5 s, so the page often shows no next topic although "
               "it opens a moment later.", "Test Result", "FT-29, NFR-P05",
     "Re-read for up to 6 s, or show \"Đang cập nhật lộ trình…\" with a reload button"),
]

from fds_screens import SCREENS  # noqa: E402  (screens import helpers above)
