package com.marinogneto.pokemon.application.port.out;

import com.marinogneto.pokemon.application.auth.DuplicateUserException;
import com.marinogneto.pokemon.domain.user.User;
import java.util.Optional;

/** Output port to stored users. Usernames and emails are passed already normalised (lower case). */
public interface UserRepository {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** @throws DuplicateUserException if the username or email was taken concurrently */
    User save(User user);
}
