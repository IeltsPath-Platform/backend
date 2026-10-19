# Red-team plan review: common-storage module (Security Adversary, Fact Checker, Contract Verifier)

Plan: `plans/261019-0813-common-storage-module/` (plan.md + phase-01..04). Review date 2026-10-19.
Scope: plan quality vs codebase evidence. No code was linted/built/tested.

## Finding 1: Ownership enforcement is an optional static helper; existing intake paths accept arbitrary references
- **Severity:** High
- **Location:** Phase 2, section "ObjectKeys" (`isOwnedBy`) and Risk Assessment row "Service quên gọi `isOwnedBy`"; Phase 3, `signedDownloadUrl`/`delete`
- **Flaw:** The only control preventing user A from reading or deleting user B's private recording/avatar is a static boolean method that each service must remember to call. The plan concedes: "không ép được ở thư viện". `signedDownloadUrl(bucket,key,ttl)` and `delete(bucket,key)` accept any syntactically valid key with no owner argument. The EXAMINER path (who is allowed to listen to a learner's recording) is not designed at all, which makes a bypass of `isOwnedBy` likely.
- **Failure scenario:** assessment-service adds `GET /submissions/{id}/audio` for examiners, skips `isOwnedBy` because "examiners can read anything", and reuses the same helper elsewhere. A learner submits `audioReference = "speaking/<victimId>/<uuid>.webm"`. The submission use case stores it unchecked; the service mints a signed URL for it. For avatars: the user stores someone else's key as `avatarReference`, then replaces it, and a "delete old avatar" step deletes the victim's object.
- **Evidence:**
  - `services/assessment-service/.../CreateLearnerSubmissionUseCase.java:24,31` only checks "text XOR audio non-null" and stores `c.audioReference()` verbatim; DTO `CreateLearnerSubmissionRequest.java:3` has `String audioReference` with no `@Size`/`@Pattern`.
  - `services/user-service/.../UpdateLearnerProfileRequest.java:16-17` allows any string up to 500 chars as `avatarReference`; `GetPublicProfilesUseCase.java:53-61` accepts any http/https URL with a host (including `http://` and third-party hosts).
  - Plan quote (phase-02 Risk): "nằm trong checklist của plan endpoint ở từng service, không ép được ở thư viện".
- **Suggested fix:** Make the safe path the only path: e.g. `signedDownloadUrl(OwnedKey)` / `deleteOwned(bucket, prefix, userId, key)` that performs `isOwnedBy` internally, with a distinct, explicitly named `...AsStaff` variant for the examiner case. Add a phase-2/3 test that the owner-checked API throws for a foreign key. Add a requirement that migrating existing `audio_reference`/`avatar_reference` values (free-form today) is in the service plans.

## Finding 2: The "check size before reading the body" defense is illusory; `byte[]` API plus client-controlled Content-Length
- **Severity:** High
- **Location:** Phase 2 "UploadPolicy.requireSize"; plan.md Rủi ro row "`put` nhận `byte[]` ngốn RAM"; Phase 1 "put nhận byte[]"
- **Flaw:** The mitigation says `maxBytes` is checked "trước khi đọc hết file". That relies on `Content-Length` (client-asserted; chunked/absent bypasses it). `check()` runs only after the entire body is in a `byte[]`. Nothing in the plan or repo sets the servlet multipart/Tomcat limits that actually bound memory, and the plan forbids touching config ("Không sửa ... config tập trung").
- **Failure scenario:** An authenticated CUSTOMER (any registered learner) sends many concurrent chunked `POST` uploads of hundreds of MB to the Speaking endpoint. The service buffers each into `byte[]` before `UploadPolicy.check` runs, so the heap (`-Xmx512m` in compose) is exhausted: DoS of assessment-service for everyone. Alternatively the Spring default `spring.servlet.multipart.max-file-size=1MB` rejects the planned 2 MB avatars/10 MB recordings and a dev "fixes" it by setting a huge limit.
- **Evidence:** grep for `max-file-size|max-request-size|multipart` across `services/**` and `infra/**` yaml/java returns no hits (no existing upload limits anywhere). `docker-compose.yml:23` `JAVA_TOOL_OPTIONS: "-Xms64m -Xmx512m"`. Plan phase-02: "Kiểm tra sớm theo Content-Length, trước khi đọc body."
- **Suggested fix:** State that the authoritative limits are `spring.servlet.multipart.max-file-size/max-request-size` (and a bounded `InputStream` read: `readNBytes(maxBytes + 1)`) in each consumer plan, and add an `UploadPolicy.readBounded(InputStream)` helper that never buffers more than `maxBytes+1`. Drop the claim that `requireSize` mitigates memory exhaustion.

