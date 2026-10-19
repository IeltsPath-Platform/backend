# Red Team Review: common-storage-module plan (Assumption Destroyer, Fact Checker, Contract Verifier)

Paths below are relative to `C:\Users\pduy8\OneDrive\Desktop\capstone-fall26\backend`.

## Finding 1: The plan's base branch does not contain what the plan says it modifies
- **Severity:** High
- **Location:** `plan.md` header and "Rủi ro" (last row); Phase 1 step 1; Phase 4 "Plan Supabase cũ"
- **Flaw:** Step 1 says to create `feat/common-storage` "từ `main` mới nhất". `main` has neither `shared/common-events` nor `plans/261016-1030-supabase-media-storage`. Phase 4 says the old plan "Đã được cập nhật ngày 2026-10-19", but that edit exists only as an uncommitted working-tree change on branch `feat/common-events`.
- **Failure scenario:** The implementer follows step 1 and branches from `main`. The "Cập nhật 2026-10-19" section and `blockedBy` are absent, or the file does not exist at all. Phase 4 then edits a file that is different from, or missing relative to, what it describes. If they stay on `feat/common-events`, `blocks: [261016...]` is a dangling pointer, because that plan's edits were never committed anywhere. The pom `<module>` conflict risk is also described wrongly: `common-events` is not merged, so the reactor module count and the AGENTS.md count differ by base. Branching from `main` gives 14 modules after the change, and from `feat/common-events` gives 15.
- **Evidence:**
  - `git ls-tree -r main --name-only` returns nothing for `^plans/261016` or `shared/common-events`.
  - `git show main:pom.xml` lists only `<module>shared/common-security</module>` under `shared`; the working-tree `pom.xml:20-21` also lists `shared/common-events`.
  - `git status --short plans` shows ` M plans/261016-1030-supabase-media-storage/plan.md` and ` M .../phase-02-api-upload-sau-mvp.md`, plus `?? plans/261019-0813-common-storage-module/`.
  - Current branch: `feat/common-events`.
  - `git show main:AGENTS.md` line 43 says "13 module".
- **Suggested fix:** Commit the old-plan edits and this plan first. Declare the base branch explicitly (rebase on `feat/common-events`, or wait for the merge). Remove the claim "đã được cập nhật" unless it is committed.

## Finding 2: Error mapping covers two exception types; other failure paths bypass the "no leak" guarantee
- **Severity:** High
- **Location:** Phase 3, "Ánh xạ lỗi" and table rows for `signedDownloadUrl` / `signedUploadUrl`
- **Flaw:** Only `RestClientResponseException` and `ResourceAccessException` are mapped. `RestClient` also throws plain `RestClientException` subclasses. Examples: `UnknownContentTypeException` on a 200 with an HTML or text body, or `HttpMessageNotReadableException` wrapped in `RestClientException` on malformed JSON. The JDK `HttpClient` can also throw `IllegalArgumentException` or `IllegalStateException` from request building. Spring's messages for these include the response body and sometimes the full URL, which contains `userId`. Separately, the plan copies Supabase's own `message` field into `StorageException.getMessage()`. Rule 4 says "không kèm key", yet nothing guarantees Supabase does not echo the object path in `message`. The leak test only uses mock bodies the author wrote.
- **Failure scenario:**
  - A CDN, proxy or Supabase maintenance page returns 200 `text/html`. A raw `RestClientException` carrying the body escapes `ObjectStorage`. Callers see an exception type the interface never declared, and the "no cause / no key in message" tests never exercise it.
  - Supabase returns `{"message":"Invalid key: speaking/<userId>/..."}`. The `userId` ends up in `StorageException.getMessage()`.
- **Evidence:**
  - Phase 3 lines 58-62 list only two mapped types.
  - Phase 3 line 116 tests only "mỗi kịch bản lỗi ở trên".
  - `plan.md:45-46` (rule 4) claims the message has no key.
  - Whether Supabase echoes the key is UNVERIFIED (external API).
- **Suggested fix:**
  - Catch `RestClientException` as the parent type, and wrap the JDK runtime exceptions the same way.
  - Add a test for a 200 non-JSON body and for an empty body.
  - Either drop the Supabase `message` or scrub the key and bucket from it before including it.

