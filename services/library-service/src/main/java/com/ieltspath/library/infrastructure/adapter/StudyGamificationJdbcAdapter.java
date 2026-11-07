package com.ieltspath.library.infrastructure.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.library.application.port.StudyGamificationRepository;
import com.ieltspath.library.application.result.StudyGamificationResult.PurchaseResult;
import com.ieltspath.library.config.StudyGamificationProperties;
import com.ieltspath.library.domain.study.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Repository @RequiredArgsConstructor
public class StudyGamificationJdbcAdapter implements StudyGamificationRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final StudyGamificationProperties properties;
    public Optional<Profile> profile(UUID user) {
        return jdbc.query("select * from vocabulary_study_profiles where user_id=:user",Map.of("user",user),(r,n)->new Profile(user,r.getString("timezone"),r.getInt("total_xp"),r.getInt("level"),
            StudyHearts.restore(user,r.getInt("hearts"),r.getInt("max_hearts"),r.getTimestamp("hearts_regen_at").toInstant(),Duration.ofHours(properties.getHearts().getRegenHours())),StudyUnitJdbcAdapter.instant(r,"max_hearts_checked_at"))).stream().findFirst();
    }
    public void createProfile(UUID user,int maximum,Instant now,String timezone) {
        jdbc.update("insert into vocabulary_study_profiles(user_id,timezone,hearts,max_hearts,hearts_regen_at,updated_at) values(:user,:zone,:max,:max,:now,:now) on conflict(user_id) do nothing",Map.of("user",user,"zone",timezone,"max",maximum,"now",Timestamp.from(now)));
    }
    public void saveProfile(Profile profile,Instant now) {
        var p=new HashMap<String,Object>();p.put("user",profile.userId());p.put("zone",profile.timezone());p.put("xp",profile.totalXp());p.put("level",profile.level());p.put("hearts",profile.hearts().hearts());p.put("max",profile.hearts().max());p.put("regen",Timestamp.from(profile.hearts().lastRegenAt()));p.put("checked",profile.maxHeartsCheckedAt()==null?null:Timestamp.from(profile.maxHeartsCheckedAt()));p.put("now",Timestamp.from(now));
        jdbc.update("update vocabulary_study_profiles set timezone=:zone,total_xp=:xp,level=:level,hearts=:hearts,max_hearts=:max,hearts_regen_at=:regen,max_hearts_checked_at=:checked,updated_at=:now where user_id=:user",p);
    }
    public StudyStreakState streak(UUID user) {
        return jdbc.query("select * from vocabulary_study_streaks where user_id=:user",Map.of("user",user),(r,n)->new StudyStreakState(r.getInt("current_days"),r.getInt("longest_days"),r.getObject("last_active_date",LocalDate.class),r.getObject("streak_start",LocalDate.class),r.getObject("evaluated_through",LocalDate.class),r.getBoolean("freeze_available"),r.getObject("freeze_eligible_from",LocalDate.class),r.getObject("frozen_run_end",LocalDate.class),r.getInt("freeze_run"),r.getInt("lost_streak_days"))).stream().findFirst().orElse(StudyStreakState.empty());
    }
    public void saveStreak(UUID user,StudyStreakState s,Instant now) {
        var p=new HashMap<String,Object>();p.put("user",user);p.put("current",s.currentDays());p.put("longest",s.longestDays());p.put("last",s.lastActive());p.put("start",s.streakStart());p.put("through",s.evaluatedThrough());p.put("available",s.freezeAvailable());p.put("eligible",s.freezeEligibleFrom());p.put("end",s.frozenRunEnd());p.put("run",s.freezeRun());p.put("lost",s.lostStreakDays());p.put("now",Timestamp.from(now));
        jdbc.update("""
            insert into vocabulary_study_streaks(user_id,current_days,longest_days,last_active_date,streak_start,evaluated_through,freeze_available,freeze_eligible_from,frozen_run_end,freeze_run,lost_streak_days,updated_at)
            values(:user,:current,:longest,:last,:start,:through,:available,:eligible,:end,:run,:lost,:now)
            on conflict(user_id) do update set current_days=:current,longest_days=:longest,last_active_date=:last,streak_start=:start,evaluated_through=:through,
              freeze_available=:available,freeze_eligible_from=:eligible,frozen_run_end=:end,freeze_run=:run,lost_streak_days=:lost,updated_at=:now
            """,p);
    }
    public int award(UUID user,String source,UUID unit,int amount,LocalDate date,Instant now) {
        if(amount==0)return 0;
        int rows=jdbc.update("insert into vocabulary_study_xp_ledger(user_id,source,source_ref,amount,local_date,created_at) values(:user,:source,:ref,:amount,:date,:now) on conflict(user_id,source,source_ref) do nothing",Map.of("user",user,"source",source,"ref",unit.toString(),"amount",amount,"date",date,"now",Timestamp.from(now)));
        return rows==1?amount:0;
    }
    public Optional<Purchase> purchase(UUID request) {
        return jdbc.query("select * from vocabulary_study_purchases where request_id=:request",Map.of("request",request),(r,n)->new Purchase(request,r.getObject("user_id",UUID.class),r.getString("kind"),r.getInt("amount"),r.getString("status"),r.getString("result")==null?null:read(r.getString("result")))).stream().findFirst();
    }
    public void createPurchase(UUID request,UUID user,String kind,int amount,Instant now) {
        jdbc.update("insert into vocabulary_study_purchases(request_id,user_id,kind,amount,status,created_at,updated_at) values(:request,:user,:kind,:amount,'PENDING',:now,:now) on conflict(request_id) do nothing",Map.of("request",request,"user",user,"kind",kind,"amount",amount,"now",Timestamp.from(now)));
    }
    public void deletePendingPurchase(UUID request){jdbc.update("delete from vocabulary_study_purchases where request_id=:request and status='PENDING'",Map.of("request",request));}
    public void finishPurchase(UUID request,UUID ledger,long balance,PurchaseResult result,Instant now) {
        jdbc.update("update vocabulary_study_purchases set status='DONE',access_ledger_id=:ledger,balance_after=:balance,result=cast(:result as jsonb),updated_at=:now where request_id=:request",Map.of("request",request,"ledger",ledger,"balance",balance,"result",json(result),"now",Timestamp.from(now)));
    }
    private String json(PurchaseResult result){try{return mapper.writeValueAsString(result);}catch(Exception e){throw new IllegalStateException("Cannot serialize study purchase",e);}}
    private PurchaseResult read(String value){try{return mapper.readValue(value,PurchaseResult.class);}catch(Exception e){throw new IllegalStateException("Cannot read study purchase",e);}}
}
