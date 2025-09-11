package com.hopngo.ai.service;

import com.hopngo.ai.dto.MatrixRequest;
import com.hopngo.ai.dto.MatrixResponse;
import com.hopngo.ai.dto.RouteRequest;
import com.hopngo.ai.dto.RouteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoutingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);
    
    private final WebClient webClient;
    private final String osrmBaseUrl;
    private final String mapboxToken;
    private final boolean useMapbox;
    
    public RoutingService(
            WebClient.Builder webClientBuilder,
            @Value("${routing.osrm.base-url:http://localhost:5000}") String osrmBaseUrl,
            @Value("${routing.mapbox.token:}") String mapboxToken,
            @Value("${routing.use-mapbox:false}") boolean useMapbox) {
        
        this.osrmBaseUrl = osrmBaseUrl;
        this.mapboxToken = mapboxToken;
        this.useMapbox = useMapbox && !mapboxToken.isEmpty();
        
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
        
        logger.info("RoutingService initialized with {} backend", 
                   this.useMapbox ? "Mapbox" : "OSRM");
    }
    
    @Cacheable(value = "routes", key = "#request.from + '_' + #request.to + '_' + #request.mode")
    public Mono<RouteResponse> calculateRoute(RouteRequest request) {
        logger.debug("Calculating route from {} to {} using mode {}", 
                    request.getFrom(), request.getTo(), request.getMode());
        
        if (useMapbox) {
            return calculateRouteMapbox(request);
        } else {
            return calculateRouteOSRM(request);
        }
    }
    
    @Cacheable(value = "matrices", key = "#request.coordinates.toString() + '_' + #request.mode")
    public Mono<MatrixResponse> calculateMatrix(MatrixRequest request) {
        logger.debug("Calculating matrix for {} coordinates using mode {}", 
                    request.getCoordinates().size(), request.getMode());
        
        if (useMapbox) {
            return calculateMatrixMapbox(request);
        } else {
            return calculateMatrixOSRM(request);
        }
    }
    
    private Mono<RouteResponse> calculateRouteOSRM(RouteRequest request) {
        // Convert lat,lng to lng,lat for OSRM
        String fromCoords = convertToOSRMFormat(request.getFrom());
        String toCoords = convertToOSRMFormat(request.getTo());
        
        String profile = mapModeToOSRMProfile(request.getMode());
        String url = String.format("%s/route/v1/%s/%s;%s", 
                                  osrmBaseUrl, profile, fromCoords, toCoords);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url.substring(osrmBaseUrl.length()))
                        .queryParam("alternatives", request.isAlternatives())
                        .queryParam("steps", request.isSteps())
                        .queryParam("geometries", request.getGeometries())
                        .queryParam("overview", "full")
                        .build())
                .retrieve()
                .bodyToMono(RouteResponse.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> logger.debug("OSRM route calculation successful"))
                .doOnError(error -> logger.error("OSRM route calculation failed: {}", error.getMessage()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("OSRM API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("Routing service unavailable"));
                });
    }
    
    private Mono<MatrixResponse> calculateMatrixOSRM(MatrixRequest request) {
        // Convert coordinates to OSRM format
        String coordinates = request.getCoordinates().stream()
                .map(this::convertToOSRMFormat)
                .collect(Collectors.joining(";"));
        
        String profile = mapModeToOSRMProfile(request.getMode());
        String url = String.format("%s/table/v1/%s/%s", osrmBaseUrl, profile, coordinates);
        
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(url.substring(osrmBaseUrl.length()));
                    
                    if (request.getSources() != null && !request.getSources().isEmpty()) {
                        String sources = request.getSources().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(";"));
                        builder.queryParam("sources", sources);
                    }
                    
                    if (request.getDestinations() != null && !request.getDestinations().isEmpty()) {
                        String destinations = request.getDestinations().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(";"));
                        builder.queryParam("destinations", destinations);
                    }
                    
                    if (request.getAnnotations() != null && !request.getAnnotations().isEmpty()) {
                        String annotations = String.join(",", request.getAnnotations());
                        builder.queryParam("annotations", annotations);
                    }
                    
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(MatrixResponse.class)
                .timeout(Duration.ofSeconds(15))
                .doOnSuccess(response -> logger.debug("OSRM matrix calculation successful"))
                .doOnError(error -> logger.error("OSRM matrix calculation failed: {}", error.getMessage()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("OSRM API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("Matrix service unavailable"));
                });
    }
    
    private Mono<RouteResponse> calculateRouteMapbox(RouteRequest request) {
        String fromCoords = convertToMapboxFormat(request.getFrom());
        String toCoords = convertToMapboxFormat(request.getTo());
        
        String profile = mapModeToMapboxProfile(request.getMode());
        String url = String.format("https://api.mapbox.com/directions/v5/mapbox/%s/%s;%s", 
                                  profile, fromCoords, toCoords);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url.substring("https://api.mapbox.com".length()))
                        .queryParam("access_token", mapboxToken)
                        .queryParam("alternatives", request.isAlternatives())
                        .queryParam("steps", request.isSteps())
                        .queryParam("geometries", request.getGeometries())
                        .queryParam("overview", "full")
                        .build())
                .retrieve()
                .bodyToMono(RouteResponse.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> logger.debug("Mapbox route calculation successful"))
                .doOnError(error -> logger.error("Mapbox route calculation failed: {}", error.getMessage()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("Mapbox API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("Routing service unavailable"));
                });
    }
    
    private Mono<MatrixResponse> calculateMatrixMapbox(MatrixRequest request) {
        String coordinates = request.getCoordinates().stream()
                .map(this::convertToMapboxFormat)
                .collect(Collectors.joining(";"));
        
        String profile = mapModeToMapboxProfile(request.getMode());
        String url = String.format("https://api.mapbox.com/directions-matrix/v1/mapbox/%s/%s", 
                                  profile, coordinates);
        
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path(url.substring("https://api.mapbox.com".length()))
                            .queryParam("access_token", mapboxToken);
                    
                    if (request.getSources() != null && !request.getSources().isEmpty()) {
                        String sources = request.getSources().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(";"));
                        builder.queryParam("sources", sources);
                    }
                    
                    if (request.getDestinations() != null && !request.getDestinations().isEmpty()) {
                        String destinations = request.getDestinations().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(";"));
                        builder.queryParam("destinations", destinations);
                    }
                    
                    if (request.getAnnotations() != null && !request.getAnnotations().isEmpty()) {
                        String annotations = String.join(",", request.getAnnotations());
                        builder.queryParam("annotations", annotations);
                    }
                    
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(MatrixResponse.class)
                .timeout(Duration.ofSeconds(15))
                .doOnSuccess(response -> logger.debug("Mapbox matrix calculation successful"))
                .doOnError(error -> logger.error("Mapbox matrix calculation failed: {}", error.getMessage()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("Mapbox API error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.error(new RuntimeException("Matrix service unavailable"));
                });
    }
    
    private String convertToOSRMFormat(String latLng) {
        // Convert "lat,lng" to "lng,lat" for OSRM
        String[] parts = latLng.split(",");
        return parts[1] + "," + parts[0];
    }
    
    private String convertToMapboxFormat(String latLng) {
        // Convert "lat,lng" to "lng,lat" for Mapbox
        String[] parts = latLng.split(",");
        return parts[1] + "," + parts[0];
    }
    
    private String mapModeToOSRMProfile(String mode) {
        return switch (mode.toLowerCase()) {
            case "walking" -> "foot";
            case "cycling" -> "bicycle";
            case "driving" -> "car";
            default -> "car";
        };
    }
    
    private String mapModeToMapboxProfile(String mode) {
        return switch (mode.toLowerCase()) {
            case "walking" -> "walking";
            case "cycling" -> "cycling";
            case "driving" -> "driving";
            default -> "driving";
        };
    }
}