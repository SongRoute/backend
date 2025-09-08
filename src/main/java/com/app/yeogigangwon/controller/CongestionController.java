package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.dto.CongestionDto;
import com.app.yeogigangwon.service.CongestionService;

import com.app.yeogigangwon.service.RealTimeCongestionService;
import com.app.yeogigangwon.service.AreaMapService;
import com.app.yeogigangwon.service.KtoService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

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

    private final CongestionService congestionService; // 종합 혼잡도
    private final RealTimeCongestionService realTimeCongestionService; // 실시간 혼잡도(yolo)
    private final AreaMapService areaMapService; // 해수욕장 면적
    private final KtoService ktoService; // 한국관광공사(KTO) 데이터 서비스

    // cctv 제공 해수욕장 현재 혼잡도 상태 조회하는 API
    @GetMapping("/status")
    public ResponseEntity<List<CongestionDto.CrowdStatus>> getCrowdStatus() {
        List<CongestionDto.CrowdStatus> statuses = congestionService.getCrowdStatus();
        return ResponseEntity.ok(statuses);
    }

    // yolo로부터 받은 실시간 인원수로 특정 해변의 혼잡도를 갱신하고 조회
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
                "beach_id", beachId, // 해변 ID
                "level", st.level, // 혼잡도 단계 (여유, 보통, 혼잡)
                "persons", req.getPersons(),
                "density_per_m2", round(density, 4),
                "density_per_100m2", round(density * 100, 2),
                "ema_density_per_m2", round(st.emaDensity, 4), // 전체 면적
                "method", "yolo_area" // 사용된 분석 방법 -> 없어도 될거같음
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

    // 한국관광공사(KTO) 기반 특정 관광지의 예측 혼잡도 조회하는 API
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
