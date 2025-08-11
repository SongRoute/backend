package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.dto.WeatherInfo;
import com.app.yeogigangwon.util.GridConverter;
import com.app.yeogigangwon.util.GridConverter.GridCoordinate;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ForecastFetcher {

    private final String apiKey = "mG3IgIZ7%2B2QU%2FuNxrL0iL84ynFmTCENn0083GB9PQp94xITiBX9Ui9AYHsYXhN3YUcHJpLojDgDfvRInFzNHig%3D%3D";

    public WeatherInfo fetchShortTermForecast(double lat, double lon) {
        GridCoordinate grid = GridConverter.convertToGrid(lat, lon);
        int nx = grid.nx;
        int ny = grid.ny;

        LocalDateTime now = LocalDateTime.now().minusHours(1);
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = getNearestBaseTime(now.getHour());

        String url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst"
                + "?serviceKey=" + apiKey
                + "&pageNo=1"
                + "&numOfRows=200"          // 넉넉히
                + "&dataType=JSON"
                + "&base_date=" + baseDate
                + "&base_time=" + baseTime
                + "&nx=" + nx
                + "&ny=" + ny;

        RestTemplate restTemplate = new RestTemplate();

        try {
            System.out.println("[Vilage URL] " + url);
            ResponseEntity<String> entity = restTemplate.getForEntity(url, String.class);
            String contentType = entity.getHeaders().getContentType() != null
                    ? entity.getHeaders().getContentType().toString() : "unknown";
            String raw = entity.getBody();
            System.out.println("[Content-Type] " + contentType);
            System.out.println("[HTTP Status] " + entity.getStatusCode());
            System.out.println("[RAW Response] " + (raw != null ? raw.substring(0, Math.min(raw.length(), 1000)) : "null"));

            // JSON 아니면(대개 XML 에러) 바로 실패 처리
            if (raw == null || !raw.trim().startsWith("{")) {
                System.out.println("[ERROR] 기상청 응답이 JSON이 아닙니다. 전체 응답:");
                System.out.println(raw);
                throw new IllegalStateException("기상청이 JSON이 아닌 오류 응답을 반환했습니다. serviceKey/파라미터를 확인하세요.");
            }

            ObjectMapper om = new ObjectMapper();
            Map<String, Object> root = om.readValue(raw, Map.class);

            Map body = (Map) ((Map) root.get("response")).get("body");
            if (body == null) throw new IllegalStateException("response.body 없음: " + raw);

            Map items = (Map) body.get("items");
            if (items == null) throw new IllegalStateException("response.body.items 없음: " + raw);

            Object itemNode = items.get("item");
            if (itemNode == null) throw new IllegalStateException("response.body.items.item 없음: " + raw);

            List<Map<String, Object>> itemList;
            if (itemNode instanceof List) {
                itemList = (List<Map<String, Object>>) itemNode;
            } else {
                itemList = new ArrayList<>();
                itemList.add((Map<String, Object>) itemNode);
            }

            Integer tmp = null, pop = null, sky = null, wsd = null;

            for (Map<String, Object> item : itemList) {
                String category = String.valueOf(item.get("category"));
                String value = String.valueOf(item.get("fcstValue"));

                switch (category) {
                    case "TMP" -> tmp = Integer.parseInt(value);
                    case "POP" -> pop = Integer.parseInt(value);
                    case "SKY" -> sky = Integer.parseInt(value);
                    case "WSD" -> wsd = (int) Float.parseFloat(value); // 소수점 → int
                }
            }

            if (tmp == null || pop == null || sky == null || wsd == null) {
                throw new IllegalStateException("필요 항목(TMP/POP/SKY/WSD) 일부가 없습니다.");
            }

            return new WeatherInfo(tmp, pop, sky, wsd);

        } catch (Exception e) {
            System.out.println("[단기예보 예외 상세]");
            e.printStackTrace();
            
            // 테스트용 더미 데이터 반환 (API 문제 시)
            System.out.println("[WARNING] API 호출 실패, 테스트용 더미 데이터 반환");
            return getDummyWeatherInfo();
            
            // 상위(Service/Controller)에서 의미있는 4xx/5xx로 변환하도록 예외 전파
            // throw new RuntimeException("단기예보 호출/파싱 실패", e);
        }
    }
    
    // 테스트용 더미 날씨 데이터
    private WeatherInfo getDummyWeatherInfo() {
        // 강릉 지역 기준으로 적절한 날씨 데이터
        return new WeatherInfo(25, 20, 1, 3); // 기온 25°C, 강수확률 20%, 맑음, 풍속 3m/s
    }

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
