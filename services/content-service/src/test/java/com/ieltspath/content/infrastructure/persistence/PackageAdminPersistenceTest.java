package com.ieltspath.content.infrastructure.persistence;

import com.ieltspath.content.application.command.UpdatePackageInfoCommand;
import com.ieltspath.content.application.usecase.ChangePackageVisibilityUseCase;
import com.ieltspath.content.application.usecase.UpdatePackageInfoUseCase;
import com.ieltspath.content.domain.aggregate.ContentPackage;
import com.ieltspath.content.domain.entity.ContentPackageVersion;
import com.ieltspath.content.domain.exception.PlacementPoolLastPublishedException;
import com.ieltspath.content.domain.exception.PlacementPoolLockedException;
import com.ieltspath.content.domain.vo.PackageType;
import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.domain.vo.PublicationStatus;
import com.ieltspath.content.infrastructure.persistence.adapter.ContentPackageRepositoryAdapter;
import com.ieltspath.content.infrastructure.persistence.mapper.ContentPackagePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The admin edits of a package against a real database: what is stored, and the placement pool counting query. */
@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ContentPackageRepositoryAdapter.class, ContentPackagePersistenceMapper.class})
@Testcontainers(disabledWithoutDocker = true)
class PackageAdminPersistenceTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private ContentPackageRepositoryAdapter packages;

    private static String code() {
        return "ADM" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private ContentPackage saveDraft(PackageType type, PlacementPool pool) {
        return packages.save(ContentPackage.create(code(), "Đề", type, null, null, null, pool));
    }

    private ContentPackage savePublished(PackageType type, PlacementPool pool) {
        ContentPackage pkg = saveDraft(type, pool);
        ContentPackageVersion version = ContentPackageVersion.create(pkg.getId(), 1, "{}");
        pkg.addVersion(version);
        pkg.publishVersion(version.getId());
        return packages.save(pkg);
    }

    @Test
    void titleAccessAndSourceAreStored() {
        ContentPackage pkg = saveDraft(PackageType.PRACTICE_TEST, null);

        new UpdatePackageInfoUseCase(packages).execute(new UpdatePackageInfoCommand(pkg.getId(), "Đề đã đổi tên", true,
                "PREMIUM_CONTENT", "IELTS_SPACE_PRO", null));

        ContentPackage stored = packages.findById(pkg.getId()).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("Đề đã đổi tên");
        assertThat(stored.getRequiredFeatureKey()).isEqualTo("PREMIUM_CONTENT");
        assertThat(stored.getSourceCode()).isEqualTo("IELTS_SPACE_PRO");
    }

    @Test
    void poolOfAPlacementTestChangesOnlyBeforeItsFirstPublication() {
        ContentPackage draft = saveDraft(PackageType.PLACEMENT_TEST, PlacementPool.BAND_0_3);
        UpdatePackageInfoUseCase update = new UpdatePackageInfoUseCase(packages);

        update.execute(new UpdatePackageInfoCommand(draft.getId(), null, false, null, null, PlacementPool.BAND_5_9));
        assertThat(packages.findById(draft.getId()).orElseThrow().getPlacementPool()).isEqualTo(PlacementPool.BAND_5_9);

        ContentPackage published = savePublished(PackageType.PLACEMENT_TEST, PlacementPool.BAND_3_5);
        assertThatThrownBy(() -> update.execute(
                new UpdatePackageInfoCommand(published.getId(), null, false, null, null, PlacementPool.BAND_0_3)))
                .isInstanceOf(PlacementPoolLockedException.class);
        assertThat(packages.findById(published.getId()).orElseThrow().getPlacementPool()).isEqualTo(PlacementPool.BAND_3_5);
    }

    @Test
    void archiveHidesAndRestoreBringsTheTestBack() {
        ContentPackage published = savePublished(PackageType.PRACTICE_TEST, null);
        ChangePackageVisibilityUseCase visibility = new ChangePackageVisibilityUseCase(packages);

        visibility.archive(published.getId());
        assertThat(packages.findById(published.getId()).orElseThrow().getStatus()).isEqualTo(PublicationStatus.ARCHIVED);

        visibility.restore(published.getId());
        assertThat(packages.findById(published.getId()).orElseThrow().getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Test
    void countsPublishedPlacementTestsOfAPoolWithoutTheGivenOne() {
        ContentPackage first = savePublished(PackageType.PLACEMENT_TEST, PlacementPool.BAND_0_3);
        long others = packages.countPublishedPlacementInPool(PlacementPool.BAND_0_3, first.getId());
        ContentPackage second = savePublished(PackageType.PLACEMENT_TEST, PlacementPool.BAND_0_3);
        saveDraft(PackageType.PLACEMENT_TEST, PlacementPool.BAND_0_3);

        assertThat(packages.countPublishedPlacementInPool(PlacementPool.BAND_0_3, first.getId())).isEqualTo(others + 1);
        assertThat(packages.countPublishedPlacementInPool(PlacementPool.BAND_0_3, second.getId())).isEqualTo(others + 1);
    }

    @Test
    void theLastPublishedTestOfAPoolCannotBeArchived() {
        ContentPackage only = savePublished(PackageType.PLACEMENT_TEST, PlacementPool.BAND_5_9);
        // The seeded pool already has published tests: hide them in SQL (the test rolls back) so this one is the last.
        jdbc.update("UPDATE content_packages SET status = 'ARCHIVED' WHERE package_type = 'PLACEMENT_TEST' "
                + "AND placement_pool = 'BAND_5_9' AND status = 'PUBLISHED' AND id <> ?", only.getId());
        ChangePackageVisibilityUseCase visibility = new ChangePackageVisibilityUseCase(packages);

        assertThatThrownBy(() -> visibility.archive(only.getId())).isInstanceOf(PlacementPoolLastPublishedException.class);
        assertThat(packages.findById(only.getId()).orElseThrow().getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
}
