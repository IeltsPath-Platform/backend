package com.ieltspath.library.infrastructure.adapter;

import com.ieltspath.library.application.port.StudyUnitRepository;
import com.ieltspath.library.domain.study.StudyWordSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class StudyUnitJdbcAdapter implements StudyUnitRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final VocabularyAudioUrlResolver audio;
    private static final String SELECT_UNIT="""
        SELECT u.*,r.revision,r.title,r.description,r.introduction_vi,r.exercise_types,r.graded_step_count,r.kind revision_kind
        FROM vocabulary_study_units u JOIN vocabulary_study_unit_revisions r
          ON r.unit_id=u.id AND r.revision=%s
        """;
    private static final String SELECT_WORD="""
        SELECT s.id sense_id,i.id item_id,i.lemma,s.part_of_speech,s.vietnamese_meaning,s.english_definition,
          s.example_sentence,s.example_translation,s.image_url,i.ipa,i.ipa_uk,i.ipa_us,i.audio_uk_reference,
          i.audio_us_reference,i.pronunciation_audio_reference,(s.status='ACTIVE' AND i.status='ACTIVE') active
        """;
    public boolean activeTopic(UUID topic) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from vocabulary_topics where id=:id and status='ACTIVE')",Map.of("id",topic),Boolean.class));
    }
    public boolean sensesExist(List<UUID> ids) {
        Integer found=jdbc.queryForObject("select count(*) from vocabulary_senses where id in (:ids)",Map.of("ids",ids),Integer.class);
        return found!=null && found==ids.size();
    }
    public void lockTopic(UUID topic) {
        // Lock the existing catalog topic first: also serializes creation of the version row and new unit ordering.
        jdbc.queryForList("select id from vocabulary_topics where id=:id for update",Map.of("id",topic));
        jdbc.update("insert into vocabulary_study_topic_versions(topic_id) values(:id) on conflict do nothing",Map.of("id",topic));
        jdbc.queryForList("select topic_id from vocabulary_study_topic_versions where topic_id=:id for update",Map.of("id",topic));
    }
    public Optional<Unit> find(UUID id,boolean draft,boolean lock) {
        String revision=draft?"coalesce(u.draft_revision,u.published_revision)":"u.published_revision";
        return jdbc.query(SELECT_UNIT.formatted(revision)+" where u.id=:id"+(lock?" for update of u":""),Map.of("id",id),(r,n)->unit(r)).stream().findFirst();
    }
    public List<Unit> list(UUID topic,boolean author) {
        return jdbc.query(SELECT_UNIT.formatted(author?"coalesce(u.draft_revision,u.published_revision)":"u.published_revision")+
            " where u.topic_id=:topic"+(author?"":" and u.archived_at is null")+
            " order by (u.archived_at is not null),u.sort_order,u.id",Map.of("topic",topic),(r,n)->unit(r));
    }
    public List<StudyWordSnapshot> words(UUID id,int revision,boolean activeOnly) {
        return jdbc.query(SELECT_WORD+",w.sort_order from vocabulary_study_unit_words w join vocabulary_senses s on s.id=w.sense_id join vocabulary_items i on i.id=s.vocabulary_item_id where w.unit_id=:id and w.revision=:revision"+
            (activeOnly?" and s.status='ACTIVE' and i.status='ACTIVE'":"")+" order by w.sort_order",Map.of("id",id,"revision",revision),(r,n)->word(r));
    }
    public Map<UUID,List<StudyWordSnapshot>> words(List<Unit> units,boolean activeOnly) {
        if(units.isEmpty()) return Map.of();
        var parameters=new HashMap<String,Object>();var tuples=new ArrayList<String>();
        for(int i=0;i<units.size();i++) {
            parameters.put("id"+i,units.get(i).id());parameters.put("revision"+i,units.get(i).revision());
            tuples.add("(:id"+i+",:revision"+i+")");
        }
        Map<UUID,List<StudyWordSnapshot>> result=new HashMap<>();
        jdbc.query(SELECT_WORD+",w.unit_id,w.sort_order from vocabulary_study_unit_words w join vocabulary_senses s on s.id=w.sense_id join vocabulary_items i on i.id=s.vocabulary_item_id where (w.unit_id,w.revision) in ("+String.join(",",tuples)+")"+
            (activeOnly?" and s.status='ACTIVE' and i.status='ACTIVE'":"")+" order by w.unit_id,w.sort_order",parameters,(org.springframework.jdbc.core.RowCallbackHandler)r->{
                result.computeIfAbsent(r.getObject("unit_id",UUID.class),ignored->new ArrayList<>()).add(word(r));
            });
        return result;
    }
    public List<StudyWordSnapshot> distractors(UUID topic) {
        return jdbc.query(SELECT_WORD+",0 sort_order from vocabulary_senses s join vocabulary_items i on i.id=s.vocabulary_item_id where s.topic_id=:topic and s.status='ACTIVE' and i.status='ACTIVE' order by s.id limit 200",Map.of("topic",topic),(r,n)->word(r));
    }
    public int version(UUID topic) {
        var values=jdbc.queryForList("select curriculum_version from vocabulary_study_topic_versions where topic_id=:id",Map.of("id",topic),Integer.class);
        return values.isEmpty()?0:values.getFirst();
    }
    public void bumpVersion(UUID topic,Instant now) {
        jdbc.update("insert into vocabulary_study_topic_versions(topic_id,curriculum_version,updated_at) values(:id,1,:now) on conflict(topic_id) do update set curriculum_version=vocabulary_study_topic_versions.curriculum_version+1,updated_at=:now",Map.of("id",topic,"now",Timestamp.from(now)));
    }
    public UUID create(UUID topic,UUID author,Draft draft,Instant now) {
        UUID id=UUID.randomUUID();
        jdbc.update("insert into vocabulary_study_units(id,topic_id,sort_order,kind,draft_revision,created_at,updated_at) select :id,:topic,coalesce(max(sort_order),0)+1,:kind,1,:now,:now from vocabulary_study_units where topic_id=:topic and archived_at is null",Map.of("id",id,"topic",topic,"kind",draft.kind(),"now",Timestamp.from(now)));
        saveDraft(id,1,author,draft,now);
        return id;
    }
    public void update(Unit unit,UUID author,Draft draft,Instant now) {
        int revision=unit.draftRevision()!=null?unit.draftRevision():unit.publishedRevision()+1;
        saveDraft(unit.id(),revision,author,draft,now);
        jdbc.update("update vocabulary_study_units set draft_revision=:r,updated_at=:now where id=:id",Map.of("id",unit.id(),"r",revision,"now",Timestamp.from(now)));
    }
    private void saveDraft(UUID id,int revision,UUID author,Draft draft,Instant now) {
        var p=new HashMap<String,Object>(); p.put("id",id); p.put("revision",revision); p.put("title",draft.title());p.put("description",draft.description());p.put("intro",draft.introductionVi());p.put("author",author);p.put("now",Timestamp.from(now));
        p.put("kind",draft.kind());
        jdbc.update("insert into vocabulary_study_unit_revisions(unit_id,revision,title,description,introduction_vi,kind,created_by,created_at) values(:id,:revision,:title,:description,:intro,:kind,:author,:now) on conflict(unit_id,revision) do update set title=:title,description=:description,introduction_vi=:intro,kind=:kind",p);
        jdbc.update("delete from vocabulary_study_unit_words where unit_id=:id and revision=:revision",p);
        int order=0;
        for(UUID sense:draft.senseIds()) {
            jdbc.update("insert into vocabulary_study_unit_words(unit_id,revision,sense_id,sort_order) values(:id,:revision,:sense,:sort)",Map.of("id",id,"revision",revision,"sense",sense,"sort",++order));
        }
    }
    public void publish(Unit unit,List<String> types,Integer graded,Instant now) {
        var p=new HashMap<String,Object>(); p.put("id",unit.id());p.put("r",unit.draftRevision());p.put("now",Timestamp.from(now));p.put("types",types==null?null:"{"+String.join(",",types)+"}");p.put("graded",graded);
        jdbc.update("update vocabulary_study_unit_revisions set published_at=:now,exercise_types=cast(:types as text[]),graded_step_count=:graded where unit_id=:id and revision=:r",p);
        p.put("kind",unit.kind());
        jdbc.update("update vocabulary_study_units set published_revision=draft_revision,draft_revision=null,kind=:kind,updated_at=:now where id=:id",p);
    }
    public void archive(UUID id,Instant now) { jdbc.update("update vocabulary_study_units set archived_at=:now,updated_at=:now where id=:id",Map.of("id",id,"now",Timestamp.from(now))); }
    public void reorder(List<UUID> ids,Instant now) {
        for(int n=0;n<ids.size();n++) jdbc.update("update vocabulary_study_units set sort_order=:n,updated_at=:now where id=:id",Map.of("id",ids.get(n),"n",n+1,"now",Timestamp.from(now)));
    }
    private Unit unit(ResultSet r) throws SQLException {
        Array types=r.getArray("exercise_types");
        return new Unit(r.getObject("id",UUID.class),r.getObject("topic_id",UUID.class),r.getInt("sort_order"),r.getString("revision_kind"),r.getObject("published_revision",Integer.class),r.getObject("draft_revision",Integer.class),instant(r,"archived_at"),instant(r,"updated_at"),r.getInt("revision"),r.getString("title"),r.getString("description"),r.getString("introduction_vi"),types==null?List.of():List.of((String[])types.getArray()),r.getObject("graded_step_count",Integer.class));
    }
    private StudyWordSnapshot word(ResultSet r) throws SQLException {
        String uk=r.getString("ipa_uk"),us=r.getString("ipa_us");
        if(uk==null && us==null) uk=us=r.getString("ipa");
        return new StudyWordSnapshot(r.getObject("item_id",UUID.class),r.getObject("sense_id",UUID.class),r.getInt("sort_order"),r.getString("lemma"),r.getString("part_of_speech"),r.getString("vietnamese_meaning"),r.getString("english_definition"),uk,us,audio.resolve(r.getString("audio_uk_reference")),audio.resolve(r.getString("audio_us_reference")),r.getString("image_url"),r.getString("example_sentence"),r.getString("example_translation"),r.getBoolean("active"));
    }
    static Instant instant(ResultSet r,String name) throws SQLException { var t=r.getTimestamp(name);return t==null?null:t.toInstant(); }
}
