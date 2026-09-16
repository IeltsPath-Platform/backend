package com.group01.user.domain.aggregate;

import com.group01.user.domain.vo.Email;
import com.group01.user.domain.vo.PhoneNumber;
import com.group01.user.domain.vo.UserStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class User {
    private UUID id;
    private Email email;
    private String passwordHash;
    private String fullName;
    private PhoneNumber phoneNumber;
    private UserStatus status;
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateProfile(String fullName, PhoneNumber phoneNumber) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống");
        }
        this.fullName = fullName.trim();
        this.phoneNumber = phoneNumber;
        touch();
    }

    public void assignRoles(Set<Role> roles) {
        this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
        touch();
    }

    public void changeStatus(UserStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Trạng thái người dùng không được để trống");
        }
        this.status = status;
        touch();
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        touch();
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(new HashSet<>(roles));
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
