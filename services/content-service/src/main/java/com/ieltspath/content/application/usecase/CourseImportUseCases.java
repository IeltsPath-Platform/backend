package com.ieltspath.content.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.content.application.command.AddQuestionVersionCommand;
import com.ieltspath.content.application.command.CreateContentPackageCommand;
import com.ieltspath.content.application.command.CreateQuestionCommand;
import com.ieltspath.content.application.command.SaveContentSectionCommand;
import com.ieltspath.content.application.port.AiUsageQuota;
import com.ieltspath.content.application.port.CourseWorkbookReader;
import com.ieltspath.content.application.port.ImportCatalogReader;
import com.ieltspath.content.application.result.QuestionResult;
import com.ieltspath.content.application.service.CourseDraftAnalysisService;
import com.ieltspath.content.application.service.CourseMaterializer;
import com.ieltspath.content.domain.aggregate.Question;
import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.entity.QuestionVersion;
import com.ieltspath.content.domain.exception.CourseImportInvalidException;
import com.ieltspath.content.domain.exception.ImportFileRejectedException;
import com.ieltspath.content.domain.exception.ImportQuotaExceededException;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.PracticeTestMilestoneRepository;
import com.ieltspath.content.domain.repository.QuestionRepository;
import com.ieltspath.content.domain.service.CourseCoverageAnalyzer;
import com.ieltspath.content.domain.service.CourseImportValidator;
import com.ieltspath.content.domain.vo.AiUsageKind;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CourseCatalogFacts;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseImportModel;
import com.ieltspath.content.domain.vo.CourseImportModel.Issue;
import com.ieltspath.content.domain.vo.CourseImportOutline;
import com.ieltspath.content.domain.vo.CourseImportPlan;
import com.ieltspath.content.domain.vo.CourseImportPlan.PackagePlan;
import com.ieltspath.content.domain.vo.CourseImportPlan.QuestionPlan;
import com.ieltspath.content.domain.vo.ImportCatalogFacts;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PracticeTestMilestone;
import com.ieltspath.content.domain.vo.QuestionOptionPayload;
import com.ieltspath.content.domain.vo.QuestionPurpose;
import com.ieltspath.content.domain.vo.Skill;
import com.ieltspath.content.infrastructure.config.ContentAiProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Checks and imports a course workbook. The check reads the file, validates its rows against the catalogue and runs the
 * knowledge tree analysis on the course it describes; it writes nothing. The import repeats the check and then creates
 * everything in one transaction, through the authoring use cases, or nothing at all.
 */
