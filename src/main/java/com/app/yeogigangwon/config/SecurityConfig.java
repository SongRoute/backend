// Spring Security 완전 비활성화
package com.app.yeogigangwon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()      // 모든 요청 허용
                )
                .csrf(csrf -> csrf.disable())          // CSRF 완전 비활성화
                .cors(cors -> cors.disable())          // Spring Security CORS 완전 비활성화
                .headers(headers -> headers.disable()) // 모든 보안 헤더 비활성화
                .sessionManagement(session -> session.disable()); // 세션 관리 비활성화
        return http.build();
    }
}
