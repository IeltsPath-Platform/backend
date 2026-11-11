package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.exception.EssayGradingException;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.domain.service.AgreementMetrics;
import com.ieltspath.assessment.domain.service.ExaminerErrorRecall;
import com.ieltspath.assessment.infrastructure.config.AssessmentLlmProperties;
import com.ieltspath.assessment.infrastructure.client.RubricPromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.security.core.context.SecurityContextHolder;

/** Stores gold essays and runs bounded AI-versus-examiner evaluations independently of learner quota. */
@Service
public class AiEvaluationService {
    private final JdbcTemplate jdbc;
    private final EssayGradingPort grader;
    private final ActiveRubricProvider rubrics;
    private final AssessmentLlmProperties llm;
    private final ObjectMapper json;
    private final int dailyLimit;
    private final BigDecimal minWithin;
    private final BigDecimal minQwk;
    private final BigDecimal maxInvalidRate;
    private final int gradingTimeoutSeconds;
    private final ZoneId quotaZone;

    public AiEvaluationService(JdbcTemplate jdbc, EssayGradingPort grader, ActiveRubricProvider rubrics,
            AssessmentLlmProperties llm, ObjectMapper json,
            @Value("${assessment.ai-eval.daily-limit:100}") int dailyLimit,
            @Value("${assessment.ai-eval.min-within-half-band:0.80}") BigDecimal minWithin,
            @Value("${assessment.ai-eval.min-qwk:0.60}") BigDecimal minQwk,
            @Value("${assessment.ai-eval.quota-timezone:Asia/Ho_Chi_Minh}") String quotaTimezone,
            @Value("${assessment.ai-eval.max-invalid-rate:0.05}") BigDecimal maxInvalidRate,
            @Value("${assessment.ai-eval.grading-timeout-seconds:20}") int gradingTimeoutSeconds) {
        this.gradingTimeoutSeconds = gradingTimeoutSeconds;
        this.jdbc = jdbc; this.grader = grader; this.rubrics = rubrics; this.llm = llm; this.json = json;
        this.dailyLimit = dailyLimit; this.minWithin = minWithin; this.minQwk = minQwk;
        this.maxInvalidRate = maxInvalidRate;
        this.quotaZone = ZoneId.of(quotaTimezone);
    }

    /** An error the examiner marked on a gold essay; {@code kind} is LANGUAGE or DATA (DATA only for Task 1). */
    public record ExaminerError(String excerpt, String errorCode, String kind) {}

    public UUID addManual(String task, String prompt, String chartFacts, String essay, BigDecimal examinerBand,
                          Object criteria, List<String> errorCodes, UUID actor) {
        return addManual(task, prompt, chartFacts, essay, examinerBand, criteria, errorCodes, List.of(), actor);
    }

