// backend/src/main/java/com/app/yeogigangwon/service/CongestionService.java
package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.CongestionDomain;
import com.app.yeogigangwon.dto.CongestionDto;
import com.app.yeogigangwon.repository.CongestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class CongestionService {

    private final CongestionRepository congestionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.api.url}")
    private String pythonApiUrl;

    private List<CongestionDto.CctvInfo> cctvList;

    @PostConstruct
    public void init() {
        cctvList = Arrays.asList(
                new CongestionDto.CctvInfo("cctv001", "남항진", "강릉시", "http://220.95.232.18/camera/4_0.jpg"),
                new CongestionDto.CctvInfo("cctv002", "강문", "강릉시", "http://220.95.232.18/camera/51_0.jpg"),
                new CongestionDto.CctvInfo("cctv003", "경포", "강릉시", "http://220.95.232.18/camera/52_0.jpg"),
                new CongestionDto.CctvInfo("cctv004", "소돌", "강릉시", "http://220.95.232.18/camera/53_0.jpg"),
                new CongestionDto.CctvInfo("cctv005", "염전", "강릉시", "http://220.95.232.18/camera/54_0.jpg"),
                new CongestionDto.CctvInfo("cctv006", "영진", "강릉시", "http://220.95.232.18/camera/55_0.jpg"),
                new CongestionDto.CctvInfo("cctv007", "정동진", "강릉시", "http://220.95.232.18/camera/56_0.jpg"),
                new CongestionDto.CctvInfo("cctv008", "공현진", "고성군", "http://220.95.232.18/camera/57_0.jpg"),
                new CongestionDto.CctvInfo("cctv009", "교암", "고성군", "http://220.95.232.18/camera/58_0.jpg"),
                new CongestionDto.CctvInfo("cctv010", "봉포", "속초시", "http://220.95.232.18/camera/59_0.jpg"),
                new CongestionDto.CctvInfo("cctv011", "초도", "고성군", "http://220.95.232.18/camera/60_0.jpg"),
                new CongestionDto.CctvInfo("cctv012", "영랑", "속초시", "http://220.95.232.18/camera/69_0.jpg"),
                new CongestionDto.CctvInfo("cctv013", "하맹방", "삼척시", "http://220.95.232.18/camera/62_0.jpg"),
                new CongestionDto.CctvInfo("cctv014", "원평", "삼척시", "http://220.95.232.18/camera/65_0.jpg"),
                new CongestionDto.CctvInfo("cctv015", "문암·초곡", "삼척시", "http://220.95.232.18/camera/88_0.jpg")
        );
    }

    // 30분마다 CCTV 이미지 분석 및 DB 저장 (저녁/새벽 시간 제외)
//    @Scheduled(fixedRate = 1800000) // 30분마다 실행
//    @Scheduled(fixedRate = 300000) // 5분마다 실행
    @Scheduled(cron = "0 */5 * * * *") //
    public void analyzeAndSaveCongestionData() {
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();

        // 22:00 ~ 05:00 사이에는 분석하지 않음
        if (currentHour >= 22 || currentHour < 5) {
            System.out.println("Crowd analysis skipped during night hours.");
            return;
        }

        System.out.println("Starting crowd analysis for all CCTV feeds...");
        for (CongestionDto.CctvInfo cctv : cctvList) {
            try {
                // Fetch image from CCTV URL
                byte[] imageBytes = restTemplate.getForObject(cctv.getCctvUrl(), byte[].class);
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // Call Python API
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("image", base64Image);
                ResponseEntity<CongestionDto.CrowdAnalysisResult> response = restTemplate.postForEntity(
                        pythonApiUrl + "/analyze_crowd",
                        requestBody,
                        CongestionDto.CrowdAnalysisResult.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    int personCount = response.getBody().getPersonCount();
                    System.out.println(String.format("CCTV ID: %s, Detected Persons: %d", cctv.getId(), personCount));

                    // Save result to MongoDB
                    CongestionDomain record = new CongestionDomain();
                    record.setBeachId(cctv.getId());
                    record.setBeachName(cctv.getBeachName());
                    record.setPersonCount(personCount);
                    record.setTimestamp(now);
                    congestionRepository.save(record);
                }
            } catch (Exception e) {
                System.err.println(String.format("Failed to analyze CCTV %s: %s", cctv.getId(), e.getMessage()));
            }
        }
    }

    public List<CongestionDto.CrowdStatus> getCrowdStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        List<CongestionDto.CrowdStatus> statuses = new ArrayList<>();

        for (CongestionDto.CctvInfo cctv : cctvList) {
            List<CongestionDomain> recentRecords = congestionRepository.findByBeachIdAndTimestampBetweenOrderByTimestampDesc(cctv.getId(), twentyFourHoursAgo, now);

            int maxCrowd = recentRecords.stream()
                    .mapToInt(CongestionDomain::getPersonCount)
                    .max()
                    .orElse(1);

            int latestCrowd = recentRecords.isEmpty() ? 0 : recentRecords.get(0).getPersonCount();

            double referenceCrowdLevel = (double) latestCrowd / maxCrowd;

            referenceCrowdLevel = applyWeights(referenceCrowdLevel, now);

            String status = determineStatus(referenceCrowdLevel, now);

            statuses.add(new CongestionDto.CrowdStatus(cctv.getId(), cctv.getBeachName(), status, latestCrowd, referenceCrowdLevel));
        }

        return statuses;
    }

    private double applyWeights(double referenceCrowdLevel, LocalDateTime now) {
        int month = now.getMonthValue();
        int hour = now.getHour();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        double adjustedLevel = referenceCrowdLevel;

        // 계절 보정
        if (month >= 7 && month <= 8) { // 성수기
            // 과거 최대 방문객 기준 적용 (현재 로직에서는 maxCrowd를 사용하므로 추가 보정 없음)
        } else if ((month == 6 && now.getDayOfMonth() >= 20) || (month == 8 && now.getDayOfMonth() <= 31)) { // 준성수기
            adjustedLevel *= 0.8;
        } else { // 비성수기
            adjustedLevel *= 0.5;
        }

        // 요일별 보정
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) { // 주말/공휴일
            adjustedLevel *= 1.2;
        } else { // 평일
            adjustedLevel *= 0.8;
        }

        // 시간대별 보정
        if (hour >= 12 && hour < 16) { // 피크타임 (12:00~16:00)
            adjustedLevel *= 1.2;
        } else if ((hour >= 9 && hour < 12) || (hour >= 16 && hour < 18)) { // 일반 시간
            adjustedLevel *= 1.0;
        } else { // 비피크 시간
            adjustedLevel *= 0.8;
        }

        return Math.min(1.0, adjustedLevel); // 1.0 초과 방지
    }

    private String determineStatus(double level, LocalDateTime now) {
        boolean isWeekend = now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY;

        double congestedThreshold = isWeekend ? 0.7 : 0.6;
        double normalThreshold = isWeekend ? 0.4 : 0.3;

        if (level > congestedThreshold) {
            return "혼잡";
        } else if (level > normalThreshold) {
            return "보통";
        } else {
            return "여유";
        }
    }
}