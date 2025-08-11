package com.app.yeogigangwon.util;

public class GridConverter {

    public static class GridCoordinate {
        public final int nx;
        public final int ny;

        public GridCoordinate(int nx, int ny) {
            this.nx = nx;
            this.ny = ny;
        }
    }

    public static GridCoordinate convertToGrid(double lat, double lon) {
        double RE = 6371.00877; // Earth radius (km)
        double GRID = 5.0; // Grid spacing (km)
        double SLAT1 = 30.0; // Projection latitude1 (degrees)
        double SLAT2 = 60.0; // Projection latitude2 (degrees)
        double OLON = 126.0; // Origin longitude
        double OLAT = 38.0; // Origin latitude
        double XO = 43; // Origin X grid coordinate (격자 기준점)
        double YO = 136; // Origin Y grid coordinate (격자 기준점)

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        return new GridCoordinate(nx, ny);
    }
}
