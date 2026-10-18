package com.ieltspath.learning.application.service;

import com.ieltspath.learning.domain.repository.KnowledgePointCatalogRepository;
import com.ieltspath.learning.domain.repository.SnapshotRepository;
import com.ieltspath.learning.domain.repository.SnapshotRepository.Activity;
import com.ieltspath.learning.domain.repository.SnapshotRepository.Period;
import com.ieltspath.learning.domain.repository.SnapshotRepository.ProfileRow;
import com.ieltspath.learning.domain.repository.SnapshotRepository.Snapshot;
import com.ieltspath.learning.domain.service.BandEstimator;
import com.ieltspath.learning.domain.service.SkillRollup;
import com.ieltspath.learning.domain.vo.KnowledgePointCatalogEntry;
import com.ieltspath.learning.domain.vo.MasteryTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the weekly and monthly snapshots of the learners who were active lately. A snapshot takes the learner's mastery
 * as it stands when the run happens and their answers in the period. Learners are read in batches, and a run can be
 * repeated: a snapshot is replaced by learner and start.
 */
@Component
public class SnapshotService {
    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);
    static final int BATCH = 500;
    static final int FOCUS_POINTS = 3;
    static final int ACTIVE_WINDOW_DAYS = 14;

    private final SnapshotRepository repository;
    private final KnowledgePointCatalogRepository catalog;
    private final ZoneId zone;
    private final int keepWeeks;
    private final int keepMonths;
    private final Clock clock;

    @Autowired
    public SnapshotService(SnapshotRepository repository, KnowledgePointCatalogRepository catalog,
                           @Value("${learning.snapshots.zone:Asia/Ho_Chi_Minh}") String zone,
                           @Value("${learning.snapshots.keep-weeks:12}") int keepWeeks,
                           @Value("${learning.snapshots.keep-months:36}") int keepMonths) {
        this(repository, catalog, ZoneId.of(zone), keepWeeks, keepMonths, Clock.systemUTC());
    }

    SnapshotService(SnapshotRepository repository, KnowledgePointCatalogRepository catalog, ZoneId zone, int keepWeeks,
                    int keepMonths, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.zone = zone;
        this.keepWeeks = keepWeeks;
        this.keepMonths = keepMonths;
        this.clock = clock;
    }

    /** The Monday of the week before the one {@code now} falls in, in the snapshot timezone. */
    public LocalDate lastWeekStart() {
        return LocalDate.now(clock.withZone(zone)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(1);
    }

    /** The first day of the month before the one {@code now} falls in. */
    public LocalDate lastMonthStart() {
        return LocalDate.now(clock.withZone(zone)).withDayOfMonth(1).minusMonths(1);
    }

    /** Snapshots the week starting {@code weekStart} for recently active learners; returns how many were written. */
    public int snapshotWeek(LocalDate weekStart) {
        return snapshot(Period.WEEK, weekStart, weekStart.plusWeeks(1));
    }

    public int snapshotMonth(LocalDate monthStart) {
        return snapshot(Period.MONTH, monthStart, monthStart.plusMonths(1));
    }

    /** Drops snapshots older than the retention: twelve weeks and thirty-six months. */
    public int purge() {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return repository.purgeBefore(Period.WEEK, today.minusWeeks(keepWeeks))
                + repository.purgeBefore(Period.MONTH, today.minusMonths(keepMonths));
    }

    private int snapshot(Period period, LocalDate start, LocalDate end) {
        long began = System.nanoTime();
        Instant from = start.atStartOfDay(zone).toInstant();
        Instant to = end.atStartOfDay(zone).toInstant();
        List<UUID> active = repository.usersActiveSince(clock.instant().minus(Duration.ofDays(ACTIVE_WINDOW_DAYS)));
        Map<UUID, KnowledgePointDirectory.Entry> described = active.isEmpty() ? Map.of() : describe();
        int written = 0;
        for (int offset = 0; offset < active.size(); offset += BATCH) {
            List<UUID> batch = active.subList(offset, Math.min(offset + BATCH, active.size()));
            Map<UUID, Activity> activity = repository.activity(batch, from, to);
            Map<UUID, List<ProfileRow>> profiles = repository.profiles(batch);
            Map<UUID, BigDecimal> placements = repository.placementBands(batch);
            List<Snapshot> snapshots = new ArrayList<>();
            for (UUID userId : batch) {
                snapshots.add(build(userId, start, activity.get(userId), profiles.getOrDefault(userId, List.of()),
                        placements.get(userId), described));
            }
            repository.save(period, snapshots);
            written += snapshots.size();
        }
        log.info("Snapshots written: period={} start={} learners={} elapsedMs={}", period, start, written,
                Duration.ofNanos(System.nanoTime() - began).toMillis());
        return written;
    }

    /**
     * The skill and band of every point, read from the catalog the learners' own requests keep fresh. A run has no
     * learner request, hence no gateway token to call Content with.
     */
    private Map<UUID, KnowledgePointDirectory.Entry> describe() {
        Map<UUID, KnowledgePointDirectory.Entry> described = new java.util.HashMap<>();
        for (KnowledgePointCatalogEntry entry : catalog.findAll()) {
            described.put(entry.knowledgePointId(), new KnowledgePointDirectory.Entry(null, null,
                    entry.skill() == null ? null : entry.skill().name(), entry.band()));
        }
        return described;
    }

    private Snapshot build(UUID userId, LocalDate start, Activity activity, List<ProfileRow> rows, BigDecimal placement,
                           Map<UUID, KnowledgePointDirectory.Entry> described) {
        List<SkillRollup.Item> items = new ArrayList<>();
        List<BandEstimator.Point> points = new ArrayList<>();
        for (ProfileRow row : rows) {
            KnowledgePointDirectory.Entry meta = described.getOrDefault(row.knowledgePointId(),
                    KnowledgePointDirectory.Entry.UNKNOWN);
            if (meta.skill() == null) continue;
            String skill = meta.skill().toUpperCase(Locale.ROOT);
            items.add(new SkillRollup.Item(skill, row.tier(), row.mastery(), row.evidenceCount()));
            points.add(new BandEstimator.Point(row.knowledgePointId(), skill, meta.band(), row.tier(), row.mastery()));
        }
        List<SkillRollup.Summary> skills = SkillRollup.summarize(items);
        Map<String, Integer> tiers = new java.util.LinkedHashMap<>();
        for (MasteryTier tier : MasteryTier.values()) tiers.put(tier.name(), 0);
        for (ProfileRow row : rows) tiers.merge(row.tier().name(), 1, Integer::sum);
        List<UUID> focus = rows.stream()
                .filter(row -> row.tier() == MasteryTier.PRIORITY || row.tier() == MasteryTier.CONSOLIDATE)
                .sorted(Comparator.comparingDouble(ProfileRow::mastery)).limit(FOCUS_POINTS)
                .map(ProfileRow::knowledgePointId).toList();
        BigDecimal band = BandEstimator.estimate(points, placement).overall();
        Activity seen = activity == null ? new Activity(0, 0, 0) : activity;
        return new Snapshot(userId, start, skills, tiers, seen.answered(), seen.correct(),
                (int) ((seen.activeMs() + 30_000) / 60_000), focus, band);
    }
}
