package com.app.yeogigangwon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

/**
 * 관광지 정보를 저장하는 MySQL 엔티티
 * 관광지의 기본 정보와 혼잡도 정보를 포함
 */
@Entity
@Table(name = "tour_places")
@Data
@NoArgsConstructor
public class TourPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;        // 관광지명
    
    @Column(length = 1000)
    private String description; // 관광지 설명
    
    @Column(columnDefinition = "DOUBLE")
    private Double latitude;    // 위도
    
    @Column(columnDefinition = "DOUBLE")
    private Double longitude;   // 경도
    
    private String category;    // 관광지 카테고리 (해변, 산, 문화재 등)
    
    private Integer crowdLevel; // 혼잡도 (1-5, 1:여유, 5:매우혼잡)
}
