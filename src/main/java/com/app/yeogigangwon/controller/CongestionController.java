/**
 * 혼잡도 API 엔드포인트를 제공하는 REST 컨트롤러임. 서비스 계층을 호출하여 조회/갱신/상태 확인 등을 처리함.
 * 코드 로직은 변경하지 않고, 이해를 돕기 위한 간략 주석만 추가했음.
 * 서비스 흐름: Controller -> Service -> Repository/외부 API -> Domain/DTO 순으로 이해하면 편함.
 */
package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.dto.CongestionDto;
import com.app.yeogigangwon.service.CongestionService;
import com.app.yeogigangwon.service.RealTimeCongestionService;
import com.app.yeogigangwon.service.AreaMapService;
import com.app.yeogigangwon.service.KtoService;
import com.app.yeogigangwon.service.KtoDataUpdateService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// 혼잡도 관련 API 요청을 처리하는 컨트롤러
@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;                 // 종합 혼잡도
    private final RealTimeCongestionService realTimeCongestionService; // 실시간 혼잡도(yolo)
    private final AreaMapService areaMapService;                       // 해수욕장 면적
    private final KtoService ktoService;                               // 한국관광공사(KTO) 데이터 서비스
    private final KtoDataUpdateService ktoDataUpdateService;           // KTO 데이터 수동/디버그

    // cctv 제공 해수욕장 현재 혼잡도 상태 조회
    @GetMapping("/status")
    public ResponseEntity<List<CongestionDto.CrowdStatus>> getCrowdStatus() {
        List<CongestionDto.CrowdStatus> statuses = congestionService.getCrowdStatus();
        return ResponseEntity.ok(statuses);
    }

    // yolo 실시간 혼잡도 갱신
    @PostMapping("/beach/{beachId}")
    public ResponseEntity<Map<String, Object>> updateFromYolo(
            @PathVariable String beachId,
            @RequestBody UpdateReq req
    ) {
        RealTimeCongestionService.State st =
                realTimeCongestionService.update(beachId, req.getPersons(), req.getRoiRatio());

        double effArea = Math.max(
                areaMapService.getSandAreaM2(beachId) * (req.getRoiRatio() == null ? 1.0 : req.getRoiRatio()),
                1e-6
        );
        double density = req.getPersons() / effArea;

        return ResponseEntity.ok(Map.of(
                "beach_id", beachId,
                "level", st.level,
                "persons", req.getPersons(),
                "density_per_m2", round(density, 4),
                "density_per_100m2", round(density * 100, 2),
                "ema_density_per_m2", round(st.emaDensity, 4),
                "method", "yolo_area"
        ));
    }

    @GetMapping("/beach/{beachId}")
    public ResponseEntity<Map<String, Object>> getBeach(@PathVariable String beachId) {
        RealTimeCongestionService.State st = realTimeCongestionService.get(beachId);
        double area = areaMapService.getSandAreaM2(beachId);

        return ResponseEntity.ok(Map.of(
                "beach_id", beachId,
                "level", st.level,
                "ema_density_per_m2", round(st.emaDensity, 4),
                "area_m2", area,
                "method", "yolo_area"
        ));
    }

    // KTO 기반 특정 관광지 예측 혼잡도 조회 (기존)
    @GetMapping("/place/{key}")
    public ResponseEntity<Map<String, Object>> getPlaceByKto(
            @PathVariable String key,
            @RequestParam(required = false) String date
    ) {
        LocalDate d = (date == null) ? LocalDate.now() : LocalDate.parse(date);
        double rate = ktoService.getRate(key, d).orElse(Double.NaN);
        String level = ktoToLevel(rate);

        return ResponseEntity.ok(Map.of(
                "place_id", key,
                "date", d.toString(),
                "kto_rate", Double.isNaN(rate) ? null : rate,
                "level", level,
                "method", "kto"
        ));
    }

    // ===== KTO 관련 추가/디버그 엔드포인트 =====

    // 관광지명으로 바로 예측 혼잡도 조회
    @GetMapping("/place-by-name")
    public ResponseEntity<Map<String, Object>> getPlaceRateByName(
            @RequestParam("name") String name,
            @RequestParam(required = false) String date
    ) {
        LocalDate d = (date == null) ? LocalDate.now() : LocalDate.parse(date);
        double rate = ktoService.getRateByPlaceName(name, d).orElse(Double.NaN);
        String level = ktoToLevel(rate);

        return ResponseEntity.ok(Map.of(
                "place_name", name,
                "date", d.toString(),
                "kto_rate", Double.isNaN(rate) ? null : rate,
                "level", level,
                "method", "kto"
        ));
    }

    // 전체 갱신(저장) 수동 트리거
    @GetMapping("/update-kto")
    public ResponseEntity<String> forceUpdateKtoData() {
        ktoDataUpdateService.updateKtoCongestionData();
        return ResponseEntity.ok("KTO 데이터 업데이트를 수동 실행했습니다.");
    }

    // 단일 호출 (JSON 요약)
    @GetMapping("/update-kto/once")
    public ResponseEntity<Map<String, Object>> updateKtoOnce(
            @RequestParam("signgu") String signgu,
            @RequestParam(value = "tAtsNm", required = false) String tAtsNm
    ) {
        Map<String, Object> summary = ktoDataUpdateService.updateOnceForDebug(signgu, tAtsNm);
        return ResponseEntity.ok(summary);
    }

    // 단일 호출 원문
    @GetMapping("/update-kto/once/raw")
    public ResponseEntity<String> updateKtoOnceRaw(
            @RequestParam("signgu") String signgu,
            @RequestParam(value = "tAtsNm", required = false) String tAtsNm
    ) {
        String raw = ktoDataUpdateService.getRawOnce(signgu, tAtsNm);
        return ResponseEntity.ok(raw);
    }

    @Data
    public static class UpdateReq {
        private int persons;
        private Double roiRatio; // null이면 1.0
    }

    private static String ktoToLevel(double rate) {
        if (Double.isNaN(rate)) return "예측없음";
        if (rate < 34) return "여유";
        if (rate < 67) return "보통";
        return "혼잡";
    }

    private static double round(double v, int s) {
        double p = Math.pow(10, s);
        return Math.round(v * p) / p;
    }
}
