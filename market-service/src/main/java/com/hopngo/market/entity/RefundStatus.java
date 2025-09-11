package com.hopngo.market.entity;

public enum RefundStatus {
    PENDING,     // Refund request created but not yet processed
    PROCESSING,  // Refund is being processed by payment provider
    SUCCEEDED,   // Refund completed successfully
    FAILED       // Refund failed
}