package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.TravelTimeInfo;
import com.app.yeogigangwon.dto.KakaoDirectionsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오맵 Directions API 클라이언트
 * 실제 도로 경로 기반 이동 시간 계산
 */
@Slf4j
@Component
public class KakaoMapApiClient {
    
    @Value("${kakao.map.api.key}")
    private String apiKey;
    
    @Autowired
    private RestTemplate restTemplate;
    private static final String KAKAO_DIRECTIONS_URL = "https://dapi.kakao.com/v2/local/search/category.json";
    
    /**
     * 두 지점 간의 실제 도로 경로 기반 이동 시간 정보 조회
     * 
     * @param originLat 출발지 위도
     * @param originLon 출발지 경도
     * @param destLat 도착지 위도
     * @param destLon 도착지 경도
     * @return 이동 시간 정보 (실제 도로 경로 기반)
     */
    public TravelTimeInfo getTravelTime(double originLat, double originLon, 
                                      double destLat, double destLon) {
        log.info("=== KakaoMapApiClient.getTravelTime 시작 ===");
        log.info("이동 시간 조회 - 출발지: ({}, {}), 도착지: ({}, {})", 
                originLat, originLon, destLat, destLon);
        
        // 현재 카카오맵 API 키로는 길찾기 불가능
        // 카카오 모빌리티 API 키가 필요함
        log.warn("카카오맵 API 키로는 길찾기 불가능. 카카오 모빌리티 API 키 필요.");
        log.warn("현재는 개선된 직선 거리 계산을 사용합니다.");
        
        return calculateImprovedStraightLineDistance(originLat, originLon, destLat, destLon);
    }
    
    /**
     * 카카오맵 Directions API 실제 호출
     */
    private KakaoDirectionsResponse callKakaoDirectionsApi(double originLat, double originLon, 
                                                         double destLat, double destLon) {
        try {
            // API URL 구성 (카카오 모빌리티 API 형식)
            String url = String.format("%s?origin=%f,%f&destination=%f,%f&priority=RECOMMEND", 
                    KAKAO_DIRECTIONS_URL, originLon, originLat, destLon, destLat);
            
            log.info("카카오맵 API 호출 URL: {}", url);
            log.info("API 키: {}", apiKey != null ? apiKey.substring(0, 8) + "..." : "null");
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKey);
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // API 호출
            ResponseEntity<KakaoDirectionsResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, KakaoDirectionsResponse.class);
            
            log.info("카카오맵 API 응답 상태: {}", response.getStatusCode());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("카카오맵 API 호출 성공!");
                return response.getBody();
            } else {
                log.warn("카카오맵 API 호출 실패 - 상태코드: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("카카오맵 API 호출 중 예외 발생: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 개선된 직선 거리 계산 (더 정확한 추정)
     */
    private TravelTimeInfo calculateImprovedStraightLineDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            // 직선 거리 계산
            double distance = calculateDistance(lat1, lon1, lat2, lon2);
            
            // 거리별 정확도 개선
            double actualDistance;
            double avgSpeed;
            
            if (distance < 10) {
                // 근거리: 도시 내 이동 (직선거리 × 1.2)
                actualDistance = distance * 1.2;
                avgSpeed = 30.0; // km/h (도시 내 평균)
            } else if (distance < 50) {
                // 중거리: 시내 간 이동 (직선거리 × 1.3)
                actualDistance = distance * 1.3;
                avgSpeed = 50.0; // km/h (국도 평균)
            } else {
                // 장거리: 시도 간 이동 (직선거리 × 1.4)
                actualDistance = distance * 1.4;
                avgSpeed = 60.0; // km/h (고속도로 포함)
            }
            
            // 최소 1분 보장 (매우 짧은 거리도 최소 1분으로 계산)
            int drivingTime = Math.max(1, (int) Math.round(actualDistance / avgSpeed * 60)); // 분 단위
            
            // 도보 시간 (평균 4km/h, 산악지형 고려)
            int walkingTime = Math.max(1, (int) Math.round(actualDistance / 4.0 * 60));  // 분 단위
            
            log.info("개선된 거리 계산 완료 - 직선거리: {}km, 실제거리: {}km, 차량: {}분, 도보: {}분", 
                    distance, actualDistance, drivingTime, walkingTime);
            
            return new TravelTimeInfo(
                "출발지", "도착지",
                drivingTime, walkingTime,
                actualDistance, actualDistance,
                "직선 경로 (개선된 추정)", "직선 경로 (개선된 추정)"
            );
            
        } catch (Exception e) {
            log.error("개선된 거리 계산 실패", e);
            return getDummyTravelTimeInfo();
        }
    }
    
    /**
     * 직선 거리 계산 (API 실패 시 대체 방법)
     */
    private TravelTimeInfo calculateStraightLineDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            // 직선 거리 계산
            double distance = calculateDistance(lat1, lon1, lat2, lon2);
            
            // 추정 시간 계산 (차량: 60km/h, 도보: 5km/h)
            int drivingTime = (int) (distance / 60.0 * 60); // 분 단위
            int walkingTime = (int) (distance / 5.0 * 60);  // 분 단위
            
            log.debug("직선 거리 계산 완료 - 직선거리: {}km, 차량: {}분, 도보: {}분", 
                    distance, drivingTime, walkingTime);
            
            return new TravelTimeInfo(
                "출발지", "도착지",
                drivingTime, walkingTime,
                distance, distance,
                "직선 경로 (추정)", "직선 경로 (추정)"
            );
            
        } catch (Exception e) {
            log.error("직선 거리 계산 실패", e);
            return getDummyTravelTimeInfo();
        }
    }
    
    /**
     * 두 지점 간의 직선 거리 계산 (Haversine 공식)
     * 
     * @param lat1 출발지 위도
     * @param lon1 출발지 경도
     * @param lat2 도착지 위도
     * @param lon2 도착지 경도
     * @return 직선 거리 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 지구 반지름 (km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * API 실패 시 반환할 기본 데이터
     * 
     * @return 기본 이동 시간 정보
     */
    private TravelTimeInfo getDummyTravelTimeInfo() {
        log.debug("API 실패로 인해 기본 데이터 반환");
        return new TravelTimeInfo(
            "출발지", "도착지",
            0, 0,  // API 실패 시 0으로 표시
            0.0, 0.0, // API 실패 시 0으로 표시
            "API 호출 실패", "API 호출 실패"
        );
    }
}

