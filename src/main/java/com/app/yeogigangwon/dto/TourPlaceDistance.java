package com.app.yeogigangwon.dto;

import com.app.yeogigangwon.domain.TourPlace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 관광지와 거리, 이동시간 정보를 함께 담는 DTO
 * 사용자 위치에서 관광지까지의 거리 및 이동시간 정보 제공
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPlaceDistance {
    
    private TourPlace place;           // 관광지 정보
    private double distance;           // 거리 (km)
    private int drivingTime;           // 차량 이동시간 (분)
    private int walkingTime;           // 도보 이동시간 (분)
    private String transportationMode; // 교통수단 ("car" 또는 "walk")
    
    // 기존 생성자 호환성을 위한 생성자
    public TourPlaceDistance(TourPlace place, double distance) {
        this.place = place;
        this.distance = distance;
        this.drivingTime = 0;
        this.walkingTime = 0;
        this.transportationMode = "car";
    }
}