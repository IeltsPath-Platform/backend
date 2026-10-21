package com.ieltspath.content.domain.aggregate;

import java.util.List;
import java.util.UUID;

/** Immutable rubric snapshot consumed by both essay graders. */
public record Rubric(String code, String task, int version, String sourceNote, String status,
                     List<Criterion> criteria, List<Example> examples) {
    public Rubric {
        if (code == null || code.isBlank() || task == null || !List.of("TASK_1", "TASK_2").contains(task)
                || version < 1 || sourceNote == null || sourceNote.isBlank()) {
            throw new IllegalArgumentException("Rubric code, task, positive version and source note are required");
        }
        if (status == null || !List.of("DRAFT", "ACTIVE", "RETIRED").contains(status)) {
            throw new IllegalArgumentException("Rubric status must be DRAFT, ACTIVE or RETIRED");
        }
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        examples = examples == null ? List.of() : List.copyOf(examples);
    }

    public record Criterion(String code, String name, String description, int sortOrder,
                            List<BandDescriptor> bandDescriptors, UUID knowledgePointId) {
        public Criterion {
            if (code == null || code.isBlank() || name == null || name.isBlank() || description == null
                    || description.isBlank() || sortOrder < 1) throw new IllegalArgumentException("Invalid rubric criterion");
            bandDescriptors = bandDescriptors == null ? List.of() : List.copyOf(bandDescriptors);
        }

        /** A criterion as authored; its knowledge point comes from the task mapping when the rubric is read. */
        public Criterion(String code, String name, String description, int sortOrder, List<BandDescriptor> bandDescriptors) {
            this(code, name, description, sortOrder, bandDescriptors, null);
        }
    }

    public record BandDescriptor(double band, String descriptor) {
        public BandDescriptor {
            if (band < 0 || band > 9 || band * 2 != Math.rint(band * 2)
                    || descriptor == null || descriptor.isBlank()) throw new IllegalArgumentException("Invalid band descriptor");
        }
    }

    public record Example(double band, String prompt, String essay, String commentary) {
        public Example {
            if (band < 0 || band > 9 || band * 2 != Math.rint(band * 2) || prompt == null || prompt.isBlank()
                    || essay == null || essay.isBlank() || commentary == null || commentary.isBlank()) {
                throw new IllegalArgumentException("Invalid rubric example");
            }
        }
    }
}
