package com.marinogneto.pokemon.application.port.out;

/** One-way password hashing (implemented with BCrypt). */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
