package com.hopngo.ai.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ModerationResponse {
    
    private String moderationId;
    private ModerationDecision decision;
    private ModerationScores scores;
    private List<String> flaggedCategories;
    private String reason;
    private Map<String, Object> metadata;
    private LocalDateTime processedAt;
    
    // Constructors
    public ModerationResponse() {}
    
    public ModerationResponse(String moderationId, ModerationDecision decision, ModerationScores scores) {
        this.moderationId = moderationId;
        this.decision = decision;
        this.scores = scores;
        this.processedAt = LocalDateTime.now();
    }
    
    public ModerationResponse(String moderationId, ModerationDecision decision, ModerationScores scores, 
                            List<String> flaggedCategories, String reason, Map<String, Object> metadata) {
        this.moderationId = moderationId;
        this.decision = decision;
        this.scores = scores;
        this.flaggedCategories = flaggedCategories;
        this.reason = reason;
        this.metadata = metadata;
        this.processedAt = LocalDateTime.now();
    }
    
    // Nested classes
    public enum ModerationDecision {
        APPROVED,
        FLAGGED,
        REJECTED
    }
    
    public static class ModerationScores {
        private double toxicity;
        private double nsfw;
        private double spam;
        private double hate;
        private double violence;
        private double harassment;
        private double overallRisk;
        
        public ModerationScores() {}
        
        public ModerationScores(double toxicity, double nsfw, double spam) {
            this.toxicity = toxicity;
            this.nsfw = nsfw;
            this.spam = spam;
            this.overallRisk = Math.max(Math.max(toxicity, nsfw), spam);
        }
        
        public ModerationScores(double toxicity, double nsfw, double spam, double hate, double violence, double harassment) {
            this.toxicity = toxicity;
            this.nsfw = nsfw;
            this.spam = spam;
            this.hate = hate;
            this.violence = violence;
            this.harassment = harassment;
            this.overallRisk = Math.max(Math.max(Math.max(toxicity, nsfw), Math.max(spam, hate)), Math.max(violence, harassment));
        }
        
        // Getters and Setters
        public double getToxicity() { return toxicity; }
        public void setToxicity(double toxicity) { this.toxicity = toxicity; }
        
        public double getNsfw() { return nsfw; }
        public void setNsfw(double nsfw) { this.nsfw = nsfw; }
        
        public double getSpam() { return spam; }
        public void setSpam(double spam) { this.spam = spam; }
        
        public double getHate() { return hate; }
        public void setHate(double hate) { this.hate = hate; }
        
        public double getViolence() { return violence; }
        public void setViolence(double violence) { this.violence = violence; }
        
        public double getHarassment() { return harassment; }
        public void setHarassment(double harassment) { this.harassment = harassment; }
        
        public double getOverallRisk() { return overallRisk; }
        public void setOverallRisk(double overallRisk) { this.overallRisk = overallRisk; }
    }
    
    // Getters and Setters
    public String getModerationId() {
        return moderationId;
    }
    
    public void setModerationId(String moderationId) {
        this.moderationId = moderationId;
    }
    
    public ModerationDecision getDecision() {
        return decision;
    }
    
    public void setDecision(ModerationDecision decision) {
        this.decision = decision;
    }
    
    public ModerationScores getScores() {
        return scores;
    }
    
    public void setScores(ModerationScores scores) {
        this.scores = scores;
    }
    
    public List<String> getFlaggedCategories() {
        return flaggedCategories;
    }
    
    public void setFlaggedCategories(List<String> flaggedCategories) {
        this.flaggedCategories = flaggedCategories;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}