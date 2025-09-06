package com.hopngo.market.config;

import com.hopngo.market.service.payment.PaymentProvider;
import com.hopngo.market.service.payment.MockPaymentProvider;
import com.hopngo.market.service.payment.StripePaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for payment providers.
 * Manages the creation and configuration of payment provider beans.
 */
@Configuration
public class PaymentProviderConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentProviderConfiguration.class);
    
    /**
     * Configuration properties for payment providers.
     */
    @ConfigurationProperties(prefix = "payment")
    public static class PaymentProperties {
        private Default defaultProvider = new Default();
        private Providers providers = new Providers();
        private Webhook webhook = new Webhook();
        private Security security = new Security();
        
        // Getters and setters
        public Default getDefault() { return defaultProvider; }
        public void setDefault(Default defaultProvider) { this.defaultProvider = defaultProvider; }
        
        public Providers getProviders() { return providers; }
        public void setProviders(Providers providers) { this.providers = providers; }
        
        public Webhook getWebhook() { return webhook; }
        public void setWebhook(Webhook webhook) { this.webhook = webhook; }
        
        public Security getSecurity() { return security; }
        public void setSecurity(Security security) { this.security = security; }
        
        public static class Default {
            private String provider = "MOCK";
            
            public String getProvider() { return provider; }
            public void setProvider(String provider) { this.provider = provider; }
        }
        
        public static class Providers {
            private MockConfig mock = new MockConfig();
            private StripeConfig stripe = new StripeConfig();
            private BkashConfig bkash = new BkashConfig();
            private NagadConfig nagad = new NagadConfig();
            
            public MockConfig getMock() { return mock; }
            public void setMock(MockConfig mock) { this.mock = mock; }
            
            public StripeConfig getStripe() { return stripe; }
            public void setStripe(StripeConfig stripe) { this.stripe = stripe; }
            
            public BkashConfig getBkash() { return bkash; }
            public void setBkash(BkashConfig bkash) { this.bkash = bkash; }
            
            public NagadConfig getNagad() { return nagad; }
            public void setNagad(NagadConfig nagad) { this.nagad = nagad; }
        }
        
        public static class MockConfig {
            private boolean enabled = true;
            private String name = "Mock Payment Provider";
            private String description = "Mock provider for testing";
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }
        
        public static class StripeConfig {
            private boolean enabled = false;
            private boolean testMode = true;
            private String apiKey;
            private String publishableKey;
            private String webhookSecret;
            private String apiVersion = "2023-10-16";
            private String currency = "BDT";
            private String name = "Stripe Payment Provider";
            private String description = "Stripe payment provider";
            
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public boolean isTestMode() { return testMode; }
            public void setTestMode(boolean testMode) { this.testMode = testMode; }
            
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            
            public String getPublishableKey() { return publishableKey; }
            public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }
            
            public String getWebhookSecret() { return webhookSecret; }
            public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
            
            public String getApiVersion() { return apiVersion; }
            public void setApiVersion(String apiVersion) { this.apiVersion = apiVersion; }
            
            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
            
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }
        
        public static class BkashConfig {
            private boolean enabled = false;
            private boolean sandboxMode = true;
            private String appKey;
            private String appSecret;
            private String username;
            private String password;
            private String baseUrl;
            private String webhookSecret;
            private String currency = "BDT";
            private String name = "bKash Payment Provider";
            private String description = "bKash mobile financial service";
            
            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public boolean isSandboxMode() { return sandboxMode; }
            public void setSandboxMode(boolean sandboxMode) { this.sandboxMode = sandboxMode; }
            
            public String getAppKey() { return appKey; }
            public void setAppKey(String appKey) { this.appKey = appKey; }
            
            public String getAppSecret() { return appSecret; }
            public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
            
            public String getUsername() { return username; }
            public void setUsername(String username) { this.username = username; }
            
            public String getPassword() { return password; }
            public void setPassword(String password) { this.password = password; }
            
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            
            public String getWebhookSecret() { return webhookSecret; }
            public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
            
            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
            
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }
        
        public static class NagadConfig {
            private boolean enabled = false;
            private boolean sandboxMode = true;
            private String merchantId;
            private String merchantPrivateKey;
            private String nagadPublicKey;
            private String baseUrl;
            private String webhookSecret;
            private String currency = "BDT";
            private String name = "Nagad Payment Provider";
            private String description = "Nagad digital financial service";
            
            // Getters and setters
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            
            public boolean isSandboxMode() { return sandboxMode; }
            public void setSandboxMode(boolean sandboxMode) { this.sandboxMode = sandboxMode; }
            
            public String getMerchantId() { return merchantId; }
            public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
            
            public String getMerchantPrivateKey() { return merchantPrivateKey; }
            public void setMerchantPrivateKey(String merchantPrivateKey) { this.merchantPrivateKey = merchantPrivateKey; }
            
            public String getNagadPublicKey() { return nagadPublicKey; }
            public void setNagadPublicKey(String nagadPublicKey) { this.nagadPublicKey = nagadPublicKey; }
            
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
            
            public String getWebhookSecret() { return webhookSecret; }
            public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
            
            public String getCurrency() { return currency; }
            public void setCurrency(String currency) { this.currency = currency; }
            
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            
            public String getDescription() { return description; }
            public void setDescription(String description) { this.description = description; }
        }
        
        public static class Webhook {
            private String timeout = "30s";
            private Retry retry = new Retry();
            private SignatureVerification signatureVerification = new SignatureVerification();
            
            public String getTimeout() { return timeout; }
            public void setTimeout(String timeout) { this.timeout = timeout; }
            
            public Retry getRetry() { return retry; }
            public void setRetry(Retry retry) { this.retry = retry; }
            
            public SignatureVerification getSignatureVerification() { return signatureVerification; }
            public void setSignatureVerification(SignatureVerification signatureVerification) { this.signatureVerification = signatureVerification; }
            
            public static class Retry {
                private int maxAttempts = 3;
                private String delay = "5s";
                
                public int getMaxAttempts() { return maxAttempts; }
                public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
                
                public String getDelay() { return delay; }
                public void setDelay(String delay) { this.delay = delay; }
            }
            
            public static class SignatureVerification {
                private boolean enabled = true;
                private java.util.Map<String, String> algorithms = new java.util.HashMap<>();
                
                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
                
                public java.util.Map<String, String> getAlgorithms() { return algorithms; }
                public void setAlgorithms(java.util.Map<String, String> algorithms) { this.algorithms = algorithms; }
            }
        }
        
        public static class Security {
            private java.util.List<String> allowedOrigins = new java.util.ArrayList<>();
            private RateLimiting rateLimiting = new RateLimiting();
            
            public java.util.List<String> getAllowedOrigins() { return allowedOrigins; }
            public void setAllowedOrigins(java.util.List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
            
            public RateLimiting getRateLimiting() { return rateLimiting; }
            public void setRateLimiting(RateLimiting rateLimiting) { this.rateLimiting = rateLimiting; }
            
            public static class RateLimiting {
                private boolean enabled = true;
                private int requestsPerMinute = 60;
                
                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
                
                public int getRequestsPerMinute() { return requestsPerMinute; }
                public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
            }
        }
    }
    
    /**
     * Creates payment properties bean.
     */
    @Bean
    @ConfigurationProperties(prefix = "payment")
    public PaymentProperties paymentProperties() {
        return new PaymentProperties();
    }
    
    /**
     * Creates mock payment provider bean.
     */
    @Bean
    public MockPaymentProvider mockPaymentProvider(PaymentProperties properties) {
        if (!properties.getProviders().getMock().isEnabled()) {
            logger.info("Mock payment provider is disabled");
            return null;
        }
        
        logger.info("Creating Mock payment provider");
        return new MockPaymentProvider();
    }
    
    /**
     * Creates Stripe payment provider bean.
     */
    @Bean
    public StripePaymentProvider stripePaymentProvider(PaymentProperties properties) {
        PaymentProperties.StripeConfig config = properties.getProviders().getStripe();
        
        if (!config.isEnabled()) {
            logger.info("Stripe payment provider is disabled");
            return null;
        }
        
        if (config.getApiKey() == null || config.getApiKey().startsWith("sk_test_your_")) {
            logger.warn("Stripe payment provider is enabled but API key is not configured properly");
            return null;
        }
        
        logger.info("Creating Stripe payment provider in {} mode", config.isTestMode() ? "test" : "live");
        return new StripePaymentProvider();
    }
    
    /**
     * Creates list of enabled payment providers.
     */
    @Bean
    @Primary
    public List<PaymentProvider> paymentProviders(
            PaymentProperties properties,
            MockPaymentProvider mockProvider,
            StripePaymentProvider stripeProvider) {
        
        List<PaymentProvider> providers = new ArrayList<>();
        
        if (mockProvider != null) {
            providers.add(mockProvider);
            logger.info("Added Mock payment provider to active providers");
        }
        
        if (stripeProvider != null) {
            providers.add(stripeProvider);
            logger.info("Added Stripe payment provider to active providers");
        }
        
        // Future: Add bKash and Nagad providers when implemented
        // if (bkashProvider != null) providers.add(bkashProvider);
        // if (nagadProvider != null) providers.add(nagadProvider);
        
        logger.info("Total active payment providers: {}", providers.size());
        return providers;
    }
}