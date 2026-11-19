package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.result.PackageVersionContentResult;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ContentPackagePlacementMappingTest {
    @ParameterizedTest
    @EnumSource(PlacementPool.class)
    void poolSurvivesPersistenceAndMediaSectionReplacement(PlacementPool pool) {
        var pkg = ContentPackage.create("PLACEMENT", "Placement", PackageType.PLACEMENT_TEST,
                null, null, null, pool);
        var mapper = new ContentPackagePersistenceMapper();
        assertThat(mapper.toDomain(mapper.toEntity(pkg))).usingRecursiveComparison().isEqualTo(pkg);
        var payload = new PackageVersionContentResult(UUID.randomUUID(), pkg.getId(), PackageType.PLACEMENT_TEST,
                null, "{}", List.of(), pool);
        assertThat(payload.withSections(List.of()).placementPool()).isEqualTo(pool);
    }
}