## Finding 3: `ObjectKeys.requireValid` is applied to every method and rejects keys that already exist in the repo's seed data
- **Severity:** High
- **Location:** Phase 2 "Quy tắc key"; Phase 3 "Kiểm tra đầu vào chung cho mọi phương thức"
- **Flaw:** Segment regex is `[a-z0-9][a-z0-9._-]{0,63}` (lowercase only). It is enforced by `publicUrl`, `delete`, `signedDownloadUrl` and `signedUploadUrl`, not only by key generation. Existing content-service assets, which the plan says were uploaded by hand ("Không upload tay nữa"), use uppercase keys. In addition, `forUser(prefix, …)` and `of(prefix, …)` do not define whether `prefix` may contain `/`. `isOwnedBy` only accepts exactly 3 segments, so a multi-segment prefix is silently never "owned".
- **Failure scenario:** content-service later calls `storage.publicUrl("media", asset.mediaReference())` or `delete(...)` on `listening/demo/numM.mp3`. It gets `IllegalArgumentException`. In user-service-style handlers that becomes an HTTP 400 for the client (see Finding 4). Deleting or replacing a legacy asset is impossible through the module.
- **Evidence:**
  - `services/content-service/src/main/resources/db/migration/V12__seed_listening_demo.sql:36` has `'listening/demo/numM.mp3'`.
  - The same file, line 43, has `spellM.mp3`, and line 56 has `trapM.mp3`.
  - Phase 2 line 51 regex is lowercase only.
  - Phase 2 lines 39-44 do not define a prefix grammar.
- **Suggested fix:** Apply the strict regex only to server-generated keys (`forUser` / `of`). Give read, delete and sign operations a looser validator that only blocks traversal and URL-breaking characters. Define the prefix as exactly one segment, or document multi-segment behavior in `isOwnedBy`.

## Finding 4: `IllegalArgumentException` as the contract for bad input is mapped to HTTP 400 with its message, in the actual consumer
- **Severity:** High
- **Location:** Phase 3, Requirements ("đầu vào sai quy về `IllegalArgumentException`"); Phase 2 `requireValid`; `plan.md` rule 4
- **Flaw:** The plan's no-leak rules and tests cover `StorageException` only. The IAE messages from `ObjectKeys.requireValid` and the bucket check are not specified and not tested for leaks. The first named consumer, user-service, turns any IAE into a 400 response and echoes `exception.getMessage()` to the client. Server-side faults are also reported as client errors. Examples: a misconfigured bucket env var failing the bucket regex, or `ttl` out of range.
- **Failure scenario:** `requireValid` is written the obvious way, `"invalid key: " + key`, and a client-supplied `audioReference` containing another user's id hits the handler. The message is echoed back in the 400 body. Alternatively, a deployment typo in `SUPABASE_AVATAR_BUCKET` makes every upload return 400 "bucket invalid" to learners.
- **Evidence:**
  - `services/user-service/src/main/java/com/ieltspath/user/api/exception/GlobalExceptionHandler.java:84,87-89` maps `IllegalArgumentException` to `BAD_REQUEST` with `exception.getMessage()`.
  - Line 119 logs the full stack trace of unexpected exceptions.
  - `plan.md:45-46` and `phase-03:116` scope the leak tests to `StorageException`.
- **Suggested fix:**
  - Specify constant IAE messages with no input echo, and add leak tests for them.
  - Validate the bucket at startup in `SupabaseStorageProperties` or the service config, not per call.
  - Consider a dedicated `InvalidStorageArgumentException`, so callers do not rely on the generic IAE.

