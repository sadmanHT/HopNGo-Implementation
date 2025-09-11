package com.hopngo.analytics.service;

import com.hopngo.analytics.entity.Subscriber;
import com.hopngo.analytics.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class NewsletterService {

    @Autowired
    private SubscriberRepository subscriberRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Subscribe a user to the newsletter
     */
    public Subscriber subscribe(String email, String source, String userId, String utmSource, String utmMedium, String utmCampaign) {
        // Validate email format
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check if email already exists
        Optional<Subscriber> existingSubscriber = subscriberRepository.findByEmail(email);
        
        if (existingSubscriber.isPresent()) {
            Subscriber subscriber = existingSubscriber.get();
            
            // If subscriber was previously unsubscribed, reactivate them
            if (subscriber.getStatus() == Subscriber.SubscriberStatus.UNSUBSCRIBED) {
                subscriber.setStatus(Subscriber.SubscriberStatus.ACTIVE);
                subscriber.setUpdatedAt(OffsetDateTime.now());
                
                // Update source and UTM parameters if provided
                if (source != null) subscriber.setSource(source);
                if (utmSource != null) subscriber.setUtmSource(utmSource);
                if (utmMedium != null) subscriber.setUtmMedium(utmMedium);
                if (utmCampaign != null) subscriber.setUtmCampaign(utmCampaign);
                
                return subscriberRepository.save(subscriber);
            } else {
                // Already subscribed
                return subscriber;
            }
        }

        // Create new subscriber
        Subscriber subscriber;
        Long userIdLong = userId != null ? Long.valueOf(userId) : null;
        if ("footer".equals(source)) {
            subscriber = Subscriber.createFromFooter(email, userIdLong);
        } else if ("popup".equals(source)) {
            subscriber = Subscriber.createFromPopup(email, userIdLong);
        } else {
            subscriber = new Subscriber();
            subscriber.setEmail(email);
            subscriber.setStatus(Subscriber.SubscriberStatus.ACTIVE);
            subscriber.setSource(source != null ? source : "unknown");
            subscriber.setUserId(userId);
            subscriber.setCreatedAt(OffsetDateTime.now());
            subscriber.setUpdatedAt(OffsetDateTime.now());
        }

        // Set UTM parameters
        subscriber.setUtmSource(utmSource);
        subscriber.setUtmMedium(utmMedium);
        subscriber.setUtmCampaign(utmCampaign);

        return subscriberRepository.save(subscriber);
    }

    /**
     * Subscribe via footer form
     */
    public Subscriber subscribeFromFooter(String email, String userId) {
        return subscribe(email, "footer", userId, null, null, null);
    }

    /**
     * Subscribe via popup form
     */
    public Subscriber subscribeFromPopup(String email, String userId, String utmSource, String utmMedium, String utmCampaign) {
        return subscribe(email, "popup", userId, utmSource, utmMedium, utmCampaign);
    }

    /**
     * Unsubscribe a user from the newsletter
     */
    public boolean unsubscribe(String email) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByEmail(email);
        
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            subscriber.unsubscribe();
            subscriberRepository.save(subscriber);
            return true;
        }
        
        return false;
    }

    /**
     * Unsubscribe using unsubscribe token
     */
    public boolean unsubscribeByToken(String unsubscribeToken) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByUnsubscribeToken(unsubscribeToken);
        
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            subscriber.unsubscribe();
            subscriberRepository.save(subscriber);
            return true;
        }
        
        return false;
    }

    /**
     * Get subscriber by email
     */
    @Transactional(readOnly = true)
    public Optional<Subscriber> getSubscriber(String email) {
        return subscriberRepository.findByEmail(email);
    }

    /**
     * Check if email is subscribed
     */
    @Transactional(readOnly = true)
    public boolean isSubscribed(String email) {
        Optional<Subscriber> subscriber = subscriberRepository.findByEmail(email);
        return subscriber.isPresent() && subscriber.get().isActive();
    }

    /**
     * Get all active subscribers
     */
    @Transactional(readOnly = true)
    public List<Subscriber> getActiveSubscribers() {
        return subscriberRepository.findByStatus(Subscriber.SubscriberStatus.ACTIVE);
    }

    /**
     * Get subscribers by source
     */
    @Transactional(readOnly = true)
    public List<Subscriber> getSubscribersBySource(String source) {
        return subscriberRepository.findBySourceAndStatus(source, Subscriber.SubscriberStatus.ACTIVE);
    }

    /**
     * Get subscription statistics
     */
    @Transactional(readOnly = true)
    public SubscriptionStats getSubscriptionStats() {
        long totalSubscribers = subscriberRepository.countByStatus(Subscriber.SubscriberStatus.ACTIVE);
        long totalUnsubscribed = subscriberRepository.countByStatus(Subscriber.SubscriberStatus.UNSUBSCRIBED);
        long footerSubscribers = subscriberRepository.countBySourceAndStatus("footer", Subscriber.SubscriberStatus.ACTIVE);
        long popupSubscribers = subscriberRepository.countBySourceAndStatus("popup", Subscriber.SubscriberStatus.ACTIVE);
        long referralSubscribers = subscriberRepository.countBySourceAndStatus("referral", Subscriber.SubscriberStatus.ACTIVE);
        
        return new SubscriptionStats(
            totalSubscribers,
            totalUnsubscribed,
            footerSubscribers,
            popupSubscribers,
            referralSubscribers
        );
    }

    /**
     * Get recent subscribers
     */
    @Transactional(readOnly = true)
    public List<Subscriber> getRecentSubscribers(int limit) {
        return subscriberRepository.findRecentSubscribers(limit);
    }

    /**
     * Add tag to subscriber
     */
    public void addTagToSubscriber(String email, String tag) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByEmail(email);
        
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            subscriber.addTag(tag);
            subscriberRepository.save(subscriber);
        }
    }

    /**
     * Remove tag from subscriber
     */
    public void removeTagFromSubscriber(String email, String tag) {
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByEmail(email);
        
        if (subscriberOpt.isPresent()) {
            Subscriber subscriber = subscriberOpt.get();
            subscriber.removeTag(tag);
            subscriberRepository.save(subscriber);
        }
    }

    /**
     * Get subscribers by tag
     */
    @Transactional(readOnly = true)
    public List<Subscriber> getSubscribersByTag(String tag) {
        return subscriberRepository.findByTagsContainingAndStatus(tag, Subscriber.SubscriberStatus.ACTIVE);
    }

    // Private helper methods
    
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // Inner class for subscription statistics
    public static class SubscriptionStats {
        private final long totalActive;
        private final long totalUnsubscribed;
        private final long footerSubscribers;
        private final long popupSubscribers;
        private final long referralSubscribers;

        public SubscriptionStats(long totalActive, long totalUnsubscribed, long footerSubscribers, 
                               long popupSubscribers, long referralSubscribers) {
            this.totalActive = totalActive;
            this.totalUnsubscribed = totalUnsubscribed;
            this.footerSubscribers = footerSubscribers;
            this.popupSubscribers = popupSubscribers;
            this.referralSubscribers = referralSubscribers;
        }

        // Getters
        public long getTotalActive() { return totalActive; }
        public long getTotalUnsubscribed() { return totalUnsubscribed; }
        public long getFooterSubscribers() { return footerSubscribers; }
        public long getPopupSubscribers() { return popupSubscribers; }
        public long getReferralSubscribers() { return referralSubscribers; }
        
        public long getTotalSubscribers() { return totalActive + totalUnsubscribed; }
        
        public double getUnsubscribeRate() {
            long total = getTotalSubscribers();
            return total > 0 ? (double) totalUnsubscribed / total * 100 : 0.0;
        }
    }
}