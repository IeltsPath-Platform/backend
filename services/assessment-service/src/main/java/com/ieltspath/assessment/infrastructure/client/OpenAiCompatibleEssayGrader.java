package com.ieltspath.assessment.infrastructure.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.assessment.application.exception.EssayGradingException;
import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.application.port.EssayGradingPort;
import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.application.port.ErrorTypeCatalog;
import com.ieltspath.assessment.infrastructure.config.AssessmentLlmProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/** OpenAI-compatible chat completions adapter. Sensitive request and response bodies are never logged. */
@Component
public class OpenAiCompatibleEssayGrader implements EssayGradingPort {
    private static final int MAX_COMMENT = 1500;
    private static final int MAX_SUMMARY = 1000;
    private static final int MAX_FOCUS = 300;
    private static final int MAX_FOCUS_ITEMS = 6;
    // Descriptor wording is intentionally kept aligned with learning-service EssayGrader.
    private final AssessmentLlmProperties properties;
    private final ObjectMapper json;
    private final RestClient restClient;
    private final ActiveRubricProvider rubrics;
    private final ErrorTypeCatalog errorTypes;

    @Autowired
    public OpenAiCompatibleEssayGrader(RestClient.Builder builder, AssessmentLlmProperties properties, ObjectMapper json,
                                       ActiveRubricProvider rubrics, ErrorTypeCatalog errorTypes) {
        this(createClient(builder), properties, json, rubrics, errorTypes);
    }

    OpenAiCompatibleEssayGrader(RestClient restClient, AssessmentLlmProperties properties, ObjectMapper json,
                                ActiveRubricProvider rubrics, ErrorTypeCatalog errorTypes) {
        this.properties = properties;
        this.json = json;
        this.restClient = restClient;
        this.rubrics = rubrics;
        this.errorTypes = errorTypes;
    }

