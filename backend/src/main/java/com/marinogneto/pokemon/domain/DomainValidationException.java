package com.marinogneto.pokemon.domain;

/**
 * A value breaks a domain rule. Carries the offending field so the web layer can answer 400 with a per-field
 * error, whatever the aggregate.
 */
public abstract class DomainValidationException extends RuntimeException {

    private final String field;
    private final String reason;

    protected DomainValidationException(String field, String reason) {
        super(field + " " + reason);
        this.field = field;
        this.reason = reason;
    }

    public String field() {
        return field;
    }

    /** The rule without the field name, e.g. "must be at most 50 characters". */
    public String reason() {
        return reason;
    }
}
