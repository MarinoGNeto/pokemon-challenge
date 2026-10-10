package com.example.taskapi.user.service;

import com.example.taskapi.security.JwtTokenService;
import com.example.taskapi.security.JwtTokenService.IssuedToken;
import com.example.taskapi.user.domain.Role;
import com.example.taskapi.user.domain.User;
import com.example.taskapi.user.domain.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal registration and login, only so the task API can be exercised.
 */
@Service
public class AuthService {

    /** Constant hash used to keep login timing similar for unknown usernames. */
    private static final String DUMMY_PASSWORD = "dummy-password-for-timing";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.dummyHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Transactional
    public User register(String username, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        try {
            return users.saveAndFlush(new User(username, passwordEncoder.encode(rawPassword), Role.USER));
        } catch (DataIntegrityViolationException e) {
            // concurrent registration with the same username
            throw new UsernameTakenException(username);
        }
    }

    @Transactional(readOnly = true)
    public IssuedToken login(String username, String rawPassword) {
        User user = users.findByUsername(username).orElse(null);
        if (user == null) {
            passwordEncoder.matches(rawPassword, dummyHash);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokens.issue(user.getId(), user.getUsername(), user.getRole().name());
    }
}
