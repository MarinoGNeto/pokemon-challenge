package com.example.taskapi.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.example.taskapi.security.JwtTokenService;
import com.example.taskapi.security.JwtTokenService.IssuedToken;
import com.example.taskapi.task.TestData;
import com.example.taskapi.user.domain.Role;
import com.example.taskapi.user.domain.User;
import com.example.taskapi.user.domain.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private JwtTokenService tokens;

    @SuppressWarnings("deprecation")
    private final PasswordEncoder encoder = NoOpPasswordEncoder.getInstance();

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, tokens);
    }

    @Test
    void registerStoresEncodedPasswordWithUserRole() {
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register("alice", "password123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("alice");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo(encoder.encode("password123"));
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerRejectsTakenUsername() {
        when(users.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register("alice", "password123"))
                .isInstanceOf(UsernameTakenException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void registerTranslatesConcurrentDuplicateToConflict() {
        when(users.existsByUsername("alice")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk"));

        assertThatThrownBy(() -> service.register("alice", "password123"))
                .isInstanceOf(UsernameTakenException.class);
    }

    @Test
    void loginIssuesTokenForValidCredentials() {
        UUID id = UUID.randomUUID();
        User alice = TestData.user(id, "alice");
        ReflectionTestUtils.setField(alice, "passwordHash", "password123");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(tokens.issue(id, "alice", "USER")).thenReturn(new IssuedToken("jwt", 3600));

        assertThat(service.login("alice", "password123")).isEqualTo(new IssuedToken("jwt", 3600));
    }

    @Test
    void loginRejectsWrongPassword() {
        User alice = TestData.user(UUID.randomUUID(), "alice");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> service.login("alice", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokens, never()).issue(any(), any(), any());
    }

    @Test
    void loginRejectsUnknownUserWithSameError() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ghost", "password123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
