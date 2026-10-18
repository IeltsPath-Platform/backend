package com.ieltspath.community.application.command;
import com.ieltspath.community.domain.vo.PostCategory;
import java.util.List;
import java.math.BigDecimal;
public record CreatePostCommand(PostCategory category, String title, String body, List<String> imageUrls, String videoUrl, BigDecimal targetBand, com.ieltspath.community.domain.vo.PostBackground background) {
 public CreatePostCommand(PostCategory category, String title, String body) { this(category,title,body,List.of(),null,null,null); }
}
