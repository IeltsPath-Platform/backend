package com.ieltspath.user.application.usecase;

import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.result.UserPage;
import com.ieltspath.user.domain.vo.RoleName;
import com.ieltspath.user.domain.vo.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUsersPageUseCaseTest {
    @Mock UserRepository userRepository;
    @InjectMocks GetUsersPageUseCase useCase;

    @Test
    void sendsValidatedFiltersAndPageToRepository() {
        when(userRepository.findPage(RoleName.ADMIN, UserStatus.LOCKED, "Minh", 1, 25))
                .thenReturn(new UserPage(List.of(), 1, 25, 26));

        UserPage page = useCase.execute(1, 25, "ADMIN", "LOCKED", "  Minh  ");

        assertThat(page.totalElements()).isEqualTo(26);
        verify(userRepository).findPage(RoleName.ADMIN, UserStatus.LOCKED, "Minh", 1, 25);
    }

    @Test
    void rejectsPageSizeAndUnknownFiltersWithVietnameseMessages() {
        assertThatThrownBy(() -> useCase.execute(-1, 25, null, null, null))
                .hasMessage("Số trang phải lớn hơn hoặc bằng 0.");
        assertThatThrownBy(() -> useCase.execute(0, 101, null, null, null))
                .hasMessage("Số dòng mỗi trang phải từ 1 đến 100.");
        assertThatThrownBy(() -> useCase.execute(0, 25, "UNKNOWN", null, null))
                .hasMessage("Vai trò người dùng không hợp lệ.");
        assertThatThrownBy(() -> useCase.execute(0, 25, null, "UNKNOWN", null))
                .hasMessage("Trạng thái người dùng không hợp lệ.");
    }
}
