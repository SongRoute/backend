package com.app.yeogigangwon.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * backend/data/beach_area_map.json 을 로드해
 * beach_id -> { beach_name_std, sand_area_m2 } 형태로 제공
 */
@Service
public class AreaMapService {
    // JSON 파싱을 위한 Jackson ObjectMapper
    private final ObjectMapper mapper = new ObjectMapper();
    // beach_id를 key로, 각 해변의 표준 이름/면적 등을 담는 맵
    // volatile → 멀티스레드 환경에서도 최신 값 보장
    private volatile Map<String, Map<String, Object>> areaMap;

    @PostConstruct
    public void load() throws Exception {
        // ClassPathResource를 사용하여 리소스 폴더의 파일을 안전하게 읽어옴
        ClassPathResource resource = new ClassPathResource("data/beach_area_map.json");
        if (!resource.exists()) return;

        try (InputStream in = resource.getInputStream()) {
            areaMap = mapper.readValue(in, new TypeReference<Map<String, Map<String, Object>>>() {});
        }
    }

    /** beach_id로 면적(m^2) 반환. 없으면 0 */
    public double getSandAreaM2(String beachId) {
        if (areaMap == null) return 0d; // 데이터가 아직 로드되지 않은 경우
        Map<String, Object> rec = areaMap.get(beachId);
        if (rec == null) return 0d; // 해당 beachId가 없을 경우
        Object v = rec.get("sand_area_m2");
        // 숫자 타입일 경우 double로 변환하여 반환
        return (v instanceof Number) ? ((Number) v).doubleValue() : 0d;
    }
}
