package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 관광지 추천 요청을 담는 DTO
 * 사용자 위치, 선호도, 필터 조건을 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {
    
    private double latitude;           // 사용자 위도
    private double longitude;          // 사용자 경도
    private List<String> preferredThemes; // 선호 테마 (해변, 산, 문화재, 실내, 실외 등)
    private int maxDistance;          // 최대 거리 (km, 기본값: 50)
    private int limit;                // 추천 개수 (기본값: 10)
    private boolean avoidCrowded;     // 혼잡한 곳 피하기 (기본값: true)
    private boolean considerWeather;  // 날씨 고려하기 (기본값: true)
    private TransportationMode transportationMode; // 이동 수단 (기본값: CAR)
    private int maxTravelTime;        // 최대 이동 시간 (분, 기본값: 60)
    private boolean considerTravelTime; // 이동 시간 고려하기 (기본값: true)
    
    /**
     * 이동 수단 열거형
     */
    public enum TransportationMode {
        CAR("차량"),      // 차량
        WALKING("도보");  // 도보
        
        private final String description;
        
        TransportationMode(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
