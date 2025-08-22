// backend/src/main/java/com/app/yeogigangwon/dto/CongestionDto.java
package com.app.yeogigangwon.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public class CongestionDto {

    // CCTV 정보를 담는 DTO (내부 사용용: 필요 시에만 Setter)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CctvInfo {
        private String id;
        private String beachName;
        private String location;
        private String cctvUrl;
    }

    // Python API의 분석 결과를 담는 DTO
    // person_count → personCount로 명시 매핑!
    @Getter
    @Setter // 역직렬화 시 값 주입을 위해 필요
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CrowdAnalysisResult {
        @JsonProperty("person_count")
        private int personCount;
    }

    // 최종 혼잡도 상태를 담는 DTO (클라이언트 응답용)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CrowdStatus {
        private String beachId;
        private String beachName;
        private String status; // 여유, 보통, 혼잡
        private int personCount;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Double referenceCrowdLevel;
    }
}
