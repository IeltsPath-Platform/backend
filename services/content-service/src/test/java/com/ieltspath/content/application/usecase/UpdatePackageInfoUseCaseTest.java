package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.command.UpdatePackageInfoCommand;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.exception.ContentPackageNotFoundException;
import com.ieltspath.content.domain.repository.ContentPackageRepository;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PlacementPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdatePackageInfoUseCaseTest {
    private ContentPackageRepository packages;
    private UpdatePackageInfoUseCase useCase;

    @BeforeEach
    void setUp() {
        packages = mock(ContentPackageRepository.class);
        when(packages.save(any())).thenAnswer(call -> call.getArgument(0));
        useCase = new UpdatePackageInfoUseCase(packages);
    }

    private ContentPackage store(PackageType type, PlacementPool pool) {
        ContentPackage pkg = ContentPackage.create("P_" + UUID.randomUUID().toString().substring(0, 6), "Đề cũ", type, null,
                null, null, pool);
        when(packages.findByIdForUpdate(pkg.getId())).thenReturn(Optional.of(pkg));
        return pkg;
    }

    @Test
    void changesTitleAndMakesATestPremium() {
        ContentPackage pkg = store(PackageType.PRACTICE_TEST, null);

        var result = useCase.execute(new UpdatePackageInfoCommand(pkg.getId(), "Đề mới", true, "PREMIUM_CONTENT", null, null));

        assertThat(result.title()).isEqualTo("Đề mới");
        assertThat(result.requiredFeatureKey()).isEqualTo("PREMIUM_CONTENT");
    }

    @Test
    void makesAPremiumTestFree() {
        ContentPackage pkg = store(PackageType.PRACTICE_TEST, null);
        pkg.changeRequiredFeatureKey("PREMIUM_CONTENT");

        var result = useCase.execute(new UpdatePackageInfoCommand(pkg.getId(), null, true, null, null, null));

        assertThat(result.requiredFeatureKey()).isNull();
        assertThat(result.title()).isEqualTo("Đề cũ");
    }

    @Test
    void leavesAccessAloneWhenNotAsked() {
        ContentPackage pkg = store(PackageType.PRACTICE_TEST, null);
        pkg.changeRequiredFeatureKey("PREMIUM_CONTENT");

        var result = useCase.execute(new UpdatePackageInfoCommand(pkg.getId(), "Tên khác", false, null, null, null));

        assertThat(result.requiredFeatureKey()).isEqualTo("PREMIUM_CONTENT");
    }

    @Test
    void checksTheSourceAndBlankTakesTheTestOutOfTheLibrary() {
        ContentPackage pkg = store(PackageType.PRACTICE_TEST, null);

        assertThatThrownBy(() -> useCase.execute(new UpdatePackageInfoCommand(pkg.getId(), null, false, null, "KHONG_CO", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Nguồn đề");

        var cleared = useCase.execute(new UpdatePackageInfoCommand(pkg.getId(), null, false, null, " ", null));
        assertThat(cleared.sourceCode()).isNull();
    }

    @Test
    void changesPoolOfAPlacementTestThatWasNeverPublished() {
        ContentPackage pkg = store(PackageType.PLACEMENT_TEST, PlacementPool.BAND_0_3);

        var result = useCase.execute(new UpdatePackageInfoCommand(pkg.getId(), null, false, null, null, PlacementPool.BAND_5_9));

        assertThat(result.placementPool()).isEqualTo(PlacementPool.BAND_5_9);
    }

    @Test
    void unknownPackageIsNotFound() {
        UUID id = UUID.randomUUID();
        when(packages.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdatePackageInfoCommand(id, "x", false, null, null, null)))
                .isInstanceOf(ContentPackageNotFoundException.class);
    }
}
