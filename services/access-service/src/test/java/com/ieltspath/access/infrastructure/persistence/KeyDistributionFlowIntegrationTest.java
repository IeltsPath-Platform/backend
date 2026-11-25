package com.ieltspath.access.infrastructure.persistence;

import com.ieltspath.access.application.command.ActivateKeyCommand;
import com.ieltspath.access.application.exception.KeyDistributionException;
import com.ieltspath.access.application.port.KeyDistributionStore;
import com.ieltspath.access.application.port.KeyDistributionStore.Allocation;
import com.ieltspath.access.application.port.KeyDistributionStore.Batch;
import com.ieltspath.access.application.port.KeyDistributionStore.KeyRow;
import com.ieltspath.access.application.port.KeyDistributionStore.State;
import com.ieltspath.access.application.port.SaleDirectory;
import com.ieltspath.access.application.port.SaleDirectory.SaleAccount;
import com.ieltspath.access.application.result.ActivationResult;
import com.ieltspath.access.application.usecase.ActivateKeyUseCase;
import com.ieltspath.access.application.usecase.KeyBatchUseCases;
import com.ieltspath.access.application.usecase.SaleKeyUseCases;
import com.ieltspath.access.domain.exception.KeyAlreadyRedeemedException;
import com.ieltspath.access.domain.exception.KeyNotIssuedException;
import com.ieltspath.access.domain.exception.KeyProductNotFoundException;
import com.ieltspath.commonresilience.DownstreamUnavailableException;
import com.ieltspath.access.domain.exception.KeyRevokedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * The whole key journey on PostgreSQL: a manager makes a batch and gives keys to two sales, a sale hands one to a student,
 * the student redeems it, and the manager revokes what is left. Runs through the use cases, which is where the rules live.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "app.auth.internal-jwt-issuer=urn:code-base:api-gateway"
})
class KeyDistributionFlowIntegrationTest {

