package com.hopngo.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.sms")
public class SmsProperties {
    
    private boolean enabled = false;
    private String provider = "stub";
    private Twilio twilio = new Twilio();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public Twilio getTwilio() {
        return twilio;
    }
    
    public void setTwilio(Twilio twilio) {
        this.twilio = twilio;
    }
    
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String fromNumber;
        
        public String getAccountSid() {
            return accountSid;
        }
        
        public void setAccountSid(String accountSid) {
            this.accountSid = accountSid;
        }
        
        public String getAuthToken() {
            return authToken;
        }
        
        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }
        
        public String getFromNumber() {
            return fromNumber;
        }
        
        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }
    }
}