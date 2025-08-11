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

// 혼잡도 분석 요청 DTO
public class CongestionRequest {
    private String imageUrl;
    private String beachName;
}
