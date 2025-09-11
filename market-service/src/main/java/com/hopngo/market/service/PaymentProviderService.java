package com.hopngo.market.service;

import com.hopngo.market.service.payment.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProviderService {
    
    private final List<PaymentProvider> paymentProviders;
    private final Map<String, PaymentProvider> providerMap;

    public PaymentProviderService(List<PaymentProvider> paymentProviders) {
        this.paymentProviders = paymentProviders;
        this.providerMap = paymentProviders.stream()
                .collect(Collectors.toMap(PaymentProvider::name, Function.identity()));
        
        log.info("Initialized PaymentProviderService with {} providers: {}", 
                paymentProviders.size(), 
                paymentProviders.stream().map(PaymentProvider::name).collect(Collectors.toList()));
    }

    /**
     * Get payment provider by name
     */
    public PaymentProvider getProvider(String providerName) {
        PaymentProvider provider = providerMap.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Payment provider not found: " + providerName);
        }
        return provider;
    }

    /**
     * Get all available payment providers
     */
    public List<PaymentProvider> getAllProviders() {
        return paymentProviders;
    }

    /**
     * Get all provider names
     */
    public List<String> getProviderNames() {
        return paymentProviders.stream()
                .map(PaymentProvider::name)
                .collect(Collectors.toList());
    }

    /**
     * Check if provider exists
     */
    public boolean hasProvider(String providerName) {
        return providerMap.containsKey(providerName);
    }
}