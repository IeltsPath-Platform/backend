package com.ieltspath.content.application.service;

import com.ieltspath.content.application.port.CourseCatalogReader;
import com.ieltspath.content.domain.service.CourseCoverageAnalyzer;
import com.ieltspath.content.domain.vo.BlockType;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.CourseCatalogFacts;
import com.ieltspath.content.domain.vo.CourseCatalogFacts.Query;
import com.ieltspath.content.domain.vo.CourseDraftAnalysis;
import com.ieltspath.content.domain.vo.CourseDraftStructure;
import com.ieltspath.content.domain.vo.CourseDraftStructure.LessonPlan;
import com.ieltspath.content.domain.vo.CourseDraftStructure.TopicPlan;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Reads what the catalogue says about a course draft, in one go, and checks the draft against it. */
@Service
public class CourseDraftAnalysisService {

    private final CourseCatalogReader catalog;
    private final CourseCoverageAnalyzer analyzer = new CourseCoverageAnalyzer();

    public CourseDraftAnalysisService(CourseCatalogReader catalog) {
        this.catalog = catalog;
    }

    public Checked analyze(CourseDraftStructure plan) {
        CourseCatalogFacts facts = catalog.read(query(plan));
        return new Checked(analyzer.analyze(plan, facts), facts);
    }

    /** As {@link #analyze(CourseDraftStructure)}, for a plan whose exercise questions are created by the caller. */
    public Checked analyze(CourseDraftStructure plan, Set<String> questionRefsCreatedByCaller) {
        CourseCatalogFacts facts = catalog.read(query(plan)).withQuestionRefs(questionRefsCreatedByCaller);
        return new Checked(analyzer.analyze(plan, facts), facts);
    }

    /** The analysis and the facts it was made from; approval needs the resolved question references. */
    public record Checked(CourseDraftAnalysis analysis, CourseCatalogFacts facts) {
    }

    static Query query(CourseDraftStructure plan) {
        Set<UUID> points = new LinkedHashSet<>();
        Set<UUID> practiceSets = new LinkedHashSet<>();
        Set<String> refs = new LinkedHashSet<>();
        Set<String> topicCodes = new LinkedHashSet<>();
        Set<String> lessonCodes = new LinkedHashSet<>();
        Set<String> packageCodes = new LinkedHashSet<>();
        Set<String> skills = new LinkedHashSet<>();
        for (TopicPlan topic : plan.topics()) {
            topicCodes.add(topic.code());
            if (topic.skill() != null) skills.add(topic.skill().name());
            if (topic.topicTest() != null) packageCodes.add(topic.topicTest().code());
            for (LessonPlan lesson : topic.lessons()) {
                lessonCodes.add(lesson.code());
                points.addAll(lesson.allKnowledgePointIds());
                practiceSets.addAll(lesson.practiceSetPackageIds());
                lesson.blocks().stream().filter(block -> block.type() == BlockType.EXERCISE)
                        .forEach(block -> refs.addAll(block.questionRefs()));
            }
        }
        if (plan.courseTest() != null) packageCodes.add(plan.courseTest().code());
        Set<UUID> milestones = new LinkedHashSet<>();
        plan.practiceTestMilestones().forEach(milestone -> milestones.add(milestone.packageId()));
        var band = plan.course() == null ? null : plan.course().bandLevel();
        CefrLevel level = band == null ? null : CefrLevel.forBand(band);
        return new Query(points, practiceSets, milestones, refs,
                plan.course() == null ? Set.of() : Set.of(plan.course().code()), topicCodes, lessonCodes, packageCodes,
                band, level == null ? null : level.name(), skills);
    }
}
