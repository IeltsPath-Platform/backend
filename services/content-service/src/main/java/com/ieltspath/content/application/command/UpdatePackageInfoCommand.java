package com.ieltspath.content.application.command;

import com.ieltspath.content.domain.vo.PlacementPool;

import java.util.UUID;

/**
 * What an author changes on a package after creating it; a null field is left as it is. {@code accessChange} says
 * whether {@code requiredFeatureKey} (null means free) is to be applied. A blank {@code sourceCode} takes a practice
 * test out of the learner library.
 */
public record UpdatePackageInfoCommand(UUID packageId, String title, boolean accessChange, String requiredFeatureKey,
                                       String sourceCode, PlacementPool placementPool) {
}
