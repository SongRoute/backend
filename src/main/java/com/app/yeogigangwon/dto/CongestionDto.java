// backend/src/main/java/com/app/yeogigangwon/dto/CongestionDto.java
package com.app.yeogigangwon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

public class CongestionDto {

    // CCTV 정보를 담는 DTO
    @Getter
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
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CrowdAnalysisResult {
        private int personCount;
    }

    // 최종 혼잡도 상태를 담는 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CrowdStatus {
        private String beachId;
        private String beachName;
        private String status; // 여유, 보통, 혼잡
        private int personCount;
        private double referenceCrowdLevel;
    }
}
