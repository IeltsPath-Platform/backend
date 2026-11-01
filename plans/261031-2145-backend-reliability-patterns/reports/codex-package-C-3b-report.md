Phase: C (3b)
Status: NEEDS_CONTEXT
Commits: none
Files: no source files changed; four uncommitted test drafts were removed after the scope mismatch was identified.
Red tests (commit test): none; the package stopped during source/spec alignment, before a test commit.
Verification: `mvn -B -ntp -pl services/{assessment,game,community,content}-service -am verify` → not run because the package hit the stop condition before implementation.
Deviations from skeleton: none.
Concerns: `LibraryVocabularySenseClient.describeActiveSenses` catches `HttpClientErrorException.NotFound` through its general `RestClientException` catch and maps it to `VocabularyServiceUnavailableException` (HTTP 503). The C3b test contract requires `notFoundStaysNotFoundAndKeepsBreakerClosed`, but the service has no existing not-found exception for this client and the permitted file/exception list does not authorize adding one or changing this public error mapping. The existing behavior, required mapping, and allowed scope therefore conflict. C3b needs a decision on whether downstream 404 should remain the current 503 or gain a 404 mapping; packages D and E are skipped because they depend on C.
