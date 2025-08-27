package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 기상청 단기 예보 정보를 담는 DTO
 * 관광지 추천에 필요한 핵심 날씨 정보만 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherInfo {
    
    private int temperature;              // 기온 (°C)
    private int precipitationProbability; // 강수확률 (%)
    private int sky;                     // 하늘 상태 (1:맑음, 3:구름많음, 4:흐림)
    private int windSpeed;               // 풍속 (m/s)
}
