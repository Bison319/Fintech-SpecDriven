package com.fintech.user.infrastructure.exception;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error,
        Metadata metadata
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null,
                new Metadata(Instant.now(), UUID.randomUUID().toString(), "user-service"));
    }

    public static <T> ApiResponse<T> error(String code, String message, List<FieldError> details) {
        return new ApiResponse<>(false, null,
                new ErrorDetail(code, message, details),
                new Metadata(Instant.now(), UUID.randomUUID().toString(), "user-service"));
    }

    public record ErrorDetail(String code, String message, List<FieldError> details) {}
    public record FieldError(String field, String message) {}
    public record Metadata(Instant timestamp, String correlationId, String service) {}
}
