package com.hopngo.booking.controller;

import com.hopngo.booking.dto.VendorResponseCreateRequest;
import com.hopngo.booking.dto.VendorResponseDto;
import com.hopngo.booking.service.VendorResponseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/reviews")
public class VendorResponseController {
    
    private final VendorResponseService vendorResponseService;
    
    @Autowired
    public VendorResponseController(VendorResponseService vendorResponseService) {
        this.vendorResponseService = vendorResponseService;
    }
    
    @PostMapping("/{reviewId}/response")
    public ResponseEntity<VendorResponseDto> createVendorResponse(
            @PathVariable UUID reviewId,
            @Valid @RequestBody VendorResponseCreateRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        VendorResponseDto response = vendorResponseService.createVendorResponse(
            reviewId, request, userId
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PutMapping("/responses/{responseId}")
    public ResponseEntity<VendorResponseDto> updateVendorResponse(
            @PathVariable UUID responseId,
            @Valid @RequestBody VendorResponseCreateRequest request,
            @RequestHeader("X-User-ID") String userId) {
        
        VendorResponseDto response = vendorResponseService.updateVendorResponse(
            responseId, request, userId
        );
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/responses/{responseId}")
    public ResponseEntity<Void> deleteVendorResponse(
            @PathVariable UUID responseId,
            @RequestHeader("X-User-ID") String userId) {
        
        vendorResponseService.deleteVendorResponse(responseId, userId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{reviewId}/responses")
    public ResponseEntity<List<VendorResponseDto>> getVendorResponsesByReview(
            @PathVariable UUID reviewId) {
        
        List<VendorResponseDto> responses = vendorResponseService.findByReviewId(reviewId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/responses/{responseId}")
    public ResponseEntity<VendorResponseDto> getVendorResponse(
            @PathVariable UUID responseId) {
        
        VendorResponseDto response = vendorResponseService.findById(responseId);
        return ResponseEntity.ok(response);
    }
}