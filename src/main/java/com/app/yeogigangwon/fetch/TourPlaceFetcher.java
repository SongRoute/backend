package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.domain.TourPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
public class TourPlaceFetcher {

    private final String apiKey = "eJ1ed98LDUYCqmitRvtH68fMMuuIRBe923y5bnQWCasOowle6P5E9FgAr3htXM1WRjtqnc36p1hkNp8nD%2BRqWA%3D%3D";

    public List<TourPlace> fetchTourPlacesFromApi() {
        String url = "https://apis.data.go.kr/B551011/KorService1/areaBasedList1?"
                + "serviceKey=" + apiKey
                + "&MobileOS=ETC"
                + "&MobileApp=test"
                + "&_type=json"
                + "&numOfRows=5"
                + "&pageNo=1"
                + "&areaCode=32"; // 강원도

        RestTemplate restTemplate = new RestTemplate();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            Map<String, Object> responseBody = (Map<String, Object>) ((Map<?, ?>) response.get("response")).get("body");
            Map<String, Object> items = (Map<String, Object>) responseBody.get("items");
            List<Map<String, Object>> itemList = (List<Map<String, Object>>) items.get("item");

            List<TourPlace> tourPlaces = new ArrayList<>();

            for (Map<String, Object> item : itemList) {
                String contentId = String.valueOf(item.get("contentid"));
                String name = (String) item.get("title");
                String description = (String) item.getOrDefault("addr1", "주소 없음");

                // 실내/실외 분류
                String placeType = classifyPlaceType(name);

                // TourPlace 객체 생성 (MySQL 버전)
                TourPlace place = new TourPlace();
                place.setName(name);
                place.setDescription(description);
                place.setCategory(placeType);
                place.setCrowdLevel(0); // 기본 혼잡도
                
                // 위도/경도는 API에서 제공하지 않으므로 null로 설정
                // 실제 운영에서는 별도 API로 좌표를 가져와야 함
                place.setLatitude(null);
                place.setLongitude(null);
                
                tourPlaces.add(place);
            }

            return tourPlaces;

        } catch (Exception e) {
            System.out.println("TourAPI 호출 또는 파싱 중 예외 발생:");
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    private String classifyPlaceType(String name) {
        List<String> indoorKeywords = Arrays.asList(
                "박물관", "미술관", "전시", "실내", "온천", "사우나", "찜질방",
                "도서관", "카페", "공연장", "체험관", "아쿠아리움", "갤러리",
                "플라네타리움", "영화관", "실내수영장", "키즈카페", "놀이방"
        );

        String text = name.toLowerCase();

        for (String keyword : indoorKeywords) {
            if (text.contains(keyword.toLowerCase())) {
                return "실내";
            }
        }

        return "실외"; // 키워드 없으면 실외로 분류
    }
}
