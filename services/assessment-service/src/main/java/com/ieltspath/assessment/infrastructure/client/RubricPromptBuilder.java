package com.ieltspath.assessment.infrastructure.client;

import com.ieltspath.assessment.application.port.ActiveRubricProvider;
import com.ieltspath.assessment.application.port.ErrorTypeCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class RubricPromptBuilder {
    public static final String TEMPLATE_VERSION = "v1";
    public static final String TEMPLATE_VERSION_2 = "v2";
    private static final String TEMPLATE = loadTemplate(TEMPLATE_VERSION);
    private static final String TEMPLATE_2 = loadTemplate(TEMPLATE_VERSION_2);
    private RubricPromptBuilder() {}

    public static boolean isKnownTemplate(String version) {
        return TEMPLATE_VERSION.equals(version) || TEMPLATE_VERSION_2.equals(version);
    }

    public static String promptVersion(ActiveRubricProvider.Rubric rubric) {
        return promptVersion(rubric, TEMPLATE_VERSION);
    }

    public static String promptVersion(ActiveRubricProvider.Rubric rubric, String templateVersion) {
        return rubric.code() + "@" + rubric.version() + "+tpl" + templateVersion;
    }

    public static String build(ActiveRubricProvider.Rubric rubric, Set<String> errorCodes) {
        return build(rubric, errorCodes.stream().sorted().map(code -> new ErrorTypeCatalog.ErrorType(code, code, "")).toList());
    }

    public static String build(ActiveRubricProvider.Rubric rubric, List<ErrorTypeCatalog.ErrorType> errorTypes) {
        return build(rubric, errorTypes, TEMPLATE_VERSION);
    }

    public static String build(ActiveRubricProvider.Rubric rubric, List<ErrorTypeCatalog.ErrorType> errorTypes,
                               String templateVersion) {
        String criteria = rubric.criteria().stream().sorted(java.util.Comparator.comparingInt(ActiveRubricProvider.Criterion::sortOrder))
                .map(RubricPromptBuilder::formatCriterion).collect(Collectors.joining("\n\n"));
        String task = "TASK_1".equals(rubric.task()) ? "Academic Writing Task 1" : "Writing Task 2";
        String codes = errorTypes.stream().filter(java.util.Objects::nonNull).filter(error -> error.code() != null)
                .sorted(java.util.Comparator.comparing(ErrorTypeCatalog.ErrorType::code))
                .map(error -> error.code() + " — " + error.name() + ": " + error.fixHint()).collect(Collectors.joining("\n"));
        String template = forTask(TEMPLATE_VERSION_2.equals(templateVersion) ? TEMPLATE_2 : TEMPLATE,
                "TASK_1".equals(rubric.task()));
        return template.replace("{{task}}", task).replace("{{criteria}}", criteria)
                .replace("{{error_codes}}", codes.isBlank() ? "(none)" : codes);
    }

    public static String template() { return TEMPLATE; }

    private static String formatCriterion(ActiveRubricProvider.Criterion criterion) {
        StringBuilder text = new StringBuilder().append(criterion.code()).append(" (").append(criterion.name())
                .append("): ").append(criterion.description());
        List<ActiveRubricProvider.BandDescriptor> bands = criterion.bandDescriptors() == null ? List.of() : criterion.bandDescriptors();
        bands.stream().sorted(java.util.Comparator.comparing(ActiveRubricProvider.BandDescriptor::band))
                .forEach(band -> text.append("\n  Band ").append(band.band().stripTrailingZeros().toPlainString())
                        .append(": ").append(band.descriptor()).append("\n"));
        return text.toString().stripTrailing();
    }

    private static String loadTemplate(String version) {
        try (InputStream stream = RubricPromptBuilder.class.getResourceAsStream("/essay-prompt-template-" + version + ".txt")) {
            if (stream == null) throw new IllegalStateException("Essay prompt template is missing");
            return withoutNotes(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException exception) { throw new IllegalStateException("Essay prompt template cannot be read", exception); }
    }

    /** Lines that start with "#" at the top of a template are notes for people; the model never sees them. */
    private static String withoutNotes(String template) {
        return template.replaceFirst("\\A(?:#[^\\r\\n]*\\R)+\\s*", "");
    }

    /** The template with only the section of the given task kept; a template without sections is unchanged. */
    static String forTask(String template, boolean taskOne) {
        String drop = taskOne ? "TASK_2" : "TASK_1";
        String keep = taskOne ? "TASK_1" : "TASK_2";
        return template.replaceAll("(?s)\\[\\[" + drop + "\\]\\].*?\\[\\[/" + drop + "\\]\\]\\R*", "")
                .replaceAll("\\[\\[/?" + keep + "\\]\\]\\R?", "");
    }
}