@Service
public class CourseImportUseCases {

    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 3, 4};

    private final CourseWorkbookReader reader;
    private final ImportCatalogReader catalog;
    private final ErrorTypeRepository errorTypes;
    private final CourseDraftAnalysisService analysis;
    private final CourseMaterializer materializer;
    private final CreateQuestionUseCase createQuestion;
    private final AddQuestionVersionUseCase addVersion;
    private final QuestionRepository questions;
    private final CodeSettingsUseCases codeSettings;
    private final CreateDraftPackageVersionUseCase createDraftVersion;
    private final CreateContentSectionUseCase createSection;
    private final AddSectionQuestionUseCase addSectionQuestion;
    private final PracticeTestMilestoneRepository milestones;
    private final AiUsageQuota quota;
    private final ContentAiProperties properties;
    private final ObjectMapper json;
    private final Clock clock;

    public CourseImportUseCases(CourseWorkbookReader reader, ImportCatalogReader catalog,
                                ErrorTypeRepository errorTypes, CourseDraftAnalysisService analysis,
                                CourseMaterializer materializer, CreateQuestionUseCase createQuestion,
                                AddQuestionVersionUseCase addVersion, QuestionRepository questions,
                                CodeSettingsUseCases codeSettings,
                                CreateDraftPackageVersionUseCase createDraftVersion,
                                CreateContentSectionUseCase createSection, AddSectionQuestionUseCase addSectionQuestion,
                                PracticeTestMilestoneRepository milestones, AiUsageQuota quota,
                                ContentAiProperties properties, ObjectMapper json, Clock clock) {
        this.reader = reader;
        this.catalog = catalog;
        this.errorTypes = errorTypes;
        this.analysis = analysis;
        this.materializer = materializer;
        this.createQuestion = createQuestion;
        this.addVersion = addVersion;
        this.questions = questions;
        this.codeSettings = codeSettings;
        this.createDraftVersion = createDraftVersion;
        this.createSection = createSection;
        this.addSectionQuestion = addSectionQuestion;
        this.milestones = milestones;
        this.quota = quota;
        this.properties = properties;
        this.json = json;
        this.clock = clock;
    }

    /** The outcome of a check. {@code plan} is null when there are errors; {@code summary} when the file was unreadable. */
    public record Report(List<Issue> errors, List<Issue> warnings, CourseImportValidator.Summary summary,
                         CourseImportPlan plan, CourseImportOutline outline) {
        public Report(List<Issue> errors, List<Issue> warnings, CourseImportValidator.Summary summary,
                      CourseImportPlan plan) {
            this(errors, warnings, summary, plan, null);
        }

        public boolean valid() {
            return errors.isEmpty();
        }

        Report withOutline(CourseImportOutline outline) {
            return new Report(errors, warnings, summary, plan, outline);
        }
    }

    /** What an import created. */
    public record Imported(UUID courseId, List<UUID> topicIds, List<UUID> lessonIds, List<UUID> questionIds,
                           List<UUID> packageIds, Map<UUID, String> packageTitles, List<Issue> warnings) {
    }

    /** Reads the workbook into rows. Nothing is written. A file that cannot be read has no model. */
    public CourseWorkbookReader.ReadResult read(String filename, byte[] content) {
        requireWorkbook(filename, content);
        try {
            return reader.read(new ByteArrayInputStream(content));
        } catch (IOException unreadable) {
            return new CourseWorkbookReader.ReadResult(null, List.of(new Issue("", 0, "", "UNREADABLE_FILE",
                    "The file could not be read as an Excel workbook.")));
        }
    }

    /** Reads and checks the workbook. Nothing is written. */
    @Transactional(readOnly = true)
    public Report validate(String filename, byte[] content) {
        CourseWorkbookReader.ReadResult read = read(filename, content);
        if (read.model() == null) {
            return new Report(read.issues(), List.of(), null, null);
        }
        if (!read.issues().isEmpty()) {
            return new Report(read.issues(), List.of(), null, null).withOutline(CourseImportOutline.from(read.model()));
        }
        return validateModel(read.model());
    }

    /** Checks rows that are already read, from a file or from an edited draft. Nothing is written. */
    @Transactional(readOnly = true)
    public Report validateModel(CourseImportModel model) {
        CourseImportOutline outline = CourseImportOutline.from(model);
        ImportCatalogFacts facts = catalogFacts(model);
        CourseImportValidator.Validated rows = CourseImportValidator.validate(model, facts);
        List<Issue> errors = new ArrayList<>(rows.errors());
        errors.addAll(lockedCodeIssues(model));
        if (!errors.isEmpty()) {
            return new Report(List.copyOf(errors), List.of(), rows.summary(), null).withOutline(outline);
        }
        return withTreeAnalysis(model, rows).withOutline(outline);
    }

    /**
     * Checks the workbook again and, if it is free of errors, creates everything in one transaction. Any problem rolls
     * all of it back.
     */
    @Transactional
    public Imported execute(UUID author, String filename, byte[] content) {
        return create(author, validate(filename, content));
    }

    /** The same as {@link #execute}, for rows that are already read. */
    @Transactional
    public Imported executeModel(UUID author, CourseImportModel model) {
        return create(author, validateModel(model));
    }

    private Imported create(UUID author, Report report) {
        if (!report.valid()) {
            throw new CourseImportInvalidException(report.errors(), report.warnings());
        }
        if (!quota.tryConsume(author, LocalDate.now(clock), AiUsageKind.IMPORT, properties.getAi().getDailyLimit())) {
            throw new ImportQuotaExceededException();
        }
        CourseImportPlan plan = report.plan();

        List<UUID> questionIds = new ArrayList<>();
        for (QuestionPlan question : plan.questions()) {
            questionIds.add(createQuestionFrom(question));
        }
        Map<String, UUID> versionsByRef = questionVersions(plan);

        CourseMaterializer.Created created = materializer.create(plan.structure(), versionsByRef);
        List<UUID> packageIds = new ArrayList<>();
        Map<UUID, String> packageTitles = new LinkedHashMap<>();
        List<PracticeTestMilestone> milestonePlans = new ArrayList<>();
        for (PackagePlan packagePlan : plan.packages()) {
            UUID packageId = createPackageFrom(packagePlan, created, plan, versionsByRef);
            packageIds.add(packageId);
            packageTitles.put(packageId, packagePlan.title());
            if (packagePlan.afterTopicSequence() != null) {
                milestonePlans.add(new PracticeTestMilestone(packageId, null, null, null,
                        packagePlan.afterTopicSequence()));
            }
        }
        if (!milestonePlans.isEmpty()) {
            milestones.lockCourse(created.courseId());
            milestones.replace(created.courseId(), milestonePlans);
        }
        return new Imported(created.courseId(), created.topicIds(), created.lessonIds(), questionIds, packageIds, packageTitles,
                report.warnings());
    }

    // --- checking

    static void requireWorkbook(String filename, byte[] content) {
        if (content == null || content.length > MAX_FILE_BYTES) {
            throw new ImportFileRejectedException(ImportFileRejectedException.FILE_TOO_LARGE);
        }
        boolean named = filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".xlsx");
        boolean zipped = content.length >= ZIP_SIGNATURE.length;
        for (int i = 0; zipped && i < ZIP_SIGNATURE.length; i++) {
            zipped = content[i] == ZIP_SIGNATURE[i];
        }
        if (!named || !zipped) {
            throw new ImportFileRejectedException(ImportFileRejectedException.NOT_XLSX);
        }
    }

    private List<Issue> lockedCodeIssues(CourseImportModel model) {
        List<Issue> issues = new ArrayList<>();
        var courseRow = model.rows(CourseImportModel.COURSE).stream().findFirst().orElse(null);
        if (courseRow == null) return issues;
        String band = normalizedBand(courseRow.get("bandLevel"));
        var courseSuggestion = importCodeSuggestion("COURSE", band, null, null, null, null, 0);
        checkLockedCode(issues, CourseImportModel.COURSE, courseRow.number(), "code", courseRow.get("code"), courseSuggestion);
        String courseCode = courseSuggestion != null && courseSuggestion.applied() && !courseSuggestion.editable()
                ? courseSuggestion.code() : courseRow.get("code");

        Map<String, Long> topicOffsets = new HashMap<>();
        Map<String, String> topicCodes = new HashMap<>();
        for (var row : model.rows(CourseImportModel.TOPICS)) {
            String skillValue = row.get("skill");
            Skill skill = parseSkill(skillValue);
            long offset = topicOffsets.merge("TOPIC", 1L, Long::sum) - 1;
            var suggestion = importCodeSuggestion("TOPIC", band, courseCode, skill, null, null, offset);
            checkLockedCode(issues, CourseImportModel.TOPICS, row.number(), "code", row.get("code"), suggestion);
            topicCodes.put(row.get("code"), suggestion != null && suggestion.applied() && !suggestion.editable()
                    ? suggestion.code() : row.get("code"));
        }

        Map<String, Long> lessonOffsets = new HashMap<>();
        for (var row : model.rows(CourseImportModel.LESSONS)) {
            String topicCode = topicCodes.getOrDefault(row.get("topicCode"), row.get("topicCode"));
            long offset = lessonOffsets.merge(topicCode, 1L, Long::sum) - 1;
            var suggestion = importCodeSuggestion("LESSON", band, courseCode, null, topicCode, null, offset);
            checkLockedCode(issues, CourseImportModel.LESSONS, row.number(), "code", row.get("code"), suggestion);
        }

        Map<PackageType, Long> packageOffsets = new HashMap<>();
        for (var row : model.rows(CourseImportModel.PRACTICE_SETS)) {
            PackageType type = PackageType.PRACTICE_SET;
            long offset = packageOffsets.merge(type, 1L, Long::sum) - 1;
            var suggestion = importCodeSuggestion("PACKAGE", band, courseCode, null, null, type, offset);
            checkLockedCode(issues, CourseImportModel.PRACTICE_SETS, row.number(), "packageCode", row.get("packageCode"), suggestion);
        }
        for (var row : model.rows(CourseImportModel.TESTS)) {
            PackageType type = switch (row.get("kind")) {
                case "TOPIC_TEST" -> PackageType.TOPIC_TEST;
                case "COURSE_TEST" -> PackageType.COURSE_TEST;
                case "PRACTICE_TEST_MILESTONE" -> PackageType.PRACTICE_TEST;
                default -> null;
            };
            if (type == null) continue;
            long offset = packageOffsets.merge(type, 1L, Long::sum) - 1;
            var suggestion = importCodeSuggestion("PACKAGE", band, courseCode, null, null, type, offset);
            checkLockedCode(issues, CourseImportModel.TESTS, row.number(), "packageCode", row.get("packageCode"), suggestion);
        }
        return issues;
    }

    private com.ieltspath.content.application.result.CodeSuggestion importCodeSuggestion(String kind, String band,
            String courseCode, Skill skill, String topicCode, PackageType type, long offset) {
        try { return codeSettings.suggestForImport(kind, band, courseCode, skill, topicCode, type, offset); }
        catch (IllegalArgumentException missingOrInvalidSetting) { return null; }
    }

    private static String normalizedBand(String value) {
        try { return new BigDecimal(value).setScale(1).toPlainString(); }
        catch (NumberFormatException | ArithmeticException invalid) { return value; }
    }

    private static Skill parseSkill(String value) {
        try { return Skill.valueOf(value); } catch (IllegalArgumentException | NullPointerException invalid) { return null; }
    }

    private static void checkLockedCode(List<Issue> issues, String sheet, int row, String column, String supplied,
                                        com.ieltspath.content.application.result.CodeSuggestion suggestion) {
        if (suggestion != null && suggestion.applied() && !suggestion.editable() && !supplied.isBlank()
                && !supplied.equals(suggestion.code())) {
            issues.add(new Issue(sheet, row, column, "CODE_CONFLICT",
                    "The code does not match the next value from the locked code setting."));
        }
    }
    private ImportCatalogFacts catalogFacts(CourseImportModel model) {
        Set<String> pointCodes = new LinkedHashSet<>();
        Set<String> refs = new LinkedHashSet<>();
        Set<String> packageCodes = new LinkedHashSet<>();
        Set<String> courseCodes = new LinkedHashSet<>();
        Set<String> topicCodes = new LinkedHashSet<>();
        Set<String> lessonCodes = new LinkedHashSet<>();
        Set<String> bands = new LinkedHashSet<>();
        Set<Integer> topicSortOrders = new LinkedHashSet<>();
        List<BigDecimal> bandValues = new ArrayList<>();
        for (String sheet : List.of(CourseImportModel.LESSONS, CourseImportModel.BLOCKS, CourseImportModel.QUESTIONS,
                CourseImportModel.PRACTICE_SETS)) {
            model.rows(sheet).forEach(row -> pointCodes.addAll(split(row.get("knowledgePointCodes"))));
        }
        for (String sheet : List.of(CourseImportModel.BLOCKS, CourseImportModel.PRACTICE_SETS, CourseImportModel.TESTS)) {
            model.rows(sheet).forEach(row -> refs.addAll(split(row.get("questionRefs"))));
        }
        model.rows(CourseImportModel.QUESTIONS).forEach(row -> refs.add(row.get("importRef")));
        model.rows(CourseImportModel.PRACTICE_SETS).forEach(row -> packageCodes.add(row.get("packageCode")));
        model.rows(CourseImportModel.TESTS).forEach(row -> packageCodes.add(row.get("packageCode")));
        model.rows(CourseImportModel.COURSE).forEach(row -> {
            courseCodes.add(row.get("code"));
            try {
                BigDecimal value = new BigDecimal(row.get("bandLevel"));
                bandValues.add(value);
                bands.add(value.stripTrailingZeros().toPlainString());
            } catch (NumberFormatException ignored) {
                // The validator reports malformed band values with their source row.
            }
        });
        model.rows(CourseImportModel.TOPICS).forEach(row -> {
            topicCodes.add(row.get("code"));
            try { topicSortOrders.add(Integer.valueOf(row.get("sortOrder"))); }
            catch (NumberFormatException ignored) { }
        });
        model.rows(CourseImportModel.LESSONS).forEach(row -> lessonCodes.add(row.get("code")));
        Set<String> activeErrorCodes = errorTypes.findApprovedActive().stream().map(ErrorType::getCode)
                .collect(Collectors.toSet());
        return new ImportCatalogFacts(catalog.activeKnowledgePoints(pointCodes), activeErrorCodes,
                catalog.existingImportRefs(refs), catalog.existingPackageCodes(packageCodes),
                catalog.existingCourseCodes(courseCodes), catalog.existingTopicCodes(topicCodes),
                catalog.existingLessonCodes(lessonCodes), catalog.existingBandLevels(bandValues),
                catalog.existingTopicSortOrders(topicSortOrders), catalog.existingQuestionSkills(refs));
    }

    /** Runs the knowledge tree checks on the course the rows describe and folds them into the report. */
    private Report withTreeAnalysis(CourseImportModel model, CourseImportValidator.Validated rows) {
        CourseImportPlan plan = rows.plan();
        Set<String> ownRefs = plan.questions().stream().map(QuestionPlan::importRef).collect(Collectors.toSet());
        CourseDraftAnalysisService.Checked checked = analysis.analyze(plan.structure(), ownRefs);
        Map<UUID, String> codeById = new HashMap<>();
        plan.knowledgePointIdsByCode().forEach((code, id) -> codeById.put(id, code));

        List<Issue> errors = new ArrayList<>();
        for (CourseDraftAnalysis.Issue issue : checked.analysis().errors()) {
            errors.add(locate(issue, model, codeById));
        }
        List<Issue> warnings = new ArrayList<>();
        for (CourseDraftAnalysis.Issue issue : checked.analysis().warnings()) {
            warnings.add(new Issue(CourseImportModel.LESSONS, 0, "", issue.code(),
                    "An expected knowledge point is taught by no lesson."));
        }
        gaps(plan, checked.facts(), model, codeById).forEach(warnings::add);
        return new Report(errors, warnings, rows.summary(), errors.isEmpty() ? plan : null);
    }

    /**
     * Practice material the knowledge points still lack once the workbook's own questions and practice sets are
     * counted in. The analysis alone would report them as missing because they are not stored yet.
     */
    private List<Issue> gaps(CourseImportPlan plan, CourseCatalogFacts facts, CourseImportModel model,
                             Map<UUID, String> codeById) {
        Map<String, QuestionPlan> byRef = new HashMap<>();
        plan.questions().forEach(question -> byRef.put(question.importRef(), question));
        Set<UUID> withFileQuestions = new HashSet<>();
        plan.questions().forEach(question -> withFileQuestions.addAll(question.knowledgePointIds()));
        Map<UUID, Integer> fileSets = new HashMap<>();
        for (PackagePlan pack : plan.packages()) {
            if (pack.type() != PackageType.PRACTICE_SET) continue;
            Set<UUID> measured = new HashSet<>();
            pack.questionRefs().forEach(ref -> {
                QuestionPlan question = byRef.get(ref);
                if (question != null) measured.addAll(question.knowledgePointIds());
            });
            measured.forEach(id -> fileSets.merge(id, 1, Integer::sum));
        }
        Set<UUID> used = new LinkedHashSet<>();
        plan.structure().lessonsInOrder().forEach(lesson -> used.addAll(lesson.allKnowledgePointIds()));
        List<Issue> issues = new ArrayList<>();
        for (UUID id : used) {
            int sets = facts.unlinkedPracticeSets().getOrDefault(id, 0) + fileSets.getOrDefault(id, 0);
            if (sets == 0) {
                issues.add(gap("NO_PRACTICE_SET", id, model, codeById));
            } else if (sets < CourseCoverageAnalyzer.WANTED_PRACTICE_SETS) {
                issues.add(gap("FEW_PRACTICE_SETS", id, model, codeById));
            }
            if (facts.withoutExerciseQuestion().contains(id) && !withFileQuestions.contains(id)) {
                issues.add(gap("NO_EXERCISE_QUESTION", id, model, codeById));
            }
        }
        return issues;
    }

    private Issue gap(String reason, UUID point, CourseImportModel model, Map<UUID, String> codeById) {
        return new Issue(CourseImportModel.LESSONS, rowWithPoint(model, codeById.get(point)), "knowledgePointCodes",
                reason, "A knowledge point has too little practice material.");
    }

    /** Puts an analysis error where the author can find it in the workbook. */
    private Issue locate(CourseDraftAnalysis.Issue issue, CourseImportModel model, Map<UUID, String> codeById) {
        switch (issue.code()) {
            case CourseDraftAnalysis.Issue.CODE_TAKEN -> {
                String sheet = switch (issue.related()) {
                    case "COURSE" -> CourseImportModel.COURSE;
                    case "TOPIC" -> CourseImportModel.TOPICS;
                    case "LESSON" -> CourseImportModel.LESSONS;
                    default -> CourseImportModel.TESTS;
                };
                return new Issue(sheet, rowWithValue(model, sheet, "code", issue.subject()), "code", "CODE_CONFLICT",
                        "This code is already used.");
            }
            case CourseDraftAnalysis.Issue.BAND_TAKEN -> {
                return new Issue(CourseImportModel.COURSE, firstRow(model, CourseImportModel.COURSE), "bandLevel",
                        "CODE_CONFLICT", "Another course already has this band.");
            }
            case CourseDraftAnalysis.Issue.TOPIC_SORT_ORDER_TAKEN -> {
                return new Issue(CourseImportModel.TOPICS, rowWithValue(model, CourseImportModel.TOPICS, "code",
                        issue.subject()), "sortOrder", "CODE_CONFLICT", "Another top-level topic has this sort order.");
            }
            case "PREREQUISITE_ORDER" -> {
                String code = codeById.get(UUID.fromString(issue.subject()));
                return new Issue(CourseImportModel.LESSONS, rowWithPoint(model, code), "knowledgePointCodes",
                        "PREREQUISITE_ORDER", "A knowledge point is taught before its prerequisite.");
            }
            default -> {
                return new Issue(CourseImportModel.LESSONS, 0, "", issue.code(), "The course plan has a problem.");
            }
        }
    }

    private static int firstRow(CourseImportModel model, String sheet) {
        return model.rows(sheet).stream().mapToInt(CourseImportModel.Row::number).findFirst().orElse(0);
    }

    private static int rowWithValue(CourseImportModel model, String sheet, String column, String value) {
        if (CourseImportModel.TESTS.equals(sheet)) {
            return model.rows(sheet).stream().filter(row -> value.equals(row.get("packageCode")))
                    .mapToInt(CourseImportModel.Row::number).findFirst().orElse(0);
        }
        return model.rows(sheet).stream().filter(row -> value.equals(row.get(column)))
                .mapToInt(CourseImportModel.Row::number).findFirst().orElse(0);
    }

    private static int rowWithPoint(CourseImportModel model, String code) {
        for (String sheet : List.of(CourseImportModel.LESSONS, CourseImportModel.BLOCKS)) {
            for (CourseImportModel.Row row : model.rows(sheet)) {
                if (split(row.get("knowledgePointCodes")).contains(code)) return row.number();
            }
        }
        return 0;
    }

    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split("[,;]")) {
            if (!part.isBlank()) parts.add(part.strip());
        }
        return parts;
    }

    // --- writing

    private UUID createQuestionFrom(QuestionPlan question) {
        QuestionResult created = createQuestion.execute(new CreateQuestionCommand(question.type(), question.skill(),
                null, QuestionPurpose.LEARNING, question.importRef()));
        List<QuestionOptionPayload> options = new ArrayList<>();
        int order = 1;
        for (CourseImportPlan.Option option : question.options()) {
            options.add(new QuestionOptionPayload(option.key(), option.content(), order++));
        }
        List<AddQuestionVersionCommand.KnowledgePointInput> points = question.knowledgePointIds().stream()
                .map(id -> new AddQuestionVersionCommand.KnowledgePointInput(null, id, BigDecimal.ONE)).toList();
        addVersion.execute(new AddQuestionVersionCommand(created.id(), 1, question.stem(), options,
                answerSpec(question), question.explanation(), null, question.difficulty(), points));
        return created.id();
    }

    private String answerSpec(QuestionPlan question) {
        ObjectNode spec = json.createObjectNode();
        if (question.choice()) {
            spec.put("type", "CHOICE");
            spec.put("correct", question.correct());
            if (!question.distractors().isEmpty()) {
                ObjectNode distractors = spec.putObject("distractors");
                question.distractors().forEach(distractors::put);
            }
        } else {
            spec.put("type", "FILL");
            spec.set("accepted", json.valueToTree(question.accepted()));
        }
        try {
            return json.writeValueAsString(spec);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Không ghi được đáp án của câu hỏi", failure);
        }
    }

    /** The latest version of every question the plan refers to, created now or already there, by import reference. */
    private Map<String, UUID> questionVersions(CourseImportPlan plan) {
        Set<String> refs = new LinkedHashSet<>();
        plan.structure().lessonsInOrder().forEach(lesson -> lesson.blocks().stream()
                .filter(block -> block.type() == BlockType.EXERCISE).forEach(block -> refs.addAll(block.questionRefs())));
        plan.packages().forEach(pack -> refs.addAll(pack.questionRefs()));
        Map<String, UUID> versions = new LinkedHashMap<>();
        for (String ref : refs) {
            versions.put(ref, latestVersion(ref).getId());
        }
        return versions;
    }

    private QuestionVersion latestVersion(String importRef) {
        // The lookup by reference returns a summary without versions, so the question is read again in full.
        Question question = questions.findByImportRef(importRef).flatMap(found -> questions.findById(found.getId()))
                .orElseThrow(() -> new IllegalStateException("Không tìm lại được một câu hỏi của tệp nhập"));
        return question.getVersions().stream().max(Comparator.comparingInt(QuestionVersion::getVersionNumber))
                .orElseThrow(() -> new IllegalStateException("Một câu hỏi của tệp nhập chưa có phiên bản"));
    }

    private UUID createPackageFrom(PackagePlan pack, CourseMaterializer.Created created, CourseImportPlan plan,
                                   Map<String, UUID> versionsByRef) {
        UUID lessonId = pack.lessonCode() == null ? null : created.lessonIdsByCode().get(pack.lessonCode());
        UUID topicId = pack.topicCode() == null ? null : created.topicIdsByCode().get(pack.topicCode());
        UUID courseId = pack.type() == PackageType.COURSE_TEST ? created.courseId() : null;
        var author = codeSettings.createPackage(new CreateContentPackageCommand(pack.code(), pack.title(),
                pack.type(), null, lessonId, null, topicId, courseId, null));
        UUID versionId = createDraftVersion.execute(author.id(), null).id();
        Skill skill = skillOf(pack, plan);
        UUID sectionId = createSection.execute(new SaveContentSectionCommand(author.id(), versionId, null,
                pack.title(), skill, 1, null, null, null, null)).id();
        for (String ref : pack.questionRefs()) {
            addSectionQuestion.execute(author.id(), versionId, sectionId, versionsByRef.get(ref), null);
        }
        return author.id();
    }

    /** The skill of a package's section: that of its first question. */
    private Skill skillOf(PackagePlan pack, CourseImportPlan plan) {
        if (pack.questionRefs().isEmpty()) return null;
        String first = pack.questionRefs().get(0);
        return plan.questions().stream().filter(question -> question.importRef().equals(first))
                .map(QuestionPlan::skill).findFirst()
                .orElseGet(() -> questions.findByImportRef(first).map(Question::getSkill).orElse(null));
    }
}
