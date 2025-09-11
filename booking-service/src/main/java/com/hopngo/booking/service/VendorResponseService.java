package com.hopngo.booking.service;

import com.hopngo.booking.dto.VendorResponseCreateRequest;
import com.hopngo.booking.dto.VendorResponseDto;
import com.hopngo.booking.entity.Review;
import com.hopngo.booking.entity.VendorResponse;
import com.hopngo.booking.mapper.VendorResponseMapper;
import com.hopngo.booking.repository.ReviewRepository;
import com.hopngo.booking.repository.VendorResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class VendorResponseService {
    
    private final VendorResponseRepository vendorResponseRepository;
    private final ReviewRepository reviewRepository;
    private final OutboxService outboxService;
    
    @Autowired
    public VendorResponseService(
            VendorResponseRepository vendorResponseRepository,
            ReviewRepository reviewRepository,
            OutboxService outboxService) {
        this.vendorResponseRepository = vendorResponseRepository;
        this.reviewRepository = reviewRepository;
        this.outboxService = outboxService;
    }
    
    public VendorResponseDto createVendorResponse(UUID reviewId, VendorResponseCreateRequest request, String vendorUserId) {
        // Find the review
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        
        // Check if vendor response already exists for this review
        if (vendorResponseRepository.existsByReviewId(reviewId)) {
            throw new IllegalStateException("Vendor response already exists for this review");
        }
        
        // Validate that the vendor user is associated with the review's vendor
        if (!review.getVendor().getUserId().equals(vendorUserId)) {
            throw new IllegalArgumentException("Vendor user is not authorized to respond to this review");
        }
        
        // Create and save the vendor response
        VendorResponse vendorResponse = new VendorResponse(review, vendorUserId, request.message());
        VendorResponse savedResponse = vendorResponseRepository.save(vendorResponse);
        
        // Publish event
        outboxService.publishVendorResponseCreatedEvent(savedResponse);
        
        return VendorResponseMapper.toResponse(savedResponse);
    }
    
    @Transactional(readOnly = true)
    public VendorResponseDto findById(UUID responseId) {
        VendorResponse response = vendorResponseRepository.findById(responseId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor response not found: " + responseId));
        return VendorResponseMapper.toResponse(response);
    }
    
    @Transactional(readOnly = true)
    public List<VendorResponseDto> findByReviewId(UUID reviewId) {
        return vendorResponseRepository.findByReviewId(reviewId)
            .stream()
            .map(VendorResponseMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<VendorResponseDto> findByVendorUserId(String vendorUserId) {
        return vendorResponseRepository.findByVendorUserId(vendorUserId)
            .stream()
            .map(VendorResponseMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<VendorResponseDto> findByListingId(UUID listingId) {
        return vendorResponseRepository.findByListingId(listingId)
            .stream()
            .map(VendorResponseMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<VendorResponseDto> findByVendorId(UUID vendorId) {
        return vendorResponseRepository.findByVendorId(vendorId)
            .stream()
            .map(VendorResponseMapper::toResponse)
            .collect(Collectors.toList());
    }
    
    public VendorResponseDto updateVendorResponse(UUID responseId, VendorResponseCreateRequest request, String vendorUserId) {
        VendorResponse response = vendorResponseRepository.findById(responseId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor response not found: " + responseId));
        
        // Validate ownership
        if (!response.isOwnedBy(vendorUserId)) {
            throw new IllegalArgumentException("Vendor user is not authorized to update this response");
        }
        
        response.setMessage(request.message());
        VendorResponse updatedResponse = vendorResponseRepository.save(response);
        
        // Publish event
        outboxService.publishVendorResponseUpdatedEvent(updatedResponse);
        
        return VendorResponseMapper.toResponse(updatedResponse);
    }
    
    public void deleteVendorResponse(UUID responseId, String vendorUserId) {
        VendorResponse response = vendorResponseRepository.findById(responseId)
            .orElseThrow(() -> new IllegalArgumentException("Vendor response not found: " + responseId));
        
        // Validate ownership
        if (!response.isOwnedBy(vendorUserId)) {
            throw new IllegalArgumentException("Vendor user is not authorized to delete this response");
        }
        
        // Publish event before deletion
        outboxService.publishVendorResponseDeletedEvent(response);
        
        vendorResponseRepository.delete(response);
    }
}