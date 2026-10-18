package com.ieltspath.community.application.command;
import com.ieltspath.community.domain.vo.PostCategory;
import java.util.List;
import java.math.BigDecimal;
public record EditPostCommand(java.util.UUID postId, PostCategory category, String title, String body, List<String> imageUrls, String videoUrl, BigDecimal targetBand) {
 public EditPostCommand(java.util.UUID postId, PostCategory category, String title, String body) { this(postId, category,title,body,List.of(),null,null); }
}
