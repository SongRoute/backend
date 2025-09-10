package com.app.yeogigangwon.dto;

import com.app.yeogigangwon.domain.TourPlace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관광지 추천 결과를 담는 DTO
 * 관광지 정보와 각종 점수 정보를 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPlaceRecommendation {
    
    private TourPlace place;           // 관광지 정보
    private double distance;           // 거리 (미터)
    private double totalScore;         // 총점 (0-100)
    private double distanceScore;      // 거리 점수 (0-100)
    private double congestionScore;   // 혼잡도 점수 (0-100)
    private double weatherScore;       // 날씨 점수 (0-100)
    private double themeScore;         // 테마 점수 (0-100)
    private double travelTimeScore;    // 이동 시간 점수 (0-100)
    private String recommendationReason; // 추천 이유
    
    // 이동 시간 관련 정보
    private int travelTimeMinutes;     // 이동 시간 (분)
    private String transportationMode; // 이동 수단 (차량/도보)
    private double travelDistance;     // 이동 거리 (km)
}
