package com.ssafy.layover.weather;

/**
 * 위경도를 기상청 동네예보 격자 좌표(nx, ny)로 변환한다.
 *
 * <p>기상청 단기예보 API는 위경도가 아니라 5km 격자 번호를 받는다.
 * 아래 상수와 수식은 기상청이 배포하는 Lambert Conformal Conic 변환 규격을 따른다.
 */
public final class KmaGridConverter {

    private static final double RE = 6371.00877;   // 지구 반경(km)
    private static final double GRID = 5.0;        // 격자 간격(km)
    private static final double SLAT1 = 30.0;      // 표준 위도 1
    private static final double SLAT2 = 60.0;      // 표준 위도 2
    private static final double OLON = 126.0;      // 기준점 경도
    private static final double OLAT = 38.0;       // 기준점 위도
    private static final double XO = 43;           // 기준점 X 격자
    private static final double YO = 136;          // 기준점 Y 격자

    private static final double DEGRAD = Math.PI / 180.0;

    public record Grid(int nx, int ny) {
    }

    private KmaGridConverter() {
    }

    public static Grid toGrid(double latitude, double longitude) {
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

        double ra = Math.tan(Math.PI * 0.25 + latitude * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = longitude * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        return new Grid(nx, ny);
    }
}