## Finding 3: Signed-upload path has no server-side content control and relies on unverifiable dashboard config
- **Severity:** High
- **Location:** plan.md "Bối cảnh" (row Bucket: "MIME và dung lượng cấu hình ở Dashboard") and Rủi ro row "Client khai sai MIME"; Phase 3 `signedUploadUrl`; Phase 4 contract test
- **Flaw:** For admin mp3/image upload the client PUTs directly to Supabase. Sniffing (`ContentSniffer`) is bypassed entirely. The only barrier is bucket `allowed_mime_types`/`file_size_limit`, which the plan says lives in the Dashboard, is not in code, and is not verified by any test (the contract test uses a bucket "không giới hạn MIME/dung lượng" and only covers `contract-test`). The `avatars` bucket "chưa tạo" so its config does not exist yet. The `media` bucket is public and already referenced by `CONTENT_MEDIA_BASE_URL`.
- **Failure scenario:** `media` is created/edited without MIME restrictions (or restrictions are later loosened for convenience). A compromised/malicious CONTENT_AUTHOR obtains a signed upload URL and PUTs `text/html` or `image/svg+xml` with script to a public bucket on the project's `supabase.co` origin; the key is then stored as `media_reference`. The existing content pipeline already accepts SVG data URIs, so the "no SVG" invariant is not system-wide. Whether Supabase neutralizes served HTML is UNVERIFIED here.
- **Evidence:** `services/content-service/.../MediaReferencePolicy.java:15-16` allows `data:image/(png|jpeg|svg\+xml);base64,...`; `.env:28` `CONTENT_MEDIA_BASE_URL=https://<project-ref>.supabase.co/storage/v1/object/public/media/` (public bucket in use today). Plan phase-03 does not read bucket metadata; phase-04 scenario 3 only checks the happy path. Signed-upload TTL: plan says unknown (Open Question 2); my search found the "2 hours" figure only on a third-party page, not Supabase docs (UNVERIFIED).
- **Suggested fix:** Add `ObjectStorage.verifyUploaded(bucket,key, UploadPolicy)` (server downloads first N bytes via a range request / `info` call and runs `ContentSniffer`) as a mandatory step before the service persists the key; add a contract-test step asserting target buckets have non-null `allowed_mime_types` and `file_size_limit` (read via `GET /bucket/{id}`); document bucket config as a deployment checklist item owned by someone.

## Finding 4: One god-key for three services, distributed through root `.env` imported by every Java service
- **Severity:** High
- **Location:** plan.md "Cấu hình" (`SUPABASE_SECRET_KEY`, "Chỉ ở `.env`") and Phase 4 doc line "chỉ service bật `app.storage.enabled` mới đọc"
- **Flaw:** The Supabase secret/service_role key bypasses RLS and has full access to every bucket (plan row "Xác thực": "Khóa server bỏ qua RLS"). The same key will be configured for user-service, assessment-service and content-service. The claim "chỉ service bật `app.storage.enabled` mới đọc" is documentation, not a control: AGENTS.md states all business services import the root `.env`.
- **Failure scenario:** An RCE/SSRF/actuator leak in the lowest-trust service (user-service avatar upload parses attacker-controlled image bytes) yields the same key that can list/download every learner's private Speaking recording and overwrite `media` content. Also the key lifecycle: Supabase says legacy keys are being deprecated by end of 2026 (UNVERIFIED beyond search result), so rotation will be needed and there is no rotation plan.
- **Evidence:** `AGENTS.md:138` ("Gateway và mọi service Java nghiệp vụ import `.env` ở root"); `services/content-service/src/main/resources/application.yaml:10` `optional:file:./.env[.properties]`; `docker-compose.yml:21` shows secrets passed through a shared anchor pattern; no per-service env scoping is described in the plan.
- **Suggested fix:** Use distinct variable names per service (`USER_SUPABASE_SECRET_KEY`...) mapped only in that service's compose block, document that Supabase cannot scope this key and therefore treat the Speaking recordings bucket as the highest-value asset; add a rotation runbook line; consider moving private-bucket access to one service (assessment) and having others call it.

