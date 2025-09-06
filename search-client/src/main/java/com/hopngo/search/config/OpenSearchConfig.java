package com.hopngo.search.config;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSearch configuration for creating REST clients
 */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchConfig {

    private final OpenSearchProperties properties;

    public OpenSearchConfig(OpenSearchProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder(
                new HttpHost(properties.getHost(), properties.getPort(), properties.getScheme())
        ).build();
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(RestClient restClient) {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(properties.getHost(), properties.getPort(), properties.getScheme())
                )
        );
    }

    @Bean
    public OpenSearchClient openSearchClient(RestClient restClient) {
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper()
        );
        return new OpenSearchClient(transport);
    }
}