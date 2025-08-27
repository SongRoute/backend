package com.app.yeogigangwon.util;

/**
 * 두 지점 간의 거리를 계산하는 유틸리티 클래스
 * Haversine 공식을 사용하여 지구 표면의 곡률을 고려한 거리 계산
 */
public class DistanceCalculator {
    
    private static final int EARTH_RADIUS_KM = 6371;  // 지구 반지름 (km)
    
    /**
     * 두 지점 간의 거리를 계산 (Haversine 공식)
     * 
     * @param lat1 첫 번째 지점의 위도 (도)
     * @param lon1 첫 번째 지점의 경도 (도)
     * @param lat2 두 번째 지점의 위도 (도)
     * @param lon2 두 번째 지점의 경도 (도)
     * @return 두 지점 간의 거리 (미터)
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 위도와 경도 차이를 라디안으로 변환
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        // Haversine 공식 계산
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // 거리를 미터 단위로 반환
        return EARTH_RADIUS_KM * c * 1000;
    }
}
