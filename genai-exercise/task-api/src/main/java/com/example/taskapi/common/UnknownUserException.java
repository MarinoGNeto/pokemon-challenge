package com.example.taskapi.common;

/**
 * The authenticated principal no longer maps to a stored user (e.g. a valid token for a
 * deleted account). Mapped to 401.
 */
public class UnknownUserException extends RuntimeException {

    public UnknownUserException() {
        super("Authenticated user does not exist");
    }
}
