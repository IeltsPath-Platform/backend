package com.ieltspath.access.infrastructure.persistence.adapter;

import com.ieltspath.access.application.exception.KeyDistributionException;
import com.ieltspath.access.application.port.KeyDistributionStore;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SQL for batches, allocations and handing out. Locks are taken inside each transaction, in the same order everywhere:
 * the batch row first, then key rows. Reading a key's state is one CASE expression so every screen agrees on it.
 */
@Repository
public class JdbcKeyDistributionStore implements KeyDistributionStore {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_NEW_CODE_ATTEMPTS = 10;

    /** READY, ISSUED, ACTIVATED, REVOKED or EXPIRED for the alias {@code k}; a key past its expiry shows EXPIRED. */
    private static final String STATE = """
            (CASE WHEN k.status = 'REDEEMED' THEN 'ACTIVATED'
                  WHEN k.status = 'REVOKED' THEN 'REVOKED'
                  WHEN k.status = 'EXPIRED' OR (k.expires_at IS NOT NULL AND k.expires_at < CURRENT_TIMESTAMP) THEN 'EXPIRED'
                  WHEN k.issued_at IS NOT NULL THEN 'ISSUED'
                  ELSE 'READY' END)""";

    /** A key that can still be given out: unused, not issued, not past its date. */
    private static final String UNISSUED = """
            k.status = 'ACTIVE' AND k.issued_at IS NULL AND (k.expires_at IS NULL OR k.expires_at > CURRENT_TIMESTAMP)""";

