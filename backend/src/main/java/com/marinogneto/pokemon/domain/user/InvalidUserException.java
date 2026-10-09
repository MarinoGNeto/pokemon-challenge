package com.marinogneto.pokemon.domain.user;

import com.marinogneto.pokemon.domain.DomainValidationException;

/** A user field breaks a domain rule (e.g. username format). Mapped to 400 with the field name. */
public class InvalidUserException extends DomainValidationException {

    public InvalidUserException(String field, String reason) {
        super(field, reason);
    }
}
