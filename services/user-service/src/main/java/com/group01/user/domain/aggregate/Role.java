package com.group01.user.domain.aggregate;

import com.group01.user.domain.vo.RoleName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Role {
    private UUID id;
    private RoleName name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
