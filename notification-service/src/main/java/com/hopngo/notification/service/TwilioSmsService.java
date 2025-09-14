package com.hopngo.notification.service;

import com.hopngo.notification.config.SmsProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class TwilioSmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwilioSmsService.class);
    
    @Autowired
    private SmsProperties smsProperties;
    
    @PostConstruct
    public void init() {
        if (smsProperties.isEnabled() && "twilio".equals(smsProperties.getProvider())) {
            String accountSid = smsProperties.getTwilio().getAccountSid();
            String authToken = smsProperties.getTwilio().getAuthToken();
            
            if (accountSid != null && !accountSid.equals("your_account_sid") && 
                authToken != null && !authToken.equals("your_auth_token")) {
                Twilio.init(accountSid, authToken);
                logger.info("Twilio SMS service initialized successfully");
            } else {
                logger.warn("Twilio SMS service not initialized - missing or default credentials");
            }
        }
    }
    
    public void sendSms(String toPhoneNumber, String message) throws Exception {
        if (!smsProperties.isEnabled()) {
            throw new IllegalStateException("SMS service is disabled");
        }
        
        if (!"twilio".equals(smsProperties.getProvider())) {
            throw new IllegalStateException("SMS provider is not set to Twilio");
        }
        
        String fromNumber = smsProperties.getTwilio().getFromNumber();
        if (fromNumber == null || fromNumber.equals("+1234567890")) {
            throw new IllegalStateException("Twilio from number is not configured");
        }
        
        try {
            Message twilioMessage = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromNumber),
                message
            ).create();
            
            logger.info("SMS sent successfully via Twilio. SID: {}, To: {}", 
                       twilioMessage.getSid(), toPhoneNumber);
            
        } catch (Exception e) {
            logger.error("Failed to send SMS via Twilio to: {}", toPhoneNumber, e);
            throw new Exception("Failed to send SMS: " + e.getMessage(), e);
        }
    }
    
    public boolean isConfigured() {
        if (!smsProperties.isEnabled() || !"twilio".equals(smsProperties.getProvider())) {
            return false;
        }
        
        String accountSid = smsProperties.getTwilio().getAccountSid();
        String authToken = smsProperties.getTwilio().getAuthToken();
        String fromNumber = smsProperties.getTwilio().getFromNumber();
        
        return accountSid != null && !accountSid.equals("your_account_sid") &&
               authToken != null && !authToken.equals("your_auth_token") &&
               fromNumber != null && !fromNumber.equals("+1234567890");
    }
}