package com.hopngo.booking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopngo.booking.dto.*;
import com.hopngo.booking.entity.*;
import com.hopngo.booking.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class BookingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Vendor testVendor;
    private Listing testListing;
    private final String testUserId = TEST_USER_ID;
    private final String testVendorUserId = TEST_VENDOR_USER_ID;

    @BeforeEach
    void setUp() {
        // Clean up
        bookingRepository.deleteAll();
        inventoryRepository.deleteAll();
        listingRepository.deleteAll();
        vendorRepository.deleteAll();

        // Create test vendor
        testVendor = new Vendor();
        testVendor.setId(UUID.randomUUID());
        testVendor.setUserId(testVendorUserId);
        testVendor.setBusinessName("Test Vendor");
        testVendor.setContactEmail("vendor@test.com");
        testVendor.setContactPhone("+1234567890");
        testVendor.setDescription("Test vendor description");
        testVendor.setStatus(Vendor.VendorStatus.ACTIVE);
        testVendor = vendorRepository.save(testVendor);

        // Create test listing
        testListing = new Listing();
        testListing.setId(UUID.randomUUID());
        testListing.setVendor(testVendor);
        testListing.setTitle("Test Accommodation");
        testListing.setDescription("A beautiful test place");
        testListing.setCategory("ACCOMMODATION");
        testListing.setBasePrice(new BigDecimal("100.00"));
        testListing.setCurrency("USD");
        testListing.setMaxGuests(4);
        testListing.setAddress("123 Test Street");
        testListing.setLatitude(new BigDecimal("40.7128"));
        testListing.setLongitude(new BigDecimal("-74.0060"));
        testListing.setAmenities(new String[]{"WiFi", "Kitchen"});
        testListing.setImages(new String[]{"image1.jpg", "image2.jpg"});
        testListing.setStatus(Listing.ListingStatus.ACTIVE);
        testListing = listingRepository.save(testListing);

        // Create inventory for the listing
        Inventory inventory = new Inventory();
        inventory.setListing(testListing);
        inventory.setDate(LocalDate.now().plusDays(1));
        inventory.setAvailableQuantity(2);
        inventory.setPriceOverride(null);
        inventoryRepository.save(inventory);

        inventory = new Inventory();
        inventory.setListing(testListing);
        inventory.setDate(LocalDate.now().plusDays(2));
        inventory.setAvailableQuantity(2);
        inventory.setPriceOverride(null);
        inventoryRepository.save(inventory);

        inventory = new Inventory();
        inventory.setListing(testListing);
        inventory.setDate(LocalDate.now().plusDays(3));
        inventory.setAvailableQuantity(2);
        inventory.setPriceOverride(null);
        inventoryRepository.save(inventory);

        inventory = new Inventory();
        inventory.setListing(testListing);
        inventory.setDate(LocalDate.now().plusDays(4));
        inventory.setAvailableQuantity(2);
        inventory.setPriceOverride(null);
        inventoryRepository.save(inventory);
    }

    @Test
    void testSuccessfulBookingFlow() throws Exception {
        // Create booking request
        BookingCreateRequest request = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            2,
            "Special request: late check-in"
        );

        // Create booking
        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", CUSTOMER_ROLE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(testListing.getId().toString()))
                .andExpect(jsonPath("$.userId").value(testUserId))
                .andExpect(jsonPath("$.numberOfGuests").value(2))
                .andExpect(jsonPath("$.totalPrice").value(100.00))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.specialRequests").value("Special request: late check-in"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void testDoubleBookingPrevention() throws Exception {
        // Create first booking
        BookingCreateRequest request1 = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            2,
            "First booking"
        );

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", CUSTOMER_ROLE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to create overlapping booking (should fail)
        BookingCreateRequest request2 = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            2,
            "Second booking - should fail"
        );

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", "another-user-789")
                .header("X-User-Role", CUSTOMER_ROLE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testBookingCancellation() throws Exception {
        // Create booking
        BookingCreateRequest request = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            2,
            "Test booking for cancellation"
        );

        String response = mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        BookingResponse bookingResponse = objectMapper.readValue(response, BookingResponse.class);
        UUID bookingId = bookingResponse.id();

        // Cancel booking
        BookingUpdateRequest updateRequest = new BookingUpdateRequest("CANCELLED");

        mockMvc.perform(patch("/api/v1/bookings/{bookingId}", bookingId)
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void testVendorBookingConfirmation() throws Exception {
        // Create booking
        BookingCreateRequest request = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            2,
            "Test booking for confirmation"
        );

        String response = mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        BookingResponse bookingResponse = objectMapper.readValue(response, BookingResponse.class);
        UUID bookingId = bookingResponse.id();

        // Vendor confirms booking
        BookingUpdateRequest updateRequest = new BookingUpdateRequest("CONFIRMED");

        mockMvc.perform(patch("/api/v1/bookings/{bookingId}", bookingId)
                .header("X-User-ID", testVendorUserId)
                .header("X-User-Role", "PROVIDER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void testGetUserBookings() throws Exception {
        // Create multiple bookings
        BookingCreateRequest request1 = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            2,
            "First booking"
        );

        BookingCreateRequest request2 = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(4),
            1,
            "Second booking"
        );

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Get user bookings
        mockMvc.perform(get("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].userId").value(testUserId))
                .andExpect(jsonPath("$[1].userId").value(testUserId));
    }

    @Test
    void testGetVendorBookings() throws Exception {
        // Create booking
        BookingCreateRequest request = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            2,
            "Vendor booking test"
        );

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get vendor bookings
        mockMvc.perform(get("/api/v1/bookings/vendor")
                .header("X-User-ID", testVendorUserId)
                .header("X-User-Role", "PROVIDER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].listingId").value(testListing.getId().toString()));
    }

    @Test
    void testInvalidBookingDates() throws Exception {
        // Test past check-in date
        BookingCreateRequest request = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().minusDays(1), // Past date
            LocalDate.now().plusDays(1),
            2,
            "Invalid past date booking"
        );

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testExceedMaxGuests() throws Exception {
        // Test exceeding max guests
        BookingCreateRequest request = new BookingCreateRequest(
            testListing.getId(),
            LocalDate.now().plusDays(1),
            LocalDate.now().plusDays(2),
            10, // Exceeds max guests (4)
            "Too many guests"
        );

        mockMvc.perform(post("/api/v1/bookings")
                .header("X-User-ID", testUserId)
                .header("X-User-Role", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}