package com.fintech.wallet.infrastructure.exception;

import com.fintech.wallet.domain.model.Wallet;
import com.fintech.wallet.domain.service.WalletService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiResponse.FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_ERROR", "Request validation failed", errors));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("WALLET_NOT_FOUND", ex.getMessage(), null));
    }

    @ExceptionHandler(Wallet.InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(Wallet.InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("WALLET_INSUFFICIENT_BALANCE", ex.getMessage(), null));
    }

    @ExceptionHandler(Wallet.WalletNotActiveException.class)
    public ResponseEntity<ApiResponse<Void>> handleWalletFrozen(Wallet.WalletNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("WALLET_FROZEN", ex.getMessage(), null));
    }

    @ExceptionHandler(WalletService.DuplicateWalletException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(WalletService.DuplicateWalletException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("WALLET_DUPLICATE", ex.getMessage(), null));
    }

    @ExceptionHandler(WalletService.OptimisticLockRetryExhaustedException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(WalletService.OptimisticLockRetryExhaustedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("WALLET_VERSION_CONFLICT", ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred", null));
    }
}
