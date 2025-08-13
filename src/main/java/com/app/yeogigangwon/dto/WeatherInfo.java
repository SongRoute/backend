package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WeatherInfo {
    // 기상청 단기 예보에서 필요한 값만 추려서 담음
    private int temperature; // TMP: 기온(°C)
    private int precipitationProbability; // POP: 강수확률(%)
    private int sky; // SKY: 하늘 상태(1: 맑음, 3: 구름 많음, 4: 흐림)
    private int windSpeed; // WSD: 풍속(m/s)

    // 관광지 추천용 날씨 점수 계산 (100점 만점)
    public int getWeatherScore() {
        int score = 100;

        // 강수확률이 높을수록 점수 감소
        if (precipitationProbability > 80) score -= 50;
        else if (precipitationProbability > 60) score -= 35;
        else if (precipitationProbability > 40) score -= 20;
        else if (precipitationProbability > 20) score -= 10;

        // 하늘상태에 따른 점수 조정
        if (sky == 1) score += 15;      // 맑음
        else if (sky == 3) score += 5;  // 구름 많음
        else if (sky == 4) score -= 20; // 흐림

        // 풍속이 너무 강하면 점수 감소
        if (windSpeed > 15) score -= 30;
        else if (windSpeed > 10) score -= 20;
        else if (windSpeed > 7) score -= 10;

        // 기온이 너무 극단적이면 점수 감소
        if (temperature < -10 || temperature > 35) score -= 25;
        else if (temperature < 0 || temperature > 30) score -= 15;

        return Math.max(0, score);
    }
}
