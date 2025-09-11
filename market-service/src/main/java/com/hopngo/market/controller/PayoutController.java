package com.hopngo.market.controller;

import com.hopngo.market.entity.Payout;
import com.hopngo.market.service.PayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Controller for managing provider payouts.
 */
@RestController
@RequestMapping("/api/v1/payouts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payouts", description = "Provider payout management")
public class PayoutController {
    
    private final PayoutService payoutService;
    
    /**
     * Request a payout (provider endpoint).
     */
    @PostMapping
    @PreAuthorize("hasRole('PROVIDER') and #request.providerId() == authentication.principal.id")
    @Operation(summary = "Request a payout", description = "Provider requests a payout from their available balance")
    public ResponseEntity<PayoutResponse> requestPayout(
            @Valid @RequestBody PayoutRequestDto request) {
        
        log.info("Payout request received: providerId={}, amount={}, currency={}",
                request.providerId(), request.amount(), request.currency());
        
        PayoutService.PayoutRequest serviceRequest = new PayoutService.PayoutRequest(
                request.providerId(),
                request.amount(),
                request.currency(),
                request.method(),
                request.providerId(), // requestedBy same as providerId for self-requests
                request.notes(),
                request.bankName(),
                request.accountNumber(),
                request.accountHolderName(),
                request.routingNumber(),
                request.mobileNumber(),
                request.mobileAccountName()
        );
        
        Payout payout = payoutService.requestPayout(serviceRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PayoutResponse.fromEntity(payout));
    }
    