## Finding 5: Several "Hợp đồng REST đã xác minh" rows are UNVERIFIED, and the design commits to them before the check that could invalidate them
- **Severity:** High
- **Location:** `plan.md` rule 8 and "Hợp đồng REST đã xác minh"; Phase 3 "Dựng RestClient" (request factory choice); Phase 4 manual DELETE comparison
- **Flaw:** Rule 8 claims "mọi giá trị … đã được đối chiếu". The "Câu hỏi còn mở" section (questions 2 and 3) admits the signed-upload TTL and the DELETE form are unresolved. The JDK `HttpClient` factory is chosen as "Chọn cố định" specifically because the SDK-style `DELETE` with a body does not work on `HttpURLConnection`. If the reference-form `DELETE /object/{bucket}/{key}` (no body) works, that justification disappears. The repo's existing clients use `SimpleClientHttpRequestFactory`. The manual comparison step is placed in Phase 4, after Phase 3 has hard-coded the body form, the factory and the test expectations. The Supabase rows themselves (`x-upsert`, `Cache-Control` header, bulk-delete body shape, 400 on duplicate, `sb_secret_` header rules) cannot be checked locally and are UNVERIFIED.
- **Failure scenario:** Phase 4 shows that the bulk form returns 4xx or silently deletes nothing (key not found returns 200 `[]`, so the plan itself says a missing key is a non-error). `delete` then reports success while orphaning objects. Or the simpler no-body form works, and Phase 3 has already built a non-repo-standard HTTP stack and its tests around the wrong choice. The contract test scenario 4 only checks that a signed GET 4xxs after delete, which cannot distinguish "deleted" from "never existed".
- **Evidence:**
  - `plan.md:52-53` (rule 8) versus `plan.md:122-125` (open questions 2, 3).
  - `phase-03:35` ("Chọn cố định … vì DELETE có body").
  - `phase-04:44` (manual step comes after Phase 3).
  - `services/assessment-service/.../OpenAiCompatibleEssayGrader.java:59-61` and `services/community-service/.../UserPublicProfileClient.java:26-28` use `SimpleClientHttpRequestFactory`.
- **Suggested fix:**
  - Move a one-shot curl check of both DELETE forms and the duplicate-status code to before Phase 3, or make it a Phase 3 gate.
  - Reword rule 8 to separate "doc-confirmed" from "not confirmed".
  - Make contract scenario 4 assert on the response of `delete` itself, such as a follow-up `exists` call, a listing, or a signed GET before and after.

## Finding 6: Integration verification is vacuous: no consumer ever loads the module
- **Severity:** Medium
- **Location:** Phase 1 step 9 and Success Criteria; Phase 3 step 6 and Success Criteria
- **Flaw:** "`mvn -q -pl services/user-service -am compile` để chắc việc thêm module không làm hỏng build của service" does not exercise the new module at all. `user-service` does not depend on `common-storage`, so `-am` never builds it.
- **Failure scenario:** The check passes whether or not `common-storage` compiles. The auto-config is proven only inside `ApplicationContextRunner` with a hand-picked configuration class. Nothing proves that the `.imports` file is discovered by `SpringApplication`, or that the bean wires into a real servlet service. Examples: servlet context plus Eureka and cloud-config classpath, the second `RestClient` and `ObjectMapper` beans, or `@ConditionalOnMissingBean(ObjectStorage.class)` combined with service-level mocks.
- **Evidence:**
  - `services/user-service/pom.xml:22` lists only `common-security`; no `common-storage` dependency exists.
  - `phase-01:119`, `phase-03:91`, `phase-03:129`.
  - `phase-01:152` mentions an IDE `ClassFormatError` risk that this check would not catch either.
- **Suggested fix:** Replace it with a real wiring test (`ApplicationContextRunner` using `AutoConfigurations.of(...)` loaded via the `.imports` file, or a tiny `@SpringBootTest` fixture). Delete the user-service compile claims or label them "regression only".

