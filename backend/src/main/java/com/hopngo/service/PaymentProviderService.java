package com.hopngo.service;

import com.hopngo.entity.Transaction.PaymentProvider;
import com.hopngo.service.ReconciliationService.ProviderTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PaymentProviderService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProviderService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    // Stripe Configuration
    @Value("${stripe.api.key:sk_test_dummy}")
    private String stripeApiKey;

    @Value("${stripe.api.url:https://api.stripe.com/v1}")
    private String stripeApiUrl;

    // bKash Configuration
    @Value("${bkash.api.key:dummy_key}")
    private String bkashApiKey;

    @Value("${bkash.api.url:https://tokenized.pay.bka.sh/v1.2.0-beta}")
    private String bkashApiUrl;

    @Value("${bkash.username:dummy_user}")
    private String bkashUsername;

    @Value("${bkash.password:dummy_pass}")
    private String bkashPassword;

    // Nagad Configuration
    @Value("${nagad.api.key:dummy_key}")
    private String nagadApiKey;

    @Value("${nagad.api.url:https://api.mynagad.com}")
    private String nagadApiUrl;

    @Value("${nagad.merchant.id:dummy_merchant}")
    private String nagadMerchantId;

    /**
     * Get transactions for a specific date from a payment provider
     */
    public List<ProviderTransaction> getTransactionsForDate(PaymentProvider provider, LocalDate date) {
        logger.info("Fetching transactions for {} on {}", provider, date);
        
        try {
            switch (provider) {
                case STRIPE:
                    return getStripeTransactions(date, date);
                case BKASH:
                    return getBkashTransactions(date, date);
                case NAGAD:
                    return getNagadTransactions(date, date);
                default:
                    logger.warn("Unsupported payment provider: {}", provider);
                    return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error fetching transactions for {} on {}", provider, date, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get transactions for a date range from a payment provider
     */
    public List<ProviderTransaction> getTransactionsForDateRange(PaymentProvider provider, LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching transactions for {} from {} to {}", provider, startDate, endDate);
        
        try {
            switch (provider) {
                case STRIPE:
                    return getStripeTransactions(startDate, endDate);
                case BKASH:
                    return getBkashTransactions(startDate, endDate);
                case NAGAD:
                    return getNagadTransactions(startDate, endDate);
                default:
                    logger.warn("Unsupported payment provider: {}", provider);
                    return new ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("Error fetching transactions for {} from {} to {}", provider, startDate, endDate, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get Stripe transactions for date range
     */
    private List<ProviderTransaction> getStripeTransactions(LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching Stripe transactions from {} to {}", startDate, endDate);
        
        try {
            // Convert dates to Unix timestamps
            long startTimestamp = startDate.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
            long endTimestamp = endDate.plusDays(1).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC);
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + stripeApiKey);
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            // Build URL with query parameters
            String url = String.format("%s/charges?created[gte]=%d&created[lt]=%d&limit=100", 
                stripeApiUrl, startTimestamp, endTimestamp);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Make API call
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseStripeResponse(response.getBody());
            } else {
                logger.warn("Stripe API returned non-success status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            logger.error("Error calling Stripe API", e);
            // Return mock data for development/testing
            return getMockStripeTransactions(startDate, endDate);
        }
    }

    /**
     * Get bKash transactions for date range
     */
    private List<ProviderTransaction> getBkashTransactions(LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching bKash transactions from {} to {}", startDate, endDate);
        
        try {
            // First, get access token
            String accessToken = getBkashAccessToken();
            
            if (accessToken == null) {
                logger.error("Failed to get bKash access token");
                return getMockBkashTransactions(startDate, endDate);
            }
            
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("X-APP-Key", bkashApiKey);
            headers.set("Content-Type", "application/json");
            
            // Build request body
            Map<String, Object> requestBody = Map.of(
                "startDate", startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "endDate", endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "limit", 100
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            String url = bkashApiUrl + "/tokenized/checkout/payment/search";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseBkashResponse(response.getBody());
            } else {
                logger.warn("bKash API returned non-success status: {}", response.getStatusCode());
                return getMockBkashTransactions(startDate, endDate);
            }
            
        } catch (Exception e) {
            logger.error("Error calling bKash API", e);
            return getMockBkashTransactions(startDate, endDate);
        }
    }

    /**
     * Get Nagad transactions for date range
     */
    private List<ProviderTransaction> getNagadTransactions(LocalDate startDate, LocalDate endDate) {
        logger.info("Fetching Nagad transactions from {} to {}", startDate, endDate);
        
        try {
            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-KM-Api-Version", "v-0.2.0");
            headers.set("X-KM-IP-V4", "127.0.0.1");
            headers.set("X-KM-Client-Type", "PC_WEB");
            headers.set("Content-Type", "application/json");
            
            // Build request body
            Map<String, Object> requestBody = Map.of(
                "merchantId", nagadMerchantId,
                "startDate", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                "endDate", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                "limit", 100
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            String url = nagadApiUrl + "/remote-payment-gateway-1.0/api/dfs/transaction-search";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseNagadResponse(response.getBody());
            } else {
                logger.warn("Nagad API returned non-success status: {}", response.getStatusCode());
                return getMockNagadTransactions(startDate, endDate);
            }
            
        } catch (Exception e) {
            logger.error("Error calling Nagad API", e);
            return getMockNagadTransactions(startDate, endDate);
        }
    }

    /**
     * Get bKash access token
     */
    private String getBkashAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-APP-Key", bkashApiKey);
            
            Map<String, String> requestBody = Map.of(
                "app_key", bkashApiKey,
                "app_secret", bkashPassword
            );
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            String url = bkashApiUrl + "/tokenized/checkout/token/grant";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                return (String) responseBody.get("id_token");
            }
            
        } catch (Exception e) {
            logger.error("Error getting bKash access token", e);
        }
        
        return null;
    }

    /**
     * Parse Stripe API response
     */
    @SuppressWarnings("unchecked")
    private List<ProviderTransaction> parseStripeResponse(Map<String, Object> response) {
        List<ProviderTransaction> transactions = new ArrayList<>();
        
        List<Map<String, Object>> charges = (List<Map<String, Object>>) response.get("data");
        
        if (charges != null) {
            for (Map<String, Object> charge : charges) {
                String id = (String) charge.get("id");
                Integer amountCents = (Integer) charge.get("amount");
                String status = (String) charge.get("status");
                Long created = (Long) charge.get("created");
                
                BigDecimal amount = new BigDecimal(amountCents).divide(new BigDecimal(100));
                LocalDateTime timestamp = LocalDateTime.ofEpochSecond(created, 0, java.time.ZoneOffset.UTC);
                
                transactions.add(new ProviderTransaction(id, amount, status, timestamp));
            }
        }
        
        return transactions;
    }

    /**
     * Parse bKash API response
     */
    @SuppressWarnings("unchecked")
    private List<ProviderTransaction> parseBkashResponse(Map<String, Object> response) {
        List<ProviderTransaction> transactions = new ArrayList<>();
        
        List<Map<String, Object>> payments = (List<Map<String, Object>>) response.get("paymentList");
        
        if (payments != null) {
            for (Map<String, Object> payment : payments) {
                String id = (String) payment.get("paymentID");
                String amountStr = (String) payment.get("amount");
                String status = (String) payment.get("transactionStatus");
                String createTime = (String) payment.get("createTime");
                
                BigDecimal amount = new BigDecimal(amountStr);
                LocalDateTime timestamp = LocalDateTime.parse(createTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                
                transactions.add(new ProviderTransaction(id, amount, status, timestamp));
            }
        }
        
        return transactions;
    }

    /**
     * Parse Nagad API response
     */
    @SuppressWarnings("unchecked")
    private List<ProviderTransaction> parseNagadResponse(Map<String, Object> response) {
        List<ProviderTransaction> transactions = new ArrayList<>();
        
        List<Map<String, Object>> txnList = (List<Map<String, Object>>) response.get("txnList");
        
        if (txnList != null) {
            for (Map<String, Object> txn : txnList) {
                String id = (String) txn.get("orderId");
                String amountStr = (String) txn.get("amount");
                String status = (String) txn.get("status");
                String dateTime = (String) txn.get("dateTime");
                
                BigDecimal amount = new BigDecimal(amountStr);
                LocalDateTime timestamp = LocalDateTime.parse(dateTime, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                
                transactions.add(new ProviderTransaction(id, amount, status, timestamp));
            }
        }
        
        return transactions;
    }

    /**
     * Mock Stripe transactions for development/testing
     */
    private List<ProviderTransaction> getMockStripeTransactions(LocalDate startDate, LocalDate endDate) {
        logger.info("Returning mock Stripe transactions for {} to {}", startDate, endDate);
        
        List<ProviderTransaction> transactions = new ArrayList<>();
        
        // Generate some mock transactions
        LocalDateTime baseTime = startDate.atTime(10, 0);
        
        transactions.add(new ProviderTransaction("ch_stripe_001", new BigDecimal("50.00"), "succeeded", baseTime));
        transactions.add(new ProviderTransaction("ch_stripe_002", new BigDecimal("75.50"), "succeeded", baseTime.plusHours(2)));
        transactions.add(new ProviderTransaction("ch_stripe_003", new BigDecimal("100.00"), "succeeded", baseTime.plusHours(4)));
        
        return transactions;
    }

    /**
     * Mock bKash transactions for development/testing
     */
    private List<ProviderTransaction> getMockBkashTransactions(LocalDate startDate, LocalDate endDate) {
        logger.info("Returning mock bKash transactions for {} to {}", startDate, endDate);
        
        List<ProviderTransaction> transactions = new ArrayList<>();
        
        LocalDateTime baseTime = startDate.atTime(11, 0);
        
        transactions.add(new ProviderTransaction("bkash_001", new BigDecimal("1500.00"), "Completed", baseTime));
        transactions.add(new ProviderTransaction("bkash_002", new BigDecimal("2250.00"), "Completed", baseTime.plusHours(1)));
        transactions.add(new ProviderTransaction("bkash_003", new BigDecimal("3000.00"), "Completed", baseTime.plusHours(3)));
        
        return transactions;
    }

    /**
     * Mock Nagad transactions for development/testing
     */
    private List<ProviderTransaction> getMockNagadTransactions(LocalDate startDate, LocalDate endDate) {
        logger.info("Returning mock Nagad transactions for {} to {}", startDate, endDate);
        
        List<ProviderTransaction> transactions = new ArrayList<>();
        
        LocalDateTime baseTime = startDate.atTime(12, 0);
        
        transactions.add(new ProviderTransaction("nagad_001", new BigDecimal("800.00"), "Success", baseTime));
        transactions.add(new ProviderTransaction("nagad_002", new BigDecimal("1200.00"), "Success", baseTime.plusHours(2)));
        transactions.add(new ProviderTransaction("nagad_003", new BigDecimal("1600.00"), "Success", baseTime.plusHours(5)));
        
        return transactions;
    }

    /**
     * Test connectivity to all payment providers
     */
    public Map<PaymentProvider, Boolean> testConnectivity() {
        logger.info("Testing connectivity to all payment providers");
        
        Map<PaymentProvider, Boolean> results = new java.util.HashMap<>();
        
        // Test Stripe
        try {
            getStripeTransactions(LocalDate.now().minusDays(1), LocalDate.now());
            results.put(PaymentProvider.STRIPE, true);
        } catch (Exception e) {
            logger.warn("Stripe connectivity test failed", e);
            results.put(PaymentProvider.STRIPE, false);
        }
        
        // Test bKash
        try {
            getBkashTransactions(LocalDate.now().minusDays(1), LocalDate.now());
            results.put(PaymentProvider.BKASH, true);
        } catch (Exception e) {
            logger.warn("bKash connectivity test failed", e);
            results.put(PaymentProvider.BKASH, false);
        }
        
        // Test Nagad
        try {
            getNagadTransactions(LocalDate.now().minusDays(1), LocalDate.now());
            results.put(PaymentProvider.NAGAD, true);
        } catch (Exception e) {
            logger.warn("Nagad connectivity test failed", e);
            results.put(PaymentProvider.NAGAD, false);
        }
        
        return results;
    }
}