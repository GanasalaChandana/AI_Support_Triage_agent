package com.triage.config;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Groq's free tier caps tokens-per-minute; a burst of requests can hit that
 * limit (observed in practice, not hypothetical). Without this, the raw
 * NonTransientAiException surfaces as an opaque 500 to callers.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NonTransientAiException.class)
    public ResponseEntity<Map<String, String>> handleAiProviderError(NonTransientAiException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error",
                        "The AI provider is temporarily rate-limited or unavailable. Please try again shortly."));
    }
}