## Finding 5: "No key/path leak" guarantee is verified only against author-written mocks; upstream `message` is forwarded verbatim
- **Severity:** Medium
- **Location:** Phase 3 "Ánh xạ lỗi" and test "không rò khóa/key"; plan.md Quy tắc 4
- **Flaw:** `StorageException.getMessage()` embeds Supabase's `message` (<=200 chars, control chars stripped) verbatim. Supabase error messages can echo object paths/identifiers; the plan's leak test feeds responses the author wrote (`"The resource already exists"`), so it cannot detect real upstream echoes. Separately, bad input raises `IllegalArgumentException` whose message is unspecified, and the consumer services' handlers return `e.getMessage()` to clients as HTTP 400.
- **Failure scenario:** Supabase returns `{"message":"Invalid key: speaking/<uuid>/..."}`; the service surfaces it, leaking another user's object path/userId. Also a mis-set server-side bucket name (config error) surfaces as a client 400 with detail instead of a 500.
- **Evidence:** `services/assessment-service/.../GlobalExceptionHandler.java:40-45` maps `IllegalArgumentException` to 400 with `e.getMessage()`; `:117-119` generic handler hides detail, but `StorageException` extends `RuntimeException` and would only hit that. Plan phase-03: "message = trường message trong JSON lỗi của Supabase ... cắt <= 200 ký tự".
- **Suggested fix:** Do not forward upstream text at all (use status + a fixed enum reason), or redact any occurrence of the key/bucket substring before building the message; specify that `requireValid`/bucket validation messages never include the offending value; use a dedicated `InvalidStorageArgumentException` or document that config-derived bucket errors are 500s.

## Finding 6: Private-bucket signed download URLs: 24 h cap, query-string bearer token, no bucket-privacy enforcement
- **Severity:** Medium
- **Location:** Phase 1 `ObjectStorage.signedDownloadUrl` ("ttl trong [1 giây, 24 giờ]"); Phase 3 table
- **Flaw:** For recordings of a learner's voice the module allows up to a 24 h capability URL whose token is in the query string (ends up in browser history, gateway/proxy access logs, Referer). The module also exposes `publicUrl` for any bucket name (private included) and `signedDownloadUrl` for public buckets with no guard, since bucket is a free parameter by design (plan Quy tắc 1).
- **Failure scenario:** A 24 h URL is pasted in a support chat or logged by api-gateway; anyone with it can download the recording until expiry, with no revocation possible (Supabase signed URLs cannot be revoked short of deleting the object).
- **Evidence:** plan Phase 1 Javadoc quote; `docker-compose.yml` routes everything through api-gateway (access logging at default); no log-redaction requirement in the plan for URLs.
- **Suggested fix:** Cap at e.g. 15 minutes in the module (overridable only via explicit property), return the URL wrapped in a type whose `toString()` is redacted, and document that services must not log it.

