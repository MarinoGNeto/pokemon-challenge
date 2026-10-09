package com.marinogneto.pokemon.application.auth;

/** Login failed. Deliberately the same for an unknown user and a wrong password. Mapped to 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
