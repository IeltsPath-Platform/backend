package com.ieltspath.community.domain.vo;
import java.util.Map;
public record PostInteractions(Map<ReactionType,Long> reactionCounts,ReactionType myReaction,long commentCount,boolean bookmarkedByMe,boolean followingAuthor) {
 public static PostInteractions empty(){return new PostInteractions(Map.of(),null,0,false,false);}
}
