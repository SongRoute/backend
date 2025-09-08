package com.app.yeogigangwon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 혼잡도 관련 DTO 모음 클래스
public class CongestionDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString

    // 개별 CCTV 정보 DTO
    public static class CctvInfo {
        private String id; // CCTV 고유 ID
        private String beachName; // CCTV가 설치된 해변 이름
        private String location; // 해변 위치
        private String cctvUrl; // CCTV 영상 접근 URL
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    // YOLO 사람 수 분석 결과 DTO
    public static class CrowdAnalysisResult {
        @JsonProperty("person_count")
        private int personCount; // 분석된 사람 수 (JSON 키: person_count)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString

    // 혼잡도 상태 응답 DTO (프론트엔드에서 혼잡도 표시용)
    public static class CrowdStatus {
        private String beachId; // 해변을 구분하기 위한 고유 ID
        private String beachName; // 해변 이름
        private String status; // 혼잡 상태 ("여유", "보통", "혼잡")
        private int personCount; // 해당 시점에서 감지된 실제 사람 수

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Double referenceCrowdLevel; // 기준 혼잡 레벨 (혼잡도 비교 지표, 필요시만 포함됨)
    }
}
