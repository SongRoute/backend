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
                + "&areaCode=32";

        RestTemplate restTemplate = new RestTemplate();

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            // JSON 응답 구조 따라 꺼내기
            Map<String, Object> responseBody = (Map<String, Object>) ((Map) response.get("response")).get("body");
            Map<String, Object> items = (Map<String, Object>) responseBody.get("items");
            List<Map<String, Object>> itemList = (List<Map<String, Object>>) items.get("item");

            List<TourPlace> tourPlaces = new ArrayList<>();

            for (Map<String, Object> item : itemList) {
                String id = String.valueOf(item.get("contentid"));
                String name = (String) item.get("title");
                String category = String.valueOf(item.get("contenttypeid"));
                String location = (String) item.get("addr1");

                TourPlace place = new TourPlace(id, name, category, location, 0); // 혼잡도는 기본값 0
                tourPlaces.add(place);
            }

            return tourPlaces;

        } catch (Exception e) {
            System.out.println("TourAPI 호출 또는 파싱 중 예외 발생:");
            e.printStackTrace();
        }

        return Collections.emptyList();
    }


}