package com.app.yeogigangwon.service;

import com.app.yeogigangwon.dto.CongestionRequest;
import com.app.yeogigangwon.dto.CongestionResponse;
import com.app.yeogigangwon.entity.Congestion;
import com.app.yeogigangwon.repository.CongestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CongestionService {

    private final CongestionRepository congestionRepository;

    public CongestionResponse analyzeCongestion(CongestionRequest request) {
//        String pythonApiUrl = "http://python-api:5000/predict";
        String pythonApiUrl = "http://localhost:5000/predict";

        RestTemplate restTemplate = new RestTemplate();


        // 폼 필드 구성 - FastAPI가 기대하는 이름으로!
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("image_url", request.getImageUrl());
        form.add("beachName", request.getBeachName());
        // 임계치도 함께 넘기면 파이썬에서 혼잡도 계산해줌(선택)
        form.add("t1", "10");
        form.add("t2", "30");
        form.add("t3", "60");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);

        // Python API에 전달할 body
//        Map<String, String> body = Map.of(
//                "image_url", request.getImageUrl(),
//                "beach_name", request.getBeachName()
//        );

//        // Map.of() 대신 HashMap 사용
//        Map<String, String> body = new HashMap<>();
//        if (request.getImageUrl() != null) {
//            body.put("image_url", request.getImageUrl());
//        }
//        if (request.getBeachName() != null) {
//            body.put("beach_name", request.getBeachName());
//        }

//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
//
//        ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);

        Number personCountNumber = (Number) response.getBody().get("personCount");
        String congestionLevel = (String) response.getBody().get("congestion");

        int personCount = 0;
        if (personCountNumber != null) {
            personCount = personCountNumber.intValue();
        }

        // MongoDB 저장
        Congestion congestion = Congestion.builder()
                .beachName(request.getBeachName())
                .personCount(personCount)
                .congestionLevel(congestionLevel)
                .timestamp(LocalDateTime.now())
                .build();

        congestionRepository.save(congestion);

        return CongestionResponse.builder()
                .beachName(request.getBeachName())
                .personCount(personCount)
                .congestionLevel(congestionLevel)
                .build();
    }
}
