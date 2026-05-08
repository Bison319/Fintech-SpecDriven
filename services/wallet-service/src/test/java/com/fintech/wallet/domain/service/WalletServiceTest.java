package com.fintech.wallet.domain.service;

import com.fintech.wallet.application.dto.AdjustBalanceRequest;
import com.fintech.wallet.application.dto.CreateWalletRequest;
import com.fintech.wallet.application.dto.WalletResponse;
import com.fintech.wallet.domain.model.Wallet;
import com.fintech.wallet.domain.model.WalletStatus;
import com.fintech.wallet.domain.repository.WalletAuditLogRepository;
import com.fintech.wallet.domain.repository.WalletRepository;
import com.fintech.wallet.infrastructure.client.UserServiceClient;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletAuditLogRepository auditLogRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private WalletService walletService;

    @Nested
    @DisplayName("createWallet")
    class CreateWallet {

        @Test
        @DisplayName("should create wallet when user exists and no duplicate")
        void shouldCreateWallet() {
            UUID userId = UUID.randomUUID();
            CreateWalletRequest request = new CreateWalletRequest(userId, "USD");

            when(userServiceClient.getUserById(userId)).thenReturn(new Object());
            when(walletRepository.existsByUserIdAndCurrency(userId, "USD")).thenReturn(false);
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            WalletResponse response = walletService.createWallet(request);

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.currency()).isEqualTo("USD");
            assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.status()).isEqualTo(WalletStatus.ACTIVE);
        }

        @Test
        @DisplayName("should throw DuplicateWalletException when wallet already exists")
        void shouldThrowOnDuplicate() {
            UUID userId = UUID.randomUUID();
            CreateWalletRequest request = new CreateWalletRequest(userId, "USD");

            when(userServiceClient.getUserById(userId)).thenReturn(new Object());
            when(walletRepository.existsByUserIdAndCurrency(userId, "USD")).thenReturn(true);

            assertThatThrownBy(() -> walletService.createWallet(request))
                    .isInstanceOf(WalletService.DuplicateWalletException.class);
        }
    }

    @Nested
    @DisplayName("getWallet")
    class GetWallet {

        @Test
        @DisplayName("should return wallet when found")
        void shouldReturnWallet() {
            UUID walletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Wallet wallet = new Wallet(userId, "USD");
            when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

            WalletResponse response = walletService.getWallet(walletId);

            assertThat(response.currency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when wallet not found")
        void shouldThrowWhenNotFound() {
            UUID walletId = UUID.randomUUID();
            when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getWallet(walletId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Wallet domain logic")
    class WalletDomainLogic {

        @Test
        @DisplayName("should credit wallet successfully")
        void shouldCreditWallet() {
            Wallet wallet = new Wallet(UUID.randomUUID(), "USD");
            wallet.credit(new BigDecimal("100.00"));

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should debit wallet successfully when sufficient balance")
        void shouldDebitWallet() {
            Wallet wallet = new Wallet(UUID.randomUUID(), "USD");
            wallet.credit(new BigDecimal("200.00"));
            wallet.debit(new BigDecimal("50.00"));

            assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("should throw InsufficientBalanceException on overdraft")
        void shouldThrowOnOverdraft() {
            Wallet wallet = new Wallet(UUID.randomUUID(), "USD");
            wallet.credit(new BigDecimal("100.00"));

            assertThatThrownBy(() -> wallet.debit(new BigDecimal("150.00")))
                    .isInstanceOf(Wallet.InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient balance");
        }

        @Test
        @DisplayName("should throw on zero or negative credit amount")
        void shouldThrowOnInvalidCreditAmount() {
            Wallet wallet = new Wallet(UUID.randomUUID(), "USD");

            assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> wallet.credit(new BigDecimal("-10.00")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
