package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.BandStatisticsStore;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class GetBandHistoryUseCase {
    private final BandStatisticsStore store;
    private final AssessmentAttemptRepository attempts;
    public GetBandHistoryUseCase(BandStatisticsStore store,AssessmentAttemptRepository attempts) { this.store=store;this.attempts=attempts; }
    public record Result(String availability,Instant generatedAt,LocalDate dataFrom,String seriesKind,String skill,
            String eligibilityReason,List<BandStatisticsStore.Point> points,String nextCursor,Double baseline,Double latest,Double delta) {}
    public record StudyContext(UUID sourceId,UUID courseId) {}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Result execute(UUID user,LocalDate from,LocalDate to,String kind,String skill,String cursor,int limit,String timezone) {
        if(from==null || to==null || from.isAfter(to) || ChronoUnit.DAYS.between(from,to)>365
                || limit<1 || limit>100 || !Set.of("PLACEMENT","MOCK","OFFICIAL_PRACTICE").contains(kind)
                || !Set.of("LISTENING","READING","WRITING","SPEAKING","OVERALL").contains(skill))
            throw new IllegalArgumentException("Invalid band range, series, skill or page size");
        if(timezone==null || !ZoneId.getAvailableZoneIds().contains(timezone))
            throw new IllegalArgumentException("Invalid IANA timezone");
        ZoneId zone=ZoneId.of(timezone);
        var page=store.history(user,from.atStartOfDay(zone).toInstant(),to.plusDays(1).atStartOfDay(zone).toInstant(),
                kind,skill,decode(cursor),limit+1);
        var points=page.points().size()>limit?page.points().subList(0,limit):page.points();
        String next=page.points().size()>limit?encode(points.getLast()):null;
        return new Result("AVAILABLE",Instant.now(),page.dataFrom()==null?null:page.dataFrom().atZone(zone).toLocalDate(),
                kind,skill,page.latest()==null?"NO_ELIGIBLE_RESULTS":null,points,next,page.baseline(),page.latest(),
                page.baseline()==null || page.latest()==null?null:page.latest()-page.baseline());
    }
    private BandStatisticsStore.Cursor decode(String value) {
        if(value==null || value.isBlank()) return null;
        if(value.length()>200) throw new IllegalArgumentException("Invalid cursor");
        try {
            var parts=new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8).split("\\|",-1);
            if(parts.length!=2) throw new IllegalArgumentException();
            return new BandStatisticsStore.Cursor(Instant.parse(parts[0]),UUID.fromString(parts[1]));
        } catch(RuntimeException e) { throw new IllegalArgumentException("Invalid cursor"); }
    }
    private String encode(BandStatisticsStore.Point point) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((point.submittedAt()+"|"+point.attemptId()).getBytes(StandardCharsets.UTF_8));
    }
    @Transactional(readOnly=true)
    public StudyContext context(UUID user,String sourceType,UUID sourceId) {
        if(!"ASSESSMENT".equals(sourceType)) throw new IllegalArgumentException("Invalid source type");
        var a=attempts.findByIdAndUserId(sourceId,user).orElseThrow(()->new AssessmentNotFoundException("Assessment not found"));
        if(a.getStatus()!=AttemptStatus.IN_PROGRESS || a.getExpiresAt()!=null && !Instant.now().isBefore(a.getExpiresAt()))
            throw new AssessmentNotFoundException("Active assessment not found");
        return new StudyContext(sourceId,null);
    }
}
