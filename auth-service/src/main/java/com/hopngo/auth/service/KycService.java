package com.hopngo.auth.service;

import com.hopngo.auth.dto.KycDecisionDto;
import com.hopngo.auth.dto.KycRequestDto;
import com.hopngo.auth.dto.KycResponseDto;
import com.hopngo.auth.entity.KycRequest;
import com.hopngo.auth.entity.UserFlags;
import com.hopngo.auth.exception.ResourceNotFoundException;
import com.hopngo.auth.exception.BadRequestException;
import com.hopngo.auth.repository.KycRequestRepository;
import com.hopngo.auth.repository.UserFlagsRepository;
import com.hopngo.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class KycService {
    
    private static final Logger logger = LoggerFactory.getLogger(KycService.class);
    
    private final KycRequestRepository kycRequestRepository;
    private final UserFlagsRepository userFlagsRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    @Autowired
    public KycService(KycRequestRepository kycRequestRepository, 
                     UserFlagsRepository userFlagsRepository,
                     UserRepository userRepository,
                     EmailService emailService) {
        this.kycRequestRepository = kycRequestRepository;
        this.userFlagsRepository = userFlagsRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    /**
     * Submit KYC request for a user
     */
    public KycResponseDto submitKycRequest(Long userId, KycRequestDto kycRequestDto) {
        logger.info("Submitting KYC request for user: {}", userId);
        
        // Check if user already has a pending request
        if (kycRequestRepository.existsPendingRequestByUserId(userId)) {
            throw new BadRequestException("User already has a pending KYC request");
        }
        
        // Convert DTO to fields map
        Map<String, Object> fields = convertDtoToFields(kycRequestDto);
        
        // Create KYC request
        KycRequest kycRequest = new KycRequest(userId, fields);
        kycRequest = kycRequestRepository.save(kycRequest);
        
        // Ensure user flags exist
        ensureUserFlagsExist(userId);
        
        logger.info("KYC request submitted successfully for user: {} with ID: {}", userId, kycRequest.getId());
        
        return KycResponseDto.fromKycRequest(kycRequest);
    }
    
    /**
     * Get KYC status for a user
     */
    @Transactional(readOnly = true)
    public KycResponseDto getKycStatus(Long userId) {
        logger.debug("Getting KYC status for user: {}", userId);
        
        Optional<KycRequest> kycRequestOpt = kycRequestRepository.findLatestByUserId(userId);
        if (kycRequestOpt.isEmpty()) {
            throw new ResourceNotFoundException("No KYC request found for user: " + userId);
        }
        
        KycRequest kycRequest = kycRequestOpt.get();
        
        // Get user flags to include verification status
        Optional<UserFlags> userFlagsOpt = userFlagsRepository.findByUserId(userId);
        Boolean verifiedProvider = userFlagsOpt.map(UserFlags::getVerifiedProvider).orElse(false);
        
        return KycResponseDto.fromKycRequestWithFlags(kycRequest, verifiedProvider);
    }
    
    /**
     * Process KYC decision (approve/reject)
     */
    public KycResponseDto processKycDecision(Long kycRequestId, KycDecisionDto decisionDto) {
        logger.info("Processing KYC decision for request: {} with decision: {}", 
                   kycRequestId, decisionDto.getDecision());
        
        if (!decisionDto.isValid()) {
            throw new BadRequestException("Invalid KYC decision: rejection requires a reason");
        }
        
        KycRequest kycRequest = kycRequestRepository.findById(kycRequestId)
            .orElseThrow(() -> new ResourceNotFoundException("KYC request not found: " + kycRequestId));
        
        if (!kycRequest.isPending()) {
            throw new BadRequestException("KYC request is not in pending status");
        }
        
        // Update KYC request status
        if (decisionDto.isApproval()) {
            kycRequest.setStatus(KycRequest.KycStatus.APPROVED);
            // Update user flags to mark as verified provider
            updateUserVerificationStatus(kycRequest.getUserId(), true);
            
            // Send approval email
            userRepository.findById(kycRequest.getUserId()).ifPresent(user -> {
                emailService.sendKycApprovalEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName());
            });
            
            logger.info("KYC request approved for user: {}", kycRequest.getUserId());
        } else {
            kycRequest.setStatus(KycRequest.KycStatus.REJECTED);
            // Add rejection reason to fields
            Map<String, Object> fields = kycRequest.getFields();
            if (fields == null) {
                fields = new HashMap<>();
            }
            fields.put("rejection_reason", decisionDto.getRejectionReason());
            if (decisionDto.getAdminNotes() != null) {
                fields.put("admin_notes", decisionDto.getAdminNotes());
            }
            kycRequest.setFields(fields);
            
            // Ensure user is not marked as verified
            updateUserVerificationStatus(kycRequest.getUserId(), false);
            
            // Send rejection email
            userRepository.findById(kycRequest.getUserId()).ifPresent(user -> {
                emailService.sendKycRejectionEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName(), decisionDto.getRejectionReason());
            });
            
            logger.info("KYC request rejected for user: {} with reason: {}", 
                       kycRequest.getUserId(), decisionDto.getRejectionReason());
        }
        
        kycRequest = kycRequestRepository.save(kycRequest);
        
        // Get updated user flags
        Optional<UserFlags> userFlagsOpt = userFlagsRepository.findByUserId(kycRequest.getUserId());
        Boolean verifiedProvider = userFlagsOpt.map(UserFlags::getVerifiedProvider).orElse(false);
        
        KycResponseDto response = KycResponseDto.fromKycRequestWithFlags(kycRequest, verifiedProvider);
        if (decisionDto.isRejection()) {
            response.setRejectionReason(decisionDto.getRejectionReason());
        }
        
        return response;
    }
    
    /**
     * Get all pending KYC requests (for admin)
     */
    @Transactional(readOnly = true)
    public Page<KycResponseDto> getPendingKycRequests(Pageable pageable) {
        logger.debug("Getting pending KYC requests with pagination");
        
        Page<KycRequest> kycRequests = kycRequestRepository.findAllPendingRequests(pageable);
        return kycRequests.map(KycResponseDto::fromKycRequest);
    }
    
    /**
     * Check if user is verified provider
     */
    @Transactional(readOnly = true)
    public boolean isVerifiedProvider(Long userId) {
        return userFlagsRepository.isVerifiedProvider(userId);
    }
    
    /**
     * Get user verification status
     */
    @Transactional(readOnly = true)
    public UserFlags getUserFlags(Long userId) {
        return userFlagsRepository.findByUserId(userId)
            .orElse(new UserFlags(userId));
    }
    
    /**
     * Get KYC request by ID
     */
    @Transactional(readOnly = true)
    public KycResponseDto getKycRequestById(Long requestId) {
        logger.debug("Getting KYC request by ID: {}", requestId);
        
        KycRequest kycRequest = kycRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("KYC request not found: " + requestId));
        
        // Get user flags to include verification status
        Optional<UserFlags> userFlagsOpt = userFlagsRepository.findByUserId(kycRequest.getUserId());
        Boolean verifiedProvider = userFlagsOpt.map(UserFlags::getVerifiedProvider).orElse(false);
        
        return KycResponseDto.fromKycRequestWithFlags(kycRequest, verifiedProvider);
    }
    
    /**
     * Get KYC statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getKycStatistics() {
        logger.debug("Getting KYC statistics");
        
        Map<String, Object> statistics = new HashMap<>();
        
        // Count by status
        long pendingCount = kycRequestRepository.countByStatus(KycRequest.KycStatus.PENDING);
        long approvedCount = kycRequestRepository.countByStatus(KycRequest.KycStatus.APPROVED);
        long rejectedCount = kycRequestRepository.countByStatus(KycRequest.KycStatus.REJECTED);
        long totalCount = pendingCount + approvedCount + rejectedCount;
        
        statistics.put("pending", pendingCount);
        statistics.put("approved", approvedCount);
        statistics.put("rejected", rejectedCount);
        statistics.put("total", totalCount);
        
        // Verified providers count
        long verifiedProvidersCount = userFlagsRepository.countVerifiedProviders();
        statistics.put("verifiedProviders", verifiedProvidersCount);
        
        return statistics;
    }
    
    /**
     * Search KYC requests by user email or name
     */
    @Transactional(readOnly = true)
    public Page<KycResponseDto> searchKycRequests(String query, Pageable pageable) {
        logger.debug("Searching KYC requests with query: {}", query);
        
        // This is a simplified search - in a real implementation, you might want to
        // join with the User table to search by email/name
        Page<KycRequest> kycRequests = kycRequestRepository.findAll(pageable);
        return kycRequests.map(KycResponseDto::fromKycRequest);
    }
    
    // Private helper methods
    
    private Map<String, Object> convertDtoToFields(KycRequestDto dto) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("document_urls", dto.getDocumentUrls());
        fields.put("personal_info", dto.getPersonalInfo());
        
        if (dto.getBusinessInfo() != null) {
            fields.put("business_info", dto.getBusinessInfo());
        }
        
        if (dto.getAdditionalNotes() != null) {
            fields.put("additional_notes", dto.getAdditionalNotes());
        }
        
        return fields;
    }
    
    private void ensureUserFlagsExist(Long userId) {
        if (!userFlagsRepository.findByUserId(userId).isPresent()) {
            UserFlags userFlags = new UserFlags(userId);
            userFlagsRepository.save(userFlags);
            logger.debug("Created user flags for user: {}", userId);
        }
    }
    
    private void updateUserVerificationStatus(Long userId, Boolean verified) {
        Optional<UserFlags> userFlagsOpt = userFlagsRepository.findByUserId(userId);
        
        if (userFlagsOpt.isPresent()) {
            UserFlags userFlags = userFlagsOpt.get();
            userFlags.setVerifiedProvider(verified);
            userFlagsRepository.save(userFlags);
        } else {
            // Create new user flags if not exists
            UserFlags userFlags = new UserFlags(userId, verified, false);
            userFlagsRepository.save(userFlags);
        }
        
        logger.debug("Updated verification status for user: {} to: {}", userId, verified);
    }
}