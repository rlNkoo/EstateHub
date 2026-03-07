package com.rlnkoo.searchservice.domain.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ErrorResponse handleAuthenticationRequired(
            AuthenticationRequiredException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication required path=[{}] message=[{}]", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied path=[{}] message=[{}]", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI());
    }

    @ExceptionHandler(InvalidBearerTokenException.class)
    public ErrorResponse handleInvalidBearerToken(
            InvalidBearerTokenException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid bearer token path=[{}] message=[{}]", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid bearer token", request.getRequestURI());
    }

    @ExceptionHandler(InvalidSearchRequestException.class)
    public ErrorResponse handleInvalidSearchRequest(
            InvalidSearchRequestException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid search request path=[{}] message=[{}]", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ReindexFailedException.class)
    public ErrorResponse handleReindexFailed(
            ReindexFailedException ex,
            HttpServletRequest request
    ) {
        log.error("Reindex failed path=[{}] message=[{}]", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ErrorResponse handleValidationException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Validation failed path=[{}] message=[{}]", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error path=[{}] message=[{}]", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request.getRequestURI());
    }

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
    }
}