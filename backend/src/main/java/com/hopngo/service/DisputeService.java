package com.hopngo.service;

import com.hopngo.entity.*;
import com.hopngo.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.hopngo.util.FinancialCalculationUtil;

@Service
@Transactional
public class DisputeService {

    private static final Logger logger = LoggerFactory.getLogger(DisputeService.class);

    @Autowired
    private DisputeRepository disputeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SupportTicketService supportTicketService;

    @Autowired
    private LedgerService ledgerService;

    // Stripe Dispute Handlers

    public void handleStripeDisputeCreated(String disputeId, String chargeId, String reason, 
                                         Integer amountCents, String currency, String status) {
        try {
            // Find the transaction by Stripe charge ID
            Optional<Transaction> transactionOpt = transactionRepository.findByProviderTransactionId(chargeId);
            
            if (!transactionOpt.isPresent()) {
                logger.warn("Transaction not found for Stripe charge: {}", chargeId);
                // Create support ticket for missing transaction
                supportTicketService.createDisputeTicket(
                    "STRIPE_DISPUTE_NO_TRANSACTION",
                    "Stripe dispute received for unknown charge: " + chargeId,
                    "HIGH",
                    "Dispute ID: " + disputeId + ", Charge ID: " + chargeId + ", Amount: " + amountCents + " " + currency
                );
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            BigDecimal disputeAmount = FinancialCalculationUtil.fromCents(amountCents, currency);
            
            // Create dispute record
            Dispute dispute = new Dispute();
            dispute.setTransaction(transaction);
            dispute.setProviderDisputeId(disputeId);
            dispute.setDisputeType(Dispute.DisputeType.CHARGEBACK);
            dispute.setStatus(mapStripeStatus(status));
            dispute.setReason(mapStripeReason(reason));
            dispute.setDisputedAmount(disputeAmount);
            dispute.setCurrency(currency.toUpperCase());
            dispute.setCreatedAt(FinancialCalculationUtil.getCurrentUtcTime());
            dispute.setEvidenceDueBy(FinancialCalculationUtil.getCurrentUtcTime().plusDays(7)); // Default 7 days
            
            disputeRepository.save(dispute);
            
            // Freeze funds by creating ledger entries
            freezeFundsForDispute(transaction, disputeAmount, "Stripe dispute: " + disputeId);
            
            // Send notifications
            notificationService.sendDisputeNotification(
                disputeId,
                "STRIPE",
                disputeAmount.toString(),
                reason
            );
            
            // Create support ticket for high-value disputes
            BigDecimal highValueThreshold = FinancialCalculationUtil.normalizeAmount(BigDecimal.valueOf(1000), currency);
            if (disputeAmount.compareTo(highValueThreshold) > 0) {
                supportTicketService.createDisputeTicket(
                    "HIGH_VALUE_STRIPE_DISPUTE",
                    "High-value Stripe dispute requires immediate attention",
                    "HIGH",
                    String.format("Dispute ID: %s, Transaction: %s, Amount: %s %s",
                        disputeId, transaction.getTransactionId(), disputeAmount, currency)
                );
            }
            
            logger.info("Stripe dispute created successfully: {}", disputeId);
            
        } catch (Exception e) {
            logger.error("Error handling Stripe dispute creation: {}", disputeId, e);
            throw new RuntimeException("Failed to process Stripe dispute", e);
        }
    }

    public void handleStripeDisputeUpdated(String disputeId, String status) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("Dispute not found for update: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            Dispute.DisputeStatus oldStatus = dispute.getStatus();
            Dispute.DisputeStatus newStatus = mapStripeStatus(status);
            
            dispute.setStatus(newStatus);
            dispute.setUpdatedAt(FinancialCalculationUtil.getCurrentUtcTime());
            
            disputeRepository.save(dispute);
            
            // Send notification if status changed significantly
            if (oldStatus != newStatus) {
                notificationService.sendDisputeNotification(
                    disputeId,
                    "STRIPE",
                    dispute.getDisputedAmount().toString(),
                    "Status changed from " + oldStatus + " to " + newStatus
                );
            }
            
            logger.info("Stripe dispute updated: {} status: {}", disputeId, status);
            
        } catch (Exception e) {
            logger.error("Error handling Stripe dispute update: {}", disputeId, e);
        }
    }

