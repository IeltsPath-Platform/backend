package com.ieltspath.content.application.usecase;

import com.ieltspath.content.domain.aggregate.Rubric;
import com.ieltspath.content.domain.exception.RubricNotFoundException;
import com.ieltspath.content.domain.repository.RubricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ManageRubricsUseCase {
    private final RubricRepository rubrics;

    public ManageRubricsUseCase(RubricRepository rubrics) { this.rubrics = rubrics; }

    public Rubric create(String code, String task, int version, String sourceNote,
                         List<Rubric.Criterion> criteria, List<Rubric.Example> examples) {
        return rubrics.create(new Rubric(code, task, version, sourceNote, "DRAFT", criteria, examples));
    }

    public Rubric updateDraft(String code, int version, String task, String sourceNote,
                              List<Rubric.Criterion> criteria, List<Rubric.Example> examples) {
        return rubrics.updateDraft(code, version, new Rubric(code, task, version, sourceNote, "DRAFT", criteria, examples));
    }

    public Rubric activate(String code, int version) { return rubrics.activate(code, version); }

    @Transactional(readOnly = true)
    public List<Rubric> active(String task) { return rubrics.findActive(requireTask(task)); }

    @Transactional(readOnly = true)
    public List<Rubric> drafts(String task) { return rubrics.findDrafts(requireTask(task)); }

    @Transactional(readOnly = true)
    public Rubric byVersion(String code, int version) {
        return rubrics.findByCodeAndVersion(code, version)
                .filter(r -> !"DRAFT".equals(r.status()))
                .orElseThrow(() -> new RubricNotFoundException("Published rubric not found: " + code + "@" + version));
    }

    @Transactional(readOnly = true)
    public Rubric activeRubric(String task) {
        return active(task).stream().findFirst()
                .orElseThrow(() -> new RubricNotFoundException("No active rubric for task: " + task));
    }

    private static String requireTask(String task) {
        if (!List.of("TASK_1", "TASK_2").contains(task)) throw new IllegalArgumentException("task must be TASK_1 or TASK_2");
        return task;
    }
}
