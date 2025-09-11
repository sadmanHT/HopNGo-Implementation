package com.hopngo.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Domain event for booking-related activities
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingEvent extends DomainEvent {
    
    @JsonProperty("bookingId")
    private String bookingId;
    
    @JsonProperty("hotelId")
    private String hotelId;
    
    @JsonProperty("roomId")
    private String roomId;
    
    @JsonProperty("checkInDate")
    private String checkInDate;
    
    @JsonProperty("checkOutDate")
    private String checkOutDate;
    
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("guestCount")
    private Integer guestCount;
    
    @JsonProperty("bookingSource")
    private String bookingSource;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    // Default constructor
    public BookingEvent() {
        super();
    }
    
    @Override
    public EventRequest toAnalyticsEvent() {
        EventRequest eventRequest = new EventRequest();
        eventRequest.setEventId(getEventId());
        eventRequest.setEventType("booking_" + determineBookingAction());
        eventRequest.setEventCategory("booking");
        eventRequest.setUserId(getUserId());
        eventRequest.setSessionId(getSessionId());
        eventRequest.setTimestamp(getTimestamp().toString());
        
        // Build event data
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("bookingId", bookingId);
        eventData.put("hotelId", hotelId);
        eventData.put("roomId", roomId);
        eventData.put("checkInDate", checkInDate);
        eventData.put("checkOutDate", checkOutDate);
        eventData.put("totalAmount", totalAmount);
        eventData.put("currency", currency);
        eventData.put("status", status);
        eventData.put("guestCount", guestCount);
        eventData.put("bookingSource", bookingSource);
        eventData.put("paymentMethod", paymentMethod);
        eventData.put("aggregateId", getAggregateId());
        eventData.put("aggregateType", getAggregateType());
        
        eventRequest.setEventData(eventData);
        eventRequest.setMetadata(getMetadata());
        
        return eventRequest;
    }
    
    private String determineBookingAction() {
        if (status == null) {
            return "unknown";
        }
        
        return switch (status.toLowerCase()) {
            case "created", "pending" -> "created";
            case "confirmed" -> "confirmed";
            case "cancelled" -> "cancelled";
            case "completed" -> "completed";
            case "modified" -> "modified";
            case "refunded" -> "refunded";
            default -> "status_changed";
        };
    }
    
    // Getters and Setters
    public String getBookingId() {
        return bookingId;
    }
    
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    
    public String getHotelId() {
        return hotelId;
    }
    
    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }
    
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getCheckInDate() {
        return checkInDate;
    }
    
    public void setCheckInDate(String checkInDate) {
        this.checkInDate = checkInDate;
    }
    
    public String getCheckOutDate() {
        return checkOutDate;
    }
    
    public void setCheckOutDate(String checkOutDate) {
        this.checkOutDate = checkOutDate;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getGuestCount() {
        return guestCount;
    }
    
    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }
    
    public String getBookingSource() {
        return bookingSource;
    }
    
    public void setBookingSource(String bookingSource) {
        this.bookingSource = bookingSource;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    @Override
    public String toString() {
        return "BookingEvent{" +
                "bookingId='" + bookingId + '\'' +
                ", hotelId='" + hotelId + '\'' +
                ", roomId='" + roomId + '\'' +
                ", checkInDate='" + checkInDate + '\'' +
                ", checkOutDate='" + checkOutDate + '\'' +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", status='" + status + '\'' +
                ", guestCount=" + guestCount +
                ", bookingSource='" + bookingSource + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                "} " + super.toString();
    }
}