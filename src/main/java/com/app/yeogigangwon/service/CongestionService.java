package com.app.yeogigangwon.service;

import com.app.yeogigangwon.dto.CongestionRequest;
import com.app.yeogigangwon.dto.CongestionResponse;
import com.app.yeogigangwon.entity.Congestion;
import com.app.yeogigangwon.repository.CongestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CongestionService {

    private final CongestionRepository congestionRepository;

    public CongestionResponse analyzeCongestion(CongestionRequest request) {
        String pythonApiUrl = "http://python-api:5000/predict";

        RestTemplate restTemplate = new RestTemplate();

        // Python API에 전달할 body
        Map<String, String> body = Map.of(
                "image_url", request.getImageUrl(),
                "beach_name", request.getBeachName()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(pythonApiUrl, entity, Map.class);

        int personCount = (int) response.getBody().get("person_count");
        String congestionLevel = (String) response.getBody().get("congestion");

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
