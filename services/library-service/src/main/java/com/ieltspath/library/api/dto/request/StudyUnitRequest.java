package com.ieltspath.library.api.dto.request;

import com.ieltspath.library.application.port.StudyUnitRepository.Draft;
import jakarta.validation.constraints.*;
import java.util.*;

public record StudyUnitRequest(@NotBlank @Size(max=120) String title, @Size(max=500) String description,
        @NotNull @Pattern(regexp="LEARN|REVIEW") String kind, @NotBlank String introductionVi,
        @NotNull @Size(min=4,max=60) List<@NotNull UUID> senseIds) {
    public Draft draft() { return new Draft(title,description,kind,introductionVi,senseIds); }
}
