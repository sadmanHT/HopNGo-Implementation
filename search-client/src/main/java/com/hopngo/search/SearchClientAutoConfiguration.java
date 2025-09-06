package com.hopngo.search;

import com.hopngo.search.config.OpenSearchConfig;
import com.hopngo.search.config.OpenSearchProperties;
import com.hopngo.search.helper.ListingsIndexHelper;
import com.hopngo.search.helper.PostsIndexHelper;
import com.hopngo.search.service.SearchClientService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Search Client Library
 */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties.class)
@ComponentScan(basePackages = "com.hopngo.search")
@Import({OpenSearchConfig.class, SearchClientService.class, PostsIndexHelper.class, ListingsIndexHelper.class})
@ConditionalOnProperty(prefix = "opensearch", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SearchClientAutoConfiguration {
    // Auto-configuration class for the search client library
}