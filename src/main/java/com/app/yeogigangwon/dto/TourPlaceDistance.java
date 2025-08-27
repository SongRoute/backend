package com.app.yeogigangwon.dto;

import com.app.yeogigangwon.domain.TourPlace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관광지와 거리 정보를 함께 담는 DTO
 * 사용자 위치에서 관광지까지의 거리 정보 제공
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPlaceDistance {
    
    private TourPlace place;   // 관광지 정보
    private double distance;    // 거리 (미터)
}