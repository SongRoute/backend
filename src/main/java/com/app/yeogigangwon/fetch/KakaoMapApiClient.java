package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.TravelTimeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 카카오맵 Directions API 클라이언트
 * 두 지점 간의 차량 및 도보 이동 시간과 거리를 계산
 */
@Slf4j
@Component
public class KakaoMapApiClient {
    
    @Value("${kakao.map.api.key}")
    private String apiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    // 카카오맵 Directions API 기본 URL
    private static final String DIRECTIONS_API_URL = "https://apis-navi.kakao.com/v1/directions";
    
    /**
     * 두 지점 간의 이동 시간 정보 조회
     * 차량과 도보 경로를 모두 조회하여 통합 정보 제공
     * 
     * @param originLat 출발지 위도
     * @param originLon 출발지 경도
     * @param destLat 도착지 위도
     * @param destLon 도착지 경도
     * @return 이동 시간 정보 (API 실패 시 더미 데이터 반환)
     */
    public TravelTimeInfo getTravelTime(double originLat, double originLon, 
                                      double destLat, double destLon) {
        log.info("이동 시간 조회 시작 - 출발지: ({}, {}), 도착지: ({}, {})", 
                originLat, originLon, destLat, destLon);
        
        try {
            // 차량 경로 조회
            Map<String, Object> drivingResult = getDrivingRoute(originLat, originLon, destLat, destLon);
            
            // 도보 경로 조회
            Map<String, Object> walkingResult = getWalkingRoute(originLat, originLon, destLat, destLon);
            
            // 응답 파싱하여 TravelTimeInfo로 변환
            return parseTravelTimeInfo(drivingResult, walkingResult);
            
        } catch (Exception e) {
            log.error("카카오맵 API 호출 실패", e);
            log.warn("API 호출 실패로 인해 더미 데이터를 반환합니다");
            return getDummyTravelTimeInfo(); // API 실패 시 더미 데이터 반환
        }
    }
    
    /**
     * 차량 경로 조회
     * 
     * @param originLat 출발지 위도
     * @param originLon 출발지 경도
     * @param destLat 도착지 위도
     * @param destLon 도착지 경도
     * @return 차량 경로 API 응답
     */
    private Map<String, Object> getDrivingRoute(double originLat, double originLon, 
                                               double destLat, double destLon) {
        log.debug("차량 경로 조회 - 출발지: ({}, {}), 도착지: ({}, {})", 
                originLat, originLon, destLat, destLon);
        
        String url = UriComponentsBuilder
                .fromHttpUrl(DIRECTIONS_API_URL)
                .queryParam("origin", originLon + "," + originLat)  // 카카오맵은 경도,위도 순서
                .queryParam("destination", destLon + "," + destLat)
                .queryParam("waypoints", "")
                .queryParam("priority", "RECOMMEND")      // 추천 경로 우선
                .queryParam("car_fuel", "GASOLINE")      // 휘발유 차량
                .queryParam("car_hipass", "false")       // 하이패스 미사용
                .queryParam("alternatives", "false")     // 대안 경로 없음
                .queryParam("road_details", "false")     // 상세 도로 정보 없음
                .build()
                .toUriString();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + apiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        
        return response.getBody();
    }
    
    /**
     * 도보 경로 조회
     * 
     * @param originLat 출발지 위도
     * @param originLon 출발지 경도
     * @param destLat 도착지 위도
     * @param destLon 도착지 경도
     * @return 도보 경로 API 응답
     */
    private Map<String, Object> getWalkingRoute(double originLat, double originLon, 
                                               double destLat, double destLon) {
        log.debug("도보 경로 조회 - 출발지: ({}, {}), 도착지: ({}, {})", 
                originLat, originLon, destLat, destLon);
        
        String url = UriComponentsBuilder
                .fromHttpUrl(DIRECTIONS_API_URL)
                .queryParam("origin", originLon + "," + originLat)
                .queryParam("destination", destLon + "," + destLat)
                .queryParam("waypoints", "")
                .queryParam("priority", "RECOMMEND")      // 추천 경로 우선
                .queryParam("alternatives", "false")     // 대안 경로 없음
                .queryParam("road_details", "false")     // 상세 도로 정보 없음
                .build()
                .toUriString();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + apiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        
        return response.getBody();
    }
    
    /**
     * API 응답을 파싱하여 TravelTimeInfo로 변환
     * 
     * @param drivingResult 차량 경로 API 응답
     * @param walkingResult 도보 경로 API 응답
     * @return 파싱된 이동 시간 정보
     */
    private TravelTimeInfo parseTravelTimeInfo(Map<String, Object> drivingResult, 
                                             Map<String, Object> walkingResult) {
        log.debug("API 응답 파싱 시작");
        
        // 실제 API 응답 구조에 맞게 파싱 로직 구현
        // 여기서는 간단한 예시로 구현
        
        int drivingTime = extractTime(drivingResult, "driving");
        int walkingTime = extractTime(walkingResult, "walking");
        double drivingDistance = extractDistance(drivingResult, "driving");
        double walkingDistance = extractDistance(walkingResult, "walking");
        
        return new TravelTimeInfo(
            "출발지", "도착지", 
            drivingTime, walkingTime,
            drivingDistance, walkingDistance,
            "차량 경로", "도보 경로"
        );
    }
    
    /**
     * API 응답에서 이동 시간 추출 (분 단위)
     * 
     * @param result API 응답 결과
     * @param type 이동 수단 타입 ("driving" 또는 "walking")
     * @return 이동 시간 (분)
     */
    private int extractTime(Map<String, Object> result, String type) {
        try {
            // TODO: 실제 API 응답 구조에 맞게 구현
            // 현재는 기본값 반환
            log.debug("{} 이동 시간 추출 - 기본값 30분 반환", type);
            return 30; // 기본값
        } catch (Exception e) {
            log.warn("{} 이동 시간 추출 실패, 기본값 반환", type, e);
            return 30;
        }
    }
    
    /**
     * API 응답에서 이동 거리 추출 (km 단위)
     * 
     * @param result API 응답 결과
     * @param type 이동 수단 타입 ("driving" 또는 "walking")
     * @return 이동 거리 (km)
     */
    private double extractDistance(Map<String, Object> result, String type) {
        try {
            // TODO: 실제 API 응답 구조에 맞게 구현
            // 현재는 기본값 반환
            log.debug("{} 이동 거리 추출 - 기본값 5.0km 반환", type);
            return 5.0; // 기본값
        } catch (Exception e) {
            log.warn("{} 이동 거리 추출 실패, 기본값 반환", type, e);
            return 5.0;
        }
    }
    
    /**
     * API 실패 시 반환할 더미 데이터
     * 
     * @return 기본 이동 시간 정보
     */
    private TravelTimeInfo getDummyTravelTimeInfo() {
        log.debug("더미 이동 시간 정보 반환");
        return new TravelTimeInfo(
            "출발지", "도착지",
            30, 15,  // 차량 30분, 도보 15분
            5.0, 1.2, // 차량 5km, 도보 1.2km
            "더미 차량 경로", "더미 도보 경로"
        );
    }
}

