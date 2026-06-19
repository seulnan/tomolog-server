package com.tomolog.presentation.common;

import com.tomolog.domain.common.ApiException;
import com.tomolog.domain.common.ErrorCode;
import com.tomolog.domain.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns exceptions thrown inside controllers into the unified error envelope. Security-layer errors
 * (401/403) are handled separately by the authentication entry point and access-denied handler,
 * since those fire before the controller is reached.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Maps a domain {@link ApiException} to its configured status and code. */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(
      ApiException ex, HttpServletRequest request) {
    ErrorCode code = ex.getErrorCode();
    return ResponseEntity.status(code.status())
        .body(ErrorResponse.of(code, ex.getMessage(), request.getRequestURI()));
  }

  /** Maps bean-validation failures on request bodies to a 400 envelope. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse(ErrorCode.INVALID_INPUT.defaultMessage());
    return ResponseEntity.status(ErrorCode.INVALID_INPUT.status())
        .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, message, request.getRequestURI()));
  }

  /** Unmatched routes return a 404 envelope rather than being swallowed into a 500. */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    return ResponseEntity.status(ErrorCode.NOT_FOUND.status())
        .body(
            ErrorResponse.of(
                ErrorCode.NOT_FOUND,
                ErrorCode.NOT_FOUND.defaultMessage(),
                request.getRequestURI()));
  }

  /** Last-resort handler so unexpected errors still return the envelope, not a stack trace. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception at {}", request.getRequestURI(), ex);
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
        .body(
            ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.defaultMessage(),
                request.getRequestURI()));
  }
}
