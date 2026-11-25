package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.result.UserPage;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUsersPageUseCase {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserPage execute(int page, int size, String role, String status, String search) {
        if (page < 0) throw new IllegalArgumentException("Số trang phải lớn hơn hoặc bằng 0.");
        if (size < 1 || size > 100) throw new IllegalArgumentException("Số dòng mỗi trang phải từ 1 đến 100.");

        RoleName roleName = parseRole(role);
        UserStatus userStatus = parseStatus(status);
        String query = search == null || search.isBlank() ? null : search.trim();
        return userRepository.findPage(roleName, userStatus, query, page, size);
    }

    private RoleName parseRole(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return RoleName.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Vai trò người dùng không hợp lệ.");
        }
    }

    private UserStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UserStatus.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Trạng thái người dùng không hợp lệ.");
        }
    }
}
