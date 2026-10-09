package com.marinogneto.pokemon.domain.user;

/** What a user may do: USER syncs and edits local Pokemon; ADMIN may also delete them. */
public enum Role {
    USER,
    ADMIN;

    /** Name used by Spring Security's {@code hasRole(...)} checks. */
    public String authority() {
        return "ROLE_" + name();
    }
}
