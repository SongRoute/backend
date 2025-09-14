package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.CongestionDomain;
import com.app.yeogigangwon.dto.CongestionDto;
import com.app.yeogigangwon.repository.CongestionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CongestionService {

    // 서비스 동작 로그 출력용
    private static final Logger log = LoggerFactory.getLogger(CongestionService.class);

    // DB 접근을 위한 JPA 레포지토리
    private final CongestionRepository congestionRepository;
    // Python API 호출 및 이미지 수집을 위한 HTTP 클라이언트
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.api.url}")
    private String pythonApiUrl;

    // 분석 대상 CCTV 메타데이터 목록
    private List<CongestionDto.CctvInfo> cctvList;

    private static final int FALLBACK_DEN = 20;
    private static final int MIN_SAMPLES   = 12;

    // 대한민국 표준시 타임존 상수
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @PostConstruct
    public void init() {
        cctvList = Arrays.asList(
                new CongestionDto.CctvInfo("cctv001", "남항진", "강릉시", "http://220.95.232.18/camera/4_0.jpg"),
                new CongestionDto.CctvInfo("cctv002", "강문",   "강릉시", "http://220.95.232.18/camera/51_0.jpg"),
                new CongestionDto.CctvInfo("cctv003", "경포",   "강릉시", "http://220.95.232.18/camera/52_0.jpg"),
                new CongestionDto.CctvInfo("cctv004", "소돌",   "강릉시", "http://220.95.232.18/camera/53_0.jpg"),
                new CongestionDto.CctvInfo("cctv005", "염전",   "강릉시", "http://220.95.232.18/camera/54_0.jpg"),
                new CongestionDto.CctvInfo("cctv006", "영진",   "강릉시", "http://220.95.232.18/camera/55_0.jpg"),
                new CongestionDto.CctvInfo("cctv007", "정동진", "강릉시", "http://220.95.232.18/camera/56_0.jpg"),
                new CongestionDto.CctvInfo("cctv008", "공현진", "고성군", "http://220.95.232.18/camera/57_0.jpg"),
                new CongestionDto.CctvInfo("cctv009", "교암",   "고성군", "http://220.95.232.18/camera/58_0.jpg"),
                new CongestionDto.CctvInfo("cctv010", "봉포",   "속초시", "http://220.95.232.18/camera/59_0.jpg"),
                new CongestionDto.CctvInfo("cctv011", "초도",   "고성군", "http://220.95.232.18/camera/60_0.jpg"),
                new CongestionDto.CctvInfo("cctv012", "영랑",   "속초시", "http://220.95.232.18/camera/69_0.jpg"),
                new CongestionDto.CctvInfo("cctv013", "하맹방", "삼척시", "http://220.95.232.18/camera/62_0.jpg"),
                new CongestionDto.CctvInfo("cctv014", "원평",   "삼척시", "http://220.95.232.18/camera/65_0.jpg"),
                new CongestionDto.CctvInfo("cctv015", "문암·초곡", "삼척시", "http://220.95.232.18/camera/88_0.jpg")
        );
    }

    @Scheduled(cron = "0 */15 * * * *", zone = "Asia/Seoul")
    public void analyzeAndSaveCongestionData() {
        // 15분마다 모든 CCTV에 대해 혼잡도 분석을 실행
        LocalDateTime now = LocalDateTime.now(KST);
        int currentHour = now.getHour();

        // 22:00 ~ 06:00 (심야 시간) 사이에는 분석하지 않음
        if (currentHour >= 22 || currentHour < 6) {
            log.info("Crowd analysis skipped during night hours.");
            return;
        }

        log.info("Starting crowd analysis for all CCTV feeds...");
        for (CongestionDto.CctvInfo cctv : cctvList) {
            try {
                // 1) CCTV 이미지 수집
                byte[] imageBytes = restTemplate.getForObject(cctv.getCctvUrl(), byte[].class);
                if (imageBytes == null || imageBytes.length == 0) {
                    log.warn("Empty image for CCTV {}", cctv.getId());
                    continue;
                }
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // 2) Python API 호출 (이미지 Base64 전송 → 사람 수 응답 수신)
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("image", base64Image);

                ResponseEntity<CongestionDto.CrowdAnalysisResult> response =
                        restTemplate.postForEntity(
                                pythonApiUrl + "/analyze_crowd",
                                requestBody,
                                CongestionDto.CrowdAnalysisResult.class
                        );

                // 3) 응답 검증 및 DB 저장
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    int personCount = response.getBody().getPersonCount();
                    log.info("CCTV ID: {}, Detected Persons: {}", cctv.getId(), personCount);

                    // MySQL 저장
                    CongestionDomain record = new CongestionDomain();
                    record.setBeachId(cctv.getId());
                    record.setBeachName(cctv.getBeachName());
                    record.setPersonCount(personCount);
                    record.setTimestamp(now);
                    congestionRepository.save(record);
                } else {
                    log.error("Python API failure for CCTV {}", cctv.getId());
                }
            } catch (Exception e) {
                // 한 CCTV 실패가 전체 스케줄을 중단하지 않도록 예외를 캡처
                log.error("Failed to analyze CCTV {}: {}", cctv.getId(), e.getMessage());
            }
        }
    }

    public List<CongestionDto.CrowdStatus> getCrowdStatus() {
        // 최근 24시간 데이터를 기준으로 각 해변의 상태를 계산하여 응답
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        List<CongestionDto.CrowdStatus> statuses = new ArrayList<>();

        for (CongestionDto.CctvInfo cctv : cctvList) {
            // 최신순 정렬로 최근 기록을 가져옴
            List<CongestionDomain> recentRecords =
                    congestionRepository.findByBeachIdAndTimestampBetweenOrderByTimestampDesc(
                            cctv.getId(), twentyFourHoursAgo, now
                    );

            // 데이터 끊김 체크
            if (recentRecords.isEmpty()
                    || recentRecords.get(0).getTimestamp().isBefore(now.minusMinutes(30))) {
                statuses.add(new CongestionDto.CrowdStatus(
                        cctv.getId(), cctv.getBeachName(), "정보없음", 0, null
                ));
                continue; // 등급/스코어 계산 건너뜀
            }

            int latestCrowd = recentRecords.isEmpty() ? 0 : recentRecords.get(0).getPersonCount();
            List<Integer> recentCounts = new ArrayList<>();
            for (CongestionDomain r : recentRecords) recentCounts.add(r.getPersonCount());

            // 1) 스코어(0~1): 데이터 충분하면 latest / p90, 부족하면 latest / FALLBACK_DEN
            double baseScore;
            if (recentCounts.size() >= MIN_SAMPLES) {
                int p90 = percentileInt(recentCounts, 90);
                baseScore = safeRatio(latestCrowd, p90);
            } else {
                baseScore = safeRatio(latestCrowd, FALLBACK_DEN);
            }

            // 2) 가중치 적용(성수기/주말/피크타임)은 점수에만 ±20% 이내로 반영
            double referenceCrowdLevel = applyWeights(baseScore, now); // 0~1로 클리핑됨
            referenceCrowdLevel = BigDecimal
                    .valueOf(referenceCrowdLevel)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            // 3) 등급 산정: 적응형 임계치(데이터 충분) or 정적 기본 임계치(부족)
            String status = determineStatus(latestCrowd, recentCounts);

            // 최종 상태를 응답 리스트에 추가
            statuses.add(new CongestionDto.CrowdStatus(
                    cctv.getId(),
                    cctv.getBeachName(),
                    status,
                    latestCrowd,
                    referenceCrowdLevel
            ));
        }

        return statuses;
    }

    // NaN/Infinity 방지 + 0~1 클리핑, 분모가 0/음수면 FALLBACK_DEN 사용
    private double safeRatio(double num, double den) {
        double d = den > 0 ? den : FALLBACK_DEN; // d는 항상 > 0
        double v = num / d;
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }

    // 정수 리스트의 p-퍼센타일 값을 반환
    private int percentileInt(List<Integer> xs, double p) {
        if (xs == null || xs.isEmpty()) return 0;
        List<Integer> sorted = new ArrayList<>(xs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

    // 등급 임계치 컨테이너(record
    private record Thresholds(int tRelax, int tBusy) {}

    /**
     * 최근 데이터 기반 적응형 임계치 계산
     * - 데이터 충분: tRelax = p50(중위수) 최소 6, tBusy = p90 최소 16
     * - 데이터 부족: 정적 기본치 tRelax=6, tBusy=16
     */
    private Thresholds deriveThresholds(List<Integer> recentCounts) {
        final int DEFAULT_RELAX = 6;   // 0~5 → 여유
        final int DEFAULT_BUSY  = 16;  // 16+  → 혼잡

        if (recentCounts == null || recentCounts.size() < MIN_SAMPLES) {
            return new Thresholds(DEFAULT_RELAX, DEFAULT_BUSY);
        }
        int p50 = percentileInt(recentCounts, 50);
        int p90 = percentileInt(recentCounts, 90);

        int tRelax = Math.max(DEFAULT_RELAX, p50);
        int tBusy  = Math.max(DEFAULT_BUSY,  p90);
        if (tBusy < tRelax + 4) tBusy = tRelax + 4; // 경계 간 최소 간격 확보
        return new Thresholds(tRelax, tBusy);
    }

    /**
     * 성수기/주말/피크타임 가중치를 점수에만 반영한다(±20% 범위).
     * 등급 결정에는 영향을 주지 않는다.
     */
    private double applyWeights(double score, LocalDateTime now) {
        double w = 1.0;

        // 성수기(7~8월) +10%
        int month = now.getMonthValue();
        if (month == 7 || month == 8) w *= 1.10;

        // 주말 +10%
        DayOfWeek dow = now.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) w *= 1.10;

        // 피크타임(13~17시) +10%
        int hour = now.getHour();
        if (hour >= 13 && hour <= 17) w *= 1.10;

        // 0.8~1.2 범위로 제한하고, 최종 0~1 클리핑
        double adj = Math.min(1.2, Math.max(0.8, w));
        double v = score * adj;
        return Math.max(0.0, Math.min(1.0, v));
    }

    /**
     * 등급은 직관적인 '사람 수' 기준으로 분류한다.
     * - 데이터 충분: 적응형 임계치 적용
     * - 데이터 부족: 정적 임계치 적용
     */
    private String determineStatus(int latestCount, List<Integer> recentCounts) {
        Thresholds th = deriveThresholds(recentCounts);

        // 실제 임계치 값을 사용하여 등급을 판정
        if (latestCount >= th.tBusy()) {
            return "혼잡";
        } else if (latestCount >= th.tRelax()) {
            return "보통";
        } else {
            return "여유";
        }
    }
}
