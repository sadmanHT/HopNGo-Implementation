package com.hopngo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itinerary_days")
public class ItineraryDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "day_number")
    private Integer dayNumber;

    @Column(name = "title")
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "location")
    private String location;

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("startTime ASC")
    private List<ItineraryActivity> activities = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id")
    private Itinerary itinerary;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public ItineraryDay() {}

    public ItineraryDay(Integer dayNumber, String title, Itinerary itinerary) {
        this.dayNumber = dayNumber;
        this.title = title;
        this.itinerary = itinerary;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<ItineraryActivity> getActivities() {
        return activities;
    }

    public void setActivities(List<ItineraryActivity> activities) {
        this.activities = activities;
    }

    public Itinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(Itinerary itinerary) {
        this.itinerary = itinerary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ItineraryDay{" +
                "id=" + id +
                ", dayNumber=" + dayNumber +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}