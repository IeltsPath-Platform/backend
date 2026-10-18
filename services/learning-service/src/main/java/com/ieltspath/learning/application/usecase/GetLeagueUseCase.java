package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.DisplayNameClient;
import com.ieltspath.learning.domain.repository.LeagueRepository;
import com.ieltspath.learning.domain.repository.LeagueRepository.HistoryRow;
import com.ieltspath.learning.domain.repository.LeagueRepository.Member;
import com.ieltspath.learning.domain.service.LeagueRules;
import com.ieltspath.learning.domain.vo.LeagueTier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The learner's league: the table of their group this week and the weeks they played. The table carries display names
 * only: no email and no user id of anyone else.
 */
@Service
public class GetLeagueUseCase {
    public static final int MAX_HISTORY_WEEKS = 26;

    /** {@code displayName} is null when the User Service could not give it; the client shows a placeholder. */
    public record Entry(int rank, String displayName, int weeklyXp, boolean you, String zone) {}

    /** {@code joined} is false until the learner earns XP this week; the table is then empty. */
    public record LeagueView(String tier, LocalDate weekStart, Instant closesAt, boolean joined, List<Entry> table,
                             int groupSize, int promoteTop, int demoteFrom) {}

    public record HistoryEntry(LocalDate weekStart, String tier, int weeklyXp, Integer finalRank, String outcome) {}

    private final LeagueRepository repository;
    private final DisplayNameClient names;
    private final Clock clock = Clock.systemUTC();

    public GetLeagueUseCase(LeagueRepository repository, DisplayNameClient names) {
        this.repository = repository;
        this.names = names;
    }

    @Transactional(readOnly = true)
    public LeagueView view(UUID userId) {
        LocalDate week = LeagueRules.weekStart(clock.instant());
        List<Member> members = repository.table(userId, week);
        int level = repository.weekTier(userId, week).or(() -> repository.tierOf(userId)).orElse(LeagueTier.LOWEST);
        // One call for the whole table, never one per row.
        Map<UUID, String> displayNames = names.names(members.stream().map(Member::userId).toList());
        List<Entry> table = members.stream().map(member -> new Entry(member.rank(), displayNames.get(member.userId()),
                member.weeklyXp(), member.userId().equals(userId),
                LeagueRules.zone(member.rank(), members.size()).name())).toList();
        return new LeagueView(LeagueTier.ofLevel(level).name(), week, LeagueRules.closesAt(week), !members.isEmpty(),
                table, LeagueRules.GROUP_SIZE, LeagueRules.PROMOTE_TOP, LeagueRules.DEMOTE_FROM);
    }

    @Transactional(readOnly = true)
    public List<HistoryEntry> history(UUID userId, int weeks) {
        if (weeks < 1 || weeks > MAX_HISTORY_WEEKS) {
            throw new LearningRequestException(400, "INVALID_WEEKS", "weeks must be between 1 and " + MAX_HISTORY_WEEKS);
        }
        return repository.history(userId, weeks).stream().map(this::toEntry).toList();
    }

    private HistoryEntry toEntry(HistoryRow row) {
        return new HistoryEntry(row.weekStart(), LeagueTier.ofLevel(row.tier()).name(), row.weeklyXp(),
                row.finalRank(), row.outcome());
    }
}
