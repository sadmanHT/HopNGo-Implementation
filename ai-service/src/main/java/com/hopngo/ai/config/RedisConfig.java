package com.hopngo.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${ai.cache.image-search-ttl:3600}")
    private long imageSearchTtl;

    @Value("${ai.cache.chatbot-ttl:1800}")
    private long chatbotTtl;

    @Value("${ai.cache.faq-ttl:7200}")
    private long faqTtl;
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for both keys and values for quota management
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use JSON serializer for cache objects
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(1800)) // Default 30 minutes
                .disableCachingNullValues();

        // Configure different TTL for different cache types
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Image search cache - longer TTL since images don't change frequently
        cacheConfigurations.put("imageSearch", 
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(imageSearchTtl))
                .disableCachingNullValues());
        
        // Chatbot cache - shorter TTL for more dynamic responses
        cacheConfigurations.put("chatbot", 
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(chatbotTtl))
                .disableCachingNullValues());
        
        // FAQ cache - longer TTL for static content
        cacheConfigurations.put("faq", 
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(faqTtl))
                .disableCachingNullValues());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}