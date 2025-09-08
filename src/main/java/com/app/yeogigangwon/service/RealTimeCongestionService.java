package com.app.yeogigangwon.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YOLO 인원수 + 면적 기반 실시간 혼잡도 계산(EMA + 히스테리시스)
 * 기존 DB/로직은 건드리지 않고, 실시간 라인만 별도로 제공
 */
@Service
public class RealTimeCongestionService {
    // 개별 해변의 실시간 상태를 표현하는 내부 클래스
    public static class State {
        public double emaDensity = 0.0; // 혼잡도 지표(명/㎡)의 EMA 값
        public String level = "여유"; // 현재 혼잡도 상태 ("여유", "보통", "혼잡")
        public long lastSwitchMs = 0L; // 마지막 상태 전환 시각(밀리초)
    }

    private final AreaMapService areas; // 해변별 면적 데이터를 제공하는 서비스
    private final Map<String, State> states = new ConcurrentHashMap<>(); // 해변 ID별 실시간 상태 저장소

    // 파라미터(해변별 튜닝 가능)
    private static final double ALPHA = 0.3;
    private static final long MIN_HOLD_MS = 180_000L; // 최소 유지 시간(3분) → 상태 변동 최소화
    private static final double UP_YB = 0.025; // 여유→보통
    private static final double UP_BH = 0.060; // 보통→혼잡
    private static final double DN_BY = 0.020; // 보통→여유
    private static final double DN_HB = 0.050; // 혼잡→보통

    public RealTimeCongestionService(AreaMapService areas) {
        this.areas = areas;
    }

    /**
     * YOLO 분석 결과(persons + roiRatio)를 입력받아 실시간 혼잡 상태를 갱신함
     * - persons: 탐지된 사람 수
     * - roiRatioNullable: ROI(관심영역) 비율 (null 또는 <=0이면 1.0으로 처리)
     * - 혼잡도 계산: (사람 수) / (해변면적 * ROI)
     * - EMA 적용으로 값의 급격한 변동을 완화
     * - 최소 유지 시간(MIN_HOLD_MS) 조건과 히스테리시스 임계치를 적용하여 상태 전환
     */
    public synchronized State update(String beachId, int persons, Double roiRatioNullable) {
        double roi = (roiRatioNullable == null || roiRatioNullable <= 0) ? 1.0 : roiRatioNullable;
        double area = Math.max(areas.getSandAreaM2(beachId) * roi, 1e-6);
        double d = persons / area; // 명/㎡

        long now = System.currentTimeMillis();
        State st = states.computeIfAbsent(beachId, k -> new State());
        // EMA(지수 이동 평균) 업데이트
        st.emaDensity = ALPHA * d + (1 - ALPHA) * st.emaDensity;

        if (now - st.lastSwitchMs >= MIN_HOLD_MS) {
            double x = st.emaDensity;
            switch (st.level) {
                case "여유":
                    if (x >= UP_YB) { st.level = "보통"; st.lastSwitchMs = now; }
                    break;
                case "보통":
                    if (x >= UP_BH) { st.level = "혼잡"; st.lastSwitchMs = now; }
                    else if (x <= DN_BY) { st.level = "여유"; st.lastSwitchMs = now; }
                    break;
                case "혼잡":
                    if (x <= DN_HB) { st.level = "보통"; st.lastSwitchMs = now; }
                    break;
            }
        }
        return st;
    }

    /**
     * 특정 해변(beachId)의 현재 상태(State)를 반환한다.
     * - 해당 해변이 아직 갱신된 적 없으면 새 State 객체를 생성해서 반환
     */
    public State get(String beachId) {
        return states.computeIfAbsent(beachId, k -> new State());
    }
}
