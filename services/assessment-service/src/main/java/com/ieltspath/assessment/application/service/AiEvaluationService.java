package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.domain.service.AgreementMetrics;
import com.ieltspath.assessment.infrastructure.config.AssessmentLlmProperties;
import com.ieltspath.assessment.infrastructure.client.RubricPromptBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final ZoneId quotaZone;

    public AiEvaluationService(JdbcTemplate jdbc, EssayGradingPort grader, ActiveRubricProvider rubrics,
            AssessmentLlmProperties llm, ObjectMapper json,
            @Value("${assessment.ai-eval.daily-limit:100}") int dailyLimit,
            @Value("${assessment.ai-eval.min-within-half-band:0.80}") BigDecimal minWithin,
            @Value("${assessment.ai-eval.min-qwk:0.60}") BigDecimal minQwk,
            @Value("${assessment.ai-eval.quota-timezone:Asia/Ho_Chi_Minh}") String quotaTimezone) {
        this.jdbc = jdbc; this.grader = grader; this.rubrics = rubrics; this.llm = llm; this.json = json;
        this.dailyLimit = dailyLimit; this.minWithin = minWithin; this.minQwk = minQwk;
        this.quotaZone = ZoneId.of(quotaTimezone);
    }

    public UUID addManual(String task, String prompt, String chartFacts, String essay, BigDecimal examinerBand,
                          Object criteria, List<String> errorCodes, UUID actor) {
        validateBand(examinerBand);
        String criteriaJson = serialize(criteria == null ? List.of() : criteria);
        UUID id = UUID.randomUUID();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ai_gold_essays(id,task,prompt,chart_facts,essay,examiner_band,examiner_criteria,
                        examiner_error_codes,source,created_by)
                    VALUES(?,?,?,?,?,?,CAST(? AS JSONB),?,'MANUAL',?)
                    """);
            statement.setObject(1, id); statement.setString(2, task); statement.setString(3, prompt);
            statement.setString(4, chartFacts); statement.setString(5, essay); statement.setBigDecimal(6, examinerBand);
            statement.setString(7, criteriaJson);
            statement.setArray(8, connection.createArrayOf("varchar", errorCodes == null ? new String[0] : errorCodes.toArray(String[]::new)));
            statement.setObject(9, actor);
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
                       examiner_criteria::text AS criteria
                FROM ai_gold_essays WHERE task=? ORDER BY created_at,id
                """, (rs, row) -> gold(rs), finalTask);
        if (gold.isEmpty()) throw new IllegalStateException("GOLD_SET_EMPTY");
        LocalDate usageDate = LocalDate.now(Clock.systemUTC().withZone(quotaZone));
        jdbc.update("INSERT INTO ai_eval_daily_usage(usage_date,units) VALUES (?,0) ON CONFLICT DO NOTHING", usageDate);
        int charged = jdbc.update("UPDATE ai_eval_daily_usage SET units=units+? WHERE usage_date=? AND units+?<=?",
                gold.size(), usageDate, gold.size(), dailyLimit);
        if (charged != 1) throw new IllegalStateException("AI_EVAL_DAILY_LIMIT");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO ai_eval_runs(id,task,model,prompt_version,status,n,started_at) VALUES(?,?,?,?,'RUNNING',?,CURRENT_TIMESTAMP)",
                id, task, llm.getModel(), expectedPromptVersion, gold.size());
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
                       error_code_false_positive,passed,started_at,finished_at,failure_code
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
            result.put("thresholds", Map.of("withinHalfBand", minWithin, "qwk", minQwk));
            result.put("items", jdbc.query("SELECT gold_id,llm_band,llm_criteria::text,llm_error_codes,diff::text FROM ai_eval_items WHERE run_id=? ORDER BY gold_id",
                    (items, ix) -> Map.of("goldId", items.getObject(1, UUID.class), "llmBand", items.getBigDecimal(2),
                            "llmCriteria", parseJson(Objects.toString(items.getString(3), "[]")), "llmErrorCodes", Arrays.asList((String[]) items.getArray(4).getArray()),
                            "diff", parseJson(Objects.toString(items.getString(5), "{}"))), id));
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
            int codeDenominator = 0, falsePositive = 0;
            String promptVersion = null;
            for (GoldEssay item : gold) {
                EssayGradingPort.EssayGrade grade = grader.grade(new EssayGradingPort.Prompt(item.prompt(), task, null,
                        item.chartFacts(), expectedPromptVersion), item.essay());
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
                String diff = json.writeValueAsString(Map.of("examinerBand", item.examinerBand(),
                        "absoluteBandDifference", item.examinerBand().subtract(grade.band()).abs()));
                jdbc.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO ai_eval_items(run_id,gold_id,llm_band,llm_criteria,llm_error_codes,diff)
                            VALUES(?,?,?,CAST(? AS JSONB),?,CAST(? AS JSONB)) ON CONFLICT DO NOTHING
                            """);
                    statement.setObject(1, runId); statement.setObject(2, item.id()); statement.setBigDecimal(3, grade.band());
                    statement.setString(4, llmCriteria.toString());
                    statement.setArray(5, connection.createArrayOf("varchar", grade.errorCodes().toArray(String[]::new)));
                    statement.setString(6, diff);
                    return statement;
                });
            }
            AgreementMetrics.Result metrics = AgreementMetrics.calculate(human, ai);
            var perCriterion = json.createObjectNode();
            for (String code : humanByCriterion.keySet()) {
                var criterion = AgreementMetrics.calculate(humanByCriterion.get(code), aiByCriterion.get(code));
                perCriterion.putObject(code).put("n", criterion.n()).put("withinHalfBand", criterion.withinHalfBand())
                        .put("mae", criterion.mae()).put("qwk", criterion.qwk());
            }
            BigDecimal fpRate = codeDenominator == 0 ? null : BigDecimal.valueOf(falsePositive)
                    .divide(BigDecimal.valueOf(codeDenominator), 5, java.math.RoundingMode.HALF_UP);
            boolean passed = metrics.withinHalfBand().compareTo(minWithin) >= 0 && metrics.qwk().compareTo(minQwk) >= 0;
            jdbc.update("""
                    UPDATE ai_eval_runs SET prompt_version=?,status='DONE',within_half_band=?,mae=?,qwk=?,
                        error_code_false_positive=?,passed=?,per_criterion=CAST(? AS JSONB),finished_at=CURRENT_TIMESTAMP WHERE id=?
                    """, promptVersion, metrics.withinHalfBand(), metrics.mae(), metrics.qwk(), fpRate, passed,
                    perCriterion.toString(), runId);
        } catch (Exception exception) {
            jdbc.update("UPDATE ai_eval_runs SET status='FAILED',failure_code=?,finished_at=CURRENT_TIMESTAMP WHERE id=?",
                    safeCode(exception), runId);
        }
    }

    private GoldEssay gold(ResultSet rs) throws java.sql.SQLException {
        Array codes = rs.getArray("examiner_error_codes");
        return new GoldEssay(rs.getObject("id", UUID.class), rs.getString("task"), rs.getString("prompt"),
                rs.getString("chart_facts"), rs.getString("essay"), rs.getBigDecimal("examiner_band"),
                codes == null ? new String[0] : (String[]) codes.getArray(), rs.getString("criteria"));
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
                             String[] errorCodes, String criteria) {}
}
