# Access debit idempotency review

Date: 2026-11-07 (Asia/Bangkok). Branch: `feat/mobile-vocabulary-study`.

## Decision and scope

Approve a narrow Access dependency fix required by the vocabulary purchase flow. Phase three assumes Access safely replays concurrent requests with the same debit key. The existing implementation could instead return 402 after another request had already paid, invalidating Library's prescribed cleanup of an unpaid `PENDING` intent.

The fix extends the existing recovery in `DebitPointsUseCase` to catch `InsufficientPointsException` after `PointDebitWriter` rolls back. It rereads the committed idempotency key, replays a matching ledger entry through the existing user/payload checks, and rethrows the original failure when no entry exists. Different payloads still return an idempotency conflict. There is no API, schema, HTTP, retry-policy, or wallet-locking design change; a genuine 402 does not trigger another business debit request.

Files reviewed:

- `services/access-service/src/main/java/com/ieltspath/access/application/usecase/DebitPointsUseCase.java`
- `services/access-service/src/test/java/com/ieltspath/access/application/usecase/DebitPointsUseCaseTest.java`
- `services/access-service/src/test/java/com/ieltspath/access/infrastructure/persistence/PointDebitConcurrencyTest.java`

## Root cause and purchase impact

1. Duplicate request B reads an absent idempotency key before request A commits.
2. A commits its debit, wallet update, ledger entry, and outbox event.
3. B enters the writer afterward and reads the depleted wallet. It throws insufficient points before attempting the duplicate ledger insert.
4. Existing recovery handled integrity and optimistic-lock failures, but not insufficient points. Access therefore returned 402 despite the committed matching debit.
5. Library could delete its `PENDING` purchase on that 402 while A was between payment and effect application, losing the durable paid intent.

The wallet repository uses ordinary `findById` and an optimistic `@Version`; this fix does not add a pessimistic lock. Recovery runs after the writer transaction, allowing the newly committed ledger to be read. Library intent restoration alone was rejected because a successful caller could crash before restoring it; an additional Library debit call on 402 would change the planned business-error retry behavior.

## Verification evidence

- **Behavioral RED:** 11 tests, 1 failure, 2 errors, 0 skipped. The PostgreSQL concurrency test failed with the missing insufficient-points recovery, as did the new winner-replay and changed-payload unit cases. Local log: `C:/Users/Admin/AppData/Local/Temp/mobile-vocab-access-concurrency-red.log`.
- `PointDebitConcurrencyTest` uses real PostgreSQL, no ambient test transaction, and a repository spy that captures B's initial empty lookup before waiting for A's committed debit. B then receives that captured miss and executes the real writer against the depleted wallet.
- **First full Access run after the fix:** 112 tests, 1 failure, 0 errors, 0 skipped. The sole failure was the new concurrency test's full-record equality assertion: the original in-memory timestamp was `.122242200Z`, while PostgreSQL stored and replayed `.122242Z`. All stable business fields matched; this exposed an incorrect database-precision assumption in the test, not a debit or recovery failure.
- Corrected only the new test: it explicitly compares all eight stable ledger fields (`id`, `userId`, `delta`, `balanceAfter`, `transactionType`, `referenceType`, `referenceId`, `description`) and compares replay `createdAt` with the timestamp actually stored in PostgreSQL. Wallet balance, total debited, one ledger entry, and one `PointDebited` outbox event assertions remain. This preserves the business conditions; production timestamp behavior is unchanged.
- Unit cases cover matching-winner replay, genuine insufficient points preserving the original exception, and changed-payload conflict preservation.
- Source review found no remaining blocker in this scoped fix. `git diff --check` passed.
- **Focused clean verification passed:** 11 tests, 0 failures, 0 errors, 0 skipped; exit 0, 1 minute 02 seconds. Local log: `C:/Users/Admin/AppData/Local/Temp/mobile-vocab-access-concurrency-green.log`.
- **Final full Access clean verification passed:** 112 tests, 0 failures, 0 errors, 0 skipped; exit 0, 5 minutes 55 seconds, completed at 2026-11-07 11:03:44 +07:00. Local log: `C:/Users/Admin/AppData/Local/Temp/mobile-vocab-access-clean.log`.

Commands used (PowerShell, with output redirected only to the local logs above):

```powershell
$env:JAVA_HOME='D:\jdk-21'
mvn.cmd -pl services/access-service -o clean test '-Dtest=DebitPointsUseCaseTest,PointDebitConcurrencyTest'
mvn.cmd -pl services/access-service -o clean test
```

## Separate accepted limitation

Two heart refills with **different request IDs** may both pass validation and debit before either applies its effect. Phase three explicitly accepts that window; this fix addresses concurrent retries of the **same key** and does not change that decision. HTTP remains outside Library transactions and database locks.

Reference: [phase-three purchase and Access contracts](../phase-03-gamification-and-points.md).