    public UUID addManual(String task, String prompt, String chartFacts, String essay, BigDecimal examinerBand,
                          Object criteria, List<String> errorCodes, List<ExaminerError> examinerErrors, UUID actor) {
        validateBand(examinerBand);
        validateExaminerErrors(task, essay, examinerErrors);
        String criteriaJson = serialize(criteria == null ? List.of() : criteria);
        String examinerErrorsJson = serialize(examinerErrors == null ? List.of() : examinerErrors);
        UUID id = UUID.randomUUID();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ai_gold_essays(id,task,prompt,chart_facts,essay,examiner_band,examiner_criteria,
                        examiner_error_codes,examiner_errors,source,created_by)
                    VALUES(?,?,?,?,?,?,CAST(? AS JSONB),?,CAST(? AS JSONB),'MANUAL',?)
                    """);
            statement.setObject(1, id); statement.setString(2, task); statement.setString(3, prompt);
            statement.setString(4, chartFacts); statement.setString(5, essay); statement.setBigDecimal(6, examinerBand);
            statement.setString(7, criteriaJson);
            statement.setArray(8, connection.createArrayOf("varchar", errorCodes == null ? new String[0] : errorCodes.toArray(String[]::new)));
            statement.setString(9, examinerErrorsJson);
            statement.setObject(10, actor);
            return statement;
        });
        return id;
    }

    public int importComparisons(UUID actor) {
        return jdbc.update("""
                INSERT INTO ai_gold_essays(task,prompt,chart_facts,essay,examiner_band,examiner_criteria,
                    examiner_error_codes,source,comparison_submission_id,created_by)
                SELECT c.task,COALESCE(l.prompt_snapshot->>'stem',''),
                    i.answer_snapshot->'answerSpec'->>'chartFacts',COALESCE(l.text_payload,''),c.examiner_band,
                    COALESCE(c.examiner_criteria,'[]'::jsonb),COALESCE(c.examiner_error_codes,'{}'::varchar[]),
                    'COMPARISON',c.submission_id,?
                FROM essay_grade_comparisons c JOIN learner_submissions l ON l.id=c.submission_id
                JOIN attempt_items i ON i.id=l.attempt_item_id
                WHERE c.examiner_band IS NOT NULL AND c.task IN ('TASK_1','TASK_2')
                ON CONFLICT(comparison_submission_id) DO NOTHING
                """, actor);
    }

    @Transactional
    public UUID start(String requestedTask) {
        if (!grader.available()) throw new IllegalStateException("LLM_UNAVAILABLE");
        String task = requestedTask;
        if (task == null || task.isBlank()) task = jdbc.queryForObject(
                "SELECT task FROM ai_gold_essays GROUP BY task ORDER BY count(*) DESC, task LIMIT 1", String.class);
        if (task == null) throw new IllegalStateException("GOLD_SET_EMPTY");
        String finalTask = task;
        ActiveRubricProvider.Rubric activeRubric = rubrics.getActive(finalTask);
        String expectedPromptVersion = RubricPromptBuilder.promptVersion(activeRubric);
        List<GoldEssay> gold = jdbc.query("""
                SELECT id,task,prompt,chart_facts,essay,examiner_band,examiner_error_codes,
                       examiner_criteria::text AS criteria,examiner_errors::text AS examiner_errors
                FROM ai_gold_essays WHERE task=? ORDER BY created_at,id
                """, (rs, row) -> gold(rs), finalTask);
        if (gold.isEmpty()) throw new IllegalStateException("GOLD_SET_EMPTY");
        LocalDate usageDate = LocalDate.now(Clock.systemUTC().withZone(quotaZone));
        jdbc.update("INSERT INTO ai_eval_daily_usage(usage_date,units) VALUES (?,0) ON CONFLICT DO NOTHING", usageDate);
        int charged = jdbc.update("UPDATE ai_eval_daily_usage SET units=units+? WHERE usage_date=? AND units+?<=?",
                gold.size(), usageDate, gold.size(), dailyLimit);
        if (charged != 1) throw new IllegalStateException("AI_EVAL_DAILY_LIMIT");
        UUID id = UUID.randomUUID();
        String reasoningEffort = llm.getReasoningEffort() == null || llm.getReasoningEffort().isBlank() ? null : llm.getReasoningEffort();
        jdbc.update("""
                INSERT INTO ai_eval_runs(id,task,model,prompt_version,status,n,started_at,reasoning_effort)
                VALUES(?,?,?,?,'RUNNING',?,CURRENT_TIMESTAMP,?)
                """, id, task, llm.getModel(), expectedPromptVersion, gold.size(), reasoningEffort);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            // This work can outlive the caller's gateway JWT. Downstream Content calls
                            // must use the short-lived assessment service identity instead.
                            SecurityContextHolder.clearContext();
                            try { evaluate(id, finalTask, gold, expectedPromptVersion); }
                            finally { SecurityContextHolder.clearContext(); }
                        });
                    }
        });
        return id;
    }

    public Map<String,Object> getRun(UUID id) {
        return jdbc.queryForObject("""
                SELECT id,task,model,prompt_version,status,n,within_half_band,mae,qwk,per_criterion::text,
                       error_code_false_positive,passed,started_at,finished_at,failure_code,
                       error_recall,data_error_recall,corrections_per_essay,p95_latency_ms,invalid_grade_rate,
                       reasoning_effort
                FROM ai_eval_runs WHERE id=?
                """, (rs, row) -> {
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("id", rs.getObject("id", UUID.class)); result.put("task", rs.getString("task"));
            result.put("model", rs.getString("model")); result.put("promptVersion", rs.getString("prompt_version"));
            result.put("status", rs.getString("status")); result.put("n", rs.getInt("n"));
            result.put("withinHalfBand", rs.getBigDecimal("within_half_band")); result.put("mae", rs.getBigDecimal("mae"));
            result.put("qwk", rs.getBigDecimal("qwk")); result.put("perCriterion", parseJson(rs.getString("per_criterion")));
            result.put("errorCodeFalsePositive", rs.getBigDecimal("error_code_false_positive"));
            result.put("passed", rs.getObject("passed")); result.put("startedAt", rs.getTimestamp("started_at"));
            result.put("finishedAt", rs.getTimestamp("finished_at")); result.put("failureCode", rs.getString("failure_code"));
            result.put("errorRecall", rs.getBigDecimal("error_recall"));
            result.put("dataErrorRecall", rs.getBigDecimal("data_error_recall"));
            result.put("correctionsPerEssay", rs.getBigDecimal("corrections_per_essay"));
            result.put("p95LatencyMs", rs.getObject("p95_latency_ms"));
            result.put("invalidGradeRate", rs.getBigDecimal("invalid_grade_rate"));
            result.put("reasoningEffort", rs.getString("reasoning_effort"));
            result.put("thresholds", Map.of("withinHalfBand", minWithin, "qwk", minQwk, "maxInvalidRate", maxInvalidRate));
            result.put("items", jdbc.query("""
                            SELECT gold_id,llm_band,llm_criteria::text,llm_error_codes,diff::text,latency_ms,failure_code
                            FROM ai_eval_items WHERE run_id=? ORDER BY gold_id
                            """,
                    (items, ix) -> {
                        Map<String,Object> item = new LinkedHashMap<>();
                        item.put("goldId", items.getObject(1, UUID.class));
                        item.put("llmBand", items.getBigDecimal(2));
                        item.put("llmCriteria", parseJson(Objects.toString(items.getString(3), "[]")));
                        Array codes = items.getArray(4);
                        item.put("llmErrorCodes", codes == null ? List.of() : Arrays.asList((String[]) codes.getArray()));
                        item.put("diff", parseJson(Objects.toString(items.getString(5), "{}")));
                        item.put("latencyMs", items.getObject(6));
                        item.put("failureCode", items.getString(7));
                        return item;
                    }, id));
            return result;
        }, id);
    }

    public void activate(UUID runId, UUID actor) {
        int updated = jdbc.update("""
                INSERT INTO ai_prompt_activations(task,prompt_version,run_id,activated_by)
                SELECT task,prompt_version,id,? FROM ai_eval_runs WHERE id=? AND status='DONE' AND passed IS TRUE
                ON CONFLICT(task) DO UPDATE SET prompt_version=EXCLUDED.prompt_version,run_id=EXCLUDED.run_id,
                    activated_by=EXCLUDED.activated_by,activated_at=CURRENT_TIMESTAMP
                """, actor, runId);
        if (updated == 0) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "EVALUATION_NOT_PASSED");
    }

    void evaluate(UUID runId, String task, List<GoldEssay> gold, String expectedPromptVersion) {
        try {
            List<BigDecimal> human = new ArrayList<>(), ai = new ArrayList<>();
            Map<String,List<BigDecimal>> humanByCriterion = new LinkedHashMap<>();
            Map<String,List<BigDecimal>> aiByCriterion = new LinkedHashMap<>();
            int codeDenominator = 0, falsePositive = 0, invalid = 0, correctionTotal = 0;
            ExaminerErrorRecall.Counts recall = ExaminerErrorRecall.Counts.NONE;
            List<Integer> latencies = new ArrayList<>();
            String promptVersion = null;
            for (GoldEssay item : gold) {
                long started = System.nanoTime();
                try {
                    EssayGradingPort.EssayGrade grade = gradeWithTimeout(new EssayGradingPort.Prompt(item.prompt(), task, null,
                            item.chartFacts(), expectedPromptVersion), item.essay());
                    int latency = latencyMs(started);
                    latencies.add(latency);
                    promptVersion = grade.promptVersion();
                    human.add(item.examinerBand()); ai.add(grade.band());
                    Set<String> goldCodes = Set.of(item.errorCodes());
                    if (!goldCodes.isEmpty()) {
                        codeDenominator += grade.errorCodes().size();
                        for (String code : grade.errorCodes()) if (!goldCodes.contains(code)) falsePositive++;
                    }
                    JsonNode llmCriteria = grade.feedback() == null ? json.createArrayNode() : json.readTree(grade.feedback()).path("criteria");
                    Map<String,BigDecimal> humanCriteria = criterionBands(json.readTree(item.criteria()));
                    Map<String,BigDecimal> aiCriteria = criterionBands(llmCriteria);
                    for (String code : humanCriteria.keySet()) if (aiCriteria.containsKey(code)) {
                        humanByCriterion.computeIfAbsent(code, ignored -> new ArrayList<>()).add(humanCriteria.get(code));
                        aiByCriterion.computeIfAbsent(code, ignored -> new ArrayList<>()).add(aiCriteria.get(code));
                    }
                    List<String> corrections = correctionExcerpts(grade.feedback());
                    correctionTotal += corrections.size();
                    recall = recall.plus(ExaminerErrorRecall.score(item.essay(), item.examinerErrors(), corrections));
                    String diff = json.writeValueAsString(Map.of("examinerBand", item.examinerBand(),
                            "absoluteBandDifference", item.examinerBand().subtract(grade.band()).abs()));
                    insertItem(runId, item.id(), grade.band(), llmCriteria.toString(), grade.errorCodes(),
                            json.writeValueAsString(corrections), latency, null, diff);
                } catch (EssayGradingException exception) {
                    // One essay the model could not grade is counted, not a failed run.
                    invalid++;
                    int latency = latencyMs(started);
                    latencies.add(latency);
                    insertItem(runId, item.id(), null, null, List.of(), null, latency, failureCode(exception), "{}");
                }
            }
            AgreementMetrics.Result metrics = human.isEmpty() ? null : AgreementMetrics.calculate(human, ai);
            var perCriterion = json.createObjectNode();
            for (String code : humanByCriterion.keySet()) {
                var criterion = AgreementMetrics.calculate(humanByCriterion.get(code), aiByCriterion.get(code));
                perCriterion.putObject(code).put("n", criterion.n()).put("withinHalfBand", criterion.withinHalfBand())
                        .put("mae", criterion.mae()).put("qwk", criterion.qwk());
            }
            BigDecimal fpRate = codeDenominator == 0 ? null : BigDecimal.valueOf(falsePositive)
                    .divide(BigDecimal.valueOf(codeDenominator), 5, RoundingMode.HALF_UP);
            BigDecimal invalidRate = BigDecimal.valueOf(invalid)
                    .divide(BigDecimal.valueOf(gold.size()), 5, RoundingMode.HALF_UP);
            BigDecimal correctionsPerEssay = human.isEmpty() ? null : BigDecimal.valueOf(correctionTotal)
                    .divide(BigDecimal.valueOf(human.size()), 2, RoundingMode.HALF_UP);
            boolean passed = metrics != null
                    && metrics.withinHalfBand().compareTo(minWithin) >= 0 && metrics.qwk().compareTo(minQwk) >= 0
                    && invalidRate.compareTo(maxInvalidRate) <= 0;
            jdbc.update("""
                    UPDATE ai_eval_runs SET prompt_version=COALESCE(?,prompt_version),status='DONE',within_half_band=?,mae=?,qwk=?,
                        error_code_false_positive=?,passed=?,per_criterion=CAST(? AS JSONB),error_recall=?,data_error_recall=?,
                        corrections_per_essay=?,p95_latency_ms=?,invalid_grade_rate=?,finished_at=CURRENT_TIMESTAMP WHERE id=?
                    """, promptVersion, metrics == null ? null : metrics.withinHalfBand(),
                    metrics == null ? null : metrics.mae(), metrics == null ? null : metrics.qwk(), fpRate, passed,
                    perCriterion.toString(), recall.languageRecall(), recall.dataRecall(), correctionsPerEssay,
                    percentile95(latencies), invalidRate, runId);
        } catch (Exception exception) {
            jdbc.update("UPDATE ai_eval_runs SET status='FAILED',failure_code=?,finished_at=CURRENT_TIMESTAMP WHERE id=?",
                    safeCode(exception), runId);
        }
    }

    /**
     * One essay, cut at the grading timeout so a slow reply counts as that essay's failure. Runs on its own thread so
     * the wait can end; the caller's security context is not needed here.
     */
    private EssayGradingPort.EssayGrade gradeWithTimeout(EssayGradingPort.Prompt prompt, String essay) {
        CompletableFuture<EssayGradingPort.EssayGrade> reply = CompletableFuture.supplyAsync(() -> grader.grade(prompt, essay));
        try {
            return reply.get(gradingTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            reply.cancel(true);
            throw new EssayGradingException("LLM_TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            reply.cancel(true);
            throw new EssayGradingException("LLM_TIMEOUT");
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException cause) throw cause;
            throw new EssayGradingException("EVALUATION_FAILED");
        }
    }

    private void insertItem(UUID runId, UUID goldId, BigDecimal band, String criteriaJson, List<String> errorCodes,
                            String correctionsJson, int latencyMs, String failureCode, String diff) {
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ai_eval_items(run_id,gold_id,llm_band,llm_criteria,llm_error_codes,diff,
                        llm_corrections,latency_ms,failure_code)
                    VALUES(?,?,?,CAST(? AS JSONB),?,CAST(? AS JSONB),CAST(? AS JSONB),?,?) ON CONFLICT DO NOTHING
                    """);
            statement.setObject(1, runId); statement.setObject(2, goldId); statement.setBigDecimal(3, band);
            statement.setString(4, criteriaJson);
            statement.setArray(5, connection.createArrayOf("varchar", errorCodes.toArray(String[]::new)));
            statement.setString(6, diff);
            statement.setString(7, correctionsJson);
            statement.setInt(8, latencyMs);
            statement.setString(9, failureCode);
            return statement;
        });
    }

    private static int latencyMs(long startedNanos) {
        return (int) Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }

    /** Nearest-rank 95th percentile; null when no essay was sent. */
    static Integer percentile95(List<Integer> values) {
        if (values.isEmpty()) return null;
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(0.95 * sorted.size());
        return sorted.get(rank - 1);
    }

    /** The excerpts of the corrections the model gave, from the grade's feedback JSON; empty when there is none. */
    private List<String> correctionExcerpts(String feedback) throws com.fasterxml.jackson.core.JsonProcessingException {
        List<String> excerpts = new ArrayList<>();
        if (feedback == null) return excerpts;
        for (JsonNode correction : json.readTree(feedback).path("corrections")) {
            String excerpt = correction.path("excerpt").asText(null);
            if (excerpt != null && !excerpt.isBlank()) excerpts.add(excerpt);
        }
        return excerpts;
    }

    private static String failureCode(EssayGradingException exception) {
        String code = exception.getCode();
        return code != null && code.matches("[A-Z0-9_]{1,40}") ? code : "EVALUATION_FAILED";
    }

    private void validateExaminerErrors(String task, String essay, List<ExaminerError> errors) {
        if (errors == null) return;
        for (ExaminerError error : errors) {
            if (error.excerpt() == null || error.excerpt().isBlank() || essay == null || !essay.contains(error.excerpt()))
                throw new IllegalArgumentException("Examiner error excerpt must be an exact part of the essay");
            if (error.errorCode() == null || error.errorCode().isBlank())
                throw new IllegalArgumentException("Examiner error needs an error code");
            if (!"LANGUAGE".equals(error.kind()) && !"DATA".equals(error.kind()))
                throw new IllegalArgumentException("Examiner error kind must be LANGUAGE or DATA");
            if ("DATA".equals(error.kind()) && !"TASK_1".equals(task))
                throw new IllegalArgumentException("DATA errors apply to Task 1 only");
        }
    }

    private GoldEssay gold(ResultSet rs) throws java.sql.SQLException {
        Array codes = rs.getArray("examiner_error_codes");
        return new GoldEssay(rs.getObject("id", UUID.class), rs.getString("task"), rs.getString("prompt"),
                rs.getString("chart_facts"), rs.getString("essay"), rs.getBigDecimal("examiner_band"),
                codes == null ? new String[0] : (String[]) codes.getArray(), rs.getString("criteria"),
                markedErrors(rs.getString("examiner_errors")));
    }

    private List<ExaminerErrorRecall.MarkedError> markedErrors(String raw) {
        List<ExaminerErrorRecall.MarkedError> marked = new ArrayList<>();
        if (raw == null) return marked;
        try {
            for (JsonNode node : json.readTree(raw)) {
                ExaminerErrorRecall.Kind kind = "DATA".equals(node.path("kind").asText())
                        ? ExaminerErrorRecall.Kind.DATA : ExaminerErrorRecall.Kind.LANGUAGE;
                marked.add(new ExaminerErrorRecall.MarkedError(node.path("excerpt").asText(), kind));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("Stored examiner errors are unreadable", exception);
        }
        return marked;
    }

    private Map<String,BigDecimal> criterionBands(JsonNode criteria) {
        Map<String,BigDecimal> result = new LinkedHashMap<>();
        if (criteria != null && criteria.isArray()) for (JsonNode criterion : criteria) {
            String code = criterion.path("code").asText(null);
            JsonNode band = criterion.path("band");
            if (code != null && band.isNumber()) result.put(code, band.decimalValue());
        }
        return result;
    }

    private String serialize(Object value) {
        try { return json.writeValueAsString(value); } catch (Exception e) { throw new IllegalArgumentException("Invalid criteria"); }
    }
    private Object parseJson(String value) {
        try { return json.readTree(value); } catch (Exception ignored) { return Map.of(); }
    }
    private static void validateBand(BigDecimal band) {
        if (band == null || band.compareTo(BigDecimal.ZERO) < 0 || band.compareTo(BigDecimal.valueOf(9)) > 0
                || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0)
            throw new IllegalArgumentException("Band must be between 0 and 9 in half-band steps");
    }
    private static String safeCode(Exception exception) {
        String value = exception.getMessage();
        return value != null && value.matches("[A-Z0-9_]{1,50}") ? value : "EVALUATION_FAILED";
    }
    record GoldEssay(UUID id, String task, String prompt, String chartFacts, String essay, BigDecimal examinerBand,
                             String[] errorCodes, String criteria, List<ExaminerErrorRecall.MarkedError> examinerErrors) {}
}
