package com.hopngo.market.service;

import com.hopngo.market.entity.*;
import com.hopngo.market.repository.AccountRepository;
import com.hopngo.market.repository.LedgerEntryRepository;
import com.hopngo.market.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing ledger operations and double-entry bookkeeping.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerService {
    
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;
    
    /**
     * Record a booking payment transaction.
     */
    @Transactional
    public Transaction recordBookingPayment(UUID bookingId, UUID userId, UUID providerId,
                                          long totalAmountMinor, long platformFeeMinor,
                                          String currency) {
        log.info("Recording booking payment: bookingId={}, totalAmount={}, platformFee={}, currency={}",
                bookingId, totalAmountMinor, platformFeeMinor, currency);
        
        // Get accounts
        Account userAccount = getOrCreateUserAccount(userId, currency);
        Account providerAccount = getOrCreateProviderAccount(providerId, currency);
        Account platformAccount = getPlatformAccount(currency);
        
        long providerAmountMinor = totalAmountMinor - platformFeeMinor;
        
        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionType(Transaction.TransactionType.BOOKING_PAYMENT)
                .referenceType(LedgerEntry.ReferenceType.BOOKING)
                .referenceId(bookingId)
                .description("Booking payment for booking " + bookingId)
                .totalAmountMinor(totalAmountMinor)
                .currency(currency)
                .status(Transaction.TransactionStatus.PENDING)
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Create ledger entries
        // Debit user account (payment out)
        LedgerEntry userDebit = LedgerEntry.createDebit(
                userAccount, totalAmountMinor, currency,
                LedgerEntry.ReferenceType.BOOKING, bookingId,
                "Payment for booking " + bookingId,
                transaction.getId()
        );
        
        // Credit provider account (earnings)
        LedgerEntry providerCredit = LedgerEntry.createCredit(
                providerAccount, providerAmountMinor, currency,
                LedgerEntry.ReferenceType.BOOKING, bookingId,
                "Earnings from booking " + bookingId,
                transaction.getId()
        );
        
        // Credit platform account (fees)
        LedgerEntry platformCredit = LedgerEntry.createCredit(
                platformAccount, platformFeeMinor, currency,
                LedgerEntry.ReferenceType.BOOKING, bookingId,
                "Platform fee from booking " + bookingId,
                transaction.getId()
        );
        
        // Save entries
        ledgerEntryRepository.saveAll(List.of(userDebit, providerCredit, platformCredit));
        
        // Update account balances
        userAccount.debit(totalAmountMinor);
        providerAccount.credit(providerAmountMinor);
        platformAccount.credit(platformFeeMinor);
        
        accountRepository.saveAll(List.of(userAccount, providerAccount, platformAccount));
        
        // Complete transaction
        transaction.complete();
        transactionRepository.save(transaction);
        
        log.info("Booking payment recorded successfully: transactionId={}", transaction.getId());
        return transaction;
    }
    
    /**
     * Record a provider payout transaction.
     */
    @Transactional
    public Transaction recordProviderPayout(UUID payoutId, UUID providerId,
                                          long amountMinor, String currency) {
        log.info("Recording provider payout: payoutId={}, providerId={}, amount={}, currency={}",
                payoutId, providerId, amountMinor, currency);
        
        // Get accounts
        Account providerAccount = getOrCreateProviderAccount(providerId, currency);
        Account platformAccount = getPlatformAccount(currency);
        
        // Validate provider has sufficient balance
        if (providerAccount.getBalanceMinor() < amountMinor) {
            throw new IllegalArgumentException("Insufficient balance for payout");
        }
        
        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionType(Transaction.TransactionType.PROVIDER_PAYOUT)
                .referenceType(LedgerEntry.ReferenceType.PAYOUT)
                .referenceId(payoutId)
                .description("Payout to provider " + providerId)
                .totalAmountMinor(amountMinor)
                .currency(currency)
                .status(Transaction.TransactionStatus.PENDING)
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Create ledger entries
        // Debit provider account (payout out)
        LedgerEntry providerDebit = LedgerEntry.createDebit(
                providerAccount, amountMinor, currency,
                LedgerEntry.ReferenceType.PAYOUT, payoutId,
                "Payout " + payoutId,
                transaction.getId()
        );
        
        // Credit platform account (cash out)
        LedgerEntry platformCredit = LedgerEntry.createCredit(
                platformAccount, amountMinor, currency,
                LedgerEntry.ReferenceType.PAYOUT, payoutId,
                "Payout to provider " + providerId,
                transaction.getId()
        );
        
        // Save entries
        ledgerEntryRepository.saveAll(List.of(providerDebit, platformCredit));
        
        // Update account balances
        providerAccount.debit(amountMinor);
        platformAccount.credit(amountMinor);
        
        accountRepository.saveAll(List.of(providerAccount, platformAccount));
        
        // Complete transaction
        transaction.complete();
        transactionRepository.save(transaction);
        
        log.info("Provider payout recorded successfully: transactionId={}", transaction.getId());
        return transaction;
    }
    
    /**
     * Record a refund transaction.
     */
    @Transactional
    public Transaction recordRefund(UUID refundId, UUID bookingId, UUID userId, UUID providerId,
                                  long refundAmountMinor, long platformFeeRefundMinor,
                                  String currency) {
        log.info("Recording refund: refundId={}, bookingId={}, refundAmount={}, currency={}",
                refundId, bookingId, refundAmountMinor, currency);
        
        // Get accounts
        Account userAccount = getOrCreateUserAccount(userId, currency);
        Account providerAccount = getOrCreateProviderAccount(providerId, currency);
        Account platformAccount = getPlatformAccount(currency);
        
        long providerRefundMinor = refundAmountMinor - platformFeeRefundMinor;
        
        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionType(Transaction.TransactionType.REFUND)
                .referenceType(LedgerEntry.ReferenceType.REFUND)
                .referenceId(refundId)
                .description("Refund for booking " + bookingId)
                .totalAmountMinor(refundAmountMinor)
                .currency(currency)
                .status(Transaction.TransactionStatus.PENDING)
                .build();
        
        transaction = transactionRepository.save(transaction);
        
        // Create ledger entries (reverse of original payment)
        // Credit user account (refund in)
        LedgerEntry userCredit = LedgerEntry.createCredit(
                userAccount, refundAmountMinor, currency,
                LedgerEntry.ReferenceType.REFUND, refundId,
                "Refund for booking " + bookingId,
                transaction.getId()
        );
        
        // Debit provider account (earnings back)
        LedgerEntry providerDebit = LedgerEntry.createDebit(
                providerAccount, providerRefundMinor, currency,
                LedgerEntry.ReferenceType.REFUND, refundId,
                "Refund for booking " + bookingId,
                transaction.getId()
        );
        
        // Debit platform account (fees back)
        LedgerEntry platformDebit = LedgerEntry.createDebit(
                platformAccount, platformFeeRefundMinor, currency,
                LedgerEntry.ReferenceType.REFUND, refundId,
                "Platform fee refund for booking " + bookingId,
                transaction.getId()
        );
        
        // Save entries
        ledgerEntryRepository.saveAll(List.of(userCredit, providerDebit, platformDebit));
        
        // Update account balances
        userAccount.credit(refundAmountMinor);
        providerAccount.debit(providerRefundMinor);
        platformAccount.debit(platformFeeRefundMinor);
        
        accountRepository.saveAll(List.of(userAccount, providerAccount, platformAccount));
        
        // Complete transaction
        transaction.complete();
        transactionRepository.save(transaction);
        
        log.info("Refund recorded successfully: transactionId={}", transaction.getId());
        return transaction;
    }
    
    /**
     * Get account balance.
     */
    public BigDecimal getAccountBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        return account.getBalanceDecimal();
    }
    
    /**
     * Get provider balance.
     */
    public BigDecimal getProviderBalance(UUID providerId, String currency) {
        Account account = accountRepository.findByOwnerIdAndOwnerTypeAndCurrency(
                providerId, Account.OwnerType.PROVIDER, currency)
                .orElse(null);
        return account != null ? account.getBalanceDecimal() : BigDecimal.ZERO;
    }
    
    /**
     * Get user balance.
     */
    public BigDecimal getUserBalance(UUID userId, String currency) {
        Account account = accountRepository.findByOwnerIdAndOwnerTypeAndCurrency(
                userId, Account.OwnerType.USER, currency)
                .orElse(null);
        return account != null ? account.getBalanceDecimal() : BigDecimal.ZERO;
    }
    
    /**
     * Get account transaction history.
     */
    public List<LedgerEntry> getAccountHistory(UUID accountId, int limit) {
        return ledgerEntryRepository.findByAccountIdOrderByCreatedAtDesc(accountId, limit);
    }
    
    /**
     * Get provider transaction history.
     */
    public List<LedgerEntry> getProviderHistory(UUID providerId, String currency, int limit) {
        Account account = getOrCreateProviderAccount(providerId, currency);
        return getAccountHistory(account.getId(), limit);
    }
    
    /**
     * Get or create user account.
     */
    private Account getOrCreateUserAccount(UUID userId, String currency) {
        return accountRepository.findByOwnerIdAndOwnerTypeAndCurrency(
                userId, Account.OwnerType.USER, currency)
                .orElseGet(() -> {
                    Account account = Account.builder()
                            .accountType(Account.AccountType.USER)
                            .ownerId(userId)
                            .ownerType(Account.OwnerType.USER)
                            .currency(currency)
                            .balanceMinor(0L)
                            .status(Account.AccountStatus.ACTIVE)
                            .build();
                    return accountRepository.save(account);
                });
    }
    
    /**
     * Get or create provider account.
     */
    private Account getOrCreateProviderAccount(UUID providerId, String currency) {
        return accountRepository.findByOwnerIdAndOwnerTypeAndCurrency(
                providerId, Account.OwnerType.PROVIDER, currency)
                .orElseGet(() -> {
                    Account account = Account.builder()
                            .accountType(Account.AccountType.PROVIDER)
                            .ownerId(providerId)
                            .ownerType(Account.OwnerType.PROVIDER)
                            .currency(currency)
                            .balanceMinor(0L)
                            .status(Account.AccountStatus.ACTIVE)
                            .build();
                    return accountRepository.save(account);
                });
    }
    
    /**
     * Get platform account.
     */
    private Account getPlatformAccount(String currency) {
        return accountRepository.findByOwnerIdAndOwnerTypeAndCurrency(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                Account.OwnerType.PLATFORM, currency)
                .orElseThrow(() -> new IllegalStateException("Platform account not found for currency: " + currency));
    }
}