package com.app.yeogigangwon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * 카카오 모빌리티 Directions API 응답 DTO
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoDirectionsResponse {
    private List<Route> routes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        private Summary summary;
        private List<Section> sections;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {
        private Origin origin;
        private Destination destination;
        private int distance; // 총 거리 (미터)
        private int duration; // 총 소요 시간 (초)
        private Double fare; // 통행료
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Origin {
        private String name;
        private double x; // 경도
        private double y; // 위도
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Destination {
        private String name;
        private double x; // 경도
        private double y; // 위도
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Section {
        private int distance; // 구간 거리 (미터)
        private int duration; // 구간 소요 시간 (초)
    }
}
