package com.hopngo.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "social-service", url = "${services.social.url}")
public interface SocialServiceClient {

    @GetMapping("/social/search")
    Map<String, Object> searchPosts(
            @RequestParam("q") String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "tags", required = false) String tags
    );

    @GetMapping("/social/search/suggestions")
    Map<String, Object> getPostSuggestions(@RequestParam("q") String query);
}