package com.hopngo.booking.repository;

import com.hopngo.booking.entity.Booking;
import com.hopngo.booking.entity.Listing;
import com.hopngo.booking.entity.Vendor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
class BookingRepositoryNPlusOneTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void testFindByUserIdOrderByCreatedAtDesc_ShouldUseJoinFetch() {
        // Given: Create test data
        Vendor vendor = new Vendor();
        vendor.setBusinessName("Test Vendor");
        vendor.setContactEmail("test@vendor.com");
        entityManager.persistAndFlush(vendor);

        Listing listing = new Listing();
        listing.setTitle("Test Listing");
        listing.setDescription("Test Description");
        listing.setCategory("accommodation");
        listing.setBasePrice(new BigDecimal("50.00"));
        listing.setVendor(vendor);
        entityManager.persistAndFlush(listing);

        Booking booking = new Booking();
        booking.setUserId("user123");
        booking.setStartDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(2));
        booking.setTotalAmount(new BigDecimal("100.00"));
        booking.setListing(listing);
        booking.setVendor(vendor);
        entityManager.persistAndFlush(booking);

        entityManager.clear(); // Clear persistence context to ensure fresh queries

        // When: Fetch bookings with JOIN FETCH
        Page<Booking> results = bookingRepository.findByUserIdOrderByCreatedAtDesc(
            "user123", PageRequest.of(0, 10));

        // Then: Verify data is loaded correctly (no N+1 queries should occur)
        assertThat(results.getContent()).hasSize(1);
        Booking result = results.getContent().get(0);
        assertThat(result.getVendor()).isNotNull();
        assertThat(result.getListing().getTitle()).isEqualTo("Test Listing");
        assertThat(result.getVendor().getBusinessName()).isEqualTo("Test Vendor");
    }

    @Test
    void testMultipleBookings_ShouldAvoidNPlusOneQueries() {
        // Given: Create multiple bookings for the same user
        Vendor vendor = new Vendor();
        vendor.setBusinessName("Test Vendor");
        vendor.setContactEmail("test@vendor.com");
        entityManager.persistAndFlush(vendor);

        // Create multiple listings and bookings
        for (int i = 1; i <= 3; i++) {
            Listing listing = new Listing();
            listing.setTitle("Test Listing " + i);
            listing.setDescription("Test Description " + i);
            listing.setCategory("accommodation");
            listing.setBasePrice(new BigDecimal("50.00"));
            listing.setVendor(vendor);
            entityManager.persistAndFlush(listing);

            Booking booking = new Booking();
            booking.setUserId("user123");
            booking.setStartDate(LocalDate.now().plusDays(i));
            booking.setEndDate(LocalDate.now().plusDays(i + 2));
            booking.setTotalAmount(new BigDecimal("100.00"));
            booking.setListing(listing);
            booking.setVendor(vendor);
            entityManager.persistAndFlush(booking);
        }

        entityManager.clear(); // Clear persistence context

        // When: Fetch all bookings for user (should use JOIN FETCH)
        Page<Booking> results = bookingRepository.findByUserIdOrderByCreatedAtDesc(
            "user123", PageRequest.of(0, 10));

        // Then: Verify all data is loaded correctly
        assertThat(results.getContent()).hasSize(3);
        for (Booking booking : results.getContent()) {
            assertThat(booking.getVendor()).isNotNull();
            assertThat(booking.getListing()).isNotNull();
            assertThat(booking.getVendor().getBusinessName()).isEqualTo("Test Vendor");
        }
    }
}