    public void handleStripeDisputeClosed(String disputeId, String status) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("Dispute not found for closure: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            dispute.setStatus(mapStripeStatus(status));
            dispute.setResolvedAt(FinancialCalculationUtil.getCurrentUtcTime());
            dispute.setUpdatedAt(FinancialCalculationUtil.getCurrentUtcTime());
            
            disputeRepository.save(dispute);
            
            // Unfreeze funds if dispute was won
            if ("won".equals(status) || "warning_closed".equals(status)) {
                unfreezeFundsForDispute(dispute.getTransaction(), dispute.getDisputedAmount(), 
                    "Stripe dispute won: " + disputeId);
            }
            
            // Send notification
            notificationService.sendDisputeNotification(
                disputeId,
                "Stripe",
                dispute.getDisputedAmount().toString(),
                "Dispute closed with status: " + status
            );
            
            logger.info("Stripe dispute closed: {} status: {}", disputeId, status);
            
        } catch (Exception e) {
            logger.error("Error handling Stripe dispute closure: {}", disputeId, e);
        }
    }

    public void handleStripeDisputeFundsWithdrawn(String disputeId, Integer amountCents) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("Dispute not found for funds withdrawal: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            BigDecimal withdrawnAmount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));
            
            // Update dispute with withdrawn amount
            dispute.setDisputedAmount(withdrawnAmount);
            dispute.setUpdatedAt(LocalDateTime.now());
            
            disputeRepository.save(dispute);
            
            // Create ledger entry for funds withdrawal
            ledgerService.createLedgerEntry(
                dispute.getTransaction(),
                LedgerEntry.EntryType.DEBIT,
                LedgerEntry.AccountType.DISPUTE_RESERVE,
                withdrawnAmount,
                "Funds withdrawn for Stripe dispute: " + disputeId
            );
            
            // Send notification
            notificationService.sendDisputeNotification(
                disputeId,
                "Stripe",
                withdrawnAmount.toString(),
                "Funds withdrawn for dispute"
            );
            
            logger.info("Stripe dispute funds withdrawn: {} amount: {}", disputeId, withdrawnAmount);
            
        } catch (Exception e) {
            logger.error("Error handling Stripe dispute funds withdrawal: {}", disputeId, e);
        }
    }

    public void handleStripeDisputeFundsReinstated(String disputeId, Integer amountCents) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("Dispute not found for funds reinstatement: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            BigDecimal reinstatedAmount = BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));
            
            // Update dispute
            dispute.setUpdatedAt(LocalDateTime.now());
            disputeRepository.save(dispute);
            
            // Create ledger entry for funds reinstatement
            ledgerService.createLedgerEntry(
                dispute.getTransaction(),
                LedgerEntry.EntryType.CREDIT,
                LedgerEntry.AccountType.DISPUTE_RESERVE,
                reinstatedAmount,
                "Funds reinstated for Stripe dispute: " + disputeId
            );
            
            // Send notification
            notificationService.sendDisputeNotification(
                disputeId,
                "Stripe",
                reinstatedAmount.toString(),
                "Funds reinstated for dispute"
            );
            
            logger.info("Stripe dispute funds reinstated: {} amount: {}", disputeId, reinstatedAmount);
            
        } catch (Exception e) {
            logger.error("Error handling Stripe dispute funds reinstatement: {}", disputeId, e);
        }
    }

    // bKash Dispute Handlers

    public void handleBkashDisputeCreated(String disputeId, String paymentId, String reason, 
                                        String amount, String status) {
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByProviderTransactionId(paymentId);
            
            if (!transactionOpt.isPresent()) {
                logger.warn("Transaction not found for bKash payment: {}", paymentId);
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            BigDecimal disputeAmount = new BigDecimal(amount);
            
            // Create dispute record
            Dispute dispute = new Dispute();
            dispute.setTransaction(transaction);
            dispute.setProviderDisputeId(disputeId);
            dispute.setDisputeType(Dispute.DisputeType.CHARGEBACK);
            dispute.setStatus(mapBkashStatus(status));
            dispute.setReason(mapBkashReason(reason));
            dispute.setDisputedAmount(disputeAmount);
            dispute.setCurrency("BDT");
            dispute.setCreatedAt(LocalDateTime.now());
            dispute.setEvidenceDueBy(LocalDateTime.now().plusDays(5)); // bKash typically gives 5 days
            
            disputeRepository.save(dispute);
            
            // Freeze funds
            freezeFundsForDispute(transaction, disputeAmount, "bKash dispute: " + disputeId);
            
            // Send notifications
            notificationService.sendDisputeNotification(
                disputeId,
                "bKash",
                amount,
                reason
            );
            
            logger.info("bKash dispute created successfully: {}", disputeId);
            
        } catch (Exception e) {
            logger.error("Error handling bKash dispute creation: {}", disputeId, e);
        }
    }

    public void handleBkashDisputeUpdated(String disputeId, String status) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("bKash dispute not found for update: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            dispute.setStatus(mapBkashStatus(status));
            dispute.setUpdatedAt(LocalDateTime.now());
            
            disputeRepository.save(dispute);
            
            logger.info("bKash dispute updated: {} status: {}", disputeId, status);
            
        } catch (Exception e) {
            logger.error("Error handling bKash dispute update: {}", disputeId, e);
        }
    }

    public void handleBkashDisputeResolved(String disputeId, String resolution) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("bKash dispute not found for resolution: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            dispute.setStatus("ACCEPTED".equals(resolution) ? 
                Dispute.DisputeStatus.WON : Dispute.DisputeStatus.LOST);
            dispute.setResolvedAt(LocalDateTime.now());
            dispute.setUpdatedAt(LocalDateTime.now());
            
            disputeRepository.save(dispute);
            
            // Unfreeze funds if dispute was won
            if ("ACCEPTED".equals(resolution)) {
                unfreezeFundsForDispute(dispute.getTransaction(), dispute.getDisputedAmount(), 
                    "bKash dispute won: " + disputeId);
            }
            
            logger.info("bKash dispute resolved: {} resolution: {}", disputeId, resolution);
            
        } catch (Exception e) {
            logger.error("Error handling bKash dispute resolution: {}", disputeId, e);
        }
    }

    public void handleBkashChargebackInitiated(String chargebackId, String paymentId, String amount) {
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByProviderTransactionId(paymentId);
            
            if (!transactionOpt.isPresent()) {
                logger.warn("Transaction not found for bKash chargeback: {}", paymentId);
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            BigDecimal chargebackAmount = new BigDecimal(amount);
            
            // Create dispute record for chargeback
            Dispute dispute = new Dispute();
            dispute.setTransaction(transaction);
            dispute.setProviderDisputeId(chargebackId);
            dispute.setDisputeType(Dispute.DisputeType.CHARGEBACK);
            dispute.setStatus(Dispute.DisputeStatus.UNDER_REVIEW);
            dispute.setReason(Dispute.DisputeReason.FRAUDULENT);
            dispute.setDisputedAmount(chargebackAmount);
            dispute.setCurrency("BDT");
            dispute.setCreatedAt(LocalDateTime.now());
            
            disputeRepository.save(dispute);
            
            // Freeze funds immediately for chargeback
            freezeFundsForDispute(transaction, chargebackAmount, "bKash chargeback: " + chargebackId);
            
            logger.info("bKash chargeback initiated: {}", chargebackId);
            
        } catch (Exception e) {
            logger.error("Error handling bKash chargeback: {}", chargebackId, e);
        }
    }

    // Nagad Dispute Handlers

    public void handleNagadDisputeCreated(String disputeId, String orderId, String reason, 
                                        String amount, String status) {
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByProviderTransactionId(orderId);
            
            if (!transactionOpt.isPresent()) {
                logger.warn("Transaction not found for Nagad order: {}", orderId);
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            BigDecimal disputeAmount = new BigDecimal(amount);
            
            // Create dispute record
            Dispute dispute = new Dispute();
            dispute.setTransaction(transaction);
            dispute.setProviderDisputeId(disputeId);
            dispute.setDisputeType(Dispute.DisputeType.CHARGEBACK);
            dispute.setStatus(mapNagadStatus(status));
            dispute.setReason(mapNagadReason(reason));
            dispute.setDisputedAmount(disputeAmount);
            dispute.setCurrency("BDT");
            dispute.setCreatedAt(LocalDateTime.now());
            dispute.setEvidenceDueBy(LocalDateTime.now().plusDays(7));
            
            disputeRepository.save(dispute);
            
            // Freeze funds
            freezeFundsForDispute(transaction, disputeAmount, "Nagad dispute: " + disputeId);
            
            // Send notifications
            notificationService.sendDisputeNotification(
                disputeId,
                "Nagad",
                amount,
                reason
            );
            
            logger.info("Nagad dispute created successfully: {}", disputeId);
            
        } catch (Exception e) {
            logger.error("Error handling Nagad dispute creation: {}", disputeId, e);
        }
    }

    public void handleNagadDisputeUpdated(String disputeId, String status) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("Nagad dispute not found for update: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            dispute.setStatus(mapNagadStatus(status));
            dispute.setUpdatedAt(LocalDateTime.now());
            
            disputeRepository.save(dispute);
            
            logger.info("Nagad dispute updated: {} status: {}", disputeId, status);
            
        } catch (Exception e) {
            logger.error("Error handling Nagad dispute update: {}", disputeId, e);
        }
    }

    public void handleNagadDisputeClosed(String disputeId, String outcome) {
        try {
            Optional<Dispute> disputeOpt = disputeRepository.findByProviderDisputeId(disputeId);
            
            if (!disputeOpt.isPresent()) {
                logger.warn("Nagad dispute not found for closure: {}", disputeId);
                return;
            }
            
            Dispute dispute = disputeOpt.get();
            dispute.setStatus("MERCHANT_WON".equals(outcome) ? 
                Dispute.DisputeStatus.WON : Dispute.DisputeStatus.LOST);
            dispute.setResolvedAt(LocalDateTime.now());
            dispute.setUpdatedAt(LocalDateTime.now());
            
            disputeRepository.save(dispute);
            
            // Unfreeze funds if dispute was won
            if ("MERCHANT_WON".equals(outcome)) {
                unfreezeFundsForDispute(dispute.getTransaction(), dispute.getDisputedAmount(), 
                    "Nagad dispute won: " + disputeId);
            }
            
            logger.info("Nagad dispute closed: {} outcome: {}", disputeId, outcome);
            
        } catch (Exception e) {
            logger.error("Error handling Nagad dispute closure: {}", disputeId, e);
        }
    }

    public void handleNagadChargebackReceived(String chargebackId, String orderId, String amount) {
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findByProviderTransactionId(orderId);
            
            if (!transactionOpt.isPresent()) {
                logger.warn("Transaction not found for Nagad chargeback: {}", orderId);
                return;
            }
            
            Transaction transaction = transactionOpt.get();
            BigDecimal chargebackAmount = new BigDecimal(amount);
            
            // Create dispute record for chargeback
            Dispute dispute = new Dispute();
            dispute.setTransaction(transaction);
            dispute.setProviderDisputeId(chargebackId);
            dispute.setDisputeType(Dispute.DisputeType.CHARGEBACK);
            dispute.setStatus(Dispute.DisputeStatus.UNDER_REVIEW);
            dispute.setReason(Dispute.DisputeReason.FRAUDULENT);
            dispute.setDisputedAmount(chargebackAmount);
            dispute.setCurrency("BDT");
            dispute.setCreatedAt(LocalDateTime.now());
            
            disputeRepository.save(dispute);
            
            // Freeze funds immediately for chargeback
            freezeFundsForDispute(transaction, chargebackAmount, "Nagad chargeback: " + chargebackId);
            
            logger.info("Nagad chargeback received: {}", chargebackId);
            
        } catch (Exception e) {
            logger.error("Error handling Nagad chargeback: {}", chargebackId, e);
        }
    }

    // Helper Methods

    private void freezeFundsForDispute(Transaction transaction, BigDecimal amount, String description) {
        try {
            // Create debit entry to freeze funds from available balance
            ledgerService.createLedgerEntry(
                transaction,
                LedgerEntry.EntryType.DEBIT,
                LedgerEntry.AccountType.AVAILABLE_BALANCE,
                amount,
                description + " - Funds frozen"
            );
            
            // Create credit entry to move funds to dispute reserve
            ledgerService.createLedgerEntry(
                transaction,
                LedgerEntry.EntryType.CREDIT,
                LedgerEntry.AccountType.DISPUTE_RESERVE,
                amount,
                description + " - Moved to dispute reserve"
            );
            
            logger.info("Funds frozen for dispute: {} amount: {}", transaction.getTransactionId(), amount);
            
        } catch (Exception e) {
            logger.error("Error freezing funds for dispute", e);
        }
    }

    private void unfreezeFundsForDispute(Transaction transaction, BigDecimal amount, String description) {
        try {
            // Create debit entry to remove funds from dispute reserve
            ledgerService.createLedgerEntry(
                transaction,
                LedgerEntry.EntryType.DEBIT,
                LedgerEntry.AccountType.DISPUTE_RESERVE,
                amount,
                description + " - Funds unfrozen"
            );
            
            // Create credit entry to return funds to available balance
            ledgerService.createLedgerEntry(
                transaction,
                LedgerEntry.EntryType.CREDIT,
                LedgerEntry.AccountType.AVAILABLE_BALANCE,
                amount,
                description + " - Returned to available balance"
            );
            
            logger.info("Funds unfrozen for dispute: {} amount: {}", transaction.getTransactionId(), amount);
            
        } catch (Exception e) {
            logger.error("Error unfreezing funds for dispute", e);
        }
    }

    // Status Mapping Methods

    private Dispute.DisputeStatus mapStripeStatus(String status) {
        switch (status.toLowerCase()) {
            case "warning_needs_response":
            case "warning_under_review":
            case "needs_response":
            case "under_review":
                return Dispute.DisputeStatus.UNDER_REVIEW;
            case "charge_refunded":
            case "won":
                return Dispute.DisputeStatus.WON;
            case "lost":
                return Dispute.DisputeStatus.LOST;
            case "warning_closed":
                return Dispute.DisputeStatus.CLOSED;
            default:
                return Dispute.DisputeStatus.UNDER_REVIEW;
        }
    }

    private Dispute.DisputeStatus mapBkashStatus(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
            case "UNDER_REVIEW":
                return Dispute.DisputeStatus.UNDER_REVIEW;
            case "ACCEPTED":
                return Dispute.DisputeStatus.WON;
            case "REJECTED":
                return Dispute.DisputeStatus.LOST;
            case "CLOSED":
                return Dispute.DisputeStatus.CLOSED;
            default:
                return Dispute.DisputeStatus.UNDER_REVIEW;
        }
    }

    private Dispute.DisputeStatus mapNagadStatus(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
            case "UNDER_REVIEW":
                return Dispute.DisputeStatus.UNDER_REVIEW;
            case "MERCHANT_WON":
                return Dispute.DisputeStatus.WON;
            case "CUSTOMER_WON":
                return Dispute.DisputeStatus.LOST;
            case "CLOSED":
                return Dispute.DisputeStatus.CLOSED;
            default:
                return Dispute.DisputeStatus.UNDER_REVIEW;
        }
    }

    private Dispute.DisputeReason mapStripeReason(String reason) {
        switch (reason.toLowerCase()) {
            case "fraudulent":
                return Dispute.DisputeReason.FRAUDULENT;
            case "subscription_canceled":
            case "product_unacceptable":
            case "product_not_received":
                return Dispute.DisputeReason.PRODUCT_NOT_RECEIVED;
            case "unrecognized":
            case "duplicate":
                return Dispute.DisputeReason.UNRECOGNIZED;
            case "credit_not_processed":
                return Dispute.DisputeReason.CREDIT_NOT_PROCESSED;
            default:
                return Dispute.DisputeReason.OTHER;
        }
    }

    private Dispute.DisputeReason mapBkashReason(String reason) {
        switch (reason.toUpperCase()) {
            case "FRAUD":
            case "FRAUDULENT":
                return Dispute.DisputeReason.FRAUDULENT;
            case "NOT_RECEIVED":
            case "PRODUCT_NOT_RECEIVED":
                return Dispute.DisputeReason.PRODUCT_NOT_RECEIVED;
            case "UNAUTHORIZED":
            case "UNRECOGNIZED":
                return Dispute.DisputeReason.UNRECOGNIZED;
            default:
                return Dispute.DisputeReason.OTHER;
        }
    }

    private Dispute.DisputeReason mapNagadReason(String reason) {
        switch (reason.toLowerCase()) {
            case "fraud":
            case "fraudulent":
                return Dispute.DisputeReason.FRAUDULENT;
            case "not_received":
            case "product_not_received":
                return Dispute.DisputeReason.PRODUCT_NOT_RECEIVED;
            case "unauthorized":
            case "unrecognized":
                return Dispute.DisputeReason.UNRECOGNIZED;
            case "refund_not_processed":
                return Dispute.DisputeReason.CREDIT_NOT_PROCESSED;
            default:
                return Dispute.DisputeReason.OTHER;
        }
    }

    // Public API Methods

    public List<Dispute> getActiveDisputes() {
        return disputeRepository.findActiveDisputes();
    }

    public List<Dispute> getDisputesByProvider(String provider) {
        return disputeRepository.findByPaymentProvider(Transaction.PaymentProvider.valueOf(provider.toUpperCase()));
    }

    public Optional<Dispute> getDisputeById(Long disputeId) {
        return disputeRepository.findById(disputeId);
    }

    public List<Dispute> getDisputesRequiringEvidence() {
        return disputeRepository.findDisputesWithUpcomingEvidenceDeadline(LocalDateTime.now().plusDays(2));
    }
}