package com.hopngo.service;

import com.hopngo.entity.LedgerEntry;
import com.hopngo.entity.LedgerEntry.AccountType;
import com.hopngo.entity.LedgerEntry.EntryType;
import com.hopngo.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LedgerVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerVerificationService.class);

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Nightly verification job - runs at 2 AM every day
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void performNightlyVerification() {
        logger.info("Starting nightly ledger verification at {}", LocalDateTime.now());
        
        try {
            // Verify double-entry balance
            boolean balanceValid = verifyDoubleEntryBalance();
            
            // Verify account balances
            boolean accountBalancesValid = verifyAccountBalances();
            
            // Check for orphaned entries
            boolean noOrphanedEntries = checkForOrphanedEntries();
            
            // Verify transaction integrity
            boolean transactionIntegrityValid = verifyTransactionIntegrity();
            
            if (balanceValid && accountBalancesValid && noOrphanedEntries && transactionIntegrityValid) {
                logger.info("Nightly ledger verification completed successfully");
            } else {
                logger.error("Ledger verification failed - sending alert to administrators");
                notificationService.sendLedgerVerificationAlert(
                    "Ledger verification failed", 
                    String.format("Balance: %s, Accounts: %s, Orphaned: %s, Integrity: %s",
                        balanceValid, accountBalancesValid, noOrphanedEntries, transactionIntegrityValid)
                );
            }
        } catch (Exception e) {
            logger.error("Error during nightly ledger verification", e);
            notificationService.sendLedgerVerificationAlert(
                "Ledger verification error", 
                "Exception occurred during verification: " + e.getMessage()
            );
        }
    }

    /**
     * Verify that total debits equal total credits (double-entry principle)
     */
    public boolean verifyDoubleEntryBalance() {
        logger.info("Verifying double-entry balance");
        
        BigDecimal totalDebits = ledgerEntryRepository.sumByEntryType(EntryType.DEBIT);
        BigDecimal totalCredits = ledgerEntryRepository.sumByEntryType(EntryType.CREDIT);
        
        if (totalDebits == null) totalDebits = BigDecimal.ZERO;
        if (totalCredits == null) totalCredits = BigDecimal.ZERO;
        
        boolean isBalanced = totalDebits.compareTo(totalCredits) == 0;
        
        if (isBalanced) {
            logger.info("Double-entry balance verified: Debits={}, Credits={}", totalDebits, totalCredits);
        } else {
            logger.error("Double-entry balance FAILED: Debits={}, Credits={}, Difference={}", 
                totalDebits, totalCredits, totalDebits.subtract(totalCredits));
        }
        
        return isBalanced;
    }

    /**
     * Verify individual account balances
     */
    public boolean verifyAccountBalances() {
        logger.info("Verifying account balances");
        
        boolean allValid = true;
        
        for (AccountType accountType : AccountType.values()) {
            BigDecimal balance = calculateAccountBalance(accountType);
            boolean isValid = validateAccountBalance(accountType, balance);
            
            if (!isValid) {
                logger.error("Invalid balance for account {}: {}", accountType, balance);
                allValid = false;
            } else {
                logger.debug("Account {} balance verified: {}", accountType, balance);
            }
        }
        
        return allValid;
    }

    /**
     * Calculate balance for a specific account type
     */
    public BigDecimal calculateAccountBalance(AccountType accountType) {
        BigDecimal debits = ledgerEntryRepository.sumByAccountTypeAndEntryType(accountType, EntryType.DEBIT);
        BigDecimal credits = ledgerEntryRepository.sumByAccountTypeAndEntryType(accountType, EntryType.CREDIT);
        
        if (debits == null) debits = BigDecimal.ZERO;
        if (credits == null) credits = BigDecimal.ZERO;
        
        // For asset and expense accounts: balance = debits - credits
        // For liability, equity, and revenue accounts: balance = credits - debits
        switch (accountType) {
            case CASH:
            case ACCOUNTS_RECEIVABLE:
            case STRIPE_BALANCE:
            case BKASH_BALANCE:
            case NAGAD_BALANCE:
            case DISPUTE_RESERVE:
            case AVAILABLE_BALANCE:
                return debits.subtract(credits);
            case ACCOUNTS_PAYABLE:
            case PROVIDER_PAYOUTS:
            case REFUNDS_PAYABLE:
            case PLATFORM_REVENUE:
            case TRANSACTION_FEES:
                return credits.subtract(debits);
            case PAYMENT_PROCESSING_FEES:
            case CHARGEBACK_FEES:
            case DISPUTE_FEES:
                return debits.subtract(credits);
            default:
                return debits.subtract(credits);
        }
    }

    /**
     * Validate that account balance is reasonable
     */
    private boolean validateAccountBalance(AccountType accountType, BigDecimal balance) {
        switch (accountType) {
            case CASH:
            case ACCOUNTS_RECEIVABLE:
            case STRIPE_BALANCE:
            case BKASH_BALANCE:
            case NAGAD_BALANCE:
            case AVAILABLE_BALANCE:
            case PLATFORM_REVENUE:
            case TRANSACTION_FEES:
                // These should generally be positive
                return balance.compareTo(BigDecimal.ZERO) >= 0;
            case ACCOUNTS_PAYABLE:
            case PROVIDER_PAYOUTS:
            case REFUNDS_PAYABLE:
                // These can be positive (we owe money) or zero
                return balance.compareTo(BigDecimal.ZERO) >= 0;
            case PAYMENT_PROCESSING_FEES:
            case CHARGEBACK_FEES:
            case DISPUTE_FEES:
            case DISPUTE_RESERVE:
                // These are typically positive (expenses/losses)
                return balance.compareTo(BigDecimal.ZERO) >= 0;
            default:
                return true; // No specific validation
        }
    }

    /**
     * Check for ledger entries without corresponding transactions or orders
     */
    public boolean checkForOrphanedEntries() {
        logger.info("Checking for orphaned ledger entries");
        
        List<LedgerEntry> orphanedEntries = ledgerEntryRepository.findOrphanedEntries();
        
        if (orphanedEntries.isEmpty()) {
            logger.info("No orphaned ledger entries found");
            return true;
        } else {
            logger.error("Found {} orphaned ledger entries", orphanedEntries.size());
            for (LedgerEntry entry : orphanedEntries) {
                logger.error("Orphaned entry: ID={}, Amount={}, Account={}", 
                    entry.getEntryId(), entry.getAmount(), entry.getAccountType());
            }
            return false;
        }
    }

    /**
     * Verify transaction integrity - each transaction should have balanced ledger entries
     */
    public boolean verifyTransactionIntegrity() {
        logger.info("Verifying transaction integrity");
        
        List<Object[]> unbalancedTransactions = ledgerEntryRepository.findUnbalancedTransactions();
        
        if (unbalancedTransactions.isEmpty()) {
            logger.info("All transactions have balanced ledger entries");
            return true;
        } else {
            logger.error("Found {} transactions with unbalanced ledger entries", unbalancedTransactions.size());
            for (Object[] result : unbalancedTransactions) {
                Long transactionId = (Long) result[0];
                BigDecimal imbalance = (BigDecimal) result[1];
                logger.error("Unbalanced transaction: ID={}, Imbalance={}", transactionId, imbalance);
            }
            return false;
        }
    }

    /**
     * Get daily balance summary
     */
    public Map<AccountType, BigDecimal> getDailyBalanceSummary() {
        return java.util.Arrays.stream(AccountType.values())
            .collect(Collectors.toMap(
                accountType -> accountType,
                this::calculateAccountBalance
            ));
    }

    /**
     * Get balance summary for a specific date
     */
    public Map<AccountType, BigDecimal> getBalanceSummaryForDate(LocalDate date) {
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        return java.util.Arrays.stream(AccountType.values())
            .collect(Collectors.toMap(
                accountType -> accountType,
                accountType -> calculateAccountBalanceAsOfDate(accountType, endOfDay)
            ));
    }

    /**
     * Calculate account balance as of a specific date
     */
    private BigDecimal calculateAccountBalanceAsOfDate(AccountType accountType, LocalDateTime asOfDate) {
        BigDecimal debits = ledgerEntryRepository.sumByAccountTypeAndEntryTypeBeforeDate(
            accountType, EntryType.DEBIT, asOfDate);
        BigDecimal credits = ledgerEntryRepository.sumByAccountTypeAndEntryTypeBeforeDate(
            accountType, EntryType.CREDIT, asOfDate);
        
        if (debits == null) debits = BigDecimal.ZERO;
        if (credits == null) credits = BigDecimal.ZERO;
        
        switch (accountType) {
            case CASH:
            case ACCOUNTS_RECEIVABLE:
            case STRIPE_BALANCE:
            case BKASH_BALANCE:
            case NAGAD_BALANCE:
            case DISPUTE_RESERVE:
            case AVAILABLE_BALANCE:
                return debits.subtract(credits);
            case ACCOUNTS_PAYABLE:
            case PROVIDER_PAYOUTS:
            case REFUNDS_PAYABLE:
            case PLATFORM_REVENUE:
            case TRANSACTION_FEES:
                return credits.subtract(debits);
            case PAYMENT_PROCESSING_FEES:
            case CHARGEBACK_FEES:
            case DISPUTE_FEES:
                return debits.subtract(credits);
            default:
                return debits.subtract(credits);
        }
    }

    /**
     * Manual verification trigger for testing or admin use
     */
    public VerificationResult performManualVerification() {
        logger.info("Performing manual ledger verification");
        
        boolean balanceValid = verifyDoubleEntryBalance();
        boolean accountBalancesValid = verifyAccountBalances();
        boolean noOrphanedEntries = checkForOrphanedEntries();
        boolean transactionIntegrityValid = verifyTransactionIntegrity();
        
        return new VerificationResult(
            balanceValid,
            accountBalancesValid,
            noOrphanedEntries,
            transactionIntegrityValid,
            LocalDateTime.now()
        );
    }

    /**
     * Result class for verification operations
     */
    public static class VerificationResult {
        private final boolean doubleEntryBalanced;
        private final boolean accountBalancesValid;
        private final boolean noOrphanedEntries;
        private final boolean transactionIntegrityValid;
        private final LocalDateTime verificationTime;

        public VerificationResult(boolean doubleEntryBalanced, boolean accountBalancesValid,
                                boolean noOrphanedEntries, boolean transactionIntegrityValid,
                                LocalDateTime verificationTime) {
            this.doubleEntryBalanced = doubleEntryBalanced;
            this.accountBalancesValid = accountBalancesValid;
            this.noOrphanedEntries = noOrphanedEntries;
            this.transactionIntegrityValid = transactionIntegrityValid;
            this.verificationTime = verificationTime;
        }

        public boolean isAllValid() {
            return doubleEntryBalanced && accountBalancesValid && noOrphanedEntries && transactionIntegrityValid;
        }

        // Getters
        public boolean isDoubleEntryBalanced() { return doubleEntryBalanced; }
        public boolean isAccountBalancesValid() { return accountBalancesValid; }
        public boolean isNoOrphanedEntries() { return noOrphanedEntries; }
        public boolean isTransactionIntegrityValid() { return transactionIntegrityValid; }
        public LocalDateTime getVerificationTime() { return verificationTime; }
    }
}