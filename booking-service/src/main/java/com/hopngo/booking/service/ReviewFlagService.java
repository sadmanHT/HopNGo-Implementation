package com.hopngo.booking.service;

import com.hopngo.booking.dto.ReviewFlagDto;
import com.hopngo.booking.dto.ReviewFlagCreateRequest;
import com.hopngo.booking.dto.ReviewFlagResolveRequest;
import com.hopngo.booking.mapper.ReviewFlagMapper;
import com.hopngo.booking.entity.Review;
import com.hopngo.booking.entity.ReviewFlag;
import com.hopngo.booking.entity.ReviewFlagStatus;
import com.hopngo.booking.repository.ReviewRepository;
import com.hopngo.booking.repository.ReviewFlagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ReviewFlagService {
    
    private final ReviewFlagRepository reviewFlagRepository;
    private final ReviewRepository reviewRepository;
    private final OutboxService outboxService;
    
    @Autowired
    private ReviewFlagMapper reviewFlagMapper;
    
    public ReviewFlagService(ReviewFlagRepository reviewFlagRepository,
                            ReviewRepository reviewRepository,
                            OutboxService outboxService) {
        this.reviewFlagRepository = reviewFlagRepository;
        this.reviewRepository = reviewRepository;
        this.outboxService = outboxService;
    }
    
    public ReviewFlagDto createReviewFlag(UUID reviewId, ReviewFlagCreateRequest request, String reporterUserId) {
        // Find the review
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        
        // Validate that user is not flagging their own review
        if (review.getUserId().equals(reporterUserId)) {
            throw new IllegalArgumentException("Cannot flag your own review");
        }
        
        // Check if user has already flagged this review
        if (reviewFlagRepository.existsByReviewIdAndReporterUserId(reviewId, reporterUserId)) {
            throw new IllegalArgumentException("You have already flagged this review");
        }
        
        // Create review flag
        ReviewFlag reviewFlag = new ReviewFlag(review, reporterUserId, request.reason());
        ReviewFlag savedFlag = reviewFlagRepository.save(reviewFlag);
        
        // Publish review flagged event
        outboxService.publishReviewFlaggedEvent(savedFlag);
        
        return reviewFlagMapper.toResponse(savedFlag);
    }
    
    public ReviewFlagDto findById(UUID flagId, String userId, String userRole) {
        ReviewFlag flag = reviewFlagRepository.findById(flagId)
            .orElseThrow(() -> new IllegalArgumentException("Review flag not found: " + flagId));
        
        validateFlagAccess(flagId, userId, userRole);
        
        return ReviewFlagMapper.toResponse(flag);
    }
    
    public List<ReviewFlagDto> findByReviewId(UUID reviewId, String userId, String userRole) {
        List<ReviewFlag> flags = reviewFlagRepository.findByReviewId(reviewId);
        
        return flags.stream()
            .filter(flag -> hasAccessToFlag(flag, userId, userRole))
            .map(ReviewFlagMapper::toResponse)
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReviewFlag> findByReporterUserId(String reporterUserId) {
        return reviewFlagRepository.findByReporterUserId(reporterUserId);
    }
    
    public List<ReviewFlagDto> findByStatus(ReviewFlagStatus status, String userId, String userRole) {
        List<ReviewFlag> flags = reviewFlagRepository.findByStatus(status);
        
        return flags.stream()
            .filter(flag -> hasAccessToFlag(flag, userId, userRole))
            .map(ReviewFlagMapper::toResponse)
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReviewFlag> findByVendorUserId(String vendorUserId) {
        return reviewFlagRepository.findByVendorUserId(vendorUserId);
    }
    
    @Transactional(readOnly = true)
    public List<ReviewFlag> findOpenFlags() {
        return reviewFlagRepository.findByStatus(ReviewFlagStatus.OPEN);
    }
    
    @Transactional(readOnly = true)
    public long countByStatus(ReviewFlagStatus status) {
        return reviewFlagRepository.countByStatus(status);
    }
    
    public ReviewFlagDto resolveReviewFlag(UUID flagId, ReviewFlagResolveRequest request, String resolverUserId, String userRole) {
        validateResolveAccess(userRole);
        
        ReviewFlag flag = reviewFlagRepository.findById(flagId)
            .orElseThrow(() -> new IllegalArgumentException("Review flag not found: " + flagId));
        
        // Validate that the status is valid for resolution
        if (request.status() == ReviewFlagStatus.OPEN) {
            throw new IllegalArgumentException("Cannot resolve flag to OPEN status");
        }
        
        flag.resolve(resolverUserId, request.decisionNote(), request.status());
        ReviewFlag resolvedFlag = reviewFlagRepository.save(flag);
        
        // Publish review flag resolved event
        outboxService.publishReviewFlagResolvedEvent(resolvedFlag);
        
        return reviewFlagMapper.toResponse(resolvedFlag);
    }
    
    public void validateFlagAccess(UUID flagId, String userId, String userRole) {
        ReviewFlag flag = reviewFlagRepository.findById(flagId)
            .orElseThrow(() -> new IllegalArgumentException("Review flag not found: " + flagId));
        
        // Admin can access all flags
        if ("ADMIN".equals(userRole)) {
            return;
        }
        
        // Provider can access flags for their own listings
        if ("PROVIDER".equals(userRole) && flag.getReview().getVendor().getUserId().equals(userId)) {
            return;
        }
        
        // Reporter can access their own flags
        if (flag.isReportedBy(userId)) {
            return;
        }
        
        throw new SecurityException("Access denied to this review flag");
    }
    
    public List<ReviewFlagDto> findAllAccessible(String userId, String userRole) {
        List<ReviewFlag> flags;
        
        if ("ADMIN".equals(userRole)) {
            flags = reviewFlagRepository.findAll();
        } else if ("PROVIDER".equals(userRole)) {
            flags = reviewFlagRepository.findByVendorUserId(userId);
        } else {
            flags = reviewFlagRepository.findByReporterUserId(userId);
        }
        
        return flags.stream()
            .map(ReviewFlagMapper::toResponse)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private boolean hasAccessToFlag(ReviewFlag flag, String userId, String userRole) {
        try {
            validateFlagAccess(flag.getId(), userId, userRole);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
    
    public void validateResolveAccess(String userRole) {
        if (!"ADMIN".equals(userRole)) {
            throw new SecurityException("Only admins can resolve review flags");
        }
    }
}