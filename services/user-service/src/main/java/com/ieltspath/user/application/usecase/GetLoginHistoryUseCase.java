package com.ieltspath.user.application.usecase;

import com.ieltspath.user.application.query.PageQuery;
import com.ieltspath.user.application.result.LoginHistoryResult;
import com.ieltspath.user.application.result.PageResult;
import com.ieltspath.user.domain.aggregate.LoginAttempt;
import com.ieltspath.user.domain.exception.UserNotFoundException;
import com.ieltspath.user.domain.repository.LoginHistoryRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.OwnedPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetLoginHistoryUseCase {
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional(readOnly = true)
    public PageResult<LoginHistoryResult> execute(UUID userId, PageQuery query) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng: " + userId));
        OwnedPage<LoginAttempt> page = loginHistoryRepository.findByUserId(userId, query.page(), query.size());
        return new PageResult<>(
                page.items().stream().map(LoginHistoryResult::from).toList(),
                query.page(), query.size(), page.total());
    }
}
