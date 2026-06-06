package com.demo.skills.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain and infrastructure exceptions to RFC 9457 {@link ProblemDetail} responses. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Handles account creation limit exceeded (422). */
  @ExceptionHandler(AccountLimitExceededException.class)
  @ResponseStatus(UNPROCESSABLE_CONTENT)
  ProblemDetail handleAccountLimitExceeded(
      final AccountLimitExceededException ex, final HttpServletRequest request) {
    log.atWarn()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .addKeyValue("customerId", ex.customerId())
        .log("Account limit exceeded");

    val problem =
        ProblemDetail.forStatusAndDetail(UNPROCESSABLE_CONTENT, ex.getMessage());
    problem.setTitle("Account Limit Exceeded");
    problem.setType(URI.create("https://errors.demo.com/account-limit-exceeded"));
    return problem;
  }

  /** Handles nickname profanity violation (422). */
  @ExceptionHandler(NicknameNotAllowedException.class)
  @ResponseStatus(UNPROCESSABLE_CONTENT)
  ProblemDetail handleNicknameNotAllowed(
      final NicknameNotAllowedException ex, final HttpServletRequest request) {
    log.atWarn()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .log("Nickname not allowed");

    val problem =
        ProblemDetail.forStatusAndDetail(UNPROCESSABLE_CONTENT, ex.getMessage());
    problem.setTitle("Nickname Not Allowed");
    problem.setType(URI.create("https://errors.demo.com/nickname-not-allowed"));
    return problem;
  }

  /** Handles `account not found` (404). */
  @ExceptionHandler(AccountNotFoundException.class)
  @ResponseStatus(NOT_FOUND)
  ProblemDetail handleAccountNotFound(
      final AccountNotFoundException ex, final HttpServletRequest request) {
    log.atWarn()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .addKeyValue("accountId", ex.accountId())
        .log("Account not found");

    val problem = ProblemDetail.forStatusAndDetail(NOT_FOUND, ex.getMessage());
    problem.setTitle("Account Not Found");
    problem.setType(URI.create("https://errors.demo.com/account-not-found"));
    problem.setProperty("accountId", ex.accountId());
    return problem;
  }

  /** Handles Bean Validation failures (400). */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(BAD_REQUEST)
  ProblemDetail handleValidation(
      final MethodArgumentNotValidException ex, final HttpServletRequest request) {
    log.atWarn()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .log("Validation error");

    val violations =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe ->
                    Map.of(
                        "field",
                        fe.getField(),
                        "message",
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"))
            .toList();

    val problem = ProblemDetail.forStatusAndDetail(BAD_REQUEST, "Validation failed");
    problem.setTitle("Validation Error");
    problem.setType(URI.create("https://errors.demo.com/validation-error"));
    problem.setProperty("violations", violations);
    return problem;
  }

  /** Handles circuit-breaker open state — database unavailable (503). */
  @ExceptionHandler(CallNotPermittedException.class)
  @ResponseStatus(SERVICE_UNAVAILABLE)
  ProblemDetail handleCircuitBreakerOpen(
      final CallNotPermittedException ex, final HttpServletRequest request) {
    log.atError()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .log("Circuit breaker open - service temporarily unavailable");

    val problem =
        ProblemDetail.forStatusAndDetail(
            SERVICE_UNAVAILABLE, "Service temporarily unavailable, please try again");
    problem.setTitle("Service Unavailable");
    problem.setType(URI.create("https://errors.demo.com/service-unavailable"));
    return problem;
  }

  /** Handles direct database failures before the circuit breaker opens (503). */
  @ExceptionHandler(DataAccessException.class)
  @ResponseStatus(SERVICE_UNAVAILABLE)
  ProblemDetail handleDatabaseFailure(
      final DataAccessException ex, final HttpServletRequest request) {
    log.atError()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .log("Database request failure");

    val problem =
        ProblemDetail.forStatusAndDetail(
            SERVICE_UNAVAILABLE, "Database temporarily unavailable, please try again");
    problem.setTitle("Service Unavailable");
    problem.setType(URI.create("https://errors.demo.com/database-unavailable"));
    return problem;
  }

  /** Handles unexpected request failures (500). */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(INTERNAL_SERVER_ERROR)
  ProblemDetail handleUnexpected(final Exception ex, final HttpServletRequest request) {
    log.atError()
        .setCause(ex)
        .addKeyValue("path", request.getRequestURI())
        .addKeyValue("method", request.getMethod())
        .log("Unexpected request failure");

    val problem =
        ProblemDetail.forStatusAndDetail(
            INTERNAL_SERVER_ERROR, "Unexpected server error");
    problem.setTitle("Internal Server Error");
    problem.setType(URI.create("https://errors.demo.com/internal-server-error"));
    return problem;
  }
}
