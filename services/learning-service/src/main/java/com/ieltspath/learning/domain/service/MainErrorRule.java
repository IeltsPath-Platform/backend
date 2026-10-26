package com.ieltspath.learning.domain.service;

import com.ieltspath.learning.application.service.ErrorTrapCatalog.ErrorTraps;
import com.ieltspath.learning.domain.aggregate.MasteryProfile;
import com.ieltspath.learning.domain.vo.MasteryTier;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Chooses the active error represented by the weakest child of a parent knowledge point. */
public final class MainErrorRule {
    private MainErrorRule() {}

    public static Optional<String> mainErrorCode(UUID parentKp, Collection<MasteryProfile> trapProfiles,
                                                  ErrorTraps traps) {
        Map<UUID, String> codeByTrap = traps.trapKpByCode().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        return trapProfiles.stream()
                .filter(profile -> traps.parentByTrapKp().get(profile.knowledgePointId()) != null
                        && traps.parentByTrapKp().get(profile.knowledgePointId()).equals(parentKp))
                .filter(profile -> profile.tier() != MasteryTier.INSUFFICIENT)
                .filter(profile -> codeByTrap.containsKey(profile.knowledgePointId()))
                .min(Comparator.comparingDouble(MasteryProfile::mastery)
                        .thenComparing(profile -> codeByTrap.get(profile.knowledgePointId())))
                .map(profile -> codeByTrap.get(profile.knowledgePointId()));
    }
}
