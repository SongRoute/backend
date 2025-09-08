package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.TravelTimeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 카카오맵 Local API 클라이언트
 * 두 지점 간의 직선 거리 기반 이동 시간 계산
 */
@Slf4j
@Component
public class KakaoMapApiClient {
    
    @Value("${kakao.map.api.key}")
    private String apiKey;
    
    /**
     * 두 지점 간의 이동 시간 정보 조회 (직선 거리 기반)
     * 
     * @param originLat 출발지 위도
     * @param originLon 출발지 경도
     * @param destLat 도착지 위도
     * @param destLon 도착지 경도
     * @return 이동 시간 정보 (추정값)
     */
    public TravelTimeInfo getTravelTime(double originLat, double originLon, 
                                      double destLat, double destLon) {
        log.info("이동 시간 조회 시작 - 출발지: ({}, {}), 도착지: ({}, {})", 
                originLat, originLon, destLat, destLon);
        
        try {
            // 직선 거리 계산
            double distance = calculateDistance(originLat, originLon, destLat, destLon);
            
            // 추정 시간 계산 (차량: 60km/h, 도보: 5km/h)
            int drivingTime = (int) (distance / 60.0 * 60); // 분 단위
            int walkingTime = (int) (distance / 5.0 * 60);  // 분 단위
            
            log.debug("거리 계산 완료 - 직선거리: {}km, 차량: {}분, 도보: {}분", 
                    distance, drivingTime, walkingTime);
            
            return new TravelTimeInfo(
                "출발지", "도착지",
                drivingTime, walkingTime,
                distance, distance,
                "직선 경로 (추정)", "직선 경로 (추정)"
            );
            
        } catch (Exception e) {
            log.error("거리 계산 실패", e);
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

