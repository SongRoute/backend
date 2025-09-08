package com.app.yeogigangwon.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;


import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.PostConstruct;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * backend/data/kto_latest.csv 를 읽어
 * place_name(or beach_id) -> date -> visitor_rate_pred(0~100) 를 제공
 */
@Service
public class KtoService {
    // key: place_name 또는 beach_id
    // value: (key별) 날짜(LocalDate) -> 방문객 예측치(0~100) 맵
    private final Map<String, Map<LocalDate, Double>> byPlace = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() throws Exception {
        // ClassPathResource를 사용하여 리소스 폴더의 파일을 안전하게 읽어옴
        ClassPathResource resource = new ClassPathResource("data/kto_latest.csv");
        if (!resource.exists()) return;

        // UTF-8 인코딩으로 CSV 파일을 파싱
        try (Reader r = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(r)) {

            for (CSVRecord rec : parser) {
                try {
                    // 1) 장소 정보 (beach_id가 있으면 우선 사용, 없으면 place_name 사용)
                    String place = safe(rec, "place_name");
                    String beachId = safe(rec, "beach_id");
                    String key = (beachId != null && !beachId.isBlank()) ? beachId : place;
                    if (key == null || key.isBlank()) continue;

                    // 2) 날짜 (yyyy-MM-dd 형식)
                    LocalDate date = LocalDate.parse(rec.get("date"));
                    // 3) 방문객 예측 지표 (0~100)
                    Double rate = parseD(safe(rec, "visitor_rate_pred"));
                    if (rate == null) continue;

                    // 4) byPlace 맵에 누적 저장
                    byPlace.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(date, rate);
                } catch (Exception e) {
                    // 특정 행 파싱 실패 시 로그 출력 후 계속 진행
                    System.err.println("Failed to parse CSV record: " + rec.toString() + " | Error: " + e.getMessage());
                }
            }
        }
    }

    // 특정 장소(key: beachId 또는 place_name)와 날짜(LocalDate)에 해당하는 방문객 예측치를 반환
    public Optional<Double> getRate(String key, LocalDate date) {
        Map<LocalDate, Double> m = byPlace.get(key);
        return (m == null) ? Optional.empty() : Optional.ofNullable(m.get(date));
    }

    private static String safe(CSVRecord r, String col) { try { return r.get(col); } catch (Exception e) { return null; } }
    private static Double parseD(String s) { try { return (s == null) ? null : Double.parseDouble(s); } catch (Exception e) { return null; } }
}
