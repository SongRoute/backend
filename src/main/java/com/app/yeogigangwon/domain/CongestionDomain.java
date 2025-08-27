// backend/src/main/java/com/app/yeogigangwon/domain/CongestionDomain.java
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
public class CongestionDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // MySQL은 Long 타입의 기본키를 주로 사용합니다.

    private String beachId;
    private String beachName;
    private int personCount;
    private LocalDateTime timestamp;
}