package com.ieltspath.community.application.usecase;
import com.ieltspath.community.application.*;
import com.ieltspath.community.application.port.PublicProfileClient;
import com.ieltspath.community.application.result.*;
import com.ieltspath.community.application.query.PageQuery;
import com.ieltspath.community.domain.repository.*;
import com.ieltspath.community.domain.vo.*;
import com.ieltspath.community.domain.exception.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class CommunityQueriesUseCase {
 private final CommunityReadRepository reads;
 private final PublicProfileClient profiles;
 private final PostRepository posts;
 private final CommentRepository comments;
 public CursorResult<PostResult> feed(CommunityActor actor,UUID author,boolean bookmarked,boolean followingOnly,PostCategory category,String cursor,int size) {
 new PageQuery(0,size);
 var rows=reads.feed(actor.userId(),author,bookmarked,followingOnly,category,FeedCursor.parse(cursor),size+1);
 boolean more=rows.size()>size;
 var page=rows.subList(0,Math.min(rows.size(),size));
 var content=enrichPosts(actor,page.stream().map(p->PostResult.from(p,Map.of())).toList());
 String next=more?new FeedCursor(page.getLast().getCreatedAt(),page.getLast().getId()).encode():null;
 return new CursorResult<>(content,next,more,size);
 }
 public CursorResult<PostResult> searchFeed(CommunityActor actor,String query,boolean followingOnly,PostCategory category,String cursor,int size) {
 new PageQuery(0,size);
 var normalizedQuery=normalizeSearchQuery(query);
 var rows=reads.search(actor.userId(),followingOnly,category,normalizedQuery,FeedCursor.parse(cursor),size+1);
 boolean more=rows.size()>size;
 var page=rows.subList(0,Math.min(rows.size(),size));
 var content=enrichPosts(actor,page.stream().map(p->PostResult.from(p,Map.of())).toList());
 String next=more?new FeedCursor(page.getLast().getCreatedAt(),page.getLast().getId()).encode():null;
 return new CursorResult<>(content,next,more,size);
 }
 private String normalizeSearchQuery(String query) {
  if(query==null) throw new CommunityException("Search query is required");
  var normalized=query.trim().replaceAll("\\s+"," ");
  if(normalized.isEmpty()) throw new CommunityException("Search query must not be blank");
  if(normalized.length()>120) throw new CommunityException("Search query must be at most 120 characters");
  return normalized;
 }
 public List<PostResult> enrichPosts(CommunityActor actor,List<PostResult> content) {
 if(content.isEmpty())return List.of();
 var ids=content.stream().map(PostResult::id).collect(Collectors.toSet());
 var authors=content.stream().map(PostResult::authorId).collect(Collectors.toSet());
 var people=profiles.findProfiles(authors);
 var interactions=reads.interactions(ids,actor.userId());
 return content.stream().map(p->p.withDetails(people.getOrDefault(p.authorId(),PublicProfile.anonymous(p.authorId())),
 interactions.getOrDefault(p.id(),PostInteractions.empty()))).toList();
 }
 public List<CommentResult> enrichComments(List<CommentResult> content) {
 if(content.isEmpty())return List.of();
 var people=profiles.findProfiles(content.stream().map(CommentResult::authorId).collect(Collectors.toSet()));
 var counts=reads.replyCounts(content.stream().map(CommentResult::id).collect(Collectors.toSet()));
 return content.stream().map(c->c.withDetails(people.getOrDefault(c.authorId(),PublicProfile.anonymous(c.authorId())),counts.getOrDefault(c.id(),0L))).toList();
 }
 public PageResult<CommentResult> thread(CommunityActor actor,UUID post,UUID parent,PageQuery query) {
 if(parent!=null) {
 var comment=comments.findById(parent).orElseThrow(()->new CommunityNotFoundException("Comment not found"));
 if(comment.getStatus()!=ContentStatus.ACTIVE)throw new CommunityNotFoundException("Comment not found");
 post=comment.getPostId();
 parent=comment.getParentCommentId()==null?comment.getId():comment.getParentCommentId();
 }
 CommunityApplicationSupport.requireVisiblePost(posts,post,actor);
 var page=reads.thread(post,parent,query.page(),query.size());
 var result=PageResult.from(page,query.page(),query.size(),CommentResult::from);
 return new PageResult<>(enrichComments(result.content()),result.page(),result.size(),result.totalElements(),result.totalPages());
 }
 public CursorResult<PublicProfile> follows(UUID user,boolean followers,String cursor,int size) {
 new PageQuery(0,size);
 var rows=reads.follows(user,followers,FeedCursor.parse(cursor),size+1);
 boolean more=rows.size()>size;var page=rows.subList(0,Math.min(rows.size(),size));
 var people=profiles.findProfiles(page.stream().map(FollowEntry::userId).collect(Collectors.toSet()));
 var content=page.stream().map(p->people.getOrDefault(p.userId(),PublicProfile.anonymous(p.userId()))).toList();
 String next=more?new FeedCursor(page.getLast().createdAt(),page.getLast().userId()).encode():null;
 return new CursorResult<>(content,next,more,size);
 }
 public Relationship relationship(CommunityActor actor,UUID user) {return reads.relationship(actor.userId(),user);}
 public void requireVisibleUser(CommunityActor actor,UUID user) {
 if(!profiles.findProfiles(Set.of(user)).containsKey(user))throw new CommunityNotFoundException("User not found");
 }
}
