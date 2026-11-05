package com.ieltspath.user.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ieltspath.user.domain.exception.PhoneAlreadyExistsException;
import com.ieltspath.user.domain.exception.EmailAlreadyExistsException;
import com.ieltspath.user.application.command.CreateUserCommand;
import com.ieltspath.user.domain.aggregate.Role;
import com.ieltspath.user.domain.aggregate.User;
import com.ieltspath.user.domain.repository.RoleRepository;
import com.ieltspath.user.domain.repository.UserRepository;
import com.ieltspath.user.domain.vo.RoleName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;

    @Test
    void createsLocalUserWithDefaultCustomerRole() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        CreateUserUseCase useCase = new CreateUserUseCase(userRepository, roleRepository, passwordEncoder);
        Role customer = Role.builder().id(UUID.randomUUID()).name(RoleName.CUSTOMER).description("Customer user").build();
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByNames(Set.of("CUSTOMER"))).thenReturn(List.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = useCase.execute(new CreateUserCommand("John@Example.com", "secret123", "John Doe", "0123456789", null));

        assertThat(created.getEmail().value()).isEqualTo("john@example.com");
        assertThat(passwordEncoder.matches("secret123", created.getPasswordHash())).isTrue();
        assertThat(created.getRoles()).extracting(role -> role.getName().name()).containsExactly("CUSTOMER");
        assertThat(created.getStatus().name()).isEqualTo("ACTIVE");
    }

    private CreateUserUseCase useCase() {
        return new CreateUserUseCase(userRepository, roleRepository, new BCryptPasswordEncoder());
    }

    @Test
    void rejectsAPasswordOver72BytesBeforeAnythingIsSaved() {
        String ninetyBytes = "ậ".repeat(30);
        assertThatThrownBy(() -> useCase().execute(
                new CreateUserCommand("john@example.com", ninetyBytes, "John Doe", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mật khẩu phải có từ 6 đến 72 byte (chữ có dấu tính nhiều byte hơn)");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsANameOverTheColumnBeforeAnythingIsSaved() {
        assertThatThrownBy(() -> useCase().execute(
                new CreateUserCommand("john@example.com", "secret123", "n".repeat(151), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Họ tên không được vượt quá 150 ký tự");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsABlankName() {
        assertThatThrownBy(() -> useCase().execute(
                new CreateUserCommand("john@example.com", "secret123", "   ", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Họ tên không được để trống");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegistered() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        assertThatThrownBy(() -> useCase().execute(
                new CreateUserCommand("john@example.com", "secret123", "John Doe", null, null)))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email đã tồn tại: john@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsAPhoneNumberAlreadyUsedEvenWhenTypedWithSpaces() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0123456789")).thenReturn(true);
        assertThatThrownBy(() -> useCase().execute(
                new CreateUserCommand("new@example.com", "secret123", "New User", "0123 456 789", null)))
                .isInstanceOf(PhoneAlreadyExistsException.class)
                .hasMessage("Số điện thoại đã tồn tại: 0123456789");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void savesThePhoneNumberWithoutSeparators() {
        Role customer = Role.builder().id(UUID.randomUUID()).name(RoleName.CUSTOMER).build();
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0123456789")).thenReturn(false);
        when(roleRepository.findByNames(Set.of("CUSTOMER"))).thenReturn(List.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = useCase().execute(
                new CreateUserCommand("john@example.com", "secret123", "John Doe", "0123 456 789", null));

        assertThat(created.getPhoneNumber().value()).isEqualTo("0123456789");
    }

    @Test
    void rejectsAPhoneNumberWithLetters() {
        assertThatThrownBy(() -> useCase().execute(
                new CreateUserCommand("john@example.com", "secret123", "John Doe", "abc", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Số điện thoại không hợp lệ");
        verify(userRepository, never()).save(any(User.class));
    }
}
