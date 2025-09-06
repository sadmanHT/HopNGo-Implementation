package com.hopngo.ai.service;

import com.hopngo.ai.dto.ModerationRequest;
import com.hopngo.ai.dto.ModerationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ModerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ModerationService.class);
    
    @Autowired
    private ImageHashService imageHashService;
    
    @Value("${moderation.thresholds.toxicity:0.7}")
    private double toxicityThreshold;
    
    @Value("${moderation.thresholds.nsfw:0.8}")
    private double nsfwThreshold;
    
    @Value("${moderation.thresholds.spam:0.6}")
    private double spamThreshold;
    
    // Mock toxic words for demonstration
    private static final Set<String> TOXIC_WORDS = Set.of(
        "hate", "stupid", "idiot", "kill", "die", "murder", "violence", "attack"
    );
    
    // Mock NSFW indicators
    private static final Set<String> NSFW_WORDS = Set.of(
        "nude", "naked", "sex", "porn", "adult", "explicit"
    );
    
    // Mock spam indicators
    private static final Set<String> SPAM_INDICATORS = Set.of(
        "buy now", "click here", "free money", "guaranteed", "limited time", "act now"
    );
    
    private static final Pattern EXCESSIVE_CAPS = Pattern.compile("[A-Z]{5,}");
    private static final Pattern EXCESSIVE_PUNCTUATION = Pattern.compile("[!?]{3,}");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");
    
    public ModerationResponse moderateContent(ModerationRequest request) {
        logger.info("Moderating content of type: {} for user: {}", request.getContentType(), request.getUserId());
        
        String moderationId = UUID.randomUUID().toString();
        
        // Calculate moderation scores
        ModerationResponse.ModerationScores scores = calculateScores(request);
        
        // Determine decision based on thresholds
        ModerationResponse.ModerationDecision decision = determineDecision(scores);
        
        // Identify flagged categories
        List<String> flaggedCategories = identifyFlaggedCategories(scores);
        
        // Generate reason if flagged
        String reason = generateReason(decision, flaggedCategories, scores);
        
        // Create metadata
        Map<String, Object> metadata = createMetadata(request, scores);
        
        ModerationResponse response = new ModerationResponse(
            moderationId, decision, scores, flaggedCategories, reason, metadata
        );
        
        logger.info("Moderation completed. ID: {}, Decision: {}, Risk Score: {}", 
                   moderationId, decision, scores.getOverallRisk());
        
        return response;
    }
    
    private ModerationResponse.ModerationScores calculateScores(ModerationRequest request) {
        String content = request.getContent().toLowerCase();
        
        // Calculate toxicity score
        double toxicity = calculateToxicityScore(content);
        
        // Calculate NSFW score
        double nsfw = calculateNsfwScore(content, request.getMediaUrls());
        
        // Calculate spam score
        double spam = calculateSpamScore(content);
        
        // Calculate additional scores
        double hate = calculateHateScore(content);
        double violence = calculateViolenceScore(content);
        double harassment = calculateHarassmentScore(content);
        
        // Check for duplicate images and adjust spam score if duplicates found
        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            List<String> duplicates = imageHashService.checkForDuplicates(request.getMediaUrls());
            if (!duplicates.isEmpty()) {
                // Increase spam score for duplicate content
                spam = Math.min(spam + 0.4, 1.0);
                logger.info("Duplicate images detected, adjusting spam score. Duplicates: {}", duplicates.size());
            }
        }
        
        return new ModerationResponse.ModerationScores(toxicity, nsfw, spam, hate, violence, harassment);
    }
    
    private double calculateToxicityScore(String content) {
        double score = 0.0;
        
        // Check for toxic words
        for (String toxicWord : TOXIC_WORDS) {
            if (content.contains(toxicWord)) {
                score += 0.3;
            }
        }
        
        // Check for excessive caps (indicates shouting/aggression)
        if (EXCESSIVE_CAPS.matcher(content).find()) {
            score += 0.2;
        }
        
        // Check for excessive punctuation
        if (EXCESSIVE_PUNCTUATION.matcher(content).find()) {
            score += 0.1;
        }
        
        // Add randomness for more realistic mock behavior
        score += Math.random() * 0.1;
        
        return Math.min(score, 1.0);
    }
    
    private double calculateNsfwScore(String content, List<String> mediaUrls) {
        double score = 0.0;
        
        // Check for NSFW words
        for (String nsfwWord : NSFW_WORDS) {
            if (content.contains(nsfwWord)) {
                score += 0.4;
            }
        }
        
        // Mock image analysis - in real implementation, this would use AI vision models
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            // Simulate image analysis based on URL patterns
            for (String url : mediaUrls) {
                if (url.contains("adult") || url.contains("nsfw")) {
                    score += 0.6;
                }
            }
            // Add base score for having images (some risk)
            score += Math.random() * 0.2;
        }
        
        return Math.min(score, 1.0);
    }
    
    private double calculateSpamScore(String content) {
        double score = 0.0;
        
        // Check for spam indicators
        for (String spamIndicator : SPAM_INDICATORS) {
            if (content.contains(spamIndicator)) {
                score += 0.3;
            }
        }
        
        // Check for excessive URLs
        long urlCount = URL_PATTERN.matcher(content).results().count();
        if (urlCount > 2) {
            score += 0.4;
        } else if (urlCount > 0) {
            score += 0.1;
        }
        
        // Check for repetitive content
        String[] words = content.split("\\s+");
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        double repetitionRatio = 1.0 - ((double) uniqueWords.size() / words.length);
        if (repetitionRatio > 0.5) {
            score += 0.3;
        }
        
        // Add randomness
        score += Math.random() * 0.1;
        
        return Math.min(score, 1.0);
    }
    
    private double calculateHateScore(String content) {
        // Simplified hate speech detection
        String[] hateIndicators = {"racist", "discrimination", "prejudice", "bigot"};
        double score = 0.0;
        
        for (String indicator : hateIndicators) {
            if (content.contains(indicator)) {
                score += 0.4;
            }
        }
        
        return Math.min(score + Math.random() * 0.1, 1.0);
    }
    
    private double calculateViolenceScore(String content) {
        // Simplified violence detection
        String[] violenceIndicators = {"fight", "punch", "weapon", "blood", "hurt"};
        double score = 0.0;
        
        for (String indicator : violenceIndicators) {
            if (content.contains(indicator)) {
                score += 0.3;
            }
        }
        
        return Math.min(score + Math.random() * 0.1, 1.0);
    }
    
    private double calculateHarassmentScore(String content) {
        // Simplified harassment detection
        String[] harassmentIndicators = {"bully", "threaten", "stalk", "harass"};
        double score = 0.0;
        
        for (String indicator : harassmentIndicators) {
            if (content.contains(indicator)) {
                score += 0.4;
            }
        }
        
        return Math.min(score + Math.random() * 0.1, 1.0);
    }
    
    private ModerationResponse.ModerationDecision determineDecision(ModerationResponse.ModerationScores scores) {
        if (scores.getToxicity() >= toxicityThreshold || 
            scores.getNsfw() >= nsfwThreshold || 
            scores.getSpam() >= spamThreshold ||
            scores.getOverallRisk() >= 0.8) {
            return ModerationResponse.ModerationDecision.REJECTED;
        } else if (scores.getOverallRisk() >= 0.5) {
            return ModerationResponse.ModerationDecision.FLAGGED;
        } else {
            return ModerationResponse.ModerationDecision.APPROVED;
        }
    }
    
    private List<String> identifyFlaggedCategories(ModerationResponse.ModerationScores scores) {
        List<String> categories = new ArrayList<>();
        
        if (scores.getToxicity() >= 0.5) categories.add("toxicity");
        if (scores.getNsfw() >= 0.5) categories.add("nsfw");
        if (scores.getSpam() >= 0.5) categories.add("spam");
        if (scores.getHate() >= 0.5) categories.add("hate");
        if (scores.getViolence() >= 0.5) categories.add("violence");
        if (scores.getHarassment() >= 0.5) categories.add("harassment");
        
        return categories;
    }
    
    private String generateReason(ModerationResponse.ModerationDecision decision, 
                                List<String> flaggedCategories, 
                                ModerationResponse.ModerationScores scores) {
        if (decision == ModerationResponse.ModerationDecision.APPROVED) {
            return "Content approved - no policy violations detected";
        }
        
        if (flaggedCategories.isEmpty()) {
            return "Content flagged for manual review due to elevated risk score";
        }
        
        StringBuilder reason = new StringBuilder("Content flagged for: ");
        reason.append(String.join(", ", flaggedCategories));
        reason.append(". Overall risk score: ").append(String.format("%.2f", scores.getOverallRisk()));
        
        return reason.toString();
    }
    
    private Map<String, Object> createMetadata(ModerationRequest request, ModerationResponse.ModerationScores scores) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contentType", request.getContentType());
        metadata.put("contextType", request.getContextType());
        metadata.put("contentLength", request.getContent().length());
        metadata.put("hasMedia", request.getMediaUrls() != null && !request.getMediaUrls().isEmpty());
        metadata.put("mediaCount", request.getMediaUrls() != null ? request.getMediaUrls().size() : 0);
        metadata.put("processingVersion", "1.0");
        metadata.put("thresholds", Map.of(
            "toxicity", toxicityThreshold,
            "nsfw", nsfwThreshold,
            "spam", spamThreshold
        ));
        
        // Add duplicate detection results
        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            List<String> duplicates = imageHashService.checkForDuplicates(request.getMediaUrls());
            metadata.put("duplicateImages", duplicates);
            metadata.put("hasDuplicates", !duplicates.isEmpty());
            metadata.put("duplicateCount", duplicates.size());
        } else {
            metadata.put("duplicateImages", Collections.emptyList());
            metadata.put("hasDuplicates", false);
            metadata.put("duplicateCount", 0);
        }
        
        return metadata;
    }
}