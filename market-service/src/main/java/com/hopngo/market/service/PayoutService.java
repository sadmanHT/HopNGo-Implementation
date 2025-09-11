package com.hopngo.market.service;

import com.hopngo.market.entity.Payout;
import com.hopngo.market.exception.InsufficientBalanceException;
import com.hopngo.market.exception.PayoutNotFoundException;
import com.hopngo.market.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing provider payouts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {
    
    private final PayoutRepository payoutRepository;
    private final LedgerService ledgerService;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Request a payout for a provider.
     */
    @Transactional
    public Payout requestPayout(PayoutRequest request) {
        log.info("Processing payout request: providerId={}, amount={}, currency={}, method={}",
                request.providerId(), request.amountDecimal(), request.currency(), request.method());
        
        // Validate provider has sufficient available balance
        BigDecimal availableBalance = getProviderAvailableBalance(request.providerId(), request.currency());
        if (availableBalance.compareTo(request.amountDecimal()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Available: %s %s, Requested: %s %s",
                            availableBalance, request.currency(),
                            request.amountDecimal(), request.currency()));
        }
        
        // Create payout entity
        Payout.PayoutBuilder payoutBuilder = Payout.builder()
                .providerId(request.providerId())
                .amountMinor(request.amountDecimal().multiply(new BigDecimal("100")).longValue())
                .currency(request.currency())
                .method(request.method())
                .status(Payout.PayoutStatus.PENDING)
                .requestedBy(request.requestedBy())
                .requestedAt(LocalDateTime.now())
                .notes(request.notes());
        
        // Set method-specific details
        if (request.method() == Payout.PayoutMethod.BANK) {
            payoutBuilder
                    .bankName(request.bankName())
                    .accountNumber(request.accountNumber())
                    .accountHolderName(request.accountHolderName())
                    .routingNumber(request.routingNumber());
        } else {
            payoutBuilder
                    .mobileNumber(request.mobileNumber())
                    .mobileAccountName(request.mobileAccountName());
        }
        
        Payout payout = payoutRepository.save(payoutBuilder.build());
        
        // Publish payout created event
        eventPublisher.publishEvent(new PayoutCreatedEvent(payout.getId(), payout.getProviderId(),
                payout.getAmountDecimal(), payout.getCurrency()));
        
        log.info("Payout request created: payoutId={}, referenceNumber={}",
                payout.getId(), payout.getReferenceNumber());
        
        return payout;
    }
    
    /**
     * Approve a payout (admin action).
     */
    @Transactional
    public Payout approvePayout(UUID payoutId, UUID approvedBy) {
        log.info("Approving payout: payoutId={}, approvedBy={}", payoutId, approvedBy);
        
        Payout payout = getPayoutById(payoutId);
        
        // Validate payout can be approved
        if (!payout.canBeApproved()) {
            throw new IllegalStateException("Payout cannot be approved in current status: " + payout.getStatus());
        }
        
        // Double-check balance availability
        BigDecimal availableBalance = getProviderAvailableBalance(payout.getProviderId(), payout.getCurrency());
        if (availableBalance.compareTo(payout.getAmountDecimal()) < 0) {
            throw new InsufficientBalanceException("Provider no longer has sufficient balance for payout");
        }
        
        payout.approve(approvedBy);
        payout = payoutRepository.save(payout);
        
        log.info("Payout approved: payoutId={}, referenceNumber={}", payoutId, payout.getReferenceNumber());
        
        return payout;
    }
    
    /**
     * Process a payout (start payment processing).
     */
    @Transactional
    public Payout processPayout(UUID payoutId, UUID processedBy) {
        log.info("Processing payout: payoutId={}, processedBy={}", payoutId, processedBy);
        
        Payout payout = getPayoutById(payoutId);
        
        if (!payout.canBeProcessed()) {
            throw new IllegalStateException("Payout cannot be processed in current status: " + payout.getStatus());
        }
        
        // Record ledger transaction for payout
        ledgerService.recordProviderPayout(payoutId, payout.getProviderId(),
                payout.getAmountMinor(), payout.getCurrency());
        
        payout.startProcessing(processedBy);
        payout = payoutRepository.save(payout);
        
        // Publish payout processing event
        eventPublisher.publishEvent(new PayoutProcessingEvent(payout.getId(), payout.getProviderId(),
                payout.getAmountDecimal(), payout.getCurrency()));
        
        log.info("Payout processing started: payoutId={}, referenceNumber={}",
                payoutId, payout.getReferenceNumber());
        
        return payout;
    }
    
    /**
     * Mark payout as paid (external payment completed).
     */
    @Transactional
    public Payout markPayoutAsPaid(UUID payoutId, String externalTransactionId) {
        log.info("Marking payout as paid: payoutId={}, externalTransactionId={}",
                payoutId, externalTransactionId);
        
        Payout payout = getPayoutById(payoutId);
        payout.markAsPaid(externalTransactionId);
        payout = payoutRepository.save(payout);
        
        // Publish payout paid event
        eventPublisher.publishEvent(new PayoutPaidEvent(payout.getId(), payout.getProviderId(),
                payout.getAmountDecimal(), payout.getCurrency(), externalTransactionId));
        
        log.info("Payout marked as paid: payoutId={}, referenceNumber={}",
                payoutId, payout.getReferenceNumber());
        
        return payout;
    }
    
    /**
     * Mark payout as failed.
     */
    @Transactional
    public Payout markPayoutAsFailed(UUID payoutId, String failureReason) {
        log.info("Marking payout as failed: payoutId={}, reason={}", payoutId, failureReason);
        
        Payout payout = getPayoutById(payoutId);
        payout.markAsFailed(failureReason);
        payout = payoutRepository.save(payout);
        
        // TODO: Reverse ledger transaction if needed
        
        // Publish payout failed event
        eventPublisher.publishEvent(new PayoutFailedEvent(payout.getId(), payout.getProviderId(),
                payout.getAmountDecimal(), payout.getCurrency(), failureReason));
        
        log.info("Payout marked as failed: payoutId={}, referenceNumber={}",
                payoutId, payout.getReferenceNumber());
        
        return payout;
    }
    
    /**
     * Cancel a payout.
     */
    @Transactional
    public Payout cancelPayout(UUID payoutId, String reason) {
        log.info("Cancelling payout: payoutId={}, reason={}", payoutId, reason);
        
        Payout payout = getPayoutById(payoutId);
        payout.cancel(reason);
        payout = payoutRepository.save(payout);
        
        log.info("Payout cancelled: payoutId={}, referenceNumber={}", payoutId, payout.getReferenceNumber());
        
        return payout;
    }
    
    /**
     * Get payout by ID.
     */
    public Payout getPayoutById(UUID payoutId) {
        return payoutRepository.findById(payoutId)
                .orElseThrow(() -> new PayoutNotFoundException("Payout not found: " + payoutId));
    }
    
    /**
     * Get provider payouts with pagination.
     */
    public Page<Payout> getProviderPayouts(UUID providerId, Pageable pageable) {
        return payoutRepository.findByProviderIdOrderByRequestedAtDesc(providerId, pageable);
    }
    
    /**
     * Get payouts requiring approval.
     */
    public Page<Payout> getPayoutsRequiringApproval(Pageable pageable) {
        return payoutRepository.findPayoutsRequiringApproval(pageable);
    }
    
    /**
     * Get approved payouts ready for processing.
     */
    public List<Payout> getApprovedPayoutsForProcessing() {
        return payoutRepository.findApprovedPayoutsForProcessing();
    }
    
    /**
     * Get provider available balance for payout.
     */
    public BigDecimal getProviderAvailableBalance(UUID providerId, String currency) {
        BigDecimal accountBalance = ledgerService.getProviderBalance(providerId, currency);
        Long pendingPayoutAmount = payoutRepository.calculatePendingPayoutAmount(providerId, currency);
        BigDecimal pendingPayouts = new BigDecimal(pendingPayoutAmount).divide(new BigDecimal("100"));
        
        return accountBalance.subtract(pendingPayouts).max(BigDecimal.ZERO);
    }
    
    /**
     * Get provider payout summary.
     */
    public ProviderPayoutSummary getProviderPayoutSummary(UUID providerId, String currency) {
        Long totalPaid = payoutRepository.calculateTotalPaidAmount(providerId, currency);
        Long totalPending = payoutRepository.calculatePendingPayoutAmount(providerId, currency);
        BigDecimal accountBalance = ledgerService.getProviderBalance(providerId, currency);
        BigDecimal availableBalance = getProviderAvailableBalance(providerId, currency);
        
        long paidCount = payoutRepository.countByProviderIdAndStatus(providerId, Payout.PayoutStatus.PAID);
        long pendingCount = payoutRepository.countByProviderIdAndStatus(providerId, Payout.PayoutStatus.PENDING);
        
        return new ProviderPayoutSummary(
                providerId,
                currency,
                accountBalance,
                availableBalance,
                new BigDecimal(totalPaid).divide(new BigDecimal("100")),
                new BigDecimal(totalPending).divide(new BigDecimal("100")),
                paidCount,
                pendingCount
        );
    }
    
    /**
     * Get payout statistics.
     */
    public PayoutStatistics getPayoutStatistics() {
        long totalPayouts = payoutRepository.count();
        long pendingPayouts = payoutRepository.countByStatus(Payout.PayoutStatus.PENDING);
        long approvedPayouts = payoutRepository.countByStatus(Payout.PayoutStatus.APPROVED);
        long processingPayouts = payoutRepository.countByStatus(Payout.PayoutStatus.PROCESSING);
        long paidPayouts = payoutRepository.countByStatus(Payout.PayoutStatus.PAID);
        long failedPayouts = payoutRepository.countByStatus(Payout.PayoutStatus.FAILED);
        
        return new PayoutStatistics(
                totalPayouts,
                pendingPayouts,
                approvedPayouts,
                processingPayouts,
                paidPayouts,
                failedPayouts
        );
    }
    
    /**
     * Find payouts needing attention.
     */
    public List<Payout> findPayoutsNeedingAttention() {
        LocalDateTime pendingCutoff = LocalDateTime.now().minusHours(24); // 24 hours old
        LocalDateTime processingCutoff = LocalDateTime.now().minusHours(2); // 2 hours old
        
        return payoutRepository.findPayoutsNeedingAttention(pendingCutoff, processingCutoff);
    }
    
    // Event classes
    public record PayoutCreatedEvent(UUID payoutId, UUID providerId, BigDecimal amount, String currency) {}
    public record PayoutProcessingEvent(UUID payoutId, UUID providerId, BigDecimal amount, String currency) {}
    public record PayoutPaidEvent(UUID payoutId, UUID providerId, BigDecimal amount, String currency, String externalTransactionId) {}
    public record PayoutFailedEvent(UUID payoutId, UUID providerId, BigDecimal amount, String currency, String failureReason) {}
    
    // Request and response DTOs
    public record PayoutRequest(
            UUID providerId,
            BigDecimal amountDecimal,
            String currency,
            Payout.PayoutMethod method,
            UUID requestedBy,
            String notes,
            // Bank details
            String bankName,
            String accountNumber,
            String accountHolderName,
            String routingNumber,
            // Mobile money details
            String mobileNumber,
            String mobileAccountName
    ) {}
    
    public record ProviderPayoutSummary(
            UUID providerId,
            String currency,
            BigDecimal accountBalance,
            BigDecimal availableBalance,
            BigDecimal totalPaid,
            BigDecimal totalPending,
            long paidCount,
            long pendingCount
    ) {}
    
    public record PayoutStatistics(
            long totalPayouts,
            long pendingPayouts,
            long approvedPayouts,
            long processingPayouts,
            long paidPayouts,
            long failedPayouts
    ) {}
}