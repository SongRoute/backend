package com.app.yeogigangwon.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "crowd_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

// crowd_records 테이블과 매핑되는 혼잡도 도메인 클래스
public class CongestionDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본 키(PK)

    private String beachId; // 해변을 구분하기 위한 고유 ID
    private String beachName; // 해변 이름
    private int personCount; // 해당 시각에 YOLO 모델로 탐지된 사람 수
    private LocalDateTime timestamp; // 데이터가 기록된 시각
}