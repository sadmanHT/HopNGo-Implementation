package com.hopngo.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Gateway filter to handle Accept-Language header and propagate language preferences
 * to downstream services. Supports Bengali (bn) and English (en) with fallback to English.
 */
@Component
public class AcceptLanguageFilter extends AbstractGatewayFilterFactory<AcceptLanguageFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AcceptLanguageFilter.class);
    
    // Supported languages
    private static final String BENGALI = "bn";
    private static final String ENGLISH = "en";
    private static final String DEFAULT_LANGUAGE = ENGLISH;
    
    // Pattern to match language codes
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^[a-z]{2}(-[A-Z]{2})?$");

    public AcceptLanguageFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String acceptLanguage = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ACCEPT_LANGUAGE);
            String preferredLanguage = determinePreferredLanguage(acceptLanguage);
            
            logger.debug("Accept-Language header: {}, Preferred language: {}", acceptLanguage, preferredLanguage);
            
            // Create modified exchange with language headers for downstream services
            ServerWebExchange modifiedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                    .header("X-Preferred-Language", preferredLanguage)
                    .header("X-Accept-Language", acceptLanguage != null ? acceptLanguage : DEFAULT_LANGUAGE)
                    .header("Accept-Language", preferredLanguage)
                    .build())
                .build();
            
            return chain.filter(modifiedExchange);
        };
    }
    
    /**
     * Determine the preferred language from Accept-Language header
     * @param acceptLanguage The Accept-Language header value
     * @return The preferred language code (bn or en)
     */
    private String determinePreferredLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.trim().isEmpty()) {
            return DEFAULT_LANGUAGE;
        }
        
        // Parse Accept-Language header (e.g., "bn,en-US;q=0.9,en;q=0.8")
        String[] languages = acceptLanguage.toLowerCase().split(",");
        
        for (String lang : languages) {
            // Remove quality factor and whitespace
            String cleanLang = lang.split(";")[0].trim();
            
            // Extract primary language code
            String primaryLang = cleanLang.split("-")[0];
            
            // Validate language code format
            if (LANGUAGE_PATTERN.matcher(cleanLang).matches() || 
                LANGUAGE_PATTERN.matcher(primaryLang).matches()) {
                
                // Check if it's one of our supported languages
                if (BENGALI.equals(primaryLang)) {
                    return BENGALI;
                } else if (ENGLISH.equals(primaryLang)) {
                    return ENGLISH;
                }
            }
        }
        
        // Fallback to default language
        return DEFAULT_LANGUAGE;
    }
    
    /**
     * Get locale from language code
     * @param languageCode The language code (bn or en)
     * @return Locale object
     */
    private Locale getLocale(String languageCode) {
        switch (languageCode) {
            case BENGALI:
                return new Locale("bn", "BD"); // Bengali (Bangladesh)
            case ENGLISH:
            default:
                return Locale.ENGLISH;
        }
    }
    
    /**
     * Check if the language is supported
     * @param languageCode The language code to check
     * @return true if supported, false otherwise
     */
    private boolean isSupportedLanguage(String languageCode) {
        return BENGALI.equals(languageCode) || ENGLISH.equals(languageCode);
    }

    public static class Config {
        private boolean enabled = true;
        private String defaultLanguage = DEFAULT_LANGUAGE;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getDefaultLanguage() {
            return defaultLanguage;
        }
        
        public void setDefaultLanguage(String defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
        }
    }
}