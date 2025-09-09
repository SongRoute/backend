package com.app.yeogigangwon.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "kto_congestion",
        indexes = {
                @Index(name = "idx_place_date", columnList = "placeId,date"),
                @Index(name = "idx_date", columnList = "date")
        })
public class KtoCongestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 내부 식별자 (예: "kto_1")
    @Column(nullable = false, length = 64)
    private String placeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private double rate;

    protected KtoCongestion() {}

    public KtoCongestion(String placeId, LocalDate date, double rate) {
        this.placeId = placeId;
        this.date = date;
        this.rate = rate;
    }

    public Long getId() { return id; }
    public String getPlaceId() { return placeId; }
    public LocalDate getDate() { return date; }
    public double getRate() { return rate; }

    public void setPlaceId(String placeId) { this.placeId = placeId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setRate(double rate) { this.rate = rate; }
}
