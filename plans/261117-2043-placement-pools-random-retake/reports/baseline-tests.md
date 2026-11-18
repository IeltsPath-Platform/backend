# Baseline verification — 2026-11-17

Status: DONE_WITH_CONCERNS

Detached clean baseline `7866f1768fba41183daddc2402de60a015969780` in `C:\Users\pduy8\OneDrive\Desktop\ielts-path\baseline-placement-check`. Primary sources were not changed. Maven 3.9.9, Oracle JDK 21.0.6, Windows 11. Docker server 29.6.1/API 1.55; Testcontainers 1.21.4 connected through the local named pipe and started Ryuk plus PostgreSQL 15.19.

Read backend AGENTS.md, CLAUDE.md, README.md, learning-service README, constitution, approved plan and phase 5. Initial workspace-root reads failed because docs live under backend; corrected before testing.

## Commands

Every shell invocation used `login: false`, `sandbox_permissions: require_escalated`, and an absolute `Set-Location -LiteralPath ... -ErrorAction Stop`.

```powershell
Set-Location -LiteralPath 'C:\Users\pduy8\OneDrive\Desktop\ielts-path\backend' -ErrorAction Stop
git worktree add --detach 'C:\Users\pduy8\OneDrive\Desktop\ielts-path\baseline-placement-check' 7866f176

Set-Location -LiteralPath 'C:\Users\pduy8\OneDrive\Desktop\ielts-path\baseline-placement-check' -ErrorAction Stop
mvn -q -pl services/learning-service -am '-Dtest=EssayKnowledgeJudgeTest,LearningServiceApplicationTests' '-Dsurefire.failIfNoSpecifiedTests=false' test *> baseline-focused.log
Get-Content baseline-focused.log -Tail 85
mvn -version
docker version --format '{{.Server.Version}}'
git rev-parse HEAD
git diff --exit-code -- services/learning-service/src/test/resources/archunit_store
git diff --exit-code
git status --porcelain
$baselineTemplateBytes = [System.IO.File]::ReadAllText((Join-Path (Get-Location) 'services/learning-service/src/main/resources/essay-kp-judgment-v3.txt'))
'Placeholder followed by CRLF: ' + $baselineTemplateBytes.Contains(('{{knowledge_points}}' + "`r`n"))
```

## Results

Focused Learning suite: **17 tests, 2 failures, 0 errors, 0 skipped**. Maven reported test failure; the command wrapper subsequently printed the log, making its shell exit 0. No full-suite success is claimed. Controller confirmed focused baseline sufficient.

- `EssayKnowledgeJudgeTest`: 16 tests, 1 failure, 0 skipped. `aQuestionWithTwoPointsPutsExactlyTwoLinesInThePrompt`, line 70: `a blank description leaves out the colon ==> expected: <true> but was: <false>`. Prompt joins intermediate point lines with LF, but its final point is inserted before the template's CRLF in this Windows checkout. The direct template check returned `Placeholder followed by CRLF: True`; assertion requires LF immediately after the final point. The source correctly omits the colon for null description.
- `LearningServiceApplicationTests`: 1 test, 1 failure, 0 skipped. `contextStartsWithMigratedSchemaAndPublicHealth`, line 60: `expected: <53> but was: <54>`. Flyway successfully applied 54 migrations including `V54__submission_history_indexes.sql` and the context started. The hardcoded expectation is stale. Later table-count and health assertions did not execute after this failure.

No unexpected test failures appeared. Background RabbitMQ listener connection/redeclaration warnings occurred against localhost:5672 without separate test errors. Mockito/JDK dynamic-agent warnings were diagnostic only.

**Testcontainers were not skipped.** The startup integration test ran against disposable PostgreSQL. `disabledWithoutDocker=true` can skip tests elsewhere; local Docker availability alone was not used as proof of execution.

`git diff --exit-code` and `git status --porcelain` remained empty. `archunit_store` was unchanged. No real LLM, secrets, shared application DB, source edit, or commit was used. Logs and Surefire reports remain in the isolated baseline worktree: `baseline-focused.log` and `services/learning-service/target/surefire-reports/`.