## Finding 7: Phase 4's "empty grep" success criterion matches the plan's own fake key
- **Severity:** Medium
- **Location:** Phase 4, Success Criteria (4th bullet)
- **Flaw:** `git diff main... | grep -nE "sb_secret_[A-Za-z0-9_-]{8,}|eyJ[...]{20,}"` is required to be empty. `sb_secret_test-not-real` (Phase 3's fake key, also in committed tests) has 13 trailing characters from the class, so it matches. The text says the fake key "đã được liệt kê", but the command has no exclusion. The check is also computed against `main...`, which includes unrelated commits if the branch is based on `feat/common-events` (Finding 1).
- **Failure scenario:** The criterion can never be satisfied literally. Either the implementer ignores it (a secret-scan that is routinely ignored is useless) or weakens the regex until it cannot catch real keys.
- **Evidence:** `phase-04:93`, `phase-03:96` (`sb_secret_test-not-real`).
- **Suggested fix:** Make the criterion `... | grep -v 'sb_secret_test-not-real'` with an explicit allowlist, or use a fake key that cannot match (such as `sb_secret_FAKE`, shorter than 8 characters).

## Finding 8: Default timeouts and `HttpClient` lifetime are not sized for the stated payloads
- **Severity:** Medium
- **Location:** `plan.md` "Cấu hình" (`read-timeout` 10s); Phase 3 "Dựng RestClient" and bean registration
- **Flaw:**
  - A single `readTimeout` of 10s applies to the 10 MB Speaking upload through `put`, to metadata calls such as sign/delete, and to the 1-second-precision signing. In Spring 6.2, `JdkClientHttpRequestFactory.setReadTimeout` maps to `HttpRequest.Builder.timeout`, which in the JDK bounds the time until response headers arrive. Whether that includes body transmission is UNVERIFIED.
  - For a 10 MB upload on a slow link this is likely too short, yet the plan gives no per-operation override. The repo precedent is 3s connect and 45s read for slow external calls.
  - `SupabaseRestClientFactory.create(props)` builds a new `java.net.http.HttpClient`, which owns a selector thread and connection pool. The bean is a singleton and never closed. The contract test also calls `create`, so every instance leaks threads until the JVM ends.
  - Property lifetime: the secret key lives in `SupabaseStorageProperties` for the whole app lifetime, and again in the `RestClient` default headers.
- **Failure scenario:** Mobile users on slow uploads get a `StorageException(status=0, HttpTimeoutException)` even though the file reached Supabase. The caller cannot retry safely, because the plan forbids retry on `put`: a retry hits 400 "exists". The user must re-upload and orphans accumulate.
- **Evidence:**
  - `plan.md:91-92`.
  - `phase-03:35,62`.
  - `services/assessment-service/.../OpenAiCompatibleEssayGrader.java:60-61`.
- **Suggested fix:** Separate timeouts (short for sign/delete, size-aware for `put`). Document the JDK timeout semantics once verified. Implement `DisposableBean` or expose the `HttpClient` close. Add an orphan-handling note for the timeout-after-write case.

## Finding 9: Contract test safety relies on a 3-name denylist and production env var names
- **Severity:** Medium
- **Location:** Phase 4, "Contract test" (bullets 1-3)
- **Flaw:** The test refuses only `media`, `avatars` and `speaking-recordings`. Any other bucket name (a future bucket, a typo such as `Media`, or a staging bucket) is accepted, and the test then writes and deletes under `contract-test/{uuid}/`. It is also enabled by `SUPABASE_URL` and `SUPABASE_SECRET_KEY`, the same names that production services use, and the root `.env` that holds them is imported by every service. A developer whose shell or IDE run configuration carries these variables will hit the real project on a plain `mvn test`, gated only by `SUPABASE_CONTRACT_BUCKET`.
- **Failure scenario:** A teammate sets `SUPABASE_CONTRACT_BUCKET=media` in a typo variant (trailing space, different case) and the guard passes. Or a bucket added later (such as `exam-attachments`) is not on the list, and the test creates and deletes objects there. Scenario 5 modifies the key by one character, which risks account-level rate limiting or lockout. The "chỉ service bật `app.storage.enabled` mới đọc" statement planned for AGENTS.md §3.4 is not enforceable, because the same `.env` is loaded by all services.
- **Evidence:**
  - `phase-04:28-30`, `phase-04:93`.
  - `AGENTS.md:138` and `CLAUDE.md:43-44` say gateway and every business service import the root `.env`.
  - `services/content-service/src/main/resources/application.yaml:10-11` shows `optional:file:../../.env[.properties]`.
- **Suggested fix:** Use an allowlist: the bucket name must start with `contract-test`. Use dedicated env var names (`STORAGE_CONTRACT_*`) so shared `.env` values cannot enable it by accident. Reword the AGENTS.md line to what is actually enforced.

## Fact Check Log

| Claim | Result |
| --- | --- |
| `pom.xml` Boot 3.5.14, modules list with common-events and common-security (`pom.xml:17-37`) | VERIFIED. No `spring-boot-starter-parent`, only BOM import (`pom.xml:39-60`) |
| `shared/common-security` `.imports` single line, `InternalJwtProperties` compact-ctor `"... must be configured"` | VERIFIED (`shared/common-security/.../InternalJwtProperties.java`; `.imports` file) |
| common-events uses compiler 3.14.0 and surefire 3.5.3 | VERIFIED (`shared/common-events/pom.xml`). Note it uses Lombok `annotationProcessorPaths`; the plan correctly omits it |
| AGENTS.md lines ~21, ~43, ~49, ~236, ~325, §3.4 (line 109), §3.5 (line 119, "shared/ chỉ chứa technical concern" at 133) | VERIFIED. §6.7 and §6.8 present (lines 330-ish). Module count "13" at line 43 is stale vs 14 modules on this branch, and will be 15 |
| CLAUDE.md ~25, ~50, ~69 | VERIFIED (`CLAUDE.md:25,50,69`) |
| README.md ~18, ~35-36 | VERIFIED (`README.md:18,36`); `common-events` is absent from both places |
| `Dockerfile.spring-service` `COPY . .` then `-pl <module> -am` | VERIFIED. It actually runs `clean package -pl "${MODULE_PATH}" -am -DskipTests`, so a plain-jar library is built fine |
| `.github/workflows/ci.yml` runs `mvn -B -ntp verify` | VERIFIED. The CI job also requires Docker for Testcontainers, unrelated to this module |
| Old plan `261016-1030-supabase-media-storage` already updated | PARTIAL. True only as an uncommitted working-tree edit on `feat/common-events` (Finding 1). The "Biến cấu hình" table still shows the old rows (`SUPABASE_STORAGE_BUCKET`, `CONTENT_MEDIA_*`), so Phase 4 task 2 is a valid TODO |
| `varchar(500)` key columns (Phase 2 line 52) | VERIFIED: `assessment-service/.../V1__create_assessment_tables.sql:59,116` (`audio_reference`), `user-service/.../V2__create_identity_tables.sql:7` (`avatar_reference`) |
| `AutoConfigurations.of`, `ApplicationContextRunner`, `MockRestServiceServer.bindTo(RestClient.Builder)`, `JdkClientHttpRequestFactory.setReadTimeout` on Spring 6.2 | UNVERIFIED locally (no sources available). `MockRestServiceServer` and `ApplicationContextRunner` are already used in 9 repo test files, but only with `RestClient.Builder`/`RestTemplate` patterns |
| All Supabase REST behaviors (`x-upsert`, bulk DELETE, `apikey` vs `Bearer`, signed-upload TTL, 400-on-duplicate, UA blocking) | UNVERIFIED (external API) |
| No existing `ObjectStorage` / `StorageException` / `StoredObject` / `SignedUpload` symbols or `app.storage` / `SUPABASE` config in `services`, `infra`, `shared`, `docker-compose.yml` | VERIFIED by grep: zero hits, so no name collision. `CONTENT_MEDIA_BASE_URL` exists at `infra/config-server/config-repo/content-service.yaml:16` |

## Contract Verifier: consumers of the new/changed contracts

- `pom.xml` `<modules>` edit: 1 consumer (the root reactor).
- New public interface `ObjectStorage` / `app.storage.*` properties: **0 consumers today**. The plan names three future services (user-service, assessment-service, content-service). Each already has its own `GlobalExceptionHandler` and its own RestClient pattern.
- Property-key contract: `app.storage.enabled` and `app.storage.supabase.*` will need to be mapped in 3 config-repo files (`infra/config-server/config-repo/{user,assessment,content}-service.yaml`) plus `.env`. The plan correctly does not edit them now.
- Docs: AGENTS.md (5 line locations), CLAUDE.md (3), README.md (2), old plan `plan.md`.

## Unresolved Questions
1. Which base branch is intended: `main`, or `feat/common-events` after its merge? Is the old-plan edit meant to be committed before this plan starts?
2. Does the project need to read or delete legacy uppercase keys through this module?
3. Is the Supabase duplicate-write status really 400 (versus 409)? Does the bulk-delete form behave as documented?
