// backend/src/main/java/com/app/yeogigangwon/service/CongestionService.java
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
import java.util.*;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CongestionService {

    private static final Logger log = LoggerFactory.getLogger(CongestionService.class);

    private final CongestionRepository congestionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${python.api.url}")
    private String pythonApiUrl;

    private List<CongestionDto.CctvInfo> cctvList;

    // ===== 내부 임계/스코어 계산용 상수 =====
    private static final int FALLBACK_DEN = 20;   // 데이터 부족 시 스코어 분모 기본값
    private static final int MIN_SAMPLES   = 12;  // 적응형 임계치 적용에 필요한 최소 표본수

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

    // 5분마다 CCTV 이미지 분석 및 DB 저장 (저녁/새벽 시간 제외)
    @Scheduled(cron = "0 */5 * * * *")
    public void analyzeAndSaveCongestionData() {
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();

        // 22:00 ~ 05:00 사이에는 분석하지 않음
        if (currentHour >= 22 || currentHour < 5) {
            log.info("Crowd analysis skipped during night hours.");
            return;
        }

        log.info("Starting crowd analysis for all CCTV feeds...");
        for (CongestionDto.CctvInfo cctv : cctvList) {
            try {
                // CCTV 이미지 수집
                byte[] imageBytes = restTemplate.getForObject(cctv.getCctvUrl(), byte[].class);
                if (imageBytes == null || imageBytes.length == 0) {
                    log.warn("Empty image for CCTV {}", cctv.getId());
                    continue;
                }
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // Python API 호출
                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("image", base64Image);

                ResponseEntity<CongestionDto.CrowdAnalysisResult> response =
                        restTemplate.postForEntity(
                                pythonApiUrl + "/analyze_crowd",
                                requestBody,
                                CongestionDto.CrowdAnalysisResult.class
                        );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    int personCount = response.getBody().getPersonCount();
                    log.info("CCTV ID: {}, Detected Persons: {}", cctv.getId(), personCount);

                    // MongoDB 저장
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
                log.error("Failed to analyze CCTV {}: {}", cctv.getId(), e.getMessage());
            }
        }
    }

    public List<CongestionDto.CrowdStatus> getCrowdStatus() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        List<CongestionDto.CrowdStatus> statuses = new ArrayList<>();

        for (CongestionDto.CctvInfo cctv : cctvList) {
            List<CongestionDomain> recentRecords =
                    congestionRepository.findByBeachIdAndTimestampBetweenOrderByTimestampDesc(
                            cctv.getId(), twentyFourHoursAgo, now
                    );

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

            // 2) 가중치(±20% 내) 적용은 '스코어'에만! 등급은 '사람 수' 기준으로 분류
            double referenceCrowdLevel = applyWeights(baseScore, now); // 0~1로 클리핑됨
            referenceCrowdLevel = BigDecimal
                    .valueOf(referenceCrowdLevel)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            // 3) 등급 산정: 적응형 임계치(데이터 충분) or 정적 기본 임계치(부족)
            String status = determineStatus(latestCrowd, recentCounts);

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

    // =========================
    //       계산 유틸리티
    // =========================

    // NaN/Infinity 방지 + 0~1 클리핑, 분모가 0/음수면 FALLBACK_DEN 사용
    private double safeRatio(double num, double den) {
        double d = den > 0 ? den : FALLBACK_DEN; // d는 항상 > 0
        double v = num / d;
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        return Math.max(0.0, Math.min(1.0, v));
    }

    private int percentileInt(List<Integer> xs, double p) {
        if (xs == null || xs.isEmpty()) return 0;
        List<Integer> sorted = new ArrayList<>(xs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

    // record로 단순 자료 컨테이너 정의
    private record Thresholds(int tRelax, int tBusy) {}

    /**
     * 최근 데이터 기반 적응형 임계치!
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
     * 가중치(성수기/주말/피크타임)는 점수에만 ±20% 내에서 완만하게 반영한다.
     * 등급에는 적용하지 않는다.
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
     * 등급은 '사람 수' 기준으로 직관적으로 분류한다.
     * - 데이터 충분: 적응형 임계치(해변별 특성 반영)
     * - 데이터 부족: 정적 임계치(tRelax=6, tBusy=16)
     */
    private String determineStatus(int latestCount, List<Integer> recentCounts) {
        Thresholds th = deriveThresholds(recentCounts);
        if (latestCount >= th.tBusy()) return "혼잡";
        if (latestCount >= th.tRelax()) return "보통";
        return "여유";
    }
}
