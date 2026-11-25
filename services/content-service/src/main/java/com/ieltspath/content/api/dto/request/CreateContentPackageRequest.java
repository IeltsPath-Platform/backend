package com.ieltspath.content.api.dto.request;

import com.ieltspath.content.domain.vo.PlacementPool;
import com.ieltspath.content.api.dto.AccessLevel;
import com.ieltspath.content.domain.vo.PackageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateContentPackageRequest(
        @NotBlank(message = "Mã không được để trống")
        @Size(max = 100, message = "Mã không được dài quá 100 ký tự")
        String code,

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 255, message = "Tiêu đề không được dài quá 255 ký tự")
        String title,

        @NotNull(message = "Cần chọn loại gói")
        PackageType packageType,

        AccessLevel accessLevel,
        // Only for PRACTICE_SET: the lesson whose Practice the set belongs to.
        UUID lessonId,
        String sourceCode,
        PlacementPool placementPool
) {
    public CreateContentPackageRequest(String code, String title, PackageType packageType,
                                      AccessLevel accessLevel, UUID lessonId, String sourceCode) {
        this(code, title, packageType, accessLevel, lessonId, sourceCode, null);
    }

    public CreateContentPackageRequest(String code, String title, PackageType packageType,
                                      AccessLevel accessLevel, UUID lessonId) {
        this(code, title, packageType, accessLevel, lessonId, null, null);
    }
    @AssertTrue(message = "Nhóm đề xếp lớp (placementPool) chỉ bắt buộc và chỉ dùng cho đề xếp lớp")
    public boolean isPlacementPoolValid() {
        return packageType == null || (packageType == PackageType.PLACEMENT_TEST) == (placementPool != null);
    }

}
