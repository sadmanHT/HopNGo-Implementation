package com.hopngo.auth.service;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.passay.*;
import org.passay.dictionary.WordListDictionary;
import org.passay.dictionary.WordLists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordValidationService {
    
    private final Zxcvbn zxcvbn;
    private PasswordValidator passwordValidator;
    
    @Value("${app.password.min-length:8}")
    private int minLength;
    
    @Value("${app.password.min-zxcvbn-score:3}")
    private int minZxcvbnScore;
    
    @Value("${app.password.require-uppercase:true}")
    private boolean requireUppercase;
    
    @Value("${app.password.require-lowercase:true}")
    private boolean requireLowercase;
    
    @Value("${app.password.require-digit:true}")
    private boolean requireDigit;
    
    @Value("${app.password.require-special:true}")
    private boolean requireSpecial;
    
    public PasswordValidationService() {
        this.zxcvbn = new Zxcvbn();
    }
    
    // @PostConstruct
    // private void init() {
    //     this.passwordValidator = createPasswordValidator();
    // }
    
    private PasswordValidator getPasswordValidator() {
        if (this.passwordValidator == null) {
            this.passwordValidator = createPasswordValidator();
        }
        return this.passwordValidator;
    }
    
    /**
     * Validate password strength and complexity
     */
    public PasswordValidationResult validatePassword(String password, String email) {
        List<String> errors = new ArrayList<>();
        
        // Basic validation using Passay
        RuleResult result = getPasswordValidator().validate(new PasswordData(password));
        if (!result.isValid()) {
            for (String message : getPasswordValidator().getMessages(result)) {
                errors.add(message);
            }
        }
        
        // Zxcvbn strength validation
        List<String> userInputs = new ArrayList<>();
        if (email != null && !email.isEmpty()) {
            userInputs.add(email);
            userInputs.add(email.split("@")[0]); // username part
        }
        
        Strength strength = zxcvbn.measure(password, userInputs);
        
        if (strength.getScore() < minZxcvbnScore) {
            errors.add(String.format("Password is too weak. Score: %d/%d. %s", 
                    strength.getScore(), 4, strength.getFeedback().getSuggestions()));
        }
        
        // Check for common patterns
        if (containsPersonalInfo(password, email)) {
            errors.add("Password should not contain personal information like email");
        }
        
        boolean isValid = errors.isEmpty();
        
        return new PasswordValidationResult(
                isValid,
                errors,
                strength.getScore(),
                strength.getFeedback().getWarning(),
                strength.getFeedback().getSuggestions()
        );
    }
    
    /**
     * Create password validator with rules
     */
    private PasswordValidator createPasswordValidator() {
        List<Rule> rules = new ArrayList<>();
        
        // Length rule
        rules.add(new LengthRule(minLength, 128));
        
        // Character rules
        if (requireUppercase) {
            rules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        }
        
        if (requireLowercase) {
            rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        }
        
        if (requireDigit) {
            rules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        }
        
        if (requireSpecial) {
            rules.add(new CharacterRule(EnglishCharacterData.Special, 1));
        }
        
        // No whitespace
        rules.add(new WhitespaceRule());
        
        // No common passwords - using a simpler approach
        // Skip dictionary rule for now to avoid sorting issues
        // TODO: Implement custom common password validation if needed
        
        return new PasswordValidator(rules);
    }
    
    /**
     * Check if password contains personal information
     */
    private boolean containsPersonalInfo(String password, String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        
        String lowerPassword = password.toLowerCase();
        String lowerEmail = email.toLowerCase();
        
        // Check if password contains email or username
        if (lowerPassword.contains(lowerEmail) || lowerPassword.contains(lowerEmail.split("@")[0])) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get list of common passwords to reject
     */
    private String getCommonPasswords() {
        List<String> passwords = List.of(
                "password", "123456", "password123", "admin", "qwerty",
                "letmein", "welcome", "monkey", "1234567890", "abc123",
                "Password1", "password1", "123456789", "welcome123",
                "admin123", "root", "toor", "pass", "test", "guest"
        );
        // Sort the passwords as required by WordLists
        return passwords.stream().sorted().collect(java.util.stream.Collectors.joining("\n"));
    }
    
    /**
     * Generate a secure password suggestion
     */
    public String generateSecurePassword() {
        PasswordGenerator generator = new PasswordGenerator();
        
        List<CharacterRule> rules = new ArrayList<>();
        rules.add(new CharacterRule(EnglishCharacterData.UpperCase, 2));
        rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 2));
        rules.add(new CharacterRule(EnglishCharacterData.Digit, 2));
        rules.add(new CharacterRule(EnglishCharacterData.Special, 2));
        
        return generator.generatePassword(12, rules);
    }
    
    /**
     * Password validation result class
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final int zxcvbnScore;
        private final String warning;
        private final List<String> suggestions;
        
        public PasswordValidationResult(boolean valid, List<String> errors, 
                                      int zxcvbnScore, String warning, List<String> suggestions) {
            this.valid = valid;
            this.errors = errors;
            this.zxcvbnScore = zxcvbnScore;
            this.warning = warning;
            this.suggestions = suggestions;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public int getZxcvbnScore() { return zxcvbnScore; }
        public String getWarning() { return warning; }
        public List<String> getSuggestions() { return suggestions; }
    }
}