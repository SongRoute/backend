package com.app.yeogigangwon.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

// 혼잡도 분석 응답 DTO
public class CongestionResponse {
    private String beachName;
    private int personCount;
    private String congestionLevel;
}
