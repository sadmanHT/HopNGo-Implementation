package com.hopngo.data.service;

import com.hopngo.data.model.DataClassification;
import com.hopngo.data.model.DataRecord;
import com.hopngo.data.model.EncryptionMetadata;
import com.hopngo.data.repository.DataClassificationRepository;
import com.hopngo.data.repository.DataRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Data Classification Service for HopNGo Platform
 * Handles data tagging, encryption, retention policies, and access control
 */
@Service
@Transactional
public class DataClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(DataClassificationService.class);

    @Autowired
    private DataClassificationRepository classificationRepository;

    @Autowired
    private DataRecordRepository dataRecordRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private AuditService auditService;

    // Data classification levels
    public enum ClassificationLevel {
        PUBLIC("PUBLIC", 0, 0),           // No encryption, no retention limit
        INTERNAL("INTERNAL", 1, 2555),    // Basic encryption, 7 years retention
        SENSITIVE("SENSITIVE", 2, 1095),  // Strong encryption, 3 years retention
        CONFIDENTIAL("CONFIDENTIAL", 3, 365); // Strongest encryption, 1 year retention

        private final String label;
        private final int level;
        private final int retentionDays;

        ClassificationLevel(String label, int level, int retentionDays) {
            this.label = label;
            this.level = level;
            this.retentionDays = retentionDays;
        }

        public String getLabel() { return label; }
        public int getLevel() { return level; }
        public int getRetentionDays() { return retentionDays; }
    }

    // Patterns for automatic classification
    private static final Map<Pattern, ClassificationLevel> CLASSIFICATION_PATTERNS = Map.of(
        // Sensitive patterns
        Pattern.compile("(?i).*(ssn|social.security|tax.id).*"), ClassificationLevel.CONFIDENTIAL,
        Pattern.compile("(?i).*(credit.card|card.number|cvv).*"), ClassificationLevel.CONFIDENTIAL,
        Pattern.compile("(?i).*(password|secret|private.key).*"), ClassificationLevel.CONFIDENTIAL,
        Pattern.compile("(?i).*(bank.account|routing.number).*"), ClassificationLevel.CONFIDENTIAL,
        
        // Internal patterns
        Pattern.compile("(?i).*(email|phone|address).*"), ClassificationLevel.SENSITIVE,
        Pattern.compile("(?i).*(user.id|customer.id|account.id).*"), ClassificationLevel.SENSITIVE,
        Pattern.compile("(?i).*(birth.date|dob|age).*"), ClassificationLevel.SENSITIVE,
        
        // Internal business data
        Pattern.compile("(?i).*(revenue|profit|financial).*"), ClassificationLevel.INTERNAL,
        Pattern.compile("(?i).*(employee|staff|internal).*"), ClassificationLevel.INTERNAL
    );

    /**
     * Classify data automatically based on content and context
     */
    public DataClassification classifyData(String dataType, String content, Map<String, Object> metadata) {
        logger.debug("Classifying data type: {}", dataType);

        ClassificationLevel level = determineClassificationLevel(dataType, content, metadata);
        
        DataClassification classification = new DataClassification();
        classification.setDataType(dataType);
        classification.setClassificationLevel(level.getLabel());
        classification.setClassificationScore(level.getLevel());
        classification.setRetentionDays(level.getRetentionDays());
        classification.setRequiresEncryption(level != ClassificationLevel.PUBLIC);
        classification.setCreatedAt(LocalDateTime.now());
        classification.setMetadata(metadata);
        
        // Set encryption requirements
        setEncryptionRequirements(classification, level);
        
        // Set access controls
        setAccessControls(classification, level);
        
        classification = classificationRepository.save(classification);
        
        logger.info("Data classified as {} for type: {}", level.getLabel(), dataType);
        return classification;
    }

    /**
     * Store classified data with appropriate encryption
     */
    public DataRecord storeClassifiedData(String dataType, Object data, Long userId, 
                                        Map<String, Object> metadata) {
        // Classify the data
        String content = data.toString();
        DataClassification classification = classifyData(dataType, content, metadata);
        
        DataRecord record = new DataRecord();
        record.setDataType(dataType);
        record.setUserId(userId);
        record.setClassificationId(classification.getId());
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(calculateExpirationDate(classification.getRetentionDays()));
        
        // Encrypt data if required
        if (classification.isRequiresEncryption()) {
            EncryptionMetadata encryptionMeta = encryptData(content, classification);
            record.setEncryptedData(encryptionMeta.getEncryptedData());
            record.setEncryptionKeyId(encryptionMeta.getKeyId());
            record.setEncryptionAlgorithm(encryptionMeta.getAlgorithm());
            record.setIsEncrypted(true);
        } else {
            record.setPlainData(content);
            record.setIsEncrypted(false);
        }
        
        record = dataRecordRepository.save(record);
        
        // Audit the data storage
        auditService.logDataAccess(userId, "STORE", dataType, classification.getClassificationLevel());
        
        logger.info("Stored classified data record: {} for user: {}", record.getId(), userId);
        return record;
    }

    /**
     * Retrieve and decrypt classified data
     */
    public Object retrieveClassifiedData(Long recordId, Long userId) {
        DataRecord record = dataRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Data record not found: " + recordId));
        
        // Check access permissions
        DataClassification classification = classificationRepository.findById(record.getClassificationId())
            .orElseThrow(() -> new IllegalArgumentException("Classification not found"));
        
        if (!hasAccessPermission(userId, classification)) {
            auditService.logDataAccess(userId, "ACCESS_DENIED", record.getDataType(), 
                classification.getClassificationLevel());
            throw new SecurityException("Access denied to classified data");
        }
        
        // Check if data has expired
        if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Attempt to access expired data record: {}", recordId);
            throw new IllegalStateException("Data record has expired");
        }
        
        Object data;
        if (record.getIsEncrypted()) {
            // Decrypt the data
            data = decryptData(record.getEncryptedData(), record.getEncryptionKeyId(), 
                record.getEncryptionAlgorithm());
        } else {
            data = record.getPlainData();
        }
        
        // Audit the data access
        auditService.logDataAccess(userId, "RETRIEVE", record.getDataType(), 
            classification.getClassificationLevel());
        
        return data;
    }

    /**
     * Update data classification
     */
    public DataClassification updateClassification(Long classificationId, ClassificationLevel newLevel, 
                                                 Long userId) {
        DataClassification classification = classificationRepository.findById(classificationId)
            .orElseThrow(() -> new IllegalArgumentException("Classification not found"));
        
        ClassificationLevel oldLevel = ClassificationLevel.valueOf(classification.getClassificationLevel());
        
        // Update classification
        classification.setClassificationLevel(newLevel.getLabel());
        classification.setClassificationScore(newLevel.getLevel());
        classification.setRetentionDays(newLevel.getRetentionDays());
        classification.setRequiresEncryption(newLevel != ClassificationLevel.PUBLIC);
        classification.setUpdatedAt(LocalDateTime.now());
        
        // Update encryption requirements
        setEncryptionRequirements(classification, newLevel);
        setAccessControls(classification, newLevel);
        
        classification = classificationRepository.save(classification);
        
        // If classification level increased, re-encrypt existing data
        if (newLevel.getLevel() > oldLevel.getLevel()) {
            reencryptDataRecords(classificationId, newLevel);
        }
        
        // Audit the classification change
        auditService.logClassificationChange(userId, classificationId, oldLevel.getLabel(), 
            newLevel.getLabel());
        
        logger.info("Updated classification {} from {} to {}", classificationId, 
            oldLevel.getLabel(), newLevel.getLabel());
        
        return classification;
    }

    /**
     * Clean up expired data records
     */
    @Transactional
    public int cleanupExpiredData() {
        LocalDateTime now = LocalDateTime.now();
        List<DataRecord> expiredRecords = dataRecordRepository.findByExpiresAtBefore(now);
        
        int deletedCount = 0;
        for (DataRecord record : expiredRecords) {
            // Securely delete encrypted data
            if (record.getIsEncrypted()) {
                encryptionService.secureDeleteKey(record.getEncryptionKeyId());
            }
            
            dataRecordRepository.delete(record);
            deletedCount++;
            
            // Audit the deletion
            auditService.logDataDeletion(null, "EXPIRED", record.getDataType(), record.getId());
        }
        
        logger.info("Cleaned up {} expired data records", deletedCount);
        return deletedCount;
    }

    /**
     * Get data classification statistics
     */
    public Map<String, Object> getClassificationStats() {
        Map<String, Object> stats = new HashMap<>();
        
        for (ClassificationLevel level : ClassificationLevel.values()) {
            long count = classificationRepository.countByClassificationLevel(level.getLabel());
            stats.put(level.getLabel().toLowerCase() + "_count", count);
        }
        
        stats.put("total_classifications", classificationRepository.count());
        stats.put("encrypted_records", dataRecordRepository.countByIsEncryptedTrue());
        stats.put("expired_records", dataRecordRepository.countByExpiresAtBefore(LocalDateTime.now()));
        
        return stats;
    }

    // Private helper methods

    private ClassificationLevel determineClassificationLevel(String dataType, String content, 
                                                           Map<String, Object> metadata) {
        // Check explicit classification in metadata
        if (metadata != null && metadata.containsKey("classification")) {
            String explicitClassification = metadata.get("classification").toString();
            try {
                return ClassificationLevel.valueOf(explicitClassification.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid explicit classification: {}", explicitClassification);
            }
        }
        
        // Pattern-based classification
        String combinedText = (dataType + " " + content).toLowerCase();
        
        for (Map.Entry<Pattern, ClassificationLevel> entry : CLASSIFICATION_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(combinedText).find()) {
                return entry.getValue();
            }
        }
        
        // Default to INTERNAL for unknown data
        return ClassificationLevel.INTERNAL;
    }

    private void setEncryptionRequirements(DataClassification classification, ClassificationLevel level) {
        switch (level) {
            case PUBLIC:
                classification.setEncryptionAlgorithm(null);
                classification.setKeyRotationDays(0);
                break;
            case INTERNAL:
                classification.setEncryptionAlgorithm("AES-256-GCM");
                classification.setKeyRotationDays(365);
                break;
            case SENSITIVE:
                classification.setEncryptionAlgorithm("AES-256-GCM");
                classification.setKeyRotationDays(90);
                break;
            case CONFIDENTIAL:
                classification.setEncryptionAlgorithm("AES-256-GCM");
                classification.setKeyRotationDays(30);
                break;
        }
    }

    private void setAccessControls(DataClassification classification, ClassificationLevel level) {
        List<String> allowedRoles = new ArrayList<>();
        
        switch (level) {
            case PUBLIC:
                allowedRoles.addAll(Arrays.asList("USER", "PROVIDER", "ADMIN"));
                break;
            case INTERNAL:
                allowedRoles.addAll(Arrays.asList("PROVIDER", "ADMIN"));
                break;
            case SENSITIVE:
            case CONFIDENTIAL:
                allowedRoles.add("ADMIN");
                break;
        }
        
        classification.setAllowedRoles(allowedRoles);
    }

    private EncryptionMetadata encryptData(String data, DataClassification classification) {
        return encryptionService.encryptData(data.getBytes(StandardCharsets.UTF_8), 
            classification.getEncryptionAlgorithm());
    }

    private String decryptData(byte[] encryptedData, String keyId, String algorithm) {
        byte[] decryptedBytes = encryptionService.decryptData(encryptedData, keyId, algorithm);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private boolean hasAccessPermission(Long userId, DataClassification classification) {
        // This would integrate with your user/role service
        // For now, simplified implementation
        return true; // Implement proper role-based access control
    }

    private LocalDateTime calculateExpirationDate(int retentionDays) {
        if (retentionDays <= 0) {
            return null; // No expiration
        }
        return LocalDateTime.now().plusDays(retentionDays);
    }

    private void reencryptDataRecords(Long classificationId, ClassificationLevel newLevel) {
        List<DataRecord> records = dataRecordRepository.findByClassificationId(classificationId);
        
        for (DataRecord record : records) {
            if (record.getIsEncrypted()) {
                // Decrypt with old key, encrypt with new key
                String data = decryptData(record.getEncryptedData(), record.getEncryptionKeyId(), 
                    record.getEncryptionAlgorithm());
                
                DataClassification classification = classificationRepository.findById(classificationId).get();
                EncryptionMetadata newEncryption = encryptData(data, classification);
                
                record.setEncryptedData(newEncryption.getEncryptedData());
                record.setEncryptionKeyId(newEncryption.getKeyId());
                record.setEncryptionAlgorithm(newEncryption.getAlgorithm());
                
                dataRecordRepository.save(record);
            }
        }
    }
}