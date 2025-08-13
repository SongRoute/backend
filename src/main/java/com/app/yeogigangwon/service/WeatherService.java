package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.WeatherForecast;
import com.app.yeogigangwon.dto.WeatherAlert;
import com.app.yeogigangwon.dto.WeatherInfo;
import com.app.yeogigangwon.dto.WeatherSummary;
import com.app.yeogigangwon.fetch.AlertFetcher;
import com.app.yeogigangwon.fetch.ForecastFetcher;
import com.app.yeogigangwon.repository.WeatherForecastRepository;
import com.app.yeogigangwon.util.GridConverter;
import com.app.yeogigangwon.util.GridConverter.GridCoordinate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final ForecastFetcher forecastFetcher;
    private final AlertFetcher alertFetcher;
    private final WeatherForecastRepository weatherForecastRepository;

    /** 기존: 실시간 요약 조회 */
    public WeatherSummary getWeatherSummary(double lat, double lon) {
        WeatherInfo info = forecastFetcher.fetchShortTermForecast(lat, lon);
        List<WeatherAlert> alerts = alertFetcher.fetchWeatherAlert("강원도");
        return new WeatherSummary(info, alerts);
    }

    /** 추가: API에서 받아 DB에 저장 */
    public WeatherForecast fetchAndSave(double lat, double lon) {
        // 기준 시각 산출
        LocalDateTime now = LocalDateTime.now().minusHours(1);
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = getNearestBaseTime(now.getHour());

        // 좌표 → 격자
        GridCoordinate grid = GridConverter.convertToGrid(lat, lon);
        int nx = grid.nx;
        int ny = grid.ny;

        // API 호출 → DTO
        WeatherInfo info = forecastFetcher.fetchShortTermForecast(lat, lon);

        // DTO → 도메인 저장
        WeatherForecast wf = new WeatherForecast();
        wf.setNx(nx);
        wf.setNy(ny);
        wf.setBaseDate(baseDate);
        wf.setBaseTime(baseTime);
        wf.setTmp(info.getTemperature());
        wf.setPop(info.getPrecipitationProbability());
        wf.setSky(info.getSky());
        wf.setWsd(info.getWindSpeed());
        wf.setCreatedAt(LocalDateTime.now());

        return weatherForecastRepository.save(wf);
    }

    /** 추가: DB에서 최신 데이터 조회(관광지 좌표 기준) */
    public WeatherInfo getLatestFromDb(double lat, double lon) {
        GridCoordinate grid = GridConverter.convertToGrid(lat, lon);
        int nx = grid.nx;
        int ny = grid.ny;

        // createdAt 기준 최신 또는 baseDate/baseTime 기준 최신 둘 중 하나 사용 가능
        Optional<WeatherForecast> opt =
                weatherForecastRepository.findTopByNxAndNyOrderByCreatedAtDesc(nx, ny);
        if (opt.isEmpty()) return null;

        WeatherForecast f = opt.get();
        return new WeatherInfo(f.getTmp(), f.getPop(), f.getSky(), f.getWsd());
    }

    private String getNearestBaseTime(int hour) {
        if (hour < 2) return "2300";
        else if (hour < 5) return "0200";
        else if (hour < 8) return "0500";
        else if (hour < 11) return "0800";
        else if (hour < 14) return "1100";
        else if (hour < 17) return "1400";
        else if (hour < 20) return "1700";
        else if (hour < 23) return "2000";
        else return "2300";
    }
}
