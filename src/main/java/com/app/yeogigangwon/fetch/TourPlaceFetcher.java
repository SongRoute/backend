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
                + "&_type=json" // xml -> json으로 받기
                + "&numOfRows=5"
                + "&pageNo=1"
                + "&areaCode=32";

        RestTemplate restTemplate = new RestTemplate();

        try {
            Map response = restTemplate.getForObject(url, Map.class);
            System.out.println("전체 응답 구조 확인:");
            System.out.println(response);
        } catch (Exception e) {
            System.out.println("예외 발생:");
            e.printStackTrace();
        }

        return Collections.emptyList(); // 일단 에러 방지용
    }

}