## Finding 7: Magic-byte sniffing gives false assurance; known weaknesses are accepted while EXIF/polyglot are pushed out of scope
- **Severity:** Medium
- **Location:** Phase 2 "ContentSniffer" table and Risk Assessment; plan.md "Phạm vi" (no EXIF removal)
- **Flaw:** Only the first 12 bytes are checked. `FF D8 FF` + arbitrary payload, PNG header + HTML/JS tail, any MP4 (video included, `ftyp` at 4-7) as `audio/mp4`, any WebM/Matroska as `audio/webm`, and `FF Ex` bytes (AAC/ADTS or random) as `audio/mpeg` all pass. Avatars are stored in a **public** bucket without EXIF stripping (explicitly deferred to user-service), so the original GPS/camera metadata becomes publicly downloadable at a predictable URL pattern that includes the userId.
- **Failure scenario:** A learner uploads a phone photo with GPS coordinates as an avatar: `ContentSniffer` returns `image/jpeg`, `put` stores the original bytes, the public URL leaks home location. Or an attacker uploads a 10 MB MP4 video as a "Speaking recording", consuming storage and examiner time.
- **Evidence:** plan phase-02 table rows (`audio/mp4` -> `"ftyp"` at 4-7; `audio/mpeg` -> `FF` and `(b1 & 0xE0)==0xE0`); plan.md line 35: "không gồm resize/xóa EXIF ảnh (việc của user-service)"; `PublicProfileResponse` exposes `avatarUrl` and `id` publicly (`user-service/.../PublicProfileResponse.java:7`).
- **Suggested fix:** Either make EXIF/metadata removal (re-encode via `ImageIO`) a hard prerequisite recorded as a blocking item in the user-service avatar plan, or have `put` for public image buckets refuse unless a `reencoded` flag is set. Tighten audio: reject `video/*`-brand `ftyp` values (allow `M4A `, `mp42`/`isom` only) and parse the EBML DocType (`webm`).

## Finding 8: Properties/URL validation is under-specified in ways that can send the secret to the wrong host; redirect policy not pinned
- **Severity:** Medium
- **Location:** Phase 1 "SupabaseStorageProperties" (http allowed for localhost); Phase 3 "Dựng RestClient"
- **Flaw:** "Cho phép `http://` chỉ khi host là `localhost`, `127.0.0.1` hoặc `::1`" does not say the host must come from `java.net.URI#getHost`. A string-prefix check accepts `http://localhost@evil.example/` or `http://localhost.evil.example`. There is no host allow-list (`*.supabase.co`/explicit self-host setting) and the default `apikey` header is attached to every request. The plan also never pins `HttpClient.Redirect.NEVER`; the JDK client's redirect handling re-sends custom headers if someone sets `followRedirects`.
- **Failure scenario:** A mistaken/hostile value for `SUPABASE_URL` in a shared config (config-repo is plain files readable by anyone with repo access and served over HTTP by Config Server) sends the full-access key in cleartext to an attacker host. Binding failures for malformed values may also be echoed by Spring's `BindException` (tests only cover the "missing" case, not "malformed value contains the secret").
- **Evidence:** plan phase-01 test list (only "http://example.com" vs "http://localhost:54321" cases; no userinfo/lookalike cases); `infra/config-server/config-repo/user-service.yaml:5,24,26` shows config-repo already carries default secrets with the same `${VAR:default}` pattern, i.e. config-repo is a real secret-distribution channel; `AGENTS.md` note on `infra/config-server`.
- **Suggested fix:** Parse with `URI`, reject userinfo, require `getHost()` match; add test cases `http://localhost@evil.com`, `http://localhost.evil.com`, `https://user:pw@x`; pin `Redirect.NEVER` and test it; add a test that a malformed `url` and a bad `secretKey` never appear in the exception chain.

