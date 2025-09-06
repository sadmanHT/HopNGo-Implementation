package com.hopngo.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "booking-service", url = "${services.booking.url}")
public interface BookingServiceClient {

    @GetMapping("/bookings/search")
    Map<String, Object> searchListings(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "checkIn", required = false) String checkIn,
            @RequestParam(value = "checkOut", required = false) String checkOut,
            @RequestParam(value = "guests", required = false) Integer guests,
            @RequestParam(value = "amenities", required = false) String amenities
    );

    @GetMapping("/bookings/search/suggestions")
    Map<String, Object> getListingSuggestions(@RequestParam("q") String query);
}