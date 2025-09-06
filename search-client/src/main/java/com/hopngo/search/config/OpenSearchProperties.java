package com.hopngo.search.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Configuration properties for OpenSearch connection
 */
@Data
@Validated
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchProperties {

    /**
     * OpenSearch host
     */
    @NotBlank
    private String host = "localhost";

    /**
     * OpenSearch port
     */
    @NotNull
    @Positive
    private Integer port = 9200;

    /**
     * Connection scheme (http or https)
     */
    @NotBlank
    private String scheme = "http";

    /**
     * Connection timeout in milliseconds
     */
    @Positive
    private int connectionTimeout = 5000;

    /**
     * Socket timeout in milliseconds
     */
    @Positive
    private int socketTimeout = 60000;

    /**
     * Maximum number of retry attempts
     */
    @Positive
    private int maxRetryTimeout = 30000;

    /**
     * Username for authentication (optional)
     */
    private String username;

    /**
     * Password for authentication (optional)
     */
    private String password;
}