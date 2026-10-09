package com.marinogneto.pokemon.application.auth;

/** Username or email already registered. Mapped to 409 with the field. */
public class DuplicateUserException extends RuntimeException {

    private final String field;

    public DuplicateUserException(String field) {
        super(field + " is already registered");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
