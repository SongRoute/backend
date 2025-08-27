package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 위치 정보 요청 DTO
 * 위도와 경도 정보를 받아서 처리
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequest {
    
    private double latitude;   // 위도
    private double longitude;  // 경도
}
