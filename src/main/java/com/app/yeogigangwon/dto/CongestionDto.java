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


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CrowdAnalysisResult {
        @JsonProperty("person_count")
        private int personCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CrowdStatus {
        private String beachId;
        private String beachName;
        private String status;
        private int personCount;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Double referenceCrowdLevel;
    }
}
