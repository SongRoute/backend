package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.WeatherAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 기상청 기상 특보 API 호출 클래스
 * 기상청 공공데이터 포털의 기상특보 API를 호출하여 특보 정보를 가져옴
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertFetcher {

    // 기상청 공공데이터 포털 API 키
    @Value("${weather.api.key}")
    private String apiKey;
    
    // 기상청 기상특보 API 기본 URL
    private static final String BASE_URL = "https://apis.data.go.kr/1360000/WthrWrnInfoService/getWthrWrnList";

    /**
     * 기상 특보 정보 조회
     * 
     * @param regionName 지역명 (예: "강원도")
     * @return 기상 특보 목록 (API 실패 시 빈 목록 반환)
     */
    public List<WeatherAlert> fetchWeatherAlert(String regionName) {
        log.info("기상 특보 조회 시작 - 지역: {}", regionName);
        
        // API URL 구성
        String url = buildApiUrl(regionName);
        log.debug("기상청 특보 API URL: {}", url);

        RestTemplate restTemplate = new RestTemplate();
        List<WeatherAlert> result = new ArrayList<>();

        try {
            // API 호출 및 응답 파싱
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                log.warn("API 응답이 null입니다");
                return getDummyAlerts();
            }

            // 응답 구조 파싱
            Map<String, Object> body = (Map<String, Object>) response.get("response");
            if (body == null) {
                log.warn("response.body가 없습니다");
                return getDummyAlerts();
            }

            Map<String, Object> items = (Map<String, Object>) body.get("items");
            if (items == null) {
                log.warn("response.body.items가 없습니다");
                return getDummyAlerts();
            }

            // item 데이터 추출
            Object rawItems = items.get("item");
            List<Map<String, Object>> itemList = parseItems(rawItems);

            // WeatherAlert 객체로 변환
            for (Map<String, Object> item : itemList) {
                String title = (String) item.get("title");
                String message = (String) item.get("msg");
                String time = (String) item.get("tm");

                WeatherAlert alert = new WeatherAlert(title, message, time);
                result.add(alert);
            }

            log.info("기상 특보 {}건 조회 완료", result.size());

        } catch (Exception e) {
            log.error("기상 특보 API 호출 실패", e);
            
            // API 실패 시 더미 데이터 반환 (테스트용)
            log.warn("API 호출 실패로 인해 더미 데이터를 반환합니다");
            return getDummyAlerts();
        }
        
        return result;
    }
    
    /**
     * API URL 구성
     */
    private String buildApiUrl(String regionName) {
        return BASE_URL
                + "?serviceKey=" + apiKey
                + "&pageNo=1"
                + "&numOfRows=10"           // 최대 10건 조회
                + "&dataType=JSON"
                + "&stnId=" + getRegionCode(regionName);
    }
    
    /**
     * API 응답의 item 데이터를 리스트로 파싱
     */
    private List<Map<String, Object>> parseItems(Object rawItems) {
        List<Map<String, Object>> itemList = new ArrayList<>();
        
        if (rawItems instanceof List) {
            itemList = (List<Map<String, Object>>) rawItems;
        } else if (rawItems instanceof Map) {
            itemList.add((Map<String, Object>) rawItems);
        }
        
        return itemList;
    }
    
    /**
     * 테스트용 더미 기상특보 데이터
     * API 호출 실패 시 반환되는 기본 데이터
     */
    private List<WeatherAlert> getDummyAlerts() {
        log.debug("더미 기상특보 데이터 반환");
        // 현재 기상특보 없음으로 설정 (빈 목록)
        return new ArrayList<>();
    }

    /**
     * 지역명에 해당하는 기상청 관측소 코드 반환
     * 
     * @param regionName 지역명
     * @return 관측소 코드
     */
    private String getRegionCode(String regionName) {
        // 강원도청 코드 (105)
        return "105";
    }
}
