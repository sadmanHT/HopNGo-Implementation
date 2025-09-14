package com.hopngo.service;

import com.hopngo.entity.LedgerEntry;
import com.hopngo.entity.Transaction;
import com.hopngo.repository.LedgerEntryRepository;
import com.hopngo.util.FinancialCalculationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for managing ledger entries and financial transactions
 */
@Service
@Transactional
public class LedgerService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerService.class);

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    /**
     * Create a new ledger entry
     *
     * @param transaction The transaction associated with this entry
     * @param entryType The type of entry (DEBIT or CREDIT)
     * @param accountType The account type for this entry
     * @param amount The amount for this entry
     * @param description Description of the entry
     * @return The created ledger entry
     */
    public LedgerEntry createLedgerEntry(Transaction transaction, 
                                       LedgerEntry.EntryType entryType,
                                       LedgerEntry.AccountType accountType,
                                       BigDecimal amount,
                                       String description) {
        try {
            // Generate unique entry ID
            String entryId = "LE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            
            // Create ledger entry
            LedgerEntry entry = new LedgerEntry();
            entry.setEntryId(entryId);
            entry.setTransaction(transaction);
            entry.setEntryType(entryType);
            entry.setAccountType(accountType);
            entry.setAmount(amount);
            entry.setCurrency(transaction != null ? transaction.getCurrency() : "USD");
            entry.setDescription(description);
            entry.setEffectiveDate(FinancialCalculationUtil.getCurrentUtcTime());
            entry.setVerified(false);
            
            // Save the entry
            LedgerEntry savedEntry = ledgerEntryRepository.save(entry);
            
            logger.info("Created ledger entry: {} for transaction: {} amount: {} {}", 
                       entryId, 
                       transaction != null ? transaction.getTransactionId() : "N/A", 
                       amount, 
                       entry.getCurrency());
            
            return savedEntry;
            
        } catch (Exception e) {
            logger.error("Error creating ledger entry for transaction: {} amount: {}", 
                        transaction != null ? transaction.getTransactionId() : "N/A", 
                        amount, e);
            throw new RuntimeException("Failed to create ledger entry", e);
        }
    }

    /**
     * Create a ledger entry without a transaction (for system entries)
     *
     * @param entryType The type of entry (DEBIT or CREDIT)
     * @param accountType The account type for this entry
     * @param amount The amount for this entry
     * @param currency The currency for this entry
     * @param description Description of the entry
     * @return The created ledger entry
     */
    public LedgerEntry createLedgerEntry(LedgerEntry.EntryType entryType,
                                       LedgerEntry.AccountType accountType,
                                       BigDecimal amount,
                                       String currency,
                                       String description) {
        return createLedgerEntry(null, entryType, accountType, amount, description);
    }
}