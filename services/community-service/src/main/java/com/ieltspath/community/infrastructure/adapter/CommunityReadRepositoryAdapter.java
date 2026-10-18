package com.ieltspath.community.infrastructure.adapter;
import com.ieltspath.community.domain.repository.CommunityReadRepository;
import com.ieltspath.community.domain.aggregate.*;
import com.ieltspath.community.domain.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.namedparam.*;
import java.sql.*;
import java.util.*;
@Repository
public class CommunityReadRepositoryAdapter implements CommunityReadRepository {
 private final NamedParameterJdbcTemplate jdbc;
 private final ObjectMapper json;
 public CommunityReadRepositoryAdapter(NamedParameterJdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
 public List<Post> feed(UUID viewer,UUID author,boolean bookmarked,boolean followingOnly,PostCategory category,FeedCursor cursor,int limit) {
 var args=new MapSqlParameterSource().addValue("viewer",viewer).addValue("limit",limit);
 String sql="SELECT p.* FROM posts p WHERE p.status='ACTIVE'";
 if(author!=null){sql+=" AND p.author_id=:author";args.addValue("author",author);}
 if(bookmarked)sql+=" AND EXISTS(SELECT 1 FROM post_bookmarks b WHERE b.post_id=p.id AND b.user_id=:viewer)";
 if(followingOnly)sql+=" AND EXISTS(SELECT 1 FROM user_follows f WHERE f.follower_id=:viewer AND f.followee_id=p.author_id)";
 if(category!=null){sql+=" AND p.category=:category";args.addValue("category",category.name());}
 if(cursor!=null){sql+=" AND (p.created_at,p.id)<(:created,:id)";cursor(args,cursor);}
 return jdbc.query(sql+" ORDER BY p.created_at DESC,p.id DESC LIMIT :limit",args,(rs,n)->post(rs));
 }
 public List<Post> search(UUID viewer,boolean followingOnly,PostCategory category,String query,FeedCursor cursor,int limit) {
 var args=new MapSqlParameterSource().addValue("viewer",viewer).addValue("query",query).addValue("limit",limit);
 String sql="SELECT p.* FROM posts p WHERE p.status='ACTIVE' AND to_tsvector('simple',coalesce(p.title,'') || ' ' || p.body) @@ plainto_tsquery('simple',:query)";
 if(followingOnly)sql+=" AND EXISTS(SELECT 1 FROM user_follows f WHERE f.follower_id=:viewer AND f.followee_id=p.author_id)";
 if(category!=null){sql+=" AND p.category=:category";args.addValue("category",category.name());}
 if(cursor!=null){sql+=" AND (p.created_at,p.id)<(:created,:id)";cursor(args,cursor);}
 return jdbc.query(sql+" ORDER BY p.created_at DESC,p.id DESC LIMIT :limit",args,(rs,n)->post(rs));
 }
 private void cursor(MapSqlParameterSource args,FeedCursor cursor){args.addValue("created",Timestamp.from(cursor.createdAt())).addValue("id",cursor.id());}
 public Map<UUID,PostInteractions> interactions(Set<UUID> ids,UUID viewer) {
 if(ids.isEmpty())return Map.of();
 var args=new MapSqlParameterSource().addValue("ids",ids).addValue("viewer",viewer);
 String sql="""
 SELECT p.id,coalesce(r.likes,0) likes,coalesce(r.loves,0) loves,coalesce(r.helpful,0) helpful,coalesce(r.accurate,0) accurate,
 coalesce(c.total,0) comments,m.reaction_type,
 EXISTS(SELECT 1 FROM post_bookmarks b WHERE b.post_id=p.id AND b.user_id=:viewer) bookmarked,
 EXISTS(SELECT 1 FROM user_follows f WHERE f.follower_id=:viewer AND f.followee_id=p.author_id) following
 FROM posts p
 LEFT JOIN (SELECT post_id,count(*) FILTER(WHERE reaction_type='LIKE') likes,count(*) FILTER(WHERE reaction_type='LOVE') loves,
 count(*) FILTER(WHERE reaction_type='HELPFUL') helpful,count(*) FILTER(WHERE reaction_type='ACCURATE') accurate
 FROM post_reactions WHERE post_id IN (:ids) GROUP BY post_id) r ON r.post_id=p.id
 LEFT JOIN (SELECT c.post_id,count(*) total FROM comments c LEFT JOIN comments parent ON parent.id=c.parent_comment_id
 WHERE c.post_id IN (:ids) AND c.status='ACTIVE' AND (c.parent_comment_id IS NULL OR parent.status='ACTIVE') GROUP BY c.post_id) c ON c.post_id=p.id
 LEFT JOIN post_reactions m ON m.post_id=p.id AND m.user_id=:viewer WHERE p.id IN (:ids)
 """;
 Map<UUID,PostInteractions> result=new HashMap<>();
 jdbc.query(sql,args,rs->{
 var counts=new EnumMap<ReactionType,Long>(ReactionType.class);
 counts.put(ReactionType.LIKE,rs.getLong("likes"));counts.put(ReactionType.LOVE,rs.getLong("loves"));
 counts.put(ReactionType.HELPFUL,rs.getLong("helpful"));counts.put(ReactionType.ACCURATE,rs.getLong("accurate"));
 String mine=rs.getString("reaction_type");
 result.put(rs.getObject("id",UUID.class),new PostInteractions(counts,mine==null?null:ReactionType.valueOf(mine),rs.getLong("comments"),rs.getBoolean("bookmarked"),rs.getBoolean("following")));
 });return result;
 }
 public List<FollowEntry> follows(UUID user,boolean followers,FeedCursor cursor,int limit) {
 String owner=followers?"followee_id":"follower_id",other=followers?"follower_id":"followee_id";
 var args=new MapSqlParameterSource().addValue("user",user).addValue("limit",limit);
 String sql="SELECT "+other+" user_id,created_at FROM user_follows WHERE "+owner+"=:user";
 if(cursor!=null){sql+=" AND (created_at,"+other+")<(:created,:id)";cursor(args,cursor);}
 return jdbc.query(sql+" ORDER BY created_at DESC,"+other+" DESC LIMIT :limit",args,(rs,n)->new FollowEntry(rs.getObject("user_id",UUID.class),rs.getTimestamp("created_at").toInstant()));
 }
 public Relationship relationship(UUID viewer,UUID user) {
 return jdbc.queryForObject("""
 SELECT EXISTS(SELECT 1 FROM user_follows WHERE follower_id=:viewer AND followee_id=:user) following,
 (SELECT count(*) FROM user_follows WHERE followee_id=:user) followers,
 (SELECT count(*) FROM user_follows WHERE follower_id=:user) followees
 """,Map.of("viewer",viewer,"user",user),(rs,n)->new Relationship(rs.getBoolean("following"),rs.getLong("followers"),rs.getLong("followees")));
 }
 public CommunityPage<Comment> thread(UUID post,UUID parent,int page,int size) {
 var args=new MapSqlParameterSource().addValue("post",post).addValue("parent",parent).addValue("limit",size).addValue("offset",(long)page*size);
 String where=" FROM comments WHERE post_id=:post AND status='ACTIVE' AND "+(parent==null?"parent_comment_id IS NULL":"parent_comment_id=:parent");
 var rows=jdbc.query("SELECT *"+where+" ORDER BY created_at,id LIMIT :limit OFFSET :offset",args,(rs,n)->comment(rs));
 Long count=jdbc.queryForObject("SELECT count(*)"+where,args,Long.class);
 return new CommunityPage<>(rows,count==null?0:count);
 }
 public Map<UUID,Long> replyCounts(Set<UUID> ids) {
 if(ids.isEmpty())return Map.of();Map<UUID,Long> counts=new HashMap<>();
 jdbc.query("SELECT parent_comment_id,count(*) total FROM comments WHERE parent_comment_id IN (:ids) AND status='ACTIVE' GROUP BY parent_comment_id",Map.of("ids",ids),rs->{counts.put(rs.getObject("parent_comment_id",UUID.class),rs.getLong("total"));});return counts;
 }
 private Post post(ResultSet rs)throws SQLException {
 var post=new Post(rs.getObject("id",UUID.class),rs.getObject("author_id",UUID.class),PostCategory.valueOf(rs.getString("category")),rs.getString("title"),rs.getString("body"),ContentStatus.valueOf(rs.getString("status")),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant(),rs.getLong("version"));
 try {post.setMedia(json.readValue(rs.getString("image_urls"),new TypeReference<List<String>>(){}),rs.getString("video_url"),rs.getBigDecimal("target_band"));
 post.setBackground(rs.getString("background_style")==null?null:com.ieltspath.community.domain.vo.PostBackground.valueOf(rs.getString("background_style")));}
 catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Invalid stored post media",e);}return post;
 }
 private Comment comment(ResultSet rs)throws SQLException {
 return new Comment(rs.getObject("id",UUID.class),rs.getObject("post_id",UUID.class),rs.getObject("author_id",UUID.class),rs.getObject("parent_comment_id",UUID.class),rs.getString("body"),ContentStatus.valueOf(rs.getString("status")),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant(),rs.getLong("version"));
 }
}
