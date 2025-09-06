package com.hopngo.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public class KycRequestDto {
    
    @JsonProperty("document_urls")
    @NotEmpty(message = "Document URLs are required")
    private Map<String, String> documentUrls;
    
    @JsonProperty("personal_info")
    @NotNull(message = "Personal information is required")
    private PersonalInfo personalInfo;
    
    @JsonProperty("business_info")
    private BusinessInfo businessInfo;
    
    @JsonProperty("additional_notes")
    private String additionalNotes;
    
    // Constructors
    public KycRequestDto() {}
    
    public KycRequestDto(Map<String, String> documentUrls, PersonalInfo personalInfo) {
        this.documentUrls = documentUrls;
        this.personalInfo = personalInfo;
    }
    
    // Getters and Setters
    public Map<String, String> getDocumentUrls() {
        return documentUrls;
    }
    
    public void setDocumentUrls(Map<String, String> documentUrls) {
        this.documentUrls = documentUrls;
    }
    
    public PersonalInfo getPersonalInfo() {
        return personalInfo;
    }
    
    public void setPersonalInfo(PersonalInfo personalInfo) {
        this.personalInfo = personalInfo;
    }
    
    public BusinessInfo getBusinessInfo() {
        return businessInfo;
    }
    
    public void setBusinessInfo(BusinessInfo businessInfo) {
        this.businessInfo = businessInfo;
    }
    
    public String getAdditionalNotes() {
        return additionalNotes;
    }
    
    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }
    
    // Nested classes for structured data
    public static class PersonalInfo {
        @JsonProperty("full_name")
        @NotEmpty(message = "Full name is required")
        private String fullName;
        
        @JsonProperty("date_of_birth")
        private String dateOfBirth;
        
        @JsonProperty("phone_number")
        @NotEmpty(message = "Phone number is required")
        private String phoneNumber;
        
        @JsonProperty("address")
        @NotNull(message = "Address is required")
        private Address address;
        
        // Constructors
        public PersonalInfo() {}
        
        // Getters and Setters
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getDateOfBirth() {
            return dateOfBirth;
        }
        
        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
        
        public Address getAddress() {
            return address;
        }
        
        public void setAddress(Address address) {
            this.address = address;
        }
    }
    
    public static class BusinessInfo {
        @JsonProperty("business_name")
        private String businessName;
        
        @JsonProperty("business_type")
        private String businessType;
        
        @JsonProperty("tax_id")
        private String taxId;
        
        @JsonProperty("business_address")
        private Address businessAddress;
        
        // Constructors
        public BusinessInfo() {}
        
        // Getters and Setters
        public String getBusinessName() {
            return businessName;
        }
        
        public void setBusinessName(String businessName) {
            this.businessName = businessName;
        }
        
        public String getBusinessType() {
            return businessType;
        }
        
        public void setBusinessType(String businessType) {
            this.businessType = businessType;
        }
        
        public String getTaxId() {
            return taxId;
        }
        
        public void setTaxId(String taxId) {
            this.taxId = taxId;
        }
        
        public Address getBusinessAddress() {
            return businessAddress;
        }
        
        public void setBusinessAddress(Address businessAddress) {
            this.businessAddress = businessAddress;
        }
    }
    
    public static class Address {
        @NotEmpty(message = "Street is required")
        private String street;
        
        @NotEmpty(message = "City is required")
        private String city;
        
        @NotEmpty(message = "State is required")
        private String state;
        
        @JsonProperty("postal_code")
        @NotEmpty(message = "Postal code is required")
        private String postalCode;
        
        @NotEmpty(message = "Country is required")
        private String country;
        
        // Constructors
        public Address() {}
        
        // Getters and Setters
        public String getStreet() {
            return street;
        }
        
        public void setStreet(String street) {
            this.street = street;
        }
        
        public String getCity() {
            return city;
        }
        
        public void setCity(String city) {
            this.city = city;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public String getPostalCode() {
            return postalCode;
        }
        
        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }
        
        public String getCountry() {
            return country;
        }
        
        public void setCountry(String country) {
            this.country = country;
        }
    }
}