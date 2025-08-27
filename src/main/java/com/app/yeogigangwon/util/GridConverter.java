package com.app.yeogigangwon.util;

/**
 * 위도/경도를 기상청 격자 좌표로 변환하는 유틸리티 클래스
 * 기상청 API 호출을 위한 격자 좌표 계산
 */
public class GridConverter {

    /**
     * 격자 좌표를 담는 내부 클래스
     */
    public static class GridCoordinate {
        public final int nx;  // 격자 X 좌표
        public final int ny;  // 격자 Y 좌표

        public GridCoordinate(int nx, int ny) {
            this.nx = nx;
            this.ny = ny;
        }
    }

    // 기상청 격자 좌표계 상수
    private static final double EARTH_RADIUS = 6371.00877;  // 지구 반지름 (km)
    private static final double GRID_SPACING = 5.0;         // 격자 간격 (km)
    private static final double PROJECTION_LAT1 = 30.0;     // 투영 위도1 (도)
    private static final double PROJECTION_LAT2 = 60.0;     // 투영 위도2 (도)
    private static final double ORIGIN_LON = 126.0;         // 원점 경도
    private static final double ORIGIN_LAT = 38.0;          // 원점 위도
    private static final double ORIGIN_X = 43;              // 원점 X 격자 좌표
    private static final double ORIGIN_Y = 136;             // 원점 Y 격자 좌표

    /**
     * 위도/경도를 기상청 격자 좌표로 변환
     * 
     * @param lat 위도 (도)
     * @param lon 경도 (도)
     * @return 격자 좌표 (nx, ny)
     */
    public static GridCoordinate convertToGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;

        double re = EARTH_RADIUS / GRID_SPACING;
        double slat1 = PROJECTION_LAT1 * DEGRAD;
        double slat2 = PROJECTION_LAT2 * DEGRAD;
        double olon = ORIGIN_LON * DEGRAD;
        double olat = ORIGIN_LAT * DEGRAD;

        // Lambert Conformal Conic 투영 계산
        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        // 입력 좌표를 격자 좌표로 변환
        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        
        // 경도 범위 조정 (-π ~ π)
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        // 최종 격자 좌표 계산
        int nx = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5);

        return new GridCoordinate(nx, ny);
    }
}
