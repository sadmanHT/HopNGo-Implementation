package com.hopngo.social.dto;

import java.util.Map;

public class SignedUploadResponse {
    
    private String signature;
    private String timestamp;
    private String apiKey;
    private String cloudName;
    private String uploadUrl;
    private String uploadPreset;
    private Map<String, Object> params;
    
    public SignedUploadResponse() {}
    
    public String getSignature() {
        return signature;
    }
    
    public void setSignature(String signature) {
        this.signature = signature;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getCloudName() {
        return cloudName;
    }
    
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }
    
    public String getUploadUrl() {
        return uploadUrl;
    }
    
    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
    
    public String getUploadPreset() {
        return uploadPreset;
    }
    
    public void setUploadPreset(String uploadPreset) {
        this.uploadPreset = uploadPreset;
    }
    
    public Map<String, Object> getParams() {
        return params;
    }
    
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}