package com.education.amenity.management;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // APIs are stateless
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/students/**").permitAll()  // Public access
                        .anyRequest().denyAll()  // Block everything else (adjust as needed)
                );
        return http.build();
    }
}