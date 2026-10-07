package com.ieltspath.user.infrastructure.persistence.mapper;

import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.vo.Email;
import com.ieltspath.user.domain.vo.PhoneNumber;
import com.ieltspath.user.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = RoleMapper.class)
public interface UserMapper {
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    User toDomain(UserJpaEntity entity);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    UserJpaEntity toEntity(User user);

    default Email toEmail(String value) {
        return new Email(value);
    }

    default String fromEmail(Email email) {
        return email == null ? null : email.value();
    }

    default PhoneNumber toPhoneNumber(String value) {
        return new PhoneNumber(value);
    }

    default String fromPhoneNumber(PhoneNumber phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.value();
    }
}
