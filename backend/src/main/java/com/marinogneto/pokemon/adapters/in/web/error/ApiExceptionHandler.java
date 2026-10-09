package com.marinogneto.pokemon.adapters.in.web.error;

import com.marinogneto.pokemon.application.auth.DuplicateUserException;
import com.marinogneto.pokemon.application.auth.InvalidCredentialsException;
import com.marinogneto.pokemon.application.port.out.CatalogUnavailableException;
import com.marinogneto.pokemon.domain.DomainValidationException;
import com.marinogneto.pokemon.domain.localpokemon.LocalPokemonNotFoundException;
import com.marinogneto.pokemon.domain.localpokemon.VersionConflictException;
import com.marinogneto.pokemon.domain.pokemon.PokemonNotFoundException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Single place that turns exceptions into RFC 9457 {@code application/problem+json} responses (ADR-010).
 * Spring MVC's own exceptions are handled by the parent class; clients never see stack traces, exception
 * types or internal messages.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** One field-level validation error, listed in the problem's {@code errors} property. */
    public record FieldError(String field, String message) {
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    ProblemDetail notFound(PokemonNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Pokémon not found", e.getMessage());
    }

    @ExceptionHandler(LocalPokemonNotFoundException.class)
    ProblemDetail localNotFound(LocalPokemonNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Local Pokémon not found", e.getMessage());
    }

    @ExceptionHandler(VersionConflictException.class)
    ProblemDetail versionConflict(VersionConflictException e) {
        return problem(HttpStatus.CONFLICT, "conflict", "Edit conflict",
                e.getMessage() + ". Reload it and apply your change again.");
    }

    /** A domain rule on a field (e.g. tag format, username format). */
    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail domainValidation(DomainValidationException e) {
        ProblemDetail body = invalidRequest("The request breaks a validation rule.");
        body.setProperty("errors", List.of(new FieldError(e.field(), e.reason())));
        return body;
    }

    @ExceptionHandler(DuplicateUserException.class)
    ProblemDetail duplicateUser(DuplicateUserException e) {
        ProblemDetail body = problem(HttpStatus.CONFLICT, "already-registered", "Already registered",
                "A user with this " + e.field() + " already exists.");
        body.setProperty("errors", List.of(new FieldError(e.field(), "is already registered")));
        return body;
    }

    /** Same answer for an unknown user and a wrong password (no username enumeration). */
    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid credentials", e.getMessage());
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail catalogUnavailable(CatalogUnavailableException e) {
        log.warn("Pokémon catalogue unavailable: {}", e.getMessage(), e);
        return problem(HttpStatus.BAD_GATEWAY, "catalog-unavailable", "Pokémon catalogue unavailable",
                "The upstream Pokémon catalogue (PokeAPI) did not answer correctly. Please try again later.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unexpected error", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Unexpected error",
                "Something went wrong on our side.");
    }

    /** Constraint violations on {@code @RequestParam}/{@code @PathVariable} (e.g. {@code size=0}). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // A @Valid request body validated together with constrained parameters arrives as ParameterErrors:
        // report its individual fields ("version"), not the parameter name ("request").
        List<FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result instanceof ParameterErrors bodyErrors
                        ? bodyErrors.getFieldErrors().stream()
                                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                        : result.getResolvableErrors().stream()
                                .map(error -> new FieldError(result.getMethodParameter().getParameterName(),
                                        error.getDefaultMessage())))
                .toList();
        ProblemDetail body = invalidRequest("One or more parameters are invalid.");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /** Bean Validation on a request body (e.g. missing {@code version}). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail body = invalidRequest("One or more fields are invalid.");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Unreadable body: malformed JSON, a wrong type, or an unknown field. Unknown fields are rejected on purpose
     * (spring.jackson.deserialization.fail-on-unknown-properties): a client sending "name" in an update must
     * learn that catalogue fields are read-only instead of being silently ignored.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getMostSpecificCause() instanceof UnrecognizedPropertyException unknown) {
            ProblemDetail body = invalidRequest("Field '" + unknown.getPropertyName() + "' cannot be set.");
            body.setProperty("errors",
                    List.of(new FieldError(unknown.getPropertyName(), "is not an editable field")));
            return ResponseEntity.badRequest().body(body);
        }
        return ResponseEntity.badRequest().body(invalidRequest("The request body is not valid JSON for this endpoint."));
    }

    /** A parameter that cannot be converted (e.g. {@code page=abc}). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail body = invalidRequest("Parameter '" + ex.getPropertyName() + "' has an invalid value.");
        body.setProperty("errors", List.of(new FieldError(ex.getPropertyName(), "must be a valid number")));
        return ResponseEntity.badRequest().body(body);
    }

    private static ProblemDetail invalidRequest(String detail) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", detail);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:pokemon-challenge:problem:" + type));
        problem.setTitle(title);
        return problem;
    }
}
