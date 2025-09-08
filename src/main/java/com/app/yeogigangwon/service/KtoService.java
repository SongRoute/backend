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
    private final Map<String, Map<LocalDate, Double>> byPlace = new ConcurrentHashMap<>();

    @PostConstruct
    public void load() throws Exception {
        // ClassPathResource를 사용하여 리소스 폴더의 파일을 안전하게 읽어옵니다.
        ClassPathResource resource = new ClassPathResource("data/kto_latest.csv");
        if (!resource.exists()) return;

        // InputStreamReader를 통해 UTF-8 인코딩을 명시적으로 지정합니다.
        try (Reader r = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(r)) {

            for (CSVRecord rec : parser) {
                try { // 레코드 하나가 잘못되어도 전체가 멈추지 않도록 try-catch로 감쌉니다.
                    String place = safe(rec, "place_name");
                    // ✅ "beach_id"를 "place_id"로 변경하고 변수명도 수정
                    String placeId = safe(rec, "place_id");
                    // ✅ 새로운 변수명을 사용하여 key를 결정
                    String key = (placeId != null && !placeId.isBlank()) ? placeId : place;
                    if (key == null || key.isBlank()) continue;

                    LocalDate date = LocalDate.parse(rec.get("date"));
                    Double rate = parseD(safe(rec, "visitor_rate_pred"));
                    if (rate == null) continue;

                    byPlace.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(date, rate);
                } catch (Exception e) {
                    // 특정 행 파싱에 실패하면 로그를 남기고 계속 진행합니다.
                    System.err.println("Failed to parse CSV record: " + rec.toString() + " | Error: " + e.getMessage());
                }
            }
        }
    }

    public Optional<Double> getRate(String key, LocalDate date) {
        Map<LocalDate, Double> m = byPlace.get(key);
        return (m == null) ? Optional.empty() : Optional.ofNullable(m.get(date));
    }

    private static String safe(CSVRecord r, String col) { try { return r.get(col); } catch (Exception e) { return null; } }
    private static Double parseD(String s) { try { return (s == null) ? null : Double.parseDouble(s); } catch (Exception e) { return null; } }
}
