package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.WeatherAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AlertFetcher {

    private final String apiKey = "mG3IgIZ7%2B2QU%2FuNxrL0iL84ynFmTCENn0083GB9PQp94xITiBX9Ui9AYHsYXhN3YUcHJpLojDgDfvRInFzNHig%3D%3D";

    public List<WeatherAlert> fetchWeatherAlert(String regionName) {
        String url = "https://apis.data.go.kr/1360000/WthrWrnInfoService/getWthrWrnList"
                + "?serviceKey=" + apiKey
                + "&pageNo=1"
                + "&numOfRows=10"
                + "&dataType=JSON"
                + "&stnId=" + getRegionCode(regionName);

        RestTemplate restTemplate = new RestTemplate();
        List<WeatherAlert> result = new ArrayList<>();

        try {
            Map response = restTemplate.getForObject(url, Map.class);
            Map body = (Map) ((Map) response.get("response")).get("body");
            Map items = (Map) body.get("items");

            Object rawItems = items.get("item");
            List<Map<String, Object>> itemList;

            if (rawItems instanceof List) {
                itemList = (List<Map<String, Object>>) rawItems;
            } else {
                itemList = new ArrayList<>();
                itemList.add((Map<String, Object>) rawItems);
            }

            for (Map<String, Object> item : itemList) {
                String title = (String) item.get("title");
                String msg = (String) item.get("msg");
                String time = (String) item.get("tm");

                WeatherAlert alert = new WeatherAlert(title, msg, time);
                result.add(alert);
            }

        } catch (Exception e) {
            System.out.println("[기상특보 파싱 예외 발생]");
            e.printStackTrace();
            
            // 테스트용 더미 데이터 반환 (API 문제 시)
            System.out.println("[WARNING] 기상특보 API 호출 실패, 테스트용 더미 데이터 반환");
            return getDummyAlerts();
        }
        
        return result;
    }
    
    // 테스트용 더미 기상특보 데이터
    private List<WeatherAlert> getDummyAlerts() {
        List<WeatherAlert> dummyAlerts = new ArrayList<>();
        // 현재 기상특보 없음으로 설정
        return dummyAlerts;
    }

    private String getRegionCode(String regionName) {
        return "105"; // 강원도청 코드
    }
}
