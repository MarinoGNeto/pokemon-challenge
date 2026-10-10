package com.example.taskapi.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.example.taskapi.common.BusinessRuleViolationException.FieldViolation;
import com.example.taskapi.user.service.InvalidCredentialsException;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Renders every error as an RFC 9457 {@link ProblemDetail}. Framework exceptions
 * (malformed JSON, type mismatches, unsupported media type, ...) are handled by the
 * {@link ResponseEntityExceptionHandler} base class; application exceptions below.
 * <p>
 * Every 400 carries an {@code errors} array of {@code {field, message}} objects, including
 * bodies that cannot be read (malformed JSON, unknown enum value, bad date) and path/query
 * parameters that cannot be converted.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String ERRORS_PROPERTY = "errors";
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 400 -----------------------------------------------------------------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldViolation> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.add(new FieldViolation(fe.getField(), fe.getDefaultMessage()));
        }
        for (ObjectError ge : ex.getBindingResult().getGlobalErrors()) {
            errors.add(new FieldViolation(ge.getObjectName(), ge.getDefaultMessage()));
        }
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldViolation> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new FieldViolation(name, error.getDefaultMessage()));
            }
        });
        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    /** Unreadable body: malformed JSON, or a value Jackson cannot convert (enum, date, number). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.badRequest().body(validationProblem(List.of(bodyViolation(ex))));
    }

    /** Path or query parameter that cannot be converted, e.g. a malformed UUID or date. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String field = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
        return ResponseEntity.badRequest()
                .body(validationProblem(List.of(new FieldViolation(field, expectation(ex.getRequiredType())))));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return validationProblem(ex.getViolations());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return validationProblem(errors);
    }

    // ---- 401 / 403 -----------------------------------------------------------------

    @ExceptionHandler({AuthenticationException.class, InvalidCredentialsException.class,
            UnknownUserException.class})
    ResponseEntity<ProblemDetail> handleUnauthorized(RuntimeException ex) {
        String detail = ex instanceof InvalidCredentialsException
                ? ex.getMessage()
                : "Full authentication with a valid bearer token is required";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setTitle("Unauthorized");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleForbidden(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        problem.setTitle("Forbidden");
        return problem;
    }

    // ---- 404 / 409 -----------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource not found");
        return problem;
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        return problem;
    }

    /** Lost update detected by Hibernate's @Version check at flush time. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The resource was modified concurrently; reload it and retry");
        problem.setTitle("Conflict");
        return problem;
    }

    // ---- 500 -----------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        problem.setTitle("Internal Server Error");
        return problem;
    }

    private static FieldViolation bodyViolation(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            return new FieldViolation(jsonPath(mismatch), expectation(mismatch.getTargetType()));
        }
        if (ex.getCause() instanceof JacksonException) {
            return new FieldViolation("body", "is not valid JSON");
        }
        return new FieldViolation("body", "is missing or unreadable");
    }

    /** Dotted path of the offending JSON property, e.g. {@code dueDate} or {@code items[2].name}. */
    private static String jsonPath(JacksonException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference ref : ex.getPath()) {
            if (ref.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(ref.getPropertyName());
            } else if (ref.getIndex() >= 0) {
                path.append('[').append(ref.getIndex()).append(']');
            }
        }
        return path.toString();
    }

    /** What a valid value looks like, phrased like the bean-validation messages. */
    private static String expectation(Class<?> type) {
        if (type == null) {
            return "has an invalid value";
        }
        if (type.isEnum()) {
            return "must be one of " + Arrays.toString(type.getEnumConstants());
        }
        if (type == LocalDate.class) {
            return "must be an ISO-8601 date (yyyy-MM-dd)";
        }
        if (type == UUID.class) {
            return "must be a valid UUID";
        }
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class && type != char.class)) {
            return "must be a number";
        }
        return "has an invalid value";
    }

    private static ProblemDetail validationProblem(List<FieldViolation> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed");
        problem.setTitle("Validation failed");
        problem.setProperty(ERRORS_PROPERTY, errors);
        return problem;
    }
}
