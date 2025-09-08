package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.WeatherAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

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
     * 기상 특보 조회
     * 
     * @param regionName 지역명 (예: "강원도")
     * @return 기상 특보 목록 (API 실패 시 예외 발생)
     * @throws RuntimeException API 호출 실패 시
     */
    public List<WeatherAlert> fetchWeatherAlerts(String regionName) {
        log.info("기상 특보 조회 시작 - 지역: {}", regionName);
        
        try {
            // API URL 구성
            String url = buildApiUrl(regionName);
            log.debug("API URL: {}", url.replace(apiKey, "***"));
            
            // API 호출
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            // HTTP 상태 코드 확인
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("기상 특보 API 호출 성공 - 상태코드: {}", response.getStatusCode());
                log.debug("API 응답: {}", response.getBody());
                
                // 응답 파싱
                return parseWeatherAlertsResponse(response.getBody());
            } else {
                log.error("기상 특보 API 호출 실패 - 상태코드: {}, 응답: {}", 
                         response.getStatusCode(), response.getBody());
                throw new RuntimeException("기상 특보 API 호출 실패: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("기상 특보 조회 실패", e);
            throw new RuntimeException("기상 특보 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * API 응답을 파싱하여 기상 특보 목록으로 변환
     * 
     * @param response API 응답
     * @return 기상 특보 목록
     */
    private List<WeatherAlert> parseWeatherAlertsResponse(Map<String, Object> response) {
        List<WeatherAlert> result = new ArrayList<>();

        try {
            if (response == null) {
                log.warn("API 응답이 null입니다");
                return result; // 빈 목록 반환 (특보 없음)
            }

            // 응답 구조 파싱
            Map<String, Object> body = (Map<String, Object>) response.get("response");
            if (body == null) {
                log.warn("response.body가 없습니다");
                return result; // 빈 목록 반환 (특보 없음)
            }

            Map<String, Object> items = (Map<String, Object>) body.get("items");
            if (items == null) {
                log.warn("response.body.items가 없습니다");
                return result; // 빈 목록 반환 (특보 없음)
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
            log.error("기상 특보 응답 파싱 실패", e);
            throw new RuntimeException("기상 특보 응답 파싱 실패: " + e.getMessage(), e);
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

    private List<WeatherAlert> getDummyAlerts() {
        throw new UnsupportedOperationException("더미 데이터는 지원하지 않습니다. 실제 API 응답을 사용하세요.");
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
