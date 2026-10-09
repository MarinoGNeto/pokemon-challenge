package com.marinogneto.pokemon.application.auth;

/** Self-registration input. There is no role: self-registered users are always USER. */
public record RegisterUserCommand(String username, String email, String password) {

    @Override
    public String toString() {
        return "RegisterUserCommand[username=" + username + ", email=" + email + ", password=****]";
    }
}
