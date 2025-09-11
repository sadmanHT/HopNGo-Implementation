package com.hopngo.support.service;

import com.hopngo.support.dto.CannedReplyCreateRequest;
import com.hopngo.support.entity.CannedReply;
import com.hopngo.support.repository.CannedReplyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CannedReplyService {

    private final CannedReplyRepository cannedReplyRepository;

    @Autowired
    public CannedReplyService(CannedReplyRepository cannedReplyRepository) {
        this.cannedReplyRepository = cannedReplyRepository;
    }

    @Transactional(readOnly = true)
    public Page<CannedReply> getAllCannedReplies(Pageable pageable) {
        return cannedReplyRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<CannedReply> getCannedRepliesByCategory(String category, Pageable pageable) {
        return cannedReplyRepository.findByCategoryIgnoreCaseOrderByCreatedAtDesc(category, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CannedReply> getCannedRepliesByCreator(String createdBy, Pageable pageable) {
        return cannedReplyRepository.findByCreatedByOrderByCreatedAtDesc(createdBy, pageable);
    }

    @Transactional(readOnly = true)
    public Page<CannedReply> searchCannedReplies(String query, Pageable pageable) {
        return cannedReplyRepository.searchCannedReplies(query, pageable);
    }

    @Transactional(readOnly = true)
    public CannedReply getCannedReplyById(Long id) {
        return cannedReplyRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return cannedReplyRepository.findAllCategories();
    }

    @Transactional(readOnly = true)
    public Page<CannedReply> getPopularCannedReplies(Pageable pageable) {
        // For now, return by creation date. In a real implementation, 
        // you might track usage statistics
        return cannedReplyRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public CannedReply createCannedReply(CannedReplyCreateRequest request) {
        CannedReply cannedReply = new CannedReply();
        cannedReply.setTitle(request.getTitle());
        cannedReply.setBody(request.getBody());
        cannedReply.setCategory(request.getCategoryOrDefault());
        cannedReply.setCreatedBy(request.getCreatedBy());
        
        return cannedReplyRepository.save(cannedReply);
    }

    public CannedReply updateCannedReply(Long id, CannedReplyCreateRequest request) {
        CannedReply existingReply = getCannedReplyById(id);
        if (existingReply == null) {
            throw new RuntimeException("Canned reply not found with id: " + id);
        }
        
        existingReply.setTitle(request.getTitle());
        existingReply.setBody(request.getBody());
        existingReply.setCategory(request.getCategoryOrDefault());
        
        return cannedReplyRepository.save(existingReply);
    }

    public void deleteCannedReply(Long id) {
        CannedReply cannedReply = getCannedReplyById(id);
        if (cannedReply == null) {
            throw new RuntimeException("Canned reply not found with id: " + id);
        }
        
        cannedReplyRepository.delete(cannedReply);
    }

    @Transactional(readOnly = true)
    public boolean canUserModifyReply(CannedReply cannedReply, String userId) {
        if (cannedReply == null || userId == null) {
            return false;
        }
        
        // Creator can modify their own replies
        if (userId.equals(cannedReply.getCreatedBy())) {
            return true;
        }
        
        // Admin can modify any reply (this would be checked at controller level with @PreAuthorize)
        return false;
    }

    public void incrementUsageCount(Long id) {
        // In a real implementation, you might track usage statistics
        // For now, this is a placeholder method
        CannedReply cannedReply = getCannedReplyById(id);
        if (cannedReply != null) {
            // Could add a usageCount field to CannedReply entity and increment it here
            // cannedReply.setUsageCount(cannedReply.getUsageCount() + 1);
            // cannedReplyRepository.save(cannedReply);
        }
    }

    @Transactional(readOnly = true)
    public long getTotalCannedReplyCount() {
        return cannedReplyRepository.count();
    }

    @Transactional(readOnly = true)
    public long getCannedReplyCountByCategory(String category) {
        return cannedReplyRepository.countByCategoryIgnoreCase(category);
    }

    @Transactional(readOnly = true)
    public long getCannedReplyCountByCreator(String createdBy) {
        return cannedReplyRepository.countByCreatedBy(createdBy);
    }

    @Transactional(readOnly = true)
    public boolean existsByTitle(String title) {
        return cannedReplyRepository.existsByTitleIgnoreCase(title);
    }

    @Transactional(readOnly = true)
    public List<CannedReply> getCannedRepliesByTitleContaining(String titlePart) {
        return cannedReplyRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(titlePart);
    }
}