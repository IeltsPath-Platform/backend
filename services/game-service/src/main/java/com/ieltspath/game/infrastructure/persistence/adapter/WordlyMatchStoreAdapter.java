package com.ieltspath.game.infrastructure.persistence.adapter;
import com.ieltspath.game.application.port.*;
import com.ieltspath.game.application.result.CurrentWordResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.*;
import java.time.Instant;
@Repository
public class WordlyMatchStoreAdapter implements WordlyMatchStore {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    private final JdbcTemplate jdbc;
    public WordlyMatchStoreAdapter(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void initialize(UUID matchId,List<WordlyWordProvider.Word> words,List<UUID> players,Instant now,boolean timed) {
        // JDBC assignments reference rows just created through JPA in the same transaction.
        if (entityManager != null) entityManager.flush();
        jdbc.update("""
            UPDATE game_match_players p SET player_display_name=rm.display_name,
                player_slot_number=rm.slot_number,score_reached_at=p.joined_at
            FROM game_room_members rm WHERE p.match_id=? AND p.room_member_id=rm.id
            """, matchId);
        jdbc.update("UPDATE game_matches SET wordly_initial_word_count=? WHERE id=?",words.size(),matchId);
        List<Object[]> rows=new ArrayList<>();UUID first=null;
        for(int i=0;i<words.size();i++) {
            var w=words.get(i);UUID id=UUID.randomUUID();if(i==0)first=id;
            rows.add(new Object[]{id,matchId,i+1,w.sourceWordId(),w.answer(),w.meaning()});
        }
        jdbc.batchUpdate("INSERT INTO game_match_words(id,match_id,sequence_number,source_word_id,answer,meaning) VALUES (?,?,?,?,?,?)",rows);
        List<Object[]> assignments=new ArrayList<>();
        for(UUID player:players)assignments.add(new Object[]{UUID.randomUUID(),matchId,player,first,Timestamp.from(now),timed?Timestamp.from(now.plusSeconds(180)):null});
        jdbc.batchUpdate("INSERT INTO game_player_words(id,match_id,match_player_id,match_word_id,started_at,deadline_at) VALUES (?,?,?,?,?,?)",assignments);
    }
    @org.springframework.transaction.annotation.Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public String prepareDictionary(List<WordlyWordProvider.Word> words) {
        var sorted=new TreeMap<String,WordlyWordProvider.Word>();
        for(var word:words)sorted.putIfAbsent(word.answer(),word);
        if(sorted.isEmpty())throw new IllegalArgumentException("Dictionary cannot be empty");
        String version;
        try {
            var digest=java.security.MessageDigest.getInstance("SHA-256");
            for(var word:sorted.values()) {
                for(var value:List.of(word.answer(),word.sourceWordId().toString(),word.meaning())) {
                    byte[] bytes=value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
                    digest.update(bytes);
                }
            }
            version=HexFormat.of().formatHex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        int inserted=jdbc.update("INSERT INTO game_wordly_dictionaries(version,word_count) VALUES (?,?) ON CONFLICT DO NOTHING",version,sorted.size());
        // A concurrent insert waits for the creator's commit, so readers never see partial versions.
        if(inserted!=0) {
            List<Object[]> rows=new ArrayList<>();
            int position=0;
            var entries=new ArrayList<>(sorted.values());
            Collections.shuffle(entries,new Random(Long.parseUnsignedLong(version.substring(0,16),16)));
            for(var w:entries)rows.add(new Object[]{version,++position,w.sourceWordId(),w.answer(),w.meaning()});
            jdbc.batchUpdate("INSERT INTO game_wordly_dictionary_words(version,position,source_word_id,answer,meaning) VALUES (?,?,?,?,?)",rows);
        }
        return version;
    }
    public void attachDictionary(UUID matchId,String version) {
        jdbc.update("UPDATE game_matches SET wordly_dictionary_version=? WHERE id=?",version,matchId);
    }
    private List<CurrentWordResult.Guess> guesses(UUID playerWordId) {
        return jdbc.query("SELECT attempt_number,guess,letter_results::text FROM game_guesses WHERE player_word_id=? AND NOT is_delete ORDER BY attempt_number",
            (rs,n)->{
                try { return new CurrentWordResult.Guess(rs.getInt(1),rs.getString(2),List.of(new com.fasterxml.jackson.databind.ObjectMapper().readValue(rs.getString(3),String[].class))); }
                catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Invalid saved letter results",e);}
            },playerWordId);
    }
    private com.ieltspath.game.application.result.WordlyHintResult hint(java.sql.ResultSet rs) throws java.sql.SQLException {
        if(rs.getObject("hint_column")==null)return null;
        try {
            return new com.ieltspath.game.application.result.WordlyHintResult(rs.getObject("id",UUID.class),rs.getString("hint_guess"),
                List.of(new com.fasterxml.jackson.databind.ObjectMapper().readValue(rs.getString("hint_letters"),String[].class)),
                new com.ieltspath.game.application.result.WordlyHintResult.RevealedCell(rs.getInt("hint_column"),rs.getString("hint_letter"),"CORRECT"),true);
        } catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Invalid saved hint",e);}
    }
    public Optional<CurrentWordResult> current(UUID matchId,UUID userId) {
        return jdbc.query("""
            SELECT pw.id,w.sequence_number,m.word_limit,w.meaning,length(w.answer) AS columns,
                pw.attempts_used,pw.deadline_at,pw.status AS word_status,p.status AS player_status,
                p.lives_remaining,p.score,p.current_streak,p.best_streak,m.word_time_limit_seconds,pw.hint_column,pw.hint_letter,src.guess AS hint_guess,src.letter_results::text AS hint_letters
            FROM game_player_words pw JOIN game_match_words w ON w.id=pw.match_word_id
            JOIN game_match_players p ON p.id=pw.match_player_id JOIN game_matches m ON m.id=pw.match_id
            LEFT JOIN game_guesses src ON src.id=pw.hint_source_guess_id
            WHERE m.id=? AND p.user_id=? AND m.game_type='WORDLY'
                AND NOT m.is_delete AND NOT p.is_delete AND NOT pw.is_delete AND NOT w.is_delete
                AND pw.status='ACTIVE'
            """, (rs,n)->new CurrentWordResult(rs.getObject("id",UUID.class),rs.getInt("sequence_number"),
                rs.getObject("word_limit",Integer.class),rs.getString("meaning"),rs.getInt("columns"),rs.getInt("columns")+1,
                rs.getInt("attempts_used"),rs.getObject("word_time_limit_seconds")!=null,
                rs.getTimestamp("deadline_at")==null?null:rs.getTimestamp("deadline_at").toInstant(),
                rs.getString("word_status"),rs.getString("player_status"),rs.getInt("lives_remaining"),rs.getLong("score"),rs.getInt("current_streak"),rs.getInt("best_streak"),guesses(rs.getObject("id",UUID.class)),hint(rs)),matchId,userId).stream().findFirst();
    }
}
