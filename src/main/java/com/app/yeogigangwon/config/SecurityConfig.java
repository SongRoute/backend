// Spring Security 설정 (CORS는 별도 설정)
package com.app.yeogigangwon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable())          // CORS를 Spring Security에서 비활성화
                .csrf(csrf -> csrf.disable())          // CSRF 보호 비활성화
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()      // 모든 요청 허용
                );
        return http.build();
    }
    
}
