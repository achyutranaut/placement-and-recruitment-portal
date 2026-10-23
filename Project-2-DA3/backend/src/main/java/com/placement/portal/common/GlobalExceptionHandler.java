package com.placement.portal.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Validation Failed", errors));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid username or password"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Access denied: You do not have permission to perform this action";
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({SQLException.class, org.springframework.dao.DataAccessException.class})
    public ResponseEntity<ApiResponse<Void>> handleDatabaseException(Exception ex) {
        String msg = extractCleanOracleMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(msg));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Requested endpoint not found: " + ex.getResourcePath()));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Uploaded file exceeds the maximum allowed size of 10MB."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        String msg = ex.getMessage();
        if (msg != null && (msg.contains("ORA-") || msg.contains("Daily limit reached") || msg.contains("constraint"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(extractCleanOracleMessage(msg)));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }

    private String extractCleanOracleMessage(String raw) {
        if (raw == null) return "A database constraint violation occurred.";
        if (raw.contains("ORA-02290") || raw.contains("CHK_INTERVIEW_RESULT")) {
            return "Interview evaluation result is invalid. Allowed status values are CLEARED, REJECTED, or PENDING.";
        }
        if (raw.contains("ORA-20011") || raw.contains("Daily limit reached")) {
            return "Daily limit reached: A student may apply to at most one placement drive per calendar day.";
        }
        if (raw.contains("ORA-20010")) {
            return "Ineligible: Student CGPA is below the minimum required CGPA for this placement drive.";
        }
        if (raw.contains("ORA-20030")) {
            return "Cannot issue offer: Application has no CLEARED interview results.";
        }
        if (raw.contains("ORA-20031")) {
            return "An offer has already been issued for this application.";
        }
        if (raw.contains("ORA-20")) {
            int start = raw.indexOf("ORA-20");
            int end = raw.indexOf("\n", start);
            String clean = end != -1 ? raw.substring(start, end) : raw.substring(start);
            return clean.replaceAll("ORA-20[0-9]{3}:\\s*", "").trim();
        }
        return raw;
    }
}
