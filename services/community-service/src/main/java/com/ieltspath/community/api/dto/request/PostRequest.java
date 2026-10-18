package com.ieltspath.community.api.dto.request;
import com.ieltspath.community.domain.vo.PostCategory;
import jakarta.validation.constraints.*;
import java.util.List;
import java.math.BigDecimal;
public record PostRequest(@NotNull PostCategory category, @Size(max=200) String title,
 @NotBlank @Size(max=20000) String body, @Size(max=4) List<@NotBlank @Size(max=500) String> imageUrls,
 @Size(max=500) String videoUrl, @DecimalMin("0.0") @DecimalMax("9.0") BigDecimal targetBand,
 com.ieltspath.community.domain.vo.PostBackground background) {}
