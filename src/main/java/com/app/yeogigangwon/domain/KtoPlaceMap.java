package com.app.yeogigangwon.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "kto_place_map",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_place_name", columnNames = "placeName"),
                @UniqueConstraint(name = "uk_internal_id", columnNames = "internalId")
        })
public class KtoPlaceMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 원본 관광지명 (API의 tAtsNm)
    @Column(nullable = false, length = 200)
    private String placeName;

    // 내부 식별자 (예: "kto_1")
    @Column(nullable = false, length = 64)
    private String internalId;

    protected KtoPlaceMap() {}

    public KtoPlaceMap(String placeName, String internalId) {
        this.placeName = placeName;
        this.internalId = internalId;
    }

    public Long getId() { return id; }
    public String getPlaceName() { return placeName; }
    public String getInternalId() { return internalId; }

    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public void setInternalId(String internalId) { this.internalId = internalId; }
}