    /**
     * Get provider's payouts.
     */
    @GetMapping("/provider/{providerId}")
    @PreAuthorize("hasRole('PROVIDER') and #providerId == authentication.principal.id or hasRole('ADMIN')")
    @Operation(summary = "Get provider payouts", description = "Get paginated list of provider's payouts")
    public ResponseEntity<Page<PayoutResponse>> getProviderPayouts(
            @Parameter(description = "Provider ID") @PathVariable UUID providerId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Payout> payouts = payoutService.getProviderPayouts(providerId, pageable);
        Page<PayoutResponse> response = payouts.map(PayoutResponse::fromEntity);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get payout by ID.
     */
    @GetMapping("/{payoutId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROVIDER') and @payoutService.getPayoutById(#payoutId).providerId == authentication.principal.id)")
    @Operation(summary = "Get payout details", description = "Get detailed information about a specific payout")
    public ResponseEntity<PayoutResponse> getPayoutById(
            @Parameter(description = "Payout ID") @PathVariable UUID payoutId) {
        
        Payout payout = payoutService.getPayoutById(payoutId);
        return ResponseEntity.ok(PayoutResponse.fromEntity(payout));
    }
    
    /**
     * Get provider's available balance.
     */
    @GetMapping("/provider/{providerId}/balance")
    @PreAuthorize("hasRole('PROVIDER') and #providerId == authentication.principal.id or hasRole('ADMIN')")
    @Operation(summary = "Get provider balance", description = "Get provider's available balance for payout")
    public ResponseEntity<BalanceResponse> getProviderBalance(
            @Parameter(description = "Provider ID") @PathVariable UUID providerId,
            @Parameter(description = "Currency code") @RequestParam(defaultValue = "BDT") String currency) {
        
        BigDecimal availableBalance = payoutService.getProviderAvailableBalance(providerId, currency);
        PayoutService.ProviderPayoutSummary summary = payoutService.getProviderPayoutSummary(providerId, currency);
        
        BalanceResponse response = new BalanceResponse(
                providerId,
                currency,
                summary.accountBalance(),
                availableBalance,
                summary.totalPaid(),
                summary.totalPending(),
                summary.paidCount(),
                summary.pendingCount()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel a payout (provider can cancel pending payouts).
     */
    @PutMapping("/{payoutId}/cancel")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PROVIDER') and @payoutService.getPayoutById(#payoutId).providerId == authentication.principal.id and @payoutService.getPayoutById(#payoutId).status == T(com.hopngo.market.entity.Payout.PayoutStatus).PENDING)")
    @Operation(summary = "Cancel payout", description = "Cancel a pending payout")
    public ResponseEntity<PayoutResponse> cancelPayout(
            @Parameter(description = "Payout ID") @PathVariable UUID payoutId,
            @RequestBody(required = false) CancelPayoutRequest request) {
        
        String reason = request != null ? request.reason() : "Cancelled by provider";
        Payout payout = payoutService.cancelPayout(payoutId, reason);
        
        return ResponseEntity.ok(PayoutResponse.fromEntity(payout));
    }
    
    // Admin endpoints
    
    /**
     * Get payouts requiring approval (admin only).
     */
    @GetMapping("/admin/pending-approval")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payouts requiring approval", description = "Admin endpoint to get payouts waiting for approval")
    public ResponseEntity<Page<PayoutResponse>> getPayoutsRequiringApproval(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Payout> payouts = payoutService.getPayoutsRequiringApproval(pageable);
        Page<PayoutResponse> response = payouts.map(PayoutResponse::fromEntity);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Approve a payout (admin only).
     */
    @PutMapping("/{payoutId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve payout", description = "Admin approves a pending payout")
    public ResponseEntity<PayoutResponse> approvePayout(
            @Parameter(description = "Payout ID") @PathVariable UUID payoutId,
            @RequestBody ApprovePayoutRequest request) {
        
        Payout payout = payoutService.approvePayout(payoutId, request.approvedBy());
        return ResponseEntity.ok(PayoutResponse.fromEntity(payout));
    }
    
    /**
     * Process a payout (admin only).
     */
    @PutMapping("/{payoutId}/process")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Process payout", description = "Admin starts processing an approved payout")
    public ResponseEntity<PayoutResponse> processPayout(
            @Parameter(description = "Payout ID") @PathVariable UUID payoutId,
            @RequestBody ProcessPayoutRequest request) {
        
        Payout payout = payoutService.processPayout(payoutId, request.processedBy());
        return ResponseEntity.ok(PayoutResponse.fromEntity(payout));
    }
    
    /**
     * Mark payout as paid (admin only).
     */
    @PutMapping("/{payoutId}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark payout as paid", description = "Admin marks a payout as successfully paid")
    public ResponseEntity<PayoutResponse> markPayoutAsPaid(
            @Parameter(description = "Payout ID") @PathVariable UUID payoutId,
            @RequestBody MarkPaidRequest request) {
        
        Payout payout = payoutService.markPayoutAsPaid(payoutId, request.externalTransactionId());
        return ResponseEntity.ok(PayoutResponse.fromEntity(payout));
    }
    
    /**
     * Mark payout as failed (admin only).
     */
    @PutMapping("/{payoutId}/failed")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mark payout as failed", description = "Admin marks a payout as failed")
    public ResponseEntity<PayoutResponse> markPayoutAsFailed(
            @Parameter(description = "Payout ID") @PathVariable UUID payoutId,
            @RequestBody MarkFailedRequest request) {
        
        Payout payout = payoutService.markPayoutAsFailed(payoutId, request.failureReason());
        return ResponseEntity.ok(PayoutResponse.fromEntity(payout));
    }
    
    /**
     * Get payout statistics (admin only).
     */
    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payout statistics", description = "Admin gets overall payout statistics")
    public ResponseEntity<PayoutService.PayoutStatistics> getPayoutStatistics() {
        PayoutService.PayoutStatistics statistics = payoutService.getPayoutStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * Get payouts needing attention (admin only).
     */
    @GetMapping("/admin/attention")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get payouts needing attention", description = "Admin gets payouts that need attention (overdue, stuck, etc.)")
    public ResponseEntity<List<PayoutResponse>> getPayoutsNeedingAttention() {
        List<Payout> payouts = payoutService.findPayoutsNeedingAttention();
        List<PayoutResponse> response = payouts.stream()
                .map(PayoutResponse::fromEntity)
                .toList();
        
        return ResponseEntity.ok(response);
    }
    
    // DTOs
    
    public record PayoutRequestDto(
            UUID providerId,
            BigDecimal amount,
            String currency,
            Payout.PayoutMethod method,
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
    
    public record PayoutResponse(
            UUID id,
            UUID providerId,
            String referenceNumber,
            BigDecimal amount,
            String currency,
            Payout.PayoutMethod method,
            Payout.PayoutStatus status,
            String notes,
            String bankName,
            String accountNumber,
            String accountHolderName,
            String routingNumber,
            String mobileNumber,
            String mobileAccountName,
            UUID requestedBy,
            String requestedAt,
            UUID approvedBy,
            String approvedAt,
            UUID processedBy,
            String processedAt,
            String executedAt,
            String externalTransactionId,
            String failureReason,
            String createdAt,
            String updatedAt
    ) {
        public static PayoutResponse fromEntity(Payout payout) {
            return new PayoutResponse(
                    payout.getId(),
                    payout.getProviderId(),
                    payout.getReferenceNumber(),
                    payout.getAmountDecimal(),
                    payout.getCurrency(),
                    payout.getMethod(),
                    payout.getStatus(),
                    payout.getNotes(),
                    payout.getBankName(),
                    payout.getAccountNumber(),
                    payout.getAccountHolderName(),
                    payout.getRoutingNumber(),
                    payout.getMobileNumber(),
                    payout.getMobileAccountName(),
                    payout.getRequestedBy(),
                    payout.getRequestedAt() != null ? payout.getRequestedAt().toString() : null,
                    payout.getApprovedBy(),
                    payout.getApprovedAt() != null ? payout.getApprovedAt().toString() : null,
                    payout.getProcessedBy(),
                    payout.getProcessedAt() != null ? payout.getProcessedAt().toString() : null,
                    payout.getExecutedAt() != null ? payout.getExecutedAt().toString() : null,
                    payout.getExternalTransactionId(),
                    payout.getFailureReason(),
                    payout.getCreatedAt() != null ? payout.getCreatedAt().toString() : null,
                    payout.getUpdatedAt() != null ? payout.getUpdatedAt().toString() : null
            );
        }
    }
    
    public record BalanceResponse(
            UUID providerId,
            String currency,
            BigDecimal accountBalance,
            BigDecimal availableBalance,
            BigDecimal totalPaid,
            BigDecimal totalPending,
            long paidCount,
            long pendingCount
    ) {}
    
    public record CancelPayoutRequest(String reason) {}
    public record ApprovePayoutRequest(UUID approvedBy) {}
    public record ProcessPayoutRequest(UUID processedBy) {}
    public record MarkPaidRequest(String externalTransactionId) {}
    public record MarkFailedRequest(String failureReason) {}
}