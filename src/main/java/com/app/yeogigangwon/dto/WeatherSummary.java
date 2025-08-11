package com.app.yeogigangwon.dto;

import java.util.List;

public class WeatherSummary {
    private WeatherInfo info;
    private List<WeatherAlert> alerts;

    public WeatherSummary() {}

    public WeatherSummary(WeatherInfo info, List<WeatherAlert> alerts) {
        this.info = info;
        this.alerts = alerts;
    }

    public WeatherInfo getInfo() {
        return info;
    }

    public void setInfo(WeatherInfo info) {
        this.info = info;
    }

    public List<WeatherAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<WeatherAlert> alerts) {
        this.alerts = alerts;
    }

    @Override
    public String toString() {
        return "WeatherSummary{" +
                "info=" + info +
                ", alerts=" + alerts +
                '}';
    }

    // 기상특보를 고려한 종합 날씨 점수 계산
    public int getOverallWeatherScore() {
        if (info == null) return 0;

        int baseScore = info.getWeatherScore();

        // 기상특보가 있으면 점수 차감
        if (alerts != null && !alerts.isEmpty()) {
            int maxPenalty = alerts.stream()
                    .mapToInt(WeatherAlert::getScorePenalty)
                    .max()
                    .orElse(0);

            baseScore -= maxPenalty;
        }

        return Math.max(0, baseScore);
    }

    // 관광지 추천 가능 여부
    public boolean isRecommendable() {
        return getOverallWeatherScore() > 30; // 30점 이상이면 추천 가능
    }

    // 관광지 추천 등급
    public String getRecommendationGrade() {
        int score = getOverallWeatherScore();

        if (score >= 80) return "A급 (매우 추천)";
        else if (score >= 60) return "B급 (추천)";
        else if (score >= 40) return "C급 (보통)";
        else if (score >= 20) return "D급 (비추천)";
        else return "F급 (관광 금지)";
    }
}
