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

    public static class State {
        public double emaDensity = 0.0;
        public String level = "여유";
        public long lastSwitchMs = 0L;
    }

    private final AreaMapService areas;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    // 파라미터(해변별 튜닝 가능)
    private static final double ALPHA = 0.3;
    private static final long MIN_HOLD_MS = 180_000L;
    private static final double UP_YB = 0.025; // 여유→보통
    private static final double UP_BH = 0.060; // 보통→혼잡
    private static final double DN_BY = 0.020; // 보통→여유
    private static final double DN_HB = 0.050; // 혼잡→보통

    public RealTimeCongestionService(AreaMapService areas) {
        this.areas = areas;
    }

    /** YOLO가 보낸 persons/roiRatio 로 상태 갱신 */
    public synchronized State update(String beachId, int persons, Double roiRatioNullable) {
        double roi = (roiRatioNullable == null || roiRatioNullable <= 0) ? 1.0 : roiRatioNullable;
        double area = Math.max(areas.getSandAreaM2(beachId) * roi, 1e-6);
        double d = persons / area; // 명/㎡

        long now = System.currentTimeMillis();
        State st = states.computeIfAbsent(beachId, k -> new State());
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

    /** 현재 상태 조회 */
    public State get(String beachId) {
        return states.computeIfAbsent(beachId, k -> new State());
    }
}
