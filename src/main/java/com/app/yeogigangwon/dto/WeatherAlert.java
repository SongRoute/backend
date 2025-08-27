package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 기상 특보 정보를 담는 DTO
 * 태풍, 호우 등 위험한 날씨 상황을 알림
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherAlert {
    
    private String title;   // 특보 제목 (예: "강원도 호우주의보")
    private String message; // 특보 내용
    private String time;    // 발표 시각
}
