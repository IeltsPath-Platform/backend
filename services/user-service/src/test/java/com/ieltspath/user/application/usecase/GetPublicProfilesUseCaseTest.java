package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.exception.LearnerProfileNotFoundException;
import com.ieltspath.user.domain.repository.PublicProfileRepository;
import com.ieltspath.user.domain.vo.ProfileVisibility;
import com.ieltspath.user.domain.vo.PublicProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetPublicProfilesUseCaseTest {
    private final PublicProfileRepository repository = mock(PublicProfileRepository.class);
    private final GetPublicProfilesUseCase useCase = new GetPublicProfilesUseCase(repository);
    private final UUID viewer = UUID.randomUUID();

    @Test
    void batchDeduplicatesPreservesOrderAndHidesPrivateProfiles() {
        UUID first = UUID.randomUUID(), community = UUID.randomUUID(), hidden = UUID.randomUUID(), missing = UUID.randomUUID();
        when(repository.findByUserIds(Set.of(first, community, hidden, missing, viewer))).thenReturn(List.of(
                profile(hidden, ProfileVisibility.PRIVATE, "https://images.test/avatar.png"),
                profile(viewer, ProfileVisibility.PRIVATE, null),
                profile(community, ProfileVisibility.COMMUNITY, null),
                profile(first, ProfileVisibility.PUBLIC, "https://images.test/avatar.png")));

        var results = useCase.executeBatch(viewer, List.of(first, community, first, hidden, missing, viewer));

        assertEquals(List.of(first, community, viewer), results.stream().map(r -> r.id()).toList());
        assertEquals("Display", results.getFirst().displayName());
        assertEquals(new BigDecimal("7.5"), results.getFirst().targetBand());
        assertEquals(4, results.getFirst().streak());
        assertEquals("https://images.test/avatar.png", results.getFirst().avatarUrl());
        verify(repository).findByUserIds(Set.of(first, community, hidden, missing, viewer));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void privateProfileIsNotFoundForOtherViewerButAvailableToOwner() {
        UUID hidden = UUID.randomUUID();
        when(repository.findByUserIds(Set.of(hidden))).thenReturn(List.of(profile(hidden, ProfileVisibility.PRIVATE, null)));
        assertThrows(LearnerProfileNotFoundException.class, () -> useCase.execute(viewer, hidden));
        assertEquals(hidden, useCase.execute(hidden, hidden).id());
    }

    @Test
    void nonexistentProfileIsNotFound() {
        when(repository.findByUserIds(Set.of(viewer))).thenReturn(List.of());
        assertThrows(LearnerProfileNotFoundException.class, () -> useCase.execute(viewer, viewer));
    }

    @Test
    void absentOptionalDataUsesExistingDefaultNameAndZeroStreak() {
        when(repository.findByUserIds(Set.of(viewer))).thenReturn(List.of(
                new PublicProfile(viewer, null, "Default name", null, null, null, null, null)));
        var result = useCase.execute(viewer, viewer);
        assertEquals("Default name", result.displayName());
        assertNull(result.targetBand());
        assertNull(result.bio());
        assertNull(result.avatarUrl());
        assertEquals(0, result.streak());
    }

    @Test
    void realAccountNameOfUserWithoutProfileIsHiddenFromOthers() {
        UUID other = UUID.randomUUID();
        when(repository.findByUserIds(Set.of(other))).thenReturn(List.of(
                new PublicProfile(other, null, "Real Account Name", null, null, null, null, null)));
        assertEquals("Learner", useCase.execute(viewer, other).displayName());
    }

    @Test
    void mentionSearchValidatesBoundsAndUsesVisibleProfileRepositoryQuery() {
        UUID found=UUID.randomUUID();
        when(repository.searchVisibleByName(viewer,"Lan",8)).thenReturn(List.of(profile(found,ProfileVisibility.COMMUNITY,null)));
        assertEquals(List.of(found),useCase.search(viewer," Lan ",8).stream().map(result -> result.id()).toList());
        assertThrows(IllegalArgumentException.class,() -> useCase.search(viewer,"x",8));
        assertThrows(IllegalArgumentException.class,() -> useCase.search(viewer,"Lan",11));
        verify(repository).searchVisibleByName(viewer,"Lan",8);
    }

    @Test
    void opaqueOrUnsafeAvatarReferencesAreNotExposedAsUrls() {
        for (String reference : List.of("avatar/key", "javascript:alert(1)", "data:image/png;base64,a",
                "//images.test/x", "https://user:password@images.test/x", "not a uri")) {
            when(repository.findByUserIds(Set.of(viewer))).thenReturn(List.of(profile(viewer, ProfileVisibility.PUBLIC, reference)));
            assertNull(useCase.execute(viewer, viewer).avatarUrl());
        }
    }

    @Test
    void invalidAndOversizedBatchesNeverQueryAndEmptyBatchIsAllowed() {
        assertThrows(IllegalArgumentException.class, () -> useCase.executeBatch(viewer, Collections.nCopies(101, viewer)));
        assertThrows(IllegalArgumentException.class, () -> useCase.executeBatch(viewer, null));
        var containsNull = new ArrayList<UUID>();
        containsNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> useCase.executeBatch(viewer, containsNull));
        assertEquals(List.of(), useCase.executeBatch(viewer, List.of()));
        verifyNoInteractions(repository);
    }

    private PublicProfile profile(UUID id, ProfileVisibility visibility, String avatar) {
        return new PublicProfile(id, "Display", "Account name", avatar, "Bio", new BigDecimal("7.5"), 4, visibility);
    }
}
