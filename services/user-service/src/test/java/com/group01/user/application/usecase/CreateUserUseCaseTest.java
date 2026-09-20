package com.group01.user.application.usecase;

import com.group01.user.application.command.CreateUserCommand;
import com.group01.user.domain.aggregate.Role;
import com.group01.user.domain.aggregate.User;
import com.group01.user.domain.repository.RoleRepository;
import com.group01.user.domain.repository.UserRepository;
import com.group01.user.domain.vo.RoleName;
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
}
