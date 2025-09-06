package com.hopngo.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SignedUploadRequest {
    
    @NotBlank(message = "Resource type is required")
    private String resourceType; // "image" or "video"
    
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize; // in bytes
    
    private String folder; // optional folder in Cloudinary
    
    public SignedUploadRequest() {}
    
    public String getResourceType() {
        return resourceType;
    }
    
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFolder() {
        return folder;
    }
    
    public void setFolder(String folder) {
        this.folder = folder;
    }
}