## Finding 9: Contract-test/ops procedure contradicts its own secret-handling rule and guards with a deny-list on the only (production) project
- **Severity:** Medium
- **Location:** Phase 4 "Contract test" (bucket guard), Implementation Step 3, and the manual `curl`/10 MB steps
- **Flaw:** (a) Guard is a deny-list of 3 bucket names; any other bucket name in the real project (a future bucket, a typo) is accepted and the test then runs `delete` with the full-access key. (b) The documented command is `SUPABASE_URL=… SUPABASE_SECRET_KEY=… mvn ...` inline, which is stored in shell history and is POSIX-only (the project owner works on Windows 11/PowerShell), immediately followed by a parenthetical saying not to type it in history-saved commands. (c) The manual `curl`/`HttpClient` steps against `DELETE /object/{bucket}/{key}` and the 10 MB PUT carry the key on the command line. (d) No separate non-production project is mentioned; the repo's `.env` already points at a real project.
- **Failure scenario:** A developer sets `SUPABASE_CONTRACT_BUCKET=documents` (real bucket, not in deny-list); the test uploads/deletes there. Or the secret lands in `~/.bash_history` (the home directory is itself an untracked git working tree per the session's `git status`: `?? ../../../.bash_history`) and later gets committed/synced (OneDrive path).
- **Evidence:** plan phase-04 lines 28-31, 81-82; `.env:28` (real `*.supabase.co` project ref in use); gitStatus listing `.bash_history`; repo path is under `OneDrive\Desktop`.
- **Suggested fix:** Use an allow-list (`bucket` must start with `contract-test-` and be created by the test itself, then dropped), read the key from a file path (`SUPABASE_SECRET_KEY_FILE`) or prompt, give PowerShell and bash variants without inline secrets, and forbid running against the production project ref.

---

## Fact Checker results (sampled; plan claims vs repo)

| Claim | Result |
| --- | --- |
| Root `pom.xml` has `<module>shared/common-security</module>` to insert after | VERIFIED `pom.xml:24` (also `common-events` at line 23) |
| AGENTS.md "13 module trong reactor: 3 infra, 1 shared, 9 business" is stale | VERIFIED stale: `AGENTS.md:43` says 13; `pom.xml:23-36` has 14 modules (2 shared) -> 15 after this plan. Plan correctly says "đếm lại" |
| AGENTS.md lines ~21, ~43, ~49, ~236, ~325 | VERIFIED: 21 module table, 43 build row, 49 shared code row, 236 tree, 325 test command |
| CLAUDE.md lines ~25, ~50, ~69 | VERIFIED (`CLAUDE.md:25,50,69`) |
| README.md lines ~18, ~35-36 | VERIFIED (`README.md:18,35,36`); `common-events` missing from both, as plan says |
| Pattern `InternalJwtProperties` compact constructor "must be configured" | VERIFIED `common-security/.../InternalJwtProperties.java:13-22` |
| common-security `.imports` registers one auto-config class | VERIFIED `META-INF/spring/...AutoConfiguration.imports:1` |
| `common-events` pom: compiler 3.14.0 + surefire 3.5.3, relativePath `../../pom.xml` | VERIFIED `shared/common-events/pom.xml:9,52,68` |
| Plan says "Không Lombok"; "giống common-events" for plugins | PARTIAL: common-events/common-security poms configure Lombok annotationProcessorPaths (`common-events/pom.xml:60`, `common-security/pom.xml:62-68`); omitting it is fine but "giống" is not exact |
| `Dockerfile.spring-service` is `COPY . .` then `mvn -pl <module> -am` | VERIFIED `Dockerfile.spring-service:6,19-22`; `.dockerignore` excludes `.env` (no secret baked into image) |
| Root `.env` gitignored | VERIFIED `.gitignore:52` |
| `assessment_service` `audio_reference VARCHAR(500)` (plan's "cột DB varchar(500)") | VERIFIED `assessment-service/.../V1__create_assessment_tables.sql:59,116`; but JPA `LearnerSubmissionJpaEntity.java:25` has no `length` (schema validation fine, noted) |
| user-service `avatar_reference VARCHAR(500)` | VERIFIED `V2__create_identity_tables.sql:7`, `LearnerProfileJpaEntity.java:46` |
| `MediaReferencePolicy` is the only place building content media URLs | VERIFIED `MediaReferencePolicy.java:7-11,31-45`; its `OBJECT_KEY` regex (`:18`) allows uppercase, unlike `ObjectKeys` -> any existing/seeded mixed-case key would fail `requireValid` (I found no uppercase seeded `.mp3/.png` keys beyond 3 hits in `V12__seed_listening_demo.sql`, which I did not inspect further: UNVERIFIED impact) |
| Old plan `261016-1030-supabase-media-storage/plan.md` has `blockedBy`, "Cập nhật 2026-10-19", and stale variable table | VERIFIED `plan.md:8,17,23,57,61` |
| `plans/reports/` exists for phase-4 report | VERIFIED (dir exists, files present) |
| Supabase: `sb_secret_` must go in `apikey`, not Bearer; secret keys 401 from browser UA | VERIFIED against Supabase docs via web search (not repo) |
| Supabase: duplicate upload returns 400 "Asset Already Exists" | VERIFIED via Supabase standard-upload docs search result; exact status can vary by version (contract test covers) |
| Supabase: signed upload URL fixed lifetime | UNVERIFIED (plan Open Question 2; only third-party source says 2 h) |
| Supabase: bulk `DELETE /object/{bucket}` body `{"prefixes":[...]}` idempotent | UNVERIFIED by me (SDK-derived claim in plan; plan itself defers to contract test) |
| `JdkClientHttpRequestFactory#setReadTimeout` exists in Spring 6.2 | UNVERIFIED in repo (no code yet); plan itself lists it as a risk |
| docker-compose / config-repo contain any `SUPABASE_*` mapping | FAILED to find: grep for `SUPABASE` in compose and config-repo returns nothing; only `.env:28` has a supabase URL. No plan owns compose/config-repo wiring (plan Quy tắc 7 forbids it here), so the first consumer plan must add it |

## Contract Verifier results

- Public contract introduced: `ObjectStorage` (5 methods), `StoredObject`, `SignedUpload`, `StorageException`, `UploadRejectedException`, `ObjectKeys`, `UploadPolicy`, property names `app.storage.enabled`, `app.storage.supabase.{url,secret-key,connect-timeout,read-timeout}`.
- Existing consumers of any of these: **0** (grep `commonstorage|ObjectStorage|app.storage` over `backend/**` returns only plan files). Planned consumers: 3 services (user-service, assessment-service, content-service), none with a plan file present in `plans/` yet; `261016-1030-supabase-media-storage` (content-service) is the only linked one (`blockedBy` verified).
- Services that already depend on `common-security` (the pattern being copied): 10 `pom.xml` matches under `services/` and `infra/` (grep `common-security` in `**/pom.xml`). Each new storage consumer must add its own dependency + `.env`/compose/config-repo entry; Phase 1's rollback statement "không service nào phụ thuộc" holds only until a consumer lands.
- Config-key consumers to be touched by consumer plans (not in this plan): `docker-compose.yml` (env block per service), `infra/config-server/config-repo/{user,assessment,content}-service.yaml`, `.env.example` (if any), plus AGENTS.md/CLAUDE.md/README per Phase 4. The plan lists the docs but not the compose/config-repo ones; there is no owner for them.
- Behavioral contract mismatch: `ObjectKeys.requireValid` (lowercase only) vs `MediaReferencePolicy.OBJECT_KEY` (mixed case permitted, `MediaReferencePolicy.java:18`). If content-service later calls `publicUrl`/`delete` with legacy mixed-case keys the module throws `IllegalArgumentException` -> HTTP 400 in current handlers.

## Unresolved questions
1. Who owns the compose/config-repo wiring for `SUPABASE_URL`/`SUPABASE_SECRET_KEY` given Phase 1 rule 7 forbids it here?
2. Is there a non-production Supabase project for contract tests, or is the single real project the only one?
3. Who owns the Dashboard bucket policy (MIME, size, public/private) and where is it recorded so Finding 3 is checkable?

Status: DONE
Summary: 9 findings (4 High, 5 Medium) plus fact-check and contract-verification tables; the strongest are optional ownership enforcement (F1), non-bounding upload-size defense (F2), uncontrolled signed-upload content (F3) and a single full-access key spread across services (F4).
Report path: C:\Users\pduy8\OneDrive\Desktop\capstone-fall26\backend\plans\261019-0813-common-storage-module\reports\from-code-reviewer-to-planner-red-team-security-adversary-plan-review-report.md