    private static final String INTERNAL_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private static final String KEY_CODE_SECRET = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.US_ASCII));

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.internal-jwt-secret", () -> INTERNAL_SECRET);
        registry.add("access.key-code.secret", () -> KEY_CODE_SECRET);
    }

    @MockBean
    SaleDirectory directory;

    @Autowired JdbcTemplate jdbc;
    @Autowired KeyBatchUseCases batches;
    @Autowired SaleKeyUseCases sales;
    @Autowired ActivateKeyUseCase activation;

    private final UUID manager = UUID.randomUUID();
    private final UUID saleA = UUID.randomUUID();
    private final UUID saleB = UUID.randomUUID();
    private UUID product;

    @BeforeEach
    void setUp() {
        product = UUID.randomUUID();
        jdbc.update("INSERT INTO key_products (id, code, name, key_type, points_amount, status) VALUES (?, ?, ?, 'POINTS', 100, 'ACTIVE')",
                product, "P-" + product, "Gói 100 AI Points");
        when(directory.sales()).thenReturn(List.of(new SaleAccount(saleA, "Sale A", true), new SaleAccount(saleB, "Sale B", true)));
    }

    private UUID verifiedLearner() {
        UUID learner = UUID.randomUUID();
        jdbc.update("INSERT INTO user_email_verification_state (user_id, verified, occurred_at) VALUES (?, true, CURRENT_TIMESTAMP)", learner);
        return learner;
    }

    private KeyRow firstKeyHeldBy(UUID sale, String batchCode) {
        return batches.batchKeys(batchCode, null, sale, false, false, null, 0, 10).items().get(0);
    }

    @Test
    @DisplayName("A key moves from the pool to a sale, is handed to a student, and redeems once")
    void aKeyFollowsTheWholeJourney() {
        Batch batch = batches.createBatch(manager, product, 5, null, "Đợt thử");
        assertThat(batch.pool()).isEqualTo(5);
        assertThat(batch.byState().get(State.READY)).isEqualTo(5);

        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 3)));
        KeyRow held = firstKeyHeldBy(saleA, batch.code());
        String readyCode = batches.reveal(manager, held.id());
        UUID learner = verifiedLearner();

        // A key nobody has handed out is refused exactly like a code that does not exist, and it stays ready.
        assertThatThrownBy(() -> activation.execute(new ActivateKeyCommand(learner, readyCode, "learner-early")))
                .isInstanceOf(KeyNotIssuedException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM activation_keys WHERE id = ?", String.class, held.id())).isEqualTo("ACTIVE");

        SaleKeyUseCases.IssuedCode issued = sales.issue(saleA, product, "sale-a-1");
        assertThat(issued.code()).startsWith("IP-");
        assertThat(issued.key().state()).isEqualTo(State.ISSUED);
        assertThat(sales.reveal(saleA, issued.keyId())).isEqualTo(issued.code());

        // The same request again returns the same key and does not take another one.
        SaleKeyUseCases.IssuedCode replay = sales.issue(saleA, product, "sale-a-1");
        assertThat(replay.keyId()).isEqualTo(issued.keyId());
        assertThat(replay.code()).isEqualTo(issued.code());

        // Another sale cannot see or reveal a key it does not hold.
        assertThatThrownBy(() -> sales.reveal(saleB, issued.keyId()))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("KEY_NOT_FOUND"));

        ActivationResult result = activation.execute(new ActivateKeyCommand(learner, issued.code(), "learner-1"));
        assertThat(result.pointsGranted()).isEqualTo(100);
        assertThat(result.newBalance()).isEqualTo(100L);

        // A second redeem of the same code is refused, not counted again.
        assertThatThrownBy(() -> activation.execute(new ActivateKeyCommand(verifiedLearner(), issued.code(), "learner-2")))
                .isInstanceOf(KeyAlreadyRedeemedException.class)
                .hasMessage("Mã này đã được sử dụng.");
    }

    @Test
    @DisplayName("Revoking a batch takes back the unused keys and skips the one already redeemed")
    void revokeSkipsRedeemedKeys() {
        Batch batch = batches.createBatch(manager, product, 4, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 2)));
        SaleKeyUseCases.IssuedCode issued = sales.issue(saleA, product, "revoke-1");
        UUID learner = verifiedLearner();
        activation.execute(new ActivateKeyCommand(learner, issued.code(), "revoke-redeem"));

        KeyDistributionStore.AllocationResult result = batches.revokeUnusedInBatch(manager, batch.code(), null);

        assertThat(result.moved()).isEqualTo(3);
        assertThat(result.skipped()).isEqualTo(1);
        KeyRow redeemed = batches.batchKeys(batch.code(), State.ACTIVATED, null, false, false, null, 0, 10).items().get(0);
        assertThat(redeemed.state()).isEqualTo(State.ACTIVATED);
    }

    @Test
    @DisplayName("A revoked key is refused with the revoked reason")
    void revokedKeyIsRefusedAsRevoked() {
        Batch batch = batches.createBatch(manager, product, 1, null, null);
        KeyRow only = batches.batchKeys(batch.code(), null, null, false, false, null, 0, 10).items().get(0);
        String code = batches.reveal(manager, only.id());
        batches.revoke(manager, List.of(only.id()), "thử");

        assertThatThrownBy(() -> activation.execute(new ActivateKeyCommand(verifiedLearner(), code, "revoked-1")))
                .isInstanceOf(KeyRevokedException.class);
    }

    @Test
    @DisplayName("Asking for more keys than the pool holds is refused and moves nothing")
    void allocatingMoreThanThePoolIsRefused() {
        Batch batch = batches.createBatch(manager, product, 2, null, null);

        assertThatThrownBy(() -> batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 3))))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> {
                    KeyDistributionException refusal = (KeyDistributionException) error;
                    assertThat(refusal.code()).isEqualTo("NOT_ENOUGH_KEYS");
                    assertThat(refusal.details()).containsEntry("available", "2");
                });
        assertThat(batches.batch(batch.code()).pool()).isEqualTo(2);
    }

    @Test
    @DisplayName("Two handing-outs at the same moment give two different keys")
    void concurrentIssuesNeverShareAKey() throws Exception {
        Batch batch = batches.createBatch(manager, product, 2, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 2)));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<SaleKeyUseCases.IssuedCode> first = executor.submit(() -> {
                start.await();
                return sales.issue(saleA, product, "race-1");
            });
            Future<SaleKeyUseCases.IssuedCode> second = executor.submit(() -> {
                start.await();
                return sales.issue(saleA, product, "race-2");
            });
            start.countDown();
            UUID firstKey = first.get(30, TimeUnit.SECONDS).keyId();
            UUID secondKey = second.get(30, TimeUnit.SECONDS).keyId();
            assertThat(firstKey).isNotEqualTo(secondKey);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("A sale that is not active cannot be given keys")
    void lockedSaleGetsNoKeys() {
        UUID lockedSale = UUID.randomUUID();
        when(directory.sales()).thenReturn(List.of(new SaleAccount(lockedSale, "Sale khóa", false)));
        Batch batch = batches.createBatch(manager, product, 2, null, null);

        assertThatThrownBy(() -> batches.allocate(manager, batch.code(), List.of(new Allocation(lockedSale, 1))))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("SALE_NOT_ELIGIBLE"));
        assertThat(batches.batch(batch.code()).pool()).isEqualTo(2);
    }

    @Test
    @DisplayName("Every step that moves or shows keys leaves one audit row, and none of them holds a code")
    void eachStepIsAudited() {
        Batch batch = batches.createBatch(manager, product, 2, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 1)));
        SaleKeyUseCases.IssuedCode issued = sales.issue(saleA, product, "audit-1");
        sales.reveal(saleA, issued.keyId());
        batches.exportCsv(manager, batch.code());

        List<String> actions = jdbc.queryForList("SELECT action FROM key_audit_log ORDER BY created_at, id", String.class);
        assertThat(actions).contains("CREATE_BATCH", "ALLOCATE", "ISSUE", "REVEAL", "EXPORT");
        String details = jdbc.queryForObject("SELECT COALESCE(string_agg(detail::text, ' '), '') FROM key_audit_log", String.class);
        assertThat(details).doesNotContain("IP-");
    }

    @Test
    @DisplayName("The export lists every key of the batch with its full code and the sale that holds it")
    void exportListsEveryKey() {
        Batch batch = batches.createBatch(manager, product, 3, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 2)));

        String csv = batches.exportCsv(manager, batch.code());

        String[] lines = csv.split("\r\n");
        assertThat(lines).hasSize(4);
        assertThat(lines[0]).startsWith("Mã key,Sản phẩm,");
        assertThat(csv).contains("\"Sale A\"");
        assertThat(csv.lines().filter(line -> line.startsWith("\"IP-")).count()).isEqualTo(3);
    }

    @Test
    @DisplayName("A key the sale holds but has not handed out cannot be shown to anyone")
    void unissuedKeyCannotBeRevealedBySale() {
        Batch batch = batches.createBatch(manager, product, 1, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 1)));
        KeyRow held = firstKeyHeldBy(saleA, batch.code());

        assertThatThrownBy(() -> sales.reveal(saleA, held.id()))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("KEY_NOT_ISSUED"));
    }

    @Test
    @DisplayName("Giving out keys after the batch was revoked is refused and nothing moves")
    void allocationAfterRevokeIsRefused() {
        Batch batch = batches.createBatch(manager, product, 2, null, null);
        batches.revokeUnusedInBatch(manager, batch.code(), null);

        assertThatThrownBy(() -> batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 1))))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("NOT_ENOUGH_KEYS"));
        assertThat(batches.batch(batch.code()).pool()).isZero();
    }

    @Test
    @DisplayName("A key with no stored code is never handed out, and its code cannot be revealed")
    void keyWithoutStoredCodeIsSkippedAndNotRevealed() {
        Batch batch = batches.createBatch(manager, product, 1, null, null);
        KeyRow only = batches.batchKeys(batch.code(), null, null, false, false, null, 0, 10).items().get(0);
        jdbc.update("UPDATE activation_keys SET code_ciphertext = NULL WHERE id = ?", only.id());
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 1)));

        assertThatThrownBy(() -> sales.issue(saleA, product, "no-code-1"))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("NO_KEY_AVAILABLE"));
        assertThatThrownBy(() -> batches.reveal(manager, only.id()))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("CODE_NOT_STORED"));
    }

    @Test
    @DisplayName("An expired key is not handed out and shows as expired")
    void expiredKeyIsNotHandedOut() {
        Batch batch = batches.createBatch(manager, product, 1, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 1)));
        jdbc.update("UPDATE activation_keys SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 day' WHERE batch_code = ?", batch.code());

        assertThatThrownBy(() -> sales.issue(saleA, product, "expired-1"))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("NO_KEY_AVAILABLE"));
        assertThat(firstKeyHeldBy(saleA, batch.code()).state()).isEqualTo(State.EXPIRED);
    }

    @Test
    @DisplayName("A manager handing out a key directly gets the same key back for the same request, and it redeems")
    void managerDirectIssueIsIdempotentAndRedeems() {
        Batch batch = batches.createBatch(manager, product, 2, null, null);

        var first = batches.issueDirect(manager, product, "direct-1");
        var replay = batches.issueDirect(manager, product, "direct-1");

        assertThat(replay.keyId()).isEqualTo(first.keyId());
        assertThat(replay.code()).isEqualTo(first.code());
        assertThat(first.key().state()).isEqualTo(State.ISSUED);
        assertThat(batches.batch(batch.code()).pool()).isEqualTo(1);
        ActivationResult result = activation.execute(new ActivateKeyCommand(verifiedLearner(), first.code(), "direct-redeem"));
        assertThat(result.pointsGranted()).isEqualTo(100);
    }

    @Test
    @DisplayName("The pool and held filters split the READY keys of a batch by whether a sale holds them")
    void poolAndHeldFiltersSplitReadyKeys() {
        Batch batch = batches.createBatch(manager, product, 5, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 2)));

        var pool = batches.batchKeys(batch.code(), State.READY, null, true, false, null, 0, 10);
        var held = batches.batchKeys(batch.code(), State.READY, null, false, true, null, 0, 10);

        assertThat(pool.total()).isEqualTo(3);
        assertThat(held.total()).isEqualTo(2);
        assertThat(held.items()).allMatch(key -> saleA.equals(key.saleId()));
    }

    @Test
    @DisplayName("Reusing a request key for another product is refused")
    void reusedRequestKeyForAnotherProductIsRefused() {
        batches.createBatch(manager, product, 1, null, null);
        batches.issueDirect(manager, product, "reuse-1");
        UUID other = UUID.randomUUID();
        jdbc.update("INSERT INTO key_products (id, code, name, key_type, points_amount, status) VALUES (?, ?, ?, 'POINTS', 50, 'ACTIVE')",
                other, "P-" + other, "Gói 50");

        assertThatThrownBy(() -> batches.issueDirect(manager, other, "reuse-1"))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("IDEMPOTENCY_KEY_REUSED"));
    }

    @Test
    @DisplayName("A manager's direct hand-out with no key left says so")
    void directIssueWithNoKeyLeftSaysSo() {
        Batch batch = batches.createBatch(manager, product, 1, null, null);
        batches.revokeUnusedInBatch(manager, batch.code(), null);

        assertThatThrownBy(() -> batches.issueDirect(manager, product, "empty-1"))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("NO_KEY_AVAILABLE"));
    }

    @Test
    @DisplayName("Moving keys between sales leaves out the ones already handed to a student")
    void reassignSkipsIssuedKeys() {
        Batch batch = batches.createBatch(manager, product, 2, null, null);
        batches.allocate(manager, batch.code(), List.of(new Allocation(saleA, 2)));
        sales.issue(saleA, product, "skip-1");
        List<UUID> both = batches.batchKeys(batch.code(), null, saleA, false, false, null, 0, 10).items().stream()
                .map(KeyRow::id).toList();

        KeyDistributionStore.AllocationResult result = batches.reassign(manager, both, saleB);

        assertThat(result.moved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("A request naming more than a thousand keys is refused before it touches any key")
    void tooManyKeysInOneActionIsRefused() {
        List<UUID> many = java.util.stream.IntStream.range(0, 1001).mapToObj(i -> UUID.randomUUID()).toList();

        assertThatThrownBy(() -> batches.revoke(manager, many, "x"))
                .isInstanceOf(KeyDistributionException.class)
                .satisfies(error -> assertThat(((KeyDistributionException) error).code()).isEqualTo("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("A full thousand-key batch exports every key once, with its full code")
    void fullBatchExportsEveryKey() {
        Batch batch = batches.createBatch(manager, product, 1000, null, null);

        String csv = batches.exportCsv(manager, batch.code());

        assertThat(csv.split("\r\n")).hasSize(1001);
        assertThat(csv.lines().filter(line -> line.startsWith("\"IP-")).count()).isEqualTo(1000);
    }

    @Test
    @DisplayName("When the user service cannot answer, the sale list fails with that reason and nothing is changed")
    void unavailableUserServiceFailsTheSaleList() {
        when(directory.sales()).thenThrow(new DownstreamUnavailableException("user", "timeout", null));

        assertThatThrownBy(() -> batches.sales()).isInstanceOf(DownstreamUnavailableException.class);
    }

    @Test
    @DisplayName("A list returns at most a hundred keys a page, however large a size is asked")
    void listPagesAreCapped() {
        Batch batch = batches.createBatch(manager, product, 3, null, null);

        var page = batches.batchKeys(batch.code(), null, null, false, false, null, 0, 500);

        assertThat(page.items()).hasSize(3);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("A batch cannot be made for a product that does not exist")
    void unknownProductCannotHaveABatch() {
        assertThatThrownBy(() -> batches.createBatch(manager, UUID.randomUUID(), 5, null, null))
                .isInstanceOf(KeyProductNotFoundException.class);
    }

    @Test
    @DisplayName("Revoke and redeem of the same key at the same moment leave it in one state, never both")
    void revokeAndRedeemRaceLeavesOneOutcome() throws Exception {
        Batch batch = batches.createBatch(manager, product, 1, null, null);
        var issued = batches.issueDirect(manager, product, "race-redeem");
        UUID learner = verifiedLearner();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Object> redeem = executor.submit(() -> {
                start.await();
                try {
                    return activation.execute(new ActivateKeyCommand(learner, issued.code(), "race-learner"));
                } catch (RuntimeException refused) {
                    return refused;
                }
            });
            Future<KeyDistributionStore.AllocationResult> revoke = executor.submit(() -> {
                start.await();
                return batches.revoke(manager, List.of(issued.keyId()), "đua");
            });
            start.countDown();
            Object redeemed = redeem.get(30, TimeUnit.SECONDS);
            int revoked = revoke.get(30, TimeUnit.SECONDS).moved();

            boolean learnerWon = redeemed instanceof ActivationResult;
            assertThat(learnerWon ^ (revoked == 1)).as("exactly one of redeem and revoke takes effect").isTrue();
            String state = jdbc.queryForObject("SELECT status FROM activation_keys WHERE id = ?", String.class, issued.keyId());
            assertThat(state).isEqualTo(learnerWon ? "REDEEMED" : "REVOKED");
        } finally {
            executor.shutdownNow();
        }
    }
}
