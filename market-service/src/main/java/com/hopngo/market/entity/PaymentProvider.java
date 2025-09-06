package com.hopngo.market.entity;

/**
 * Payment provider enum for the Payment entity.
 * Defines the available payment providers in the system.
 */
public enum PaymentProvider {
    MOCK,
    STRIPE,
    STRIPE_TEST,
    BKASH,
    NAGAD,
    PAYPAL
}