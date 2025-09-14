package com.hopngo.ai.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final UserIdFilter userIdFilter;

    public SecurityConfig(@Autowired(required = false) UserIdFilter userIdFilter) {
        this.userIdFilter = userIdFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/ai/**").authenticated()
                .anyRequest().permitAll()
            )
            ;
        
        // Only add the filter if it's available (Redis is enabled)
        if (userIdFilter != null) {
            http.addFilterBefore(userIdFilter, UsernamePasswordAuthenticationFilter.class);
        }
        
        return http.build();
    }
}