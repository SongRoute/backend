package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카카오맵 API를 통한 이동 시간 정보 DTO
 * 차량과 도보 이동 시간 및 거리 정보 제공
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelTimeInfo {
    
    private String originName;        // 출발지 이름
    private String destinationName;   // 도착지 이름
    private int drivingTime;          // 차량 이동 시간 (분)
    private int walkingTime;          // 도보 이동 시간 (분)
    private double drivingDistance;   // 차량 이동 거리 (km)
    private double walkingDistance;   // 도보 이동 거리 (km)
    private String drivingRoute;      // 차량 경로 요약
    private String walkingRoute;      // 도보 경로 요약
}