    private static final String KEY_COLUMNS = """
            SELECT k.id, k.batch_code, COALESCE(k.seq, 0) AS seq, k.product_id, p.name AS product_name, k.code_hint,
                   %s AS state, k.sale_id, k.issued_at, k.redeemed_at, k.revoked_at, k.expires_at, k.note,
                   (k.code_ciphertext IS NOT NULL) AS code_stored
            FROM activation_keys k JOIN key_products p ON p.id = k.product_id
            """.formatted(STATE);

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcKeyDistributionStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- batches ----------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public String createBatch(UUID actor, UUID productId, int quantity, LocalDate expiresOn, Instant expiresAt,
                              String label, Supplier<NewKey> codes) {
        LocalDate today = LocalDate.now(ZONE);
        String prefix = "B-" + DateTimeFormatter.ofPattern("yyMM").format(today) + "-";
        // One batch number is handed out at a time per month, so two managers never get the same code.
        jdbc.queryForList("SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtext(:name))) AS acquired",
                new MapSqlParameterSource("name", "key-batch:" + prefix), Integer.class);
        Integer next = jdbc.queryForObject("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(code FROM 8) AS INT)), 0) + 1 FROM key_batches WHERE code LIKE :like
                """, new MapSqlParameterSource("like", prefix + "%"), Integer.class);
        String code = prefix + String.format("%03d", next == null ? 1 : next);
        String batchLabel = label == null || label.isBlank()
                ? "Đợt " + DateTimeFormatter.ofPattern("MM/yyyy").format(today) : label.strip();
        jdbc.update("""
                INSERT INTO key_batches (code, label, product_id, quantity, expires_on, created_by)
                VALUES (:code, :label, :product, :quantity, :expiresOn, :actor)
                """, new MapSqlParameterSource().addValue("code", code).addValue("label", batchLabel)
                .addValue("product", productId).addValue("quantity", quantity).addValue("expiresOn", expiresOn)
                .addValue("actor", actor));

        String insert = """
                INSERT INTO activation_keys (id, product_id, code_hash, code_hint, status, expires_at, created_by,
                                             batch_code, seq, code_ciphertext)
                VALUES (gen_random_uuid(), :product, :hash, :hint, 'ACTIVE', :expiresAt, :actor, :batch, :seq, :cipher)
                ON CONFLICT (code_hash) DO NOTHING
                """;
        List<SqlParameterSource> params = new ArrayList<>();
        for (int seq = 1; seq <= quantity; seq++) params.add(keyParams(codes.get(), productId, expiresAt, actor, code, seq));
        int[] inserted = jdbc.batchUpdate(insert, params.toArray(SqlParameterSource[]::new));
        for (int index = 0; index < inserted.length; index++) {
            // A hash that already exists (practically never) gets another code for the same sequence number.
            for (int attempt = 0; inserted[index] == 0; attempt++) {
                if (attempt >= MAX_NEW_CODE_ATTEMPTS) throw new IllegalStateException("Không sinh được mã key không trùng");
                inserted[index] = jdbc.update(insert, keyParams(codes.get(), productId, expiresAt, actor, code, index + 1));
            }
        }
        audit("CREATE_BATCH", actor, null, code, quantity, "{}");
        return code;
    }

    private static MapSqlParameterSource keyParams(NewKey key, UUID productId, Instant expiresAt, UUID actor, String batch, int seq) {
        return new MapSqlParameterSource().addValue("product", productId).addValue("hash", key.codeHash())
                .addValue("hint", key.codeHint()).addValue("expiresAt", expiresAt == null ? null : java.sql.Timestamp.from(expiresAt))
                .addValue("actor", actor).addValue("batch", batch).addValue("seq", seq).addValue("cipher", key.ciphertext());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean productExists(UUID productId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM key_products WHERE id = :id",
                new MapSqlParameterSource("id", productId), Integer.class);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Batch> listBatches(UUID productId, String month, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("product", productId)
                .addValue("month", month).addValue("size", size).addValue("offset", page * size);
        String where = """
                WHERE (CAST(:product AS uuid) IS NULL OR b.product_id = CAST(:product AS uuid))
                  AND (CAST(:month AS text) IS NULL OR to_char(b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') = CAST(:month AS text))
                """;
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM key_batches b " + where, params, Long.class);
        List<BatchHeader> headers = jdbc.query("""
                SELECT b.code, b.label, b.product_id, p.name AS product_name, b.quantity, b.expires_on, b.created_at, b.created_by
                FROM key_batches b JOIN key_products p ON p.id = b.product_id
                """ + where + " ORDER BY b.created_at DESC, b.code DESC LIMIT :size OFFSET :offset", params, JdbcKeyDistributionStore::header);
        return new Page<>(withCounts(headers), total == null ? 0 : total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Batch> findBatch(String code) {
        List<BatchHeader> headers = jdbc.query("""
                SELECT b.code, b.label, b.product_id, p.name AS product_name, b.quantity, b.expires_on, b.created_at, b.created_by
                FROM key_batches b JOIN key_products p ON p.id = b.product_id WHERE b.code = :code
                """, new MapSqlParameterSource("code", code), JdbcKeyDistributionStore::header);
        return withCounts(headers).stream().findFirst();
    }

    private record BatchHeader(String code, String label, UUID productId, String productName, int quantity,
                               LocalDate expiresOn, Instant createdAt, UUID createdBy) {}

    private static BatchHeader header(ResultSet rs, int row) throws SQLException {
        java.sql.Date expires = rs.getDate("expires_on");
        return new BatchHeader(rs.getString("code"), rs.getString("label"), rs.getObject("product_id", UUID.class),
                rs.getString("product_name"), rs.getInt("quantity"), expires == null ? null : expires.toLocalDate(),
                instant(rs, "created_at"), rs.getObject("created_by", UUID.class));
    }

    /** One grouped query for all the batches of a page, so the list never asks once per batch. */
    private List<Batch> withCounts(List<BatchHeader> headers) {
        if (headers.isEmpty()) return List.of();
        Map<String, Map<State, Integer>> byBatch = new HashMap<>();
        Map<String, Integer> pool = new HashMap<>();
        jdbc.query("SELECT k.batch_code, " + STATE + " AS state, (k.sale_id IS NULL) AS pooled, COUNT(*) AS n "
                        + "FROM activation_keys k WHERE k.batch_code IN (:codes) GROUP BY k.batch_code, state, pooled",
                new MapSqlParameterSource("codes", headers.stream().map(BatchHeader::code).toList()), rs -> {
                    String batch = rs.getString("batch_code");
                    State state = State.valueOf(rs.getString("state"));
                    int n = rs.getInt("n");
                    byBatch.computeIfAbsent(batch, key -> new EnumMap<>(State.class)).merge(state, n, Integer::sum);
                    if (state == State.READY && rs.getBoolean("pooled")) pool.merge(batch, n, Integer::sum);
                });
        return headers.stream().map(h -> new Batch(h.code(), h.label(), h.productId(), h.productName(), h.quantity(),
                h.expiresOn(), h.createdAt(), h.createdBy(), complete(byBatch.get(h.code())), pool.getOrDefault(h.code(), 0))).toList();
    }

    private static Map<State, Integer> complete(Map<State, Integer> counts) {
        Map<State, Integer> all = new EnumMap<>(State.class);
        for (State state : State.values()) all.put(state, counts == null ? 0 : counts.getOrDefault(state, 0));
        return all;
    }

    // ---- reading keys -----------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<KeyRow> listKeys(KeyFilter filter, int page, int size) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("size", size).addValue("offset", page * size);
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (filter.batchCode() != null) { where.append(" AND k.batch_code = :batch"); params.addValue("batch", filter.batchCode()); }
        else where.append(" AND k.batch_code IS NOT NULL");
        if (filter.state() != null) { where.append(" AND ").append(STATE).append(" = :state"); params.addValue("state", filter.state().name()); }
        if (filter.saleId() != null) { where.append(" AND k.sale_id = :sale"); params.addValue("sale", filter.saleId()); }
        if (filter.pool()) where.append(" AND k.sale_id IS NULL");
        if (filter.held()) where.append(" AND k.sale_id IS NOT NULL");
        if (filter.productId() != null) { where.append(" AND k.product_id = :product"); params.addValue("product", filter.productId()); }
        if (filter.hint() != null && !filter.hint().isBlank()) {
            where.append(" AND UPPER(k.code_hint) LIKE :hint");
            params.addValue("hint", "%" + filter.hint().strip().toUpperCase().replace("%", "").replace("_", "") + "%");
        }
        if (filter.issuedOnly()) where.append(" AND k.issued_at IS NOT NULL");
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM activation_keys k" + where, params, Long.class);
        String order = filter.newestFirst() ? " ORDER BY k.issued_at DESC NULLS LAST, k.seq, k.id" : " ORDER BY k.batch_code, k.seq, k.id";
        List<KeyRow> rows = jdbc.query(KEY_COLUMNS + where + order + " LIMIT :size OFFSET :offset", params, JdbcKeyDistributionStore::keyRow);
        return new Page<>(rows, total == null ? 0 : total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KeyRow> findKey(UUID keyId) {
        return jdbc.query(KEY_COLUMNS + " WHERE k.id = :id", new MapSqlParameterSource("id", keyId),
                JdbcKeyDistributionStore::keyRow).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> ciphertextOf(UUID keyId) {
        List<String> stored = jdbc.query("SELECT code_ciphertext FROM activation_keys WHERE id = :id",
                new MapSqlParameterSource("id", keyId), (rs, row) -> rs.getString("code_ciphertext"));
        return stored.isEmpty() ? Optional.empty() : Optional.of(stored.get(0) == null ? "" : stored.get(0));
    }

    private static KeyRow keyRow(ResultSet rs, int row) throws SQLException {
        return new KeyRow(rs.getObject("id", UUID.class), rs.getString("batch_code"), rs.getInt("seq"),
                rs.getObject("product_id", UUID.class), rs.getString("product_name"), rs.getString("code_hint"),
                State.valueOf(rs.getString("state")), rs.getObject("sale_id", UUID.class), instant(rs, "issued_at"),
                instant(rs, "redeemed_at"), instant(rs, "revoked_at"), instant(rs, "expires_at"), rs.getString("note"),
                rs.getBoolean("code_stored"));
    }

    // ---- overview ---------------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<Counts> countsByBatch(String month) {
        return counts("""
                SELECT b.code AS key, b.label AS label, %s AS state, COUNT(*) AS n
                FROM activation_keys k JOIN key_batches b ON b.code = k.batch_code
                WHERE (CAST(:month AS text) IS NULL OR to_char(b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') = CAST(:month AS text))
                GROUP BY b.code, b.label, state ORDER BY b.code DESC
                """.formatted(STATE), new MapSqlParameterSource("month", month));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Counts> countsByProduct(String month) {
        return counts("""
                SELECT p.id::text AS key, p.name AS label, %s AS state, COUNT(*) AS n
                FROM activation_keys k JOIN key_batches b ON b.code = k.batch_code JOIN key_products p ON p.id = k.product_id
                WHERE (CAST(:month AS text) IS NULL OR to_char(b.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM') = CAST(:month AS text))
                GROUP BY p.id, p.name, state ORDER BY p.name
                """.formatted(STATE), new MapSqlParameterSource("month", month));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleHolding> holdings() {
        return jdbc.query("""
                SELECT sale_id, COUNT(*) FILTER (WHERE status <> 'REVOKED') AS held,
                       COUNT(*) FILTER (WHERE issued_at IS NOT NULL AND status <> 'REVOKED') AS issued,
                       COUNT(*) FILTER (WHERE status = 'REDEEMED') AS activated
                FROM activation_keys WHERE sale_id IS NOT NULL GROUP BY sale_id
                """, (rs, row) -> new SaleHolding(rs.getObject("sale_id", UUID.class), rs.getInt("held"),
                rs.getInt("issued"), rs.getInt("activated")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Counts> saleStockByProduct(UUID saleId) {
        return counts("""
                SELECT p.id::text AS key, p.name AS label, %s AS state, COUNT(*) AS n
                FROM activation_keys k JOIN key_products p ON p.id = k.product_id
                WHERE k.sale_id = :sale GROUP BY p.id, p.name, state ORDER BY p.name
                """.formatted(STATE), new MapSqlParameterSource("sale", saleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Counts> saleStockByBatch(UUID saleId) {
        return counts("""
                SELECT b.code AS key, p.name AS label, %s AS state, COUNT(*) AS n
                FROM activation_keys k JOIN key_batches b ON b.code = k.batch_code JOIN key_products p ON p.id = k.product_id
                WHERE k.sale_id = :sale GROUP BY b.code, p.name, state ORDER BY b.code DESC
                """.formatted(STATE), new MapSqlParameterSource("sale", saleId));
    }

    private List<Counts> counts(String sql, SqlParameterSource params) {
        Map<String, String> labels = new LinkedHashMap<>();
        Map<String, Map<State, Integer>> byKey = new LinkedHashMap<>();
        jdbc.query(sql, params, rs -> {
            String key = rs.getString("key");
            labels.put(key, rs.getString("label"));
            byKey.computeIfAbsent(key, k -> new EnumMap<>(State.class)).merge(State.valueOf(rs.getString("state")), rs.getInt("n"), Integer::sum);
        });
        return byKey.entrySet().stream().map(e -> new Counts(e.getKey(), labels.get(e.getKey()), complete(e.getValue()))).toList();
    }

    // ---- moving and removing ----------------------------------------------------------------------------------

    @Override
    @Transactional
    public void allocate(UUID actor, String batchCode, List<Allocation> allocations) {
        // The batch row is the lock: two managers allocating the same batch take turns and each sees the true stock.
        List<Integer> locked = jdbc.query("SELECT 1 FROM key_batches WHERE code = :code FOR UPDATE",
                new MapSqlParameterSource("code", batchCode), (rs, row) -> 1);
        if (locked.isEmpty()) throw KeyDistributionException.batchNotFound();
        String free = " FROM activation_keys k WHERE k.batch_code = :batch AND k.sale_id IS NULL AND " + UNISSUED;
        Integer available = jdbc.queryForObject("SELECT COUNT(*)" + free, new MapSqlParameterSource("batch", batchCode), Integer.class);
        int wanted = allocations.stream().mapToInt(Allocation::quantity).sum();
        if (available == null || wanted > available) throw KeyDistributionException.notEnoughKeys(available == null ? 0 : available);
        for (Allocation allocation : allocations) {
            List<UUID> ids = jdbc.query("SELECT k.id" + free + " ORDER BY k.seq LIMIT :n FOR UPDATE",
                    new MapSqlParameterSource("batch", batchCode).addValue("n", allocation.quantity()),
                    (rs, row) -> rs.getObject("id", UUID.class));
            if (ids.size() < allocation.quantity()) throw KeyDistributionException.notEnoughKeys(ids.size());
            jdbc.update("UPDATE activation_keys SET sale_id = :sale WHERE id IN (:ids)",
                    new MapSqlParameterSource("sale", allocation.saleId()).addValue("ids", ids));
            jdbc.update("""
                    INSERT INTO key_allocations (batch_code, action, from_sale_id, to_sale_id, quantity, allocated_by)
                    VALUES (:batch, 'ALLOCATE', NULL, :sale, :n, :actor)
                    """, new MapSqlParameterSource().addValue("batch", batchCode).addValue("sale", allocation.saleId())
                    .addValue("n", allocation.quantity()).addValue("actor", actor));
        }
        audit("ALLOCATE", actor, null, batchCode, wanted, "{\"sales\":" + allocations.size() + "}");
    }

    @Override
    @Transactional
    public AllocationResult reassign(UUID actor, List<UUID> keyIds, UUID toSale) {
        List<UUID> ids = distinct(keyIds);
        List<Move> moves = jdbc.query("""
                WITH picked AS (
                    SELECT k.id, k.batch_code, k.sale_id FROM activation_keys k
                    WHERE k.id IN (:ids) AND k.batch_code IS NOT NULL AND %s AND k.sale_id IS DISTINCT FROM :to
                    ORDER BY k.id FOR UPDATE
                ), moved AS (
                    UPDATE activation_keys k SET sale_id = :to FROM picked WHERE k.id = picked.id
                    RETURNING picked.batch_code AS batch_code, picked.sale_id AS from_sale
                )
                SELECT batch_code, from_sale, COUNT(*) AS n FROM moved GROUP BY batch_code, from_sale
                """.formatted(UNISSUED), new MapSqlParameterSource("ids", ids).addValue("to", toSale),
                (rs, row) -> new Move(rs.getString("batch_code"), rs.getObject("from_sale", UUID.class), rs.getInt("n")));
        int moved = 0;
        for (Move move : moves) {
            moved += move.count();
            jdbc.update("""
                    INSERT INTO key_allocations (batch_code, action, from_sale_id, to_sale_id, quantity, allocated_by)
                    VALUES (:batch, :action, :from, :to, :n, :actor)
                    """, new MapSqlParameterSource().addValue("batch", move.batch())
                    .addValue("action", move.fromSale() == null ? "ALLOCATE" : "REASSIGN").addValue("from", move.fromSale())
                    .addValue("to", toSale).addValue("n", move.count()).addValue("actor", actor));
        }
        if (moved > 0) audit("REASSIGN", actor, null, null, moved, "{\"requested\":" + ids.size() + "}");
        return new AllocationResult(moved, ids.size() - moved);
    }

    @Override
    @Transactional
    public AllocationResult withdraw(UUID actor, List<UUID> keyIds) {
        List<UUID> ids = distinct(keyIds);
        List<Move> moves = jdbc.query("""
                WITH picked AS (
                    SELECT k.id, k.batch_code, k.sale_id FROM activation_keys k
                    WHERE k.id IN (:ids) AND k.batch_code IS NOT NULL AND %s AND k.sale_id IS NOT NULL
                    ORDER BY k.id FOR UPDATE
                ), moved AS (
                    UPDATE activation_keys k SET sale_id = NULL FROM picked WHERE k.id = picked.id
                    RETURNING picked.batch_code AS batch_code, picked.sale_id AS from_sale
                )
                SELECT batch_code, from_sale, COUNT(*) AS n FROM moved GROUP BY batch_code, from_sale
                """.formatted(UNISSUED), new MapSqlParameterSource("ids", ids),
                (rs, row) -> new Move(rs.getString("batch_code"), rs.getObject("from_sale", UUID.class), rs.getInt("n")));
        int moved = 0;
        for (Move move : moves) {
            moved += move.count();
            jdbc.update("""
                    INSERT INTO key_allocations (batch_code, action, from_sale_id, to_sale_id, quantity, allocated_by)
                    VALUES (:batch, 'WITHDRAW', :from, NULL, :n, :actor)
                    """, new MapSqlParameterSource().addValue("batch", move.batch()).addValue("from", move.fromSale())
                    .addValue("n", move.count()).addValue("actor", actor));
        }
        if (moved > 0) audit("WITHDRAW", actor, null, null, moved, "{\"requested\":" + ids.size() + "}");
        return new AllocationResult(moved, ids.size() - moved);
    }

    private record Move(String batch, UUID fromSale, int count) {}

    @Override
    @Transactional
    public AllocationResult revoke(UUID actor, List<UUID> keyIds, String note) {
        List<UUID> ids = distinct(keyIds);
        // Conditional on status: a key a learner redeemed a moment ago is not touched, and one that is being redeemed makes
        // this statement wait for that transaction (activation locks the same row) and then skip it.
        int revoked = jdbc.update("""
                UPDATE activation_keys SET status = 'REVOKED', revoked_at = CURRENT_TIMESTAMP, note = :note
                WHERE id IN (:ids) AND batch_code IS NOT NULL AND status = 'ACTIVE'
                """, new MapSqlParameterSource("ids", ids).addValue("note", note));
        if (revoked > 0) audit("REVOKE", actor, null, null, revoked, "{\"requested\":" + ids.size() + "}");
        return new AllocationResult(revoked, ids.size() - revoked);
    }

    @Override
    @Transactional
    public AllocationResult revokeUnusedInBatch(UUID actor, String batchCode, String note) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM activation_keys WHERE batch_code = :batch",
                new MapSqlParameterSource("batch", batchCode), Integer.class);
        if (total == null || total == 0) throw KeyDistributionException.batchNotFound();
        int revoked = jdbc.update("""
                UPDATE activation_keys SET status = 'REVOKED', revoked_at = CURRENT_TIMESTAMP, note = :note
                WHERE batch_code = :batch AND status = 'ACTIVE'
                """, new MapSqlParameterSource("batch", batchCode).addValue("note", note));
        if (revoked > 0) audit("REVOKE", actor, null, batchCode, revoked, "{}");
        return new AllocationResult(revoked, total - revoked);
    }

    // ---- handing out ------------------------------------------------------------------------------------------

    @Override
    @Transactional
    public IssuedKey issue(UUID actor, UUID holder, UUID productId, String idempotencyKey) {
        int claimed = jdbc.update("""
                INSERT INTO key_issue_requests (idempotency_key, actor_id, product_id) VALUES (:key, :actor, :product)
                ON CONFLICT (idempotency_key) DO NOTHING
                """, new MapSqlParameterSource().addValue("key", idempotencyKey).addValue("actor", actor).addValue("product", productId));
        if (claimed == 0) return replay(actor, productId, idempotencyKey);

        String holderCondition = holder == null ? "k.sale_id IS NULL" : "k.sale_id = :holder";
        MapSqlParameterSource params = new MapSqlParameterSource("product", productId).addValue("holder", holder);
        // SKIP LOCKED: two people handing out at once get two different keys; neither waits for the other.
        List<IssuedKey> picked = jdbc.query("""
                SELECT k.id, k.code_ciphertext, k.code_hint, k.batch_code FROM activation_keys k
                WHERE k.product_id = :product AND k.batch_code IS NOT NULL AND k.code_ciphertext IS NOT NULL AND %s AND %s
                ORDER BY k.expires_at NULLS LAST, k.batch_code, k.seq
                LIMIT 1 FOR UPDATE SKIP LOCKED
                """.formatted(holderCondition, UNISSUED), params,
                (rs, row) -> new IssuedKey(rs.getObject("id", UUID.class), rs.getString("code_ciphertext"),
                        rs.getString("code_hint"), rs.getString("batch_code")));
        if (picked.isEmpty()) throw KeyDistributionException.noKeyAvailable();
        IssuedKey key = picked.get(0);
        jdbc.update("UPDATE activation_keys SET issued_at = CURRENT_TIMESTAMP WHERE id = :id", new MapSqlParameterSource("id", key.keyId()));
        jdbc.update("UPDATE key_issue_requests SET key_id = :key WHERE idempotency_key = :idem",
                new MapSqlParameterSource("key", key.keyId()).addValue("idem", idempotencyKey));
        audit("ISSUE", actor, key.keyId(), key.batchCode(), 1, "{}");
        return key;
    }

    private IssuedKey replay(UUID actor, UUID productId, String idempotencyKey) {
        List<Replay> found = jdbc.query("""
                SELECT r.actor_id, r.product_id, r.key_id, k.code_ciphertext, k.code_hint, k.batch_code
                FROM key_issue_requests r LEFT JOIN activation_keys k ON k.id = r.key_id WHERE r.idempotency_key = :key
                """, new MapSqlParameterSource("key", idempotencyKey), (rs, row) -> new Replay(
                rs.getObject("actor_id", UUID.class), rs.getObject("product_id", UUID.class), rs.getObject("key_id", UUID.class),
                rs.getString("code_ciphertext"), rs.getString("code_hint"), rs.getString("batch_code")));
        Replay first = found.isEmpty() ? null : found.get(0);
        if (first == null || !first.actor().equals(actor) || !first.product().equals(productId) || first.keyId() == null) {
            throw new KeyDistributionException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                    "Mã chống gửi trùng này đã được dùng cho một yêu cầu khác.");
        }
        return new IssuedKey(first.keyId(), first.ciphertext(), first.hint(), first.batch());
    }

    private record Replay(UUID actor, UUID product, UUID keyId, String ciphertext, String hint, String batch) {}

    // ---- audit and export -------------------------------------------------------------------------------------

    @Override
    @Transactional
    public void auditReveal(UUID actor, UUID keyId) {
        audit("REVEAL", actor, keyId, null, 1, "{}");
    }

    @Override
    @Transactional
    public List<ExportRow> exportBatch(UUID actor, String batchCode) {
        List<ExportRow> rows = jdbc.query("SELECT k.seq, k.code_ciphertext, k.code_hint, " + STATE + " AS state, k.sale_id, "
                        + "k.issued_at, k.redeemed_at, k.expires_at, p.name AS product_name "
                        + "FROM activation_keys k JOIN key_products p ON p.id = k.product_id WHERE k.batch_code = :batch ORDER BY k.seq",
                new MapSqlParameterSource("batch", batchCode), (rs, row) -> new ExportRow(rs.getInt("seq"),
                        rs.getString("code_ciphertext"), rs.getString("code_hint"), State.valueOf(rs.getString("state")),
                        rs.getObject("sale_id", UUID.class), instant(rs, "issued_at"), instant(rs, "redeemed_at"),
                        instant(rs, "expires_at"), rs.getString("product_name")));
        if (rows.isEmpty()) throw KeyDistributionException.batchNotFound();
        audit("EXPORT", actor, null, batchCode, rows.size(), "{}");
        return rows;
    }

    /** One row per action. The detail is a small JSON of counts and never contains a key code. */
    private void audit(String action, UUID actor, UUID keyId, String batchCode, int quantity, String detail) {
        jdbc.update("""
                INSERT INTO key_audit_log (action, actor_id, key_id, batch_code, quantity, detail)
                VALUES (:action, :actor, :key, :batch, :quantity, CAST(:detail AS jsonb))
                """, new MapSqlParameterSource().addValue("action", action).addValue("actor", actor).addValue("key", keyId)
                .addValue("batch", batchCode).addValue("quantity", quantity).addValue("detail", detail));
    }

    private static List<UUID> distinct(List<UUID> ids) {
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
