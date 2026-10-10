package com.example.taskapi.common;

import java.util.List;

/**
 * One or more business rules failed for specific fields. Mapped to 400 with the same
 * {@code errors} shape as bean-validation failures.
 */
public class BusinessRuleViolationException extends RuntimeException {

    private final transient List<FieldViolation> violations;

    public BusinessRuleViolationException(List<FieldViolation> violations) {
        super(violations.stream().map(v -> v.field() + ": " + v.message()).reduce((a, b) -> a + "; " + b)
                .orElse("Business rule violated"));
        this.violations = List.copyOf(violations);
    }

    public BusinessRuleViolationException(String field, String message) {
        this(List.of(new FieldViolation(field, message)));
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }

    public record FieldViolation(String field, String message) {
    }
}