    private static RestClient createClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(45));
        return builder.requestFactory(factory).build();
    }

    @Override
    public boolean available() { return properties.configured(); }

    @Override
    public EssayGrade grade(Prompt prompt, String essay) {
        if (!available()) throw new EssayGradingException("LLM_UNAVAILABLE");
        if (prompt == null || essay == null || essay.isBlank()
                || !("TASK_1".equals(prompt.task()) || "TASK_2".equals(prompt.task()))
                || ("TASK_1".equals(prompt.task()) && (prompt.chartFacts() == null || prompt.chartFacts().isBlank()))) {
            throw new EssayGradingException("INVALID_PROMPT");
        }
        try {
            ActiveRubricProvider.Rubric rubric = resolveRubric(prompt);
            var activeErrorTypes = errorTypes.activeErrorTypes();
            Set<String> allowedErrorCodes = activeErrorTypes.stream().map(ErrorTypeCatalog.ErrorType::code)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            ObjectNode body = json.createObjectNode().put("model", properties.getModel());
            body.putObject("response_format").put("type", "json_object");
            body.putArray("messages")
                    .addObject().put("role", "system").put("content", RubricPromptBuilder.build(rubric, activeErrorTypes));
            body.withArray("messages").addObject().put("role", "user").put("content", userPrompt(prompt, essay));
            String response = restClient.post().uri(endpoint(properties.getBaseUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(body).retrieve().body(String.class);
            return parseGrade(response, rubric, allowedErrorCodes, essay);
        } catch (EssayGradingException exception) {
            throw exception;
        } catch (ContentUnavailableException exception) {
            throw new EssayGradingException("RUBRIC_UNAVAILABLE");
        } catch (RestClientException exception) {
            throw new EssayGradingException("LLM_REQUEST_FAILED");
        } catch (Exception exception) {
            throw new EssayGradingException("INVALID_GRADE");
        }
    }

    private ActiveRubricProvider.Rubric resolveRubric(Prompt prompt) {
        ActiveRubricProvider.Rubric rubric;
        if (prompt.expectedPromptVersion() == null) {
            rubric = rubrics.getActive(prompt.task());
        } else {
            String[] parts = prompt.expectedPromptVersion().split("@|\\+tpl", -1);
            if (parts.length != 3 || !RubricPromptBuilder.TEMPLATE_VERSION.equals(parts[2]))
                throw new EssayGradingException("PROMPT_NOT_ACTIVATED");
            rubric = rubrics.getVersion(parts[0], parts[1]);
        }
        if (rubric == null || !prompt.task().equals(rubric.task())) throw new EssayGradingException("RUBRIC_UNAVAILABLE");
        String version = RubricPromptBuilder.promptVersion(rubric);
        if (prompt.expectedPromptVersion() != null && !prompt.expectedPromptVersion().equals(version))
            throw new EssayGradingException("PROMPT_VERSION_UNAVAILABLE");
        return rubric;
    }

    /** A bare host gets the conventional {@code /v1}; a base URL that already has a path, such as {@code .../v1beta/openai}, is used as given. */
    private static String endpoint(String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = java.net.URI.create(base).getPath();
        boolean hostOnly = path == null || path.isEmpty();
        return hostOnly ? base + "/v1/chat/completions" : base + "/chat/completions";
    }

    private static String userPrompt(Prompt prompt, String essay) {
        StringBuilder text = new StringBuilder("Question:\n").append(prompt.stem() == null ? "" : prompt.stem())
                .append("\n\nMinimum words: ").append(prompt.minWords() == null ? "" : prompt.minWords());
        if ("TASK_1".equals(prompt.task())) {
            text.append("\n\n<chart_facts>\n").append(neutralize(prompt.chartFacts(), "chart_facts"))
                    .append("\n</chart_facts>");
        }
        return text.append("\n\n<essay>\n").append(neutralize(essay, "essay")).append("\n</essay>").toString();
    }

    /** Bands are validated strictly; comments are optional, trimmed and capped so a chatty reply cannot bloat a row. */
    private EssayGrade parseGrade(String response, ActiveRubricProvider.Rubric rubric, Set<String> allowedErrorCodes,
                                  String essay) {
        try {
            JsonNode outer = json.readTree(response);
            String content = outer.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) throw new EssayGradingException("INVALID_GRADE");
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) throw new EssayGradingException("INVALID_GRADE");
            JsonNode reply = json.readTree(content.substring(start, end + 1));
            JsonNode criteria = reply.path("criteria");
            Set<String> expected = rubric.criteria().stream().map(ActiveRubricProvider.Criterion::code).collect(java.util.stream.Collectors.toSet());
            if (!criteria.isArray() || criteria.size() != expected.size()) throw new EssayGradingException("INVALID_GRADE");
            Set<String> seen = new HashSet<>();
            BigDecimal sum = BigDecimal.ZERO;
            ArrayNode comments = json.createArrayNode();
            for (JsonNode criterion : criteria) {
                String code = criterion.path("code").asText(null);
                JsonNode rawBand = criterion.path("band");
                if (code == null || !expected.contains(code) || !seen.add(code) || !rawBand.isNumber()) {
                    throw new EssayGradingException("INVALID_GRADE");
                }
                BigDecimal band = rawBand.decimalValue();
                if (band.compareTo(BigDecimal.ZERO) < 0 || band.compareTo(BigDecimal.valueOf(9)) > 0
                        || band.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
                    throw new EssayGradingException("INVALID_GRADE");
                }
                sum = sum.add(band);
                ObjectNode criterionFeedback = comments.addObject().put("code", code).put("band", band);
                String comment = text(criterion.path("comment"), MAX_COMMENT);
                if (comment != null) criterionFeedback.put("comment", comment);
            }
            if (!seen.equals(expected)) throw new EssayGradingException("INVALID_GRADE");
            BigDecimal average = sum.divide(BigDecimal.valueOf(expected.size()), 6, RoundingMode.HALF_UP);
            BigDecimal overall = average.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
            ArrayNode corrections = json.createArrayNode();
            Set<String> usedCodes = new java.util.LinkedHashSet<>();
            JsonNode rawCorrections = reply.path("corrections");
            if (rawCorrections.isArray()) for (JsonNode correction : rawCorrections) {
                String errorCode = text(correction.path("errorCode"), 60);
                String excerpt = text(correction.path("excerpt"), 500);
                String suggestion = text(correction.path("suggestion"), MAX_COMMENT);
                if (errorCode == null || !allowedErrorCodes.contains(errorCode) || excerpt == null || !essay.contains(excerpt)
                        || suggestion == null) continue;
                ObjectNode item = corrections.addObject().put("excerpt", excerpt).put("suggestion", suggestion).put("errorCode", errorCode);
                usedCodes.add(errorCode);
            }
            String promptVersion = RubricPromptBuilder.promptVersion(rubric);
            return new EssayGrade(overall, feedback(reply, comments, corrections), promptVersion, List.copyOf(usedCodes));
        } catch (EssayGradingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EssayGradingException("INVALID_GRADE");
        }
    }

    /** Null when the reply carried no comment at all, so the report can say the essay has bands only. */
    private String feedback(JsonNode reply, ArrayNode criteria, ArrayNode corrections) throws JsonProcessingException {
        String summary = text(reply.path("summary"), MAX_SUMMARY);
        ArrayNode focus = json.createArrayNode();
        JsonNode rawFocus = reply.path("focus");
        if (rawFocus.isArray()) {
            for (JsonNode item : rawFocus) {
                String value = text(item, MAX_FOCUS);
                if (value != null && focus.size() < MAX_FOCUS_ITEMS) focus.add(value);
            }
        }
        boolean commented = summary != null || !focus.isEmpty() || !corrections.isEmpty()
                || criteria.findValues("comment").stream().anyMatch(value -> !value.isNull());
        if (!commented) return null;
        ObjectNode feedback = json.createObjectNode().put("summary", summary);
        feedback.set("criteria", criteria);
        feedback.set("focus", focus);
        feedback.set("corrections", corrections);
        return json.writeValueAsString(feedback);
    }

    private static String text(JsonNode value, int max) {
        if (!value.isTextual()) return null;
        String text = value.textValue().strip();
        if (text.isEmpty()) return null;
        return text.length() > max ? text.substring(0, max) + "…" : text;
    }

    private static String neutralize(String value, String tag) {
        return value.replaceAll("(?i)</\\s*" + tag + "\\s*>", "[/" + tag + "]");
    }
}
