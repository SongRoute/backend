package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.TourPlace;
import com.app.yeogigangwon.dto.*;
import com.app.yeogigangwon.fetch.KakaoMapApiClient;
import com.app.yeogigangwon.repository.TourPlaceRepository;
import com.app.yeogigangwon.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 관광지 추천 서비스
 * 혼잡도, 거리, 테마, 날씨를 종합적으로 고려한 관광지 추천
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourPlaceRecommendationService {

    private final TourPlaceRepository tourPlaceRepository;
    private final WeatherService weatherService;
    private final KtoService ktoService;
    private final KakaoMapApiClient kakaoMapApiClient;

    // 가중치 설정 (이동 시간 고려 시)
    private static final double DISTANCE_WEIGHT = 0.25;
    private static final double CONGESTION_WEIGHT = 0.2;
    private static final double WEATHER_WEIGHT = 0.2;
    private static final double THEME_WEIGHT = 0.15;
    private static final double TRAVEL_TIME_WEIGHT = 0.2;

    /**
     * 종합적인 관광지 추천
     * 
     * @param request 추천 요청 정보
     * @return 추천된 관광지 목록 (점수순 정렬)
     */
    public List<TourPlaceRecommendation> getRecommendations(RecommendationRequest request) {
        log.info("관광지 추천 시작 - 위치: ({}, {}), 선호테마: {}", 
                request.getLatitude(), request.getLongitude(), request.getPreferredThemes());

        try {
            // 1. 기본 조건으로 관광지 필터링 (테마 매핑 적용)
            List<TourPlace> candidatePlaces = getCandidatePlaces(request);
            
            if (candidatePlaces.isEmpty()) {
                log.warn("추천 가능한 관광지가 없습니다");
                return Collections.emptyList();
            }

            // 2. 날씨 정보 조회 (한 번만 조회)
            final WeatherSummary weatherSummary = request.isConsiderWeather() ? 
                getWeatherSummarySafely(request.getLatitude(), request.getLongitude()) : null;

            // 3. 각 관광지에 대해 점수 계산 (이동 시간 포함)
            List<TourPlaceRecommendation> recommendations = candidatePlaces.stream()
                    .map(place -> calculateRecommendationScoreWithTravelTime(place, request, weatherSummary))
                    .filter(Objects::nonNull)
                    .filter(rec -> {
                        // 이동 시간 필터링
                        if (request.isConsiderTravelTime() && request.getMaxTravelTime() > 0) {
                            return rec.getTravelTimeMinutes() <= request.getMaxTravelTime();
                        }
                        return true;
                    })
                    .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                    .limit(request.getLimit())
                    .collect(Collectors.toList());

            log.info("추천 완료 - {}개의 관광지 추천", recommendations.size());
            return recommendations;

        } catch (Exception e) {
            log.error("관광지 추천 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    /**
     * 🔄 **새로운 테마 매핑 로직 추가**
     * 프론트엔드에서 받은 세부 테마를 실제 DB 카테고리로 매핑
     */
    private List<String> mapThemesToCategories(List<String> themes) {
        if (themes == null || themes.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<String> mappedCategories = new HashSet<>();
        
        for (String theme : themes) {
            String lowerTheme = theme.toLowerCase().trim();
            
            switch (lowerTheme) {
                // 실내 테마들은 모두 "실내" 카테고리로 매핑
                case "박물관":
                case "미술관": 
                case "체험관":
                case "온천":
                case "공연장":
                case "카페":
                case "실내":
                    mappedCategories.add("실내");
                    log.debug("테마 '{}' -> '실내' 카테고리로 매핑", theme);
                    break;
                    
                // 실외 테마들은 모두 "실외" 카테고리로 매핑
                case "산":
                case "해변":
                case "문화재":
                case "공원":
                case "관광명소":
                case "레저":
                case "실외":
                    mappedCategories.add("실외");
                    log.debug("테마 '{}' -> '실외' 카테고리로 매핑", theme);
                    break;
                    
                default:
                    // 알 수 없는 테마는 실외로 기본 처리
                    mappedCategories.add("실외");
                    log.warn("알 수 없는 테마 '{}' -> '실외' 카테고리로 기본 매핑", theme);
                    break;
            }
        }
        
        List<String> result = new ArrayList<>(mappedCategories);
        log.info("테마 매핑 결과: {} -> {}", themes, result);
        return result;
    }

    /**
     * 🔧 **수정된 기본 조건으로 관광지 필터링 (테마 매핑 적용)**
     */
    private List<TourPlace> getCandidatePlaces(RecommendationRequest request) {
        List<TourPlace> allPlaces = tourPlaceRepository.findAll();
        log.info("전체 관광지 수: {}, 요청된 테마: {}", allPlaces.size(), request.getPreferredThemes());
        
        // 🔄 **테마 매핑 적용**
        List<String> mappedCategories = mapThemesToCategories(request.getPreferredThemes());
        
        List<TourPlace> candidatePlaces = allPlaces.stream()
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null)
                .filter(place -> {
                    // 거리 필터링
                    double distance = DistanceCalculator.calculateDistance(
                            request.getLatitude(), request.getLongitude(),
                            place.getLatitude(), place.getLongitude()
                    );
                    return distance <= (request.getMaxDistance() * 1000); // km를 미터로 변환
                })
                .filter(place -> {
                    // 🔧 **개선된 테마 필터링 - 매핑된 카테고리 사용**
                    if (mappedCategories != null && !mappedCategories.isEmpty()) {
                        String placeCategory = place.getCategory();
                        if (placeCategory == null || placeCategory.trim().isEmpty()) {
                            return false; // 카테고리가 없는 관광지는 제외
                        }
                        
                        // 매핑된 카테고리 중 하나라도 일치하면 포함
                        boolean matches = mappedCategories.stream()
                                .anyMatch(category -> category.equals(placeCategory.trim()));
                        
                        if (matches) {
                            log.debug("관광지 '{}' (카테고리: '{}')가 매핑된 카테고리 {}와 일치", 
                                    place.getName(), placeCategory, mappedCategories);
                        }
                        
                        return matches;
                    }
                    return true; // 테마 지정이 없으면 모든 관광지 포함
                })
                .collect(Collectors.toList());
        
        log.info("필터링 후 후보 관광지 수: {} (매핑된 카테고리: {})", candidatePlaces.size(), mappedCategories);
        return candidatePlaces;
    }

    /**
     * 개별 관광지의 추천 점수 계산 (이동 시간 포함)
     */
    private TourPlaceRecommendation calculateRecommendationScoreWithTravelTime(
            TourPlace place, RecommendationRequest request, WeatherSummary weatherSummary) {
        
        try {
            // 기본 거리 및 위치 정보 계산
            double distance = DistanceCalculator.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    place.getLatitude(), place.getLongitude()
            );

            // 이동 시간 및 교통 정보 계산
            TravelTimeInfo travelInfo = calculateTravelTime(place, request);

            // 각 요소별 점수 계산
            double distanceScore = calculateDistanceScore(place, request.getLatitude(), request.getLongitude());
            double congestionScore = calculateCongestionScore(place);
            double weatherScore = calculateWeatherScore(weatherSummary);
            double themeScore = calculateThemeScore(place, request.getPreferredThemes());
            double travelTimeScore = calculateTravelTimeScore(travelInfo.getTravelTimeMinutes(), request.getMaxTravelTime());

            // 총 점수 계산 (가중치 적용)
            double totalScore;
            if (request.isConsiderTravelTime()) {
                totalScore = (distanceScore * DISTANCE_WEIGHT) +
                           (congestionScore * CONGESTION_WEIGHT) +
                           (weatherScore * WEATHER_WEIGHT) +
                           (themeScore * THEME_WEIGHT) +
                           (travelTimeScore * TRAVEL_TIME_WEIGHT);
            } else {
                // 이동 시간을 고려하지 않는 경우, 가중치 재분배
                totalScore = (distanceScore * 0.3) +
                           (congestionScore * 0.25) +
                           (weatherScore * 0.25) +
                           (themeScore * 0.2);
            }

            // 추천 이유 생성
            String reason = generateRecommendationReason(distanceScore, congestionScore, weatherScore, themeScore, travelTimeScore);

            return TourPlaceRecommendation.builder()
                    .place(place)
                    .totalScore(totalScore)
                    .distanceScore(distanceScore)
                    .congestionScore(congestionScore)
                    .weatherScore(weatherScore)
                    .themeScore(themeScore)
                    .travelTimeScore(travelTimeScore)
                    .distance(distance)
                    .travelTimeMinutes(travelInfo.getTravelTimeMinutes())
                    .transportationMode(travelInfo.getTransportationMode())
                    .travelDistance(distance / 1000.0) // km 단위
                    .recommendationReason(reason)
                    .build();

        } catch (Exception e) {
            log.error("관광지 '{}' 점수 계산 중 오류 발생", place.getName(), e);
            return null;
        }
    }

    /**
     * 혼잡도 점수 계산 (0-100점)
     * 혼잡도가 낮을수록 높은 점수
     */
    private double calculateCongestionScore(TourPlace place) {
        try {
            // KTO 혼잡도 데이터 조회 시도
            Integer ktoLevel = ktoService.getCongestionLevel(place);
            if (ktoLevel != null) {
                // KTO 혼잡도 레벨 (1-5) -> 점수 (100-20)
                return Math.max(20, 120 - (ktoLevel * 20));
            }

            // 관광지 자체 혼잡도 정보 사용
            Integer crowdLevel = place.getCrowdLevel();
            if (crowdLevel != null) {
                // 혼잡도 레벨 (1-5) -> 점수 (100-20)
                return Math.max(20, 120 - (crowdLevel * 20));
            }

            // 데이터가 없으면 카테고리별 추정
            return estimateCongestionByCategory(place);

        } catch (Exception e) {
            log.warn("혼잡도 점수 계산 실패: {}", e.getMessage());
            return 70; // 기본값 (중간보다 약간 높게)
        }
    }

    /**
     * 관광지 카테고리 기반 혼잡도 추정
     * KTO 데이터가 없는 관광지에 대한 대안적 점수 계산
     */
    private double estimateCongestionByCategory(TourPlace place) {
        String category = place.getCategory();
        if (category == null) {
            return 70; // 카테고리가 없으면 중간 점수
        }

        String categoryLower = category.toLowerCase();
        
        // 카테고리별 일반적인 혼잡도 패턴 적용
        if (categoryLower.contains("해변") || categoryLower.contains("해수욕장")) {
            // 해변은 성수기(7-8월)에 매우 혼잡하지만, 현재는 중간 정도로 추정
            return 75;
        } else if (categoryLower.contains("산") || categoryLower.contains("등산")) {
            // 산은 상대적으로 여유로움
            return 85;
        } else if (categoryLower.contains("박물관") || categoryLower.contains("미술관")) {
            // 문화시설은 보통 정도
            return 80;
        } else if (categoryLower.contains("온천") || categoryLower.contains("스파")) {
            // 온천은 주말에 혼잡하지만 평일에는 여유로움
            return 75;
        } else if (categoryLower.contains("카페") || categoryLower.contains("식당")) {
            // 음식점은 시간대에 따라 다름, 중간 정도로 추정
            return 70;
        } else if (categoryLower.contains("실내")) {
            // 실내 관광지는 날씨에 관계없이 일정한 혼잡도
            return 75;
        } else if (categoryLower.contains("실외")) {
            // 실외 관광지는 날씨에 따라 혼잡도가 달라짐
            return 80;
        } else {
            // 기타 관광지는 중간 점수
            return 75;
        }
    }

    /**
     * 날씨 점수 계산 (0-100점)
     */
    private double calculateWeatherScore(WeatherSummary weatherSummary) {
        if (weatherSummary == null) {
            return 80; // 날씨 정보가 없으면 기본 점수
        }

        try {
            // 온도 점수 (15-25도가 최적)
            double tempScore = calculateTemperatureScore(weatherSummary.getTemperature());
            
            // 날씨 상태 점수
            double conditionScore = calculateWeatherConditionScore(weatherSummary.getWeatherDescription());
            
            // 강수확률 점수
            double precipitationScore = calculatePrecipitationScore(weatherSummary.getRainProbability());
            
            // 전체 날씨 점수 (각 요소의 평균)
            double weatherScore = (tempScore + conditionScore + precipitationScore) / 3.0;
            
            log.debug("날씨 점수 계산: 온도={}, 상태={}, 강수={} -> 총점={}", 
                    tempScore, conditionScore, precipitationScore, weatherScore);
            
            return Math.max(0, Math.min(100, weatherScore));
            
        } catch (Exception e) {
            log.error("날씨 점수 계산 중 오류 발생", e);
            return 70; // 오류 시 기본 점수
        }
    }

    private double calculateTemperatureScore(Double temperature) {
        if (temperature == null) return 70;
        
        if (temperature >= 15 && temperature <= 25) {
            return 100; // 최적 온도
        } else if (temperature >= 10 && temperature <= 30) {
            return 80; // 좋은 온도
        } else if (temperature >= 5 && temperature <= 35) {
            return 60; // 보통 온도
        } else {
            return 40; // 극한 온도
        }
    }

    private double calculateWeatherConditionScore(String description) {
        if (description == null) return 70;
        
        String desc = description.toLowerCase();
        if (desc.contains("맑음") || desc.contains("clear")) {
            return 100;
        } else if (desc.contains("구름") || desc.contains("cloud")) {
            return 80;
        } else if (desc.contains("흐림") || desc.contains("overcast")) {
            return 60;
        } else if (desc.contains("비") || desc.contains("rain") || desc.contains("shower")) {
            return 30;
        } else if (desc.contains("눈") || desc.contains("snow")) {
            return 40;
        } else {
            return 70;
        }
    }

    private double calculatePrecipitationScore(Double rainProbability) {
        if (rainProbability == null) return 80;
        
        if (rainProbability <= 20) {
            return 100;
        } else if (rainProbability <= 40) {
            return 80;
        } else if (rainProbability <= 60) {
            return 60;
        } else if (rainProbability <= 80) {
            return 40;
        } else {
            return 20;
        }
    }

    /**
     * 테마 점수 계산 (0-100점)
     * 관광지가 사용자의 선호 테마와 얼마나 일치하는지 평가
     */
    private double calculateThemeScore(TourPlace place, List<String> preferredThemes) {
        if (preferredThemes == null || preferredThemes.isEmpty()) {
            return 50; // 선호 테마가 없으면 중간 점수
        }

        String placeCategory = place.getCategory();
        if (placeCategory == null || placeCategory.trim().isEmpty()) {
            return 30; // 카테고리가 없으면 낮은 점수
        }

        String placeName = place.getName().toLowerCase();
        String placeDescription = place.getDescription() != null ? place.getDescription().toLowerCase() : "";
        
        double maxScore = 0;
        
        for (String theme : preferredThemes) {
            double themeScore = 50; // 기본 점수
            String lowerTheme = theme.toLowerCase().trim();
            
            // 카테고리 직접 매칭
            if (placeCategory.trim().toLowerCase().equals(lowerTheme)) {
                themeScore = 90;
            }
            // 이름이나 설명에서 테마 키워드 매칭
            else if (placeName.contains(lowerTheme) || placeDescription.contains(lowerTheme)) {
                themeScore = 85;
            }
            // 세부 테마별 특별 처리
            else {
                themeScore = calculateDetailedThemeScore(place, theme, placeName, placeDescription);
            }
            
            maxScore = Math.max(maxScore, themeScore);
        }
        
        log.debug("관광지 '{}' 테마 점수: {} (카테고리: {}, 테마: {})", 
                place.getName(), maxScore, placeCategory, preferredThemes);
        
        return maxScore;
    }

    private double calculateDetailedThemeScore(TourPlace place, String theme, String placeName, String placeDescription) {
        switch (theme.toLowerCase().trim()) {
            case "박물관":
                if (placeName.contains("박물관") || placeDescription.contains("박물관")) {
                    return 95;
                } else if (placeName.contains("전시") || placeDescription.contains("전시")) {
                    return 80;
                }
                break;
                
            case "미술관":
                if (placeName.contains("미술관") || placeName.contains("갤러리") || 
                    placeDescription.contains("미술") || placeDescription.contains("갤러리")) {
                    return 95;
                }
                break;
                
            case "체험관":
                if (placeName.contains("체험") || placeName.contains("키즈") || placeName.contains("놀이") ||
                    placeDescription.contains("체험") || placeDescription.contains("놀이")) {
                    return 95;
                }
                break;
                
            case "온천":
                if (placeName.contains("온천") || placeName.contains("스파") || placeName.contains("찜질") ||
                    placeDescription.contains("온천") || placeDescription.contains("스파")) {
                    return 95;
                }
                break;
                
            case "공연장":
                if (placeName.contains("공연") || placeName.contains("극장") || placeName.contains("콘서트") ||
                    placeDescription.contains("공연") || placeDescription.contains("극장")) {
                    return 95;
                }
                break;
                
            case "카페":
                if (placeName.contains("카페") || placeName.contains("커피") || placeName.contains("맛집") ||
                    placeDescription.contains("카페") || placeDescription.contains("음식")) {
                    return 95;
                }
                break;
                
            // 실외 세부 테마들
            case "산":
                if (placeName.contains("산") || placeName.contains("등산") || placeName.contains("봉우리") ||
                    placeDescription.contains("산") || placeDescription.contains("등산")) {
                    return 95;
                }
                break;
                
            case "해변":
                if (placeName.contains("해변") || placeName.contains("바다") || placeName.contains("해수욕") ||
                    placeDescription.contains("해변") || placeDescription.contains("바다")) {
                    return 95;
                }
                break;
                
            case "문화재":
                if (placeName.contains("문화재") || placeName.contains("유적") || placeName.contains("고궁") ||
                    placeDescription.contains("문화재") || placeDescription.contains("유적")) {
                    return 95;
                }
                break;
                
            case "공원":
                if (placeName.contains("공원") || placeName.contains("정원") || placeName.contains("수목원") ||
                    placeDescription.contains("공원") || placeDescription.contains("정원")) {
                    return 95;
                }
                break;
                
            case "관광명소":
                if (placeName.contains("명소") || placeName.contains("관광지") ||
                    placeDescription.contains("명소") || placeDescription.contains("관광")) {
                    return 90;
                }
                break;
                
            case "레저":
                if (placeName.contains("레저") || placeName.contains("놀이공원") || placeName.contains("스키") ||
                    placeDescription.contains("레저") || placeDescription.contains("놀이")) {
                    return 95;
                }
                break;
        }
        
        return 50; // 기본 점수
    }

    /**
     * 이동 시간 점수 계산 (0-100점)
     */
    private double calculateTravelTimeScore(int travelTimeMinutes, int maxTravelTime) {
        if (maxTravelTime <= 0) {
            return 100; // 이동 시간 제한이 없으면 최고 점수
        }

        if (travelTimeMinutes <= maxTravelTime * 0.5) {
            return 100; // 제한 시간의 50% 이하면 최고 점수
        } else if (travelTimeMinutes <= maxTravelTime * 0.7) {
            return 80; // 제한 시간의 70% 이하면 좋은 점수
        } else if (travelTimeMinutes <= maxTravelTime) {
            return 60; // 제한 시간 이내면 보통 점수
        } else {
            return 30; // 제한 시간 초과하면 낮은 점수
        }
    }

    /**
     * 거리 점수 계산 (0-100점)
     * 가까울수록 높은 점수
     */
    private double calculateDistanceScore(TourPlace place, double userLat, double userLon) {
        double distance = DistanceCalculator.calculateDistance(userLat, userLon, place.getLatitude(), place.getLongitude());
        double distanceKm = distance / 1000.0;

        if (distanceKm <= 5) {
            return 100;
        } else if (distanceKm <= 15) {
            return 90;
        } else if (distanceKm <= 30) {
            return 80;
        } else if (distanceKm <= 50) {
            return 70;
        } else {
            return 50;
        }
    }

    /**
     * 이동 시간 및 교통 정보 계산
     */
    private TravelTimeInfo calculateTravelTime(TourPlace place, RecommendationRequest request) {
        try {
            double distance = DistanceCalculator.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    place.getLatitude(), place.getLongitude()
            );

            String transportMode;
            int travelTimeMinutes;

            if (request.getTransportationMode() == RecommendationRequest.TransportationMode.CAR) {
                // 자동차 이동 시간 계산 (Kakao API 사용 시도)
                try {
                    TravelTimeInfo kakaoInfo = kakaoMapApiClient.getTravelTime(
                            request.getLatitude(), request.getLongitude(),
                            place.getLatitude(), place.getLongitude(),
                            "car"
                    );
                    if (kakaoInfo != null && kakaoInfo.getTravelTimeMinutes() > 0) {
                        return kakaoInfo;
                    }
                } catch (Exception e) {
                    log.debug("Kakao API 호출 실패, 추정 시간 사용: {}", e.getMessage());
                }
                
                // API 실패 시 추정 계산
                double avgSpeedKmh = 40; // 평균 속도 40km/h
                travelTimeMinutes = (int) Math.ceil((distance / 1000.0) / avgSpeedKmh * 60);
                transportMode = "차량";
                
            } else {
                // 도보/대중교통 이동 시간 추정
                double avgSpeedKmh = 5; // 평균 속도 5km/h
                travelTimeMinutes = (int) Math.ceil((distance / 1000.0) / avgSpeedKmh * 60);
                transportMode = "도보";
            }

            return TravelTimeInfo.builder()
                    .distance(distance)
                    .travelTimeMinutes(travelTimeMinutes)
                    .transportationMode(transportMode)
                    .build();

        } catch (Exception e) {
            log.error("이동 시간 계산 중 오류 발생", e);
            // 기본값 반환
            double distance = DistanceCalculator.calculateDistance(
                    request.getLatitude(), request.getLongitude(),
                    place.getLatitude(), place.getLongitude()
            );
            return TravelTimeInfo.builder()
                    .distance(distance)
                    .travelTimeMinutes(30) // 기본 30분
                    .transportationMode("추정")
                    .build();
        }
    }

    /**
     * 추천 이유 생성
     */
    private String generateRecommendationReason(double distanceScore, double congestionScore, 
                                               double weatherScore, double themeScore, double travelTimeScore) {
        List<String> reasons = new ArrayList<>();

        if (distanceScore >= 80) reasons.add("가까운 거리");
        if (congestionScore >= 80) reasons.add("여유로운 분위기");
        if (weatherScore >= 80) reasons.add("좋은 날씨");
        if (themeScore >= 80) reasons.add("선호 테마 일치");
        if (travelTimeScore >= 80) reasons.add("빠른 이동");

        if (reasons.isEmpty()) {
            reasons.add("균형잡힌 선택");
        }

        return String.join(", ", reasons) + "로 추천";
    }

    /**
     * 안전한 날씨 정보 조회
     */
    private WeatherSummary getWeatherSummarySafely(double latitude, double longitude) {
        try {
            return weatherService.getWeatherSummary(latitude, longitude);
        } catch (Exception e) {
            log.warn("날씨 정보 조회 실패, 기본값 사용: {}", e.getMessage());
            return null;
        }
    }
}