package com.hopngo.analytics.service;

import com.hopngo.analytics.entity.Referral;
import com.hopngo.analytics.entity.PointsLedger;
import com.hopngo.analytics.entity.Subscriber;
import com.hopngo.analytics.repository.ReferralRepository;
import com.hopngo.analytics.repository.PointsLedgerRepository;
import com.hopngo.analytics.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReferralService {

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private PointsLedgerRepository pointsLedgerRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    /**
     * Create a new referral code for a user
     */
    public Referral createReferral(String userId, String campaign) {
        String referralCode = generateReferralCode(userId);
        
        Referral referral = new Referral();
        referral.setUserId(userId);
        referral.setReferralCode(referralCode);
        referral.setCampaign(campaign != null ? campaign : "default");
        referral.setStatus(Referral.ReferralStatus.ACTIVE);
        referral.setCreatedAt(OffsetDateTime.now());
        referral.setExpiresAt(OffsetDateTime.now().plusDays(30)); // 30 days expiry
        
        return referralRepository.save(referral);
    }

    /**
     * Track a referral click/visit
     */
    public void trackReferralClick(String referralCode, String ipAddress, String userAgent) {
        Optional<Referral> referralOpt = referralRepository.findByReferralCodeAndStatus(referralCode, Referral.ReferralStatus.ACTIVE);
        
        if (referralOpt.isPresent()) {
            Referral referral = referralOpt.get();
            if (!referral.isExpired()) {
                referral.setClickCount(referral.getClickCount() + 1);
                referral.setLastClickedAt(OffsetDateTime.now());
                referralRepository.save(referral);
                
                // Track analytics event
                trackAnalyticsEvent("referral_click", referral.getUserId(), referralCode, ipAddress, userAgent);
            }
        }
    }

    /**
     * Process a successful referral conversion
     */
    public void processReferralConversion(String referralCode, String newUserId, String conversionType) {
        Optional<Referral> referralOpt = referralRepository.findByReferralCodeAndStatus(referralCode, Referral.ReferralStatus.ACTIVE);
        
        if (referralOpt.isPresent()) {
            Referral referral = referralOpt.get();
            if (!referral.isExpired() && !referral.getUserId().equals(newUserId)) {
                // Update referral stats
                referral.setConversionCount(referral.getConversionCount() + 1);
                referral.setLastConvertedAt(OffsetDateTime.now());
                
                // Award points to referrer
                int pointsAwarded = calculateReferralPoints(conversionType);
                if (pointsAwarded > 0) {
                    awardReferralPoints(referral.getUserId(), pointsAwarded, referralCode, conversionType);
                }
                
                // Mark as completed if it's a one-time referral
                if ("signup".equals(conversionType)) {
                    referral.markAsCompleted(conversionType, 0L, pointsAwarded);
                }
                
                referralRepository.save(referral);
                
                // Track analytics event
                trackAnalyticsEvent("referral_conversion", referral.getUserId(), referralCode, null, conversionType);
            }
        }
    }

    /**
     * Get referral statistics for a user
     */
    @Transactional(readOnly = true)
    public ReferralStats getReferralStats(String userId) {
        List<Referral> userReferrals = referralRepository.findByUserId(userId);
        
        int totalClicks = userReferrals.stream().mapToInt(Referral::getClickCount).sum();
        int totalConversions = userReferrals.stream().mapToInt(Referral::getConversionCount).sum();
        int totalPointsEarned = pointsLedgerRepository.findTotalPointsByUserIdAndType(userId, PointsLedger.TransactionType.REFERRAL_BONUS);
        
        return new ReferralStats(userReferrals.size(), totalClicks, totalConversions, totalPointsEarned);
    }

    /**
     * Get active referral codes for a user
     */
    @Transactional(readOnly = true)
    public List<Referral> getActiveReferrals(String userId) {
        return referralRepository.findByUserIdAndStatus(userId, Referral.ReferralStatus.ACTIVE);
    }

    /**
     * Validate and get referral by code
     */
    @Transactional(readOnly = true)
    public Optional<Referral> validateReferralCode(String referralCode) {
        Optional<Referral> referralOpt = referralRepository.findByReferralCodeAndStatus(referralCode, Referral.ReferralStatus.ACTIVE);
        
        if (referralOpt.isPresent() && !referralOpt.get().isExpired()) {
            return referralOpt;
        }
        
        return Optional.empty();
    }

    /**
     * Subscribe user via referral
     */
    public Subscriber subscribeViaReferral(String email, String referralCode, String userId) {
        Subscriber subscriber = Subscriber.createFromReferral(email, referralCode, userId);
        subscriber = subscriberRepository.save(subscriber);
        
        // Track the referral conversion
        processReferralConversion(referralCode, userId, "subscription");
        
        return subscriber;
    }

    // Private helper methods
    
    private String generateReferralCode(String userId) {
        // Generate a unique referral code based on user ID and timestamp
        String baseCode = userId.substring(0, Math.min(userId.length(), 4)).toUpperCase();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return baseCode + randomSuffix;
    }

    private int calculateReferralPoints(String conversionType) {
        switch (conversionType) {
            case "signup":
                return 100;
            case "subscription":
                return 50;
            case "booking":
                return 200;
            case "purchase":
                return 150;
            default:
                return 25;
        }
    }

    private void awardReferralPoints(String userId, int points, String referralCode, String reason) {
        // Get current balance using the repository
        Integer currentBalance = pointsLedgerRepository.findTotalPointsByUserIdAndType(userId, PointsLedger.TransactionType.EARNED);
        if (currentBalance == null) currentBalance = 0;
        Integer newBalance = currentBalance + points;
        
        PointsLedger pointsEntry = PointsLedger.createEarning(
            userId,
            points,
            newBalance,
            "referral",
            referralCode,
            "Referral bonus for " + reason + " (Code: " + referralCode + ")"
        );
        pointsEntry.setExpiresAt(OffsetDateTime.now().plusYears(1)); // Points expire in 1 year
        
        pointsLedgerRepository.save(pointsEntry);
    }

    private void trackAnalyticsEvent(String eventType, String userId, String referralCode, String ipAddress, String metadata) {
        // This would integrate with your existing analytics tracking system
        // For now, we'll just log it (in a real implementation, you'd send to analytics service)
        System.out.println(String.format("Analytics Event: %s - User: %s, Code: %s, IP: %s, Meta: %s", 
            eventType, userId, referralCode, ipAddress, metadata));
    }

    // Inner class for referral statistics
    public static class ReferralStats {
        private final int totalReferrals;
        private final int totalClicks;
        private final int totalConversions;
        private final int totalPointsEarned;

        public ReferralStats(int totalReferrals, int totalClicks, int totalConversions, int totalPointsEarned) {
            this.totalReferrals = totalReferrals;
            this.totalClicks = totalClicks;
            this.totalConversions = totalConversions;
            this.totalPointsEarned = totalPointsEarned;
        }

        // Getters
        public int getTotalReferrals() { return totalReferrals; }
        public int getTotalClicks() { return totalClicks; }
        public int getTotalConversions() { return totalConversions; }
        public int getTotalPointsEarned() { return totalPointsEarned; }
        
        public double getConversionRate() {
            return totalClicks > 0 ? (double) totalConversions / totalClicks * 100 : 0.0;
        }
    }
}