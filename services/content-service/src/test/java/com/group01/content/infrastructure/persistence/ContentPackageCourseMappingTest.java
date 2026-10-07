package com.group01.content.infrastructure.persistence;

import com.group01.content.domain.aggregate.ContentPackage;
import com.group01.content.domain.vo.*;
import com.group01.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class ContentPackageCourseMappingTest {
    @Test
    void courseMembershipRoundTripsThroughPersistence() {
        ContentPackage pkg = new ContentPackage(UUID.randomUUID(), "FINAL", "Final", PackageType.COURSE_TEST,
                null, PublicationStatus.DRAFT, null, null, null, List.of(), null, UUID.randomUUID());
        var mapper = new ContentPackagePersistenceMapper();
        assertThat(mapper.toDomain(mapper.toEntity(pkg))).usingRecursiveComparison().isEqualTo(pkg);
    }
}
