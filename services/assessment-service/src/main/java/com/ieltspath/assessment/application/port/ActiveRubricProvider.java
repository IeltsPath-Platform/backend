package com.ieltspath.assessment.application.port;

import java.util.List;
import java.math.BigDecimal;

public interface ActiveRubricProvider {
    Rubric getActive(String task);
    Rubric getVersion(String code, String version);

    record Rubric(String code, String task, int version, String sourceNote, List<Criterion> criteria, List<Example> examples) {}
    record Criterion(String code, String name, String description, int sortOrder, List<BandDescriptor> bandDescriptors) {}
    record BandDescriptor(BigDecimal band, String descriptor) {}
    record Example(BigDecimal band, String prompt, String essay, String commentary) {}
}
