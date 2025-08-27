package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 날씨 정보 요약 DTO
 * 단기 예보와 기상 특보를 함께 제공
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherSummary {
    
    private WeatherInfo info;           // 단기 예보 정보
    private List<WeatherAlert> alerts; // 기상 특보 목록
}
