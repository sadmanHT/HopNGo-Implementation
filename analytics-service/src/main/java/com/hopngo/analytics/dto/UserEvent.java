package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event for user-related activities
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEvent extends DomainEvent {
    
    @JsonProperty("action")
    private String action;
    
    @JsonProperty("userType")
    private String userType;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;
    
    @JsonProperty("country")
    private String country;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("registrationSource")
    private String registrationSource;
    
    @JsonProperty("verificationStatus")
    private String verificationStatus;
    
    @JsonProperty("previousValue")
    private String previousValue;
    
    @JsonProperty("newValue")
    private String newValue;
    
    // Default constructor
    public UserEvent() {
        super();
    }
    
    @Override
    public EventRequest toAnalyticsEvent() {
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventId(getEventId());
        eventRequest.setEventType("user_" + determineUserAction());
        eventRequest.setEventCategory("user");
        eventRequest.setUserId(getUserId());
        eventRequest.setSessionId(getSessionId());
        eventRequest.setTimestamp(getTimestamp().toString());
        
        // Build event data
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("action", action);
        eventData.put("userType", userType);
        eventData.put("email", email);
        eventData.put("firstName", firstName);
        eventData.put("lastName", lastName);
        eventData.put("country", country);
        eventData.put("city", city);
        eventData.put("registrationSource", registrationSource);
        eventData.put("verificationStatus", verificationStatus);
        eventData.put("previousValue", previousValue);
        eventData.put("newValue", newValue);
        eventData.put("aggregateId", getAggregateId());
        eventData.put("aggregateType", getAggregateType());
        
        eventRequest.setEventData(eventData);
        eventRequest.setMetadata(getMetadata());
        
        return eventRequest;
    }
    
    private String determineUserAction() {
        if (action == null) {
            return "unknown";
        }
        
        return switch (action.toLowerCase()) {
            case "registered", "created" -> "registered";
            case "login", "logged_in" -> "login";
            case "logout", "logged_out" -> "logout";
            case "profile_updated", "updated" -> "profile_updated";
            case "email_verified" -> "email_verified";
            case "password_changed" -> "password_changed";
            case "account_deleted", "deleted" -> "account_deleted";
            case "account_suspended" -> "account_suspended";
            case "account_reactivated" -> "account_reactivated";
            case "preferences_updated" -> "preferences_updated";
            case "avatar_updated" -> "avatar_updated";
            default -> action.toLowerCase().replace(" ", "_");
        };
    }
    
    // Getters and Setters
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getUserType() {
        return userType;
    }
    
    public void setUserType(String userType) {
        this.userType = userType;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getRegistrationSource() {
        return registrationSource;
    }
    
    public void setRegistrationSource(String registrationSource) {
        this.registrationSource = registrationSource;
    }
    
    public String getVerificationStatus() {
        return verificationStatus;
    }
    
    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
    
    public String getPreviousValue() {
        return previousValue;
    }
    
    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    @Override
    public String toString() {
        return "UserEvent{" +
                "action='" + action + '\'' +
                ", userType='" + userType + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", registrationSource='" + registrationSource + '\'' +
                ", verificationStatus='" + verificationStatus + '\'' +
                ", previousValue='" + previousValue + '\'' +
                ", newValue='" + newValue + '\'' +
                "} " + super.toString();
    }
}