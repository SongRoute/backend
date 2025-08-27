package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.WeatherInfo;
import com.app.yeogigangwon.util.GridConverter;
import com.app.yeogigangwon.util.GridConverter.GridCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 기상청 단기 예보 API 호출 클래스
 * 기상청 공공데이터 포털의 단기예보 API를 호출하여 날씨 정보를 가져옴
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForecastFetcher {

    // 기상청 공공데이터 포털 API 키
    @Value("${weather.api.key}")
    private String apiKey;
    
    // 기상청 단기예보 API 기본 URL
    private static final String BASE_URL = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst";

    /**
     * 단기 예보 정보 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @return 단기 예보 정보 (API 실패 시 더미 데이터 반환)
     */
    public WeatherInfo fetchShortTermForecast(double lat, double lon) {
        log.info("단기 예보 조회 시작 - 위도: {}, 경도: {}", lat, lon);
        
        // 위도/경도를 격자 좌표로 변환
        GridCoordinate grid = GridConverter.convertToGrid(lat, lon);
        
        // 기준 시각 계산 (1시간 전 기준)
        LocalDateTime now = LocalDateTime.now().minusHours(1);
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = getNearestBaseTime(now.getHour());

        // API URL 구성
        String url = buildApiUrl(grid.nx, grid.ny, baseDate, baseTime);
        log.debug("기상청 API URL: {}", url);

        RestTemplate restTemplate = new RestTemplate();

        try {
            // API 호출
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            
            log.debug("API 응답 상태: {}, Content-Type: {}", 
                    response.getStatusCode(), 
                    response.getHeaders().getContentType());

            // JSON 응답 검증
            if (responseBody == null || !responseBody.trim().startsWith("{")) {
                log.error("기상청 응답이 JSON이 아닙니다: {}", responseBody);
                throw new IllegalStateException("기상청이 JSON이 아닌 오류 응답을 반환했습니다.");
            }

            // JSON 파싱 및 데이터 추출
            return parseWeatherResponse(responseBody);

        } catch (Exception e) {
            log.error("단기 예보 API 호출 실패", e);
            
            // API 실패 시 더미 데이터 반환 (테스트용)
            log.warn("API 호출 실패로 인해 더미 데이터를 반환합니다");
            return getDummyWeatherInfo();
        }
    }
    
    /**
     * 기상청 API URL 구성
     */
    private String buildApiUrl(int nx, int ny, String baseDate, String baseTime) {
        return BASE_URL
                + "?serviceKey=" + apiKey
                + "&pageNo=1"
                + "&numOfRows=200"          // 충분한 데이터 요청
                + "&dataType=JSON"
                + "&base_date=" + baseDate
                + "&base_time=" + baseTime
                + "&nx=" + nx
                + "&ny=" + ny;
    }
    
    /**
     * 기상청 API 응답을 파싱하여 WeatherInfo로 변환
     */
    private WeatherInfo parseWeatherResponse(String responseBody) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> root = objectMapper.readValue(responseBody, Map.class);

        // 응답 구조 파싱
        Map<String, Object> body = (Map<String, Object>) ((Map<String, Object>) root.get("response")).get("body");
        if (body == null) {
            throw new IllegalStateException("response.body가 없습니다");
        }

        Map<String, Object> items = (Map<String, Object>) body.get("items");
        if (items == null) {
            throw new IllegalStateException("response.body.items가 없습니다");
        }

        Object itemNode = items.get("item");
        if (itemNode == null) {
            throw new IllegalStateException("response.body.items.item이 없습니다");
        }

        // item이 단일 객체인지 리스트인지 확인
        List<Map<String, Object>> itemList;
        if (itemNode instanceof List) {
            itemList = (List<Map<String, Object>>) itemNode;
        } else {
            itemList = new ArrayList<>();
            itemList.add((Map<String, Object>) itemNode);
        }

        // 필요한 날씨 데이터 추출
        Integer tmp = null, pop = null, sky = null, wsd = null;

        for (Map<String, Object> item : itemList) {
            String category = String.valueOf(item.get("category"));
            String value = String.valueOf(item.get("fcstValue"));

            switch (category) {
                case "TMP" -> tmp = Integer.parseInt(value);           // 기온
                case "POP" -> pop = Integer.parseInt(value);           // 강수확률
                case "SKY" -> sky = Integer.parseInt(value);           // 하늘상태
                case "WSD" -> wsd = (int) Float.parseFloat(value);    // 풍속 (소수점 처리)
            }
        }

        // 필수 데이터 검증
        if (tmp == null || pop == null || sky == null || wsd == null) {
            throw new IllegalStateException("필수 날씨 데이터(TMP/POP/SKY/WSD)가 누락되었습니다");
        }

        return new WeatherInfo(tmp, pop, sky, wsd);
    }
    
    /**
     * 테스트용 더미 날씨 데이터
     * API 호출 실패 시 반환되는 기본 데이터
     */
    private WeatherInfo getDummyWeatherInfo() {
        // 강릉 지역 기준으로 적절한 날씨 데이터
        return new WeatherInfo(25, 20, 1, 3); // 기온 25°C, 강수확률 20%, 맑음, 풍속 3m/s
    }

    /**
     * 현재 시간에 가장 가까운 기상청 예보 기준 시각 반환
     * 기상청은 3시간마다 예보를 발표 (02, 05, 08, 11, 14, 17, 20, 23시)
     * 
     * @param hour 현재 시간 (0-23)
     * @return 예보 기준 시각 (HHMM 형식)
     */
    private String getNearestBaseTime(int hour) {
        if (hour < 2) return "2300";
        else if (hour < 5) return "0200";
        else if (hour < 8) return "0500";
        else if (hour < 11) return "0800";
        else if (hour < 14) return "1100";
        else if (hour < 17) return "1400";
        else if (hour < 20) return "1700";
        else if (hour < 23) return "2000";
        else return "2300";
    }
}
