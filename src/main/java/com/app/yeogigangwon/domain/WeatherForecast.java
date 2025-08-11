package com.app.yeogigangwon.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "weather_forecast")
public class WeatherForecast {

    @Id
    private String id;

    // 기상청 격자 좌표
    private int nx;
    private int ny;

    // 예보 기준(요청) 시각
    private String baseDate;
    private String baseTime;

    // 단기예보 주요 지표
    private int tmp; // 기온
    private int pop; // 강수확률
    private int sky; // 하늘상태
    private int wsd; // 풍속

    // 생성 시각(서버 기준)
    private LocalDateTime createdAt;

    public WeatherForecast() {}

    public WeatherForecast(String id, int nx, int ny, String baseDate, String baseTime,
                           int tmp, int pop, int sky, int wsd, LocalDateTime createdAt) {
        this.id = id;
        this.nx = nx;
        this.ny = ny;
        this.baseDate = baseDate;
        this.baseTime = baseTime;
        this.tmp = tmp;
        this.pop = pop;
        this.sky = sky;
        this.wsd = wsd;
        this.createdAt = createdAt;
    }

    // Getter & Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getNx() { return nx; }
    public void setNx(int nx) { this.nx = nx; }

    public int getNy() { return ny; }
    public void setNy(int ny) { this.ny = ny; }

    public String getBaseDate() { return baseDate; }
    public void setBaseDate(String baseDate) { this.baseDate = baseDate; }

    public String getBaseTime() { return baseTime; }
    public void setBaseTime(String baseTime) { this.baseTime = baseTime; }

    public int getTmp() { return tmp; }
    public void setTmp(int tmp) { this.tmp = tmp; }

    public int getPop() { return pop; }
    public void setPop(int pop) { this.pop = pop; }

    public int getSky() { return sky; }
    public void setSky(int sky) { this.sky = sky; }

    public int getWsd() { return wsd; }
    public void setWsd(int wsd) { this.wsd = wsd; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
