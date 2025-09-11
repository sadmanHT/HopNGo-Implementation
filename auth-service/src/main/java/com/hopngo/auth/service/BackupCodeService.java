package com.hopngo.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BackupCodeService {
    
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;
    
    public BackupCodeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Generate backup codes
     */
    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                int index = secureRandom.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }
            codes.add(code.toString());
        }
        
        return codes;
    }
    
    /**
     * Convert backup codes list to JSON string for storage
     */
    public String backupCodesToJson(List<String> codes) {
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize backup codes", e);
        }
    }
    
    /**
     * Convert JSON string to backup codes list
     */
    public List<String> jsonToBackupCodes(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize backup codes", e);
        }
    }
    
    /**
     * Verify and consume a backup code
     */
    public boolean verifyAndConsumeBackupCode(String inputCode, List<String> availableCodes) {
        if (inputCode == null || availableCodes == null) {
            return false;
        }
        
        // Remove the code if it exists (one-time use)
        return availableCodes.remove(inputCode.toUpperCase());
    }
    
    /**
     * Format backup codes for display (add dashes for readability)
     */
    public List<String> formatBackupCodesForDisplay(List<String> codes) {
        return codes.stream()
                .map(code -> code.substring(0, 4) + "-" + code.substring(4))
                .collect(Collectors.toList());
    }
    
    /**
     * Remove formatting from backup code for verification
     */
    public String normalizeBackupCode(String code) {
        return code.replaceAll("[^A-Z0-9]", "").toUpperCase();
    }
}