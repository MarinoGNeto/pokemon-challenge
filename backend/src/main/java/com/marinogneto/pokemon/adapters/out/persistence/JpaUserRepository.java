package com.marinogneto.pokemon.adapters.out.persistence;

import com.marinogneto.pokemon.application.auth.DuplicateUserException;
import com.marinogneto.pokemon.application.port.out.UserRepository;
import com.marinogneto.pokemon.domain.user.User;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link UserRepository} on PostgreSQL. RegisterUser checks for duplicates first (clear errors); the unique
 * constraints are the real guarantee when two registrations race, and are translated back to the same error.
 */
@Transactional(readOnly = true)
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        try {
            return jpa.saveAndFlush(UserEntity.from(user)).toDomain();
        } catch (DataIntegrityViolationException e) {
            String detail = String.valueOf(e.getMostSpecificCause().getMessage());
            if (detail.contains("uk_users_username")) {
                throw new DuplicateUserException("username");
            }
            if (detail.contains("uk_users_email")) {
                throw new DuplicateUserException("email");
            }
            throw e;
        }
    }
}
