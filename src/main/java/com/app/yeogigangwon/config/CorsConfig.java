package com.app.yeogigangwon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

//@Configuration  // 일시적으로 비활성화
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 모든 오리진 허용 (개발 환경)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)  // credentials false로 변경
                .maxAge(3600);
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(false);  // credentials false로 변경
        config.setAllowedOriginPatterns(Arrays.asList("*"));  // 모든 오리진 허용
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
    
    // 추가 CORS 설정: Spring Boot의 기본 CORS 처리기 비활성화
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        org.springframework.boot.web.servlet.FilterRegistrationBean<CorsFilter> registration = 
            new org.springframework.boot.web.servlet.FilterRegistrationBean<>(corsFilter());
        registration.setOrder(-100); // 가장 높은 우선순위
        return registration;
    }
}
