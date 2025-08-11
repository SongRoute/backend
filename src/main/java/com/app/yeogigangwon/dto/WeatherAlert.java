package com.app.yeogigangwon.dto;

public class WeatherAlert {
    private String title;
    private String message;
    private String time;

    public WeatherAlert() {}

    public WeatherAlert(String title, String message, String time) {
        this.title = title;
        this.message = message;
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "WeatherAlert{" +
                "title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", time='" + time + '\'' +
                '}';
    }

    // 기상특보 위험도 레벨 추가
    public int getDangerLevel() {
        String titleLower = title.toLowerCase();

        // 가장 위험한 레벨 (태풍, 호우주의보)
        if (titleLower.contains("태풍") || titleLower.contains("호우주의보") ||
                titleLower.contains("폭풍주의보")) {
            return 5;
        }
        // 높은 위험도 (강풍주의보, 대설주의보)
        else if (titleLower.contains("강풍주의보") || titleLower.contains("대설주의보") ||
                titleLower.contains("폭염주의보")) {
            return 4;
        }
        // 중간 위험도 (주의보)
        else if (titleLower.contains("주의보")) {
            return 3;
        }
        // 낮은 위험도 (예비특보)
        else if (titleLower.contains("예비특보")) {
            return 2;
        }
        // 정보성 (기타)
        else {
            return 1;
        }
    }

    // 위험도에 따른 점수 차감
    public int getScorePenalty() {
        int level = getDangerLevel();
        return switch (level) {
            case 5 -> 100; // 태풍/호우주의보는 완전 차단
            case 4 -> 80;  // 강풍/대설주의보는 거의 차단
            case 3 -> 60;  // 주의보는 상당한 차단
            case 2 -> 30;  // 예비특보는 부분적 차단
            default -> 0;  // 정보성은 영향 없음
        };
    }
}
