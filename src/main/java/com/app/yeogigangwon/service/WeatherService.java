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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 날씨 정보 서비스
 * 기상청 API 호출, 데이터 저장, 조회 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final ForecastFetcher forecastFetcher;
    private final AlertFetcher alertFetcher;
    private final WeatherForecastRepository weatherForecastRepository;

    /**
     * 실시간 날씨 요약 정보 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @return 날씨 요약 정보
     */
    public WeatherSummary getWeatherSummary(double lat, double lon) {
       log.info("날씨 요약 조회 - 위도: {}, 경도: {}", lat, lon);
        
        try {
            // 1. 먼저 DB에서 최신 데이터 조회 (15분 이내)
            WeatherInfo cachedInfo = getLatestFromDb(lat, lon);
            
            // 2. 캐시된 데이터가 있고 15분 이내라면 사용
            if (cachedInfo != null) {
                log.info("DB에서 최신 날씨 데이터 사용 (온도: {}도)", cachedInfo.getTemperature());
                
                // 기상 특보 조회 (실패 시 빈 목록 반환)
                List<WeatherAlert> alerts = new ArrayList<>();
                try {
                    alerts = alertFetcher.fetchWeatherAlerts("강원도");
                } catch (Exception e) {
                    log.warn("기상 특보 조회 실패, 빈 목록으로 처리: {}", e.getMessage());
                }
                
                return new WeatherSummary(cachedInfo, alerts);
            }
            
            // 3. 캐시된 데이터가 없거나 오래되었다면 새로운 API 호출
            log.info("DB에 최신 데이터가 없어 새로운 API 호출을 시도합니다");
            WeatherInfo info = forecastFetcher.fetchWeatherForecast(
                GridConverter.convertToGrid(lat, lon).nx,
                GridConverter.convertToGrid(lat, lon).ny
            );
            
            // 4. 새로운 데이터를 DB에 저장
            try {
                fetchAndSave(lat, lon);
                log.info("새로운 날씨 데이터를 DB에 저장했습니다");
            } catch (Exception e) {
                log.warn("DB 저장 실패, 하지만 API 데이터는 사용: {}", e.getMessage());
            }
            
            // 기상 특보 조회 (실패 시 빈 목록 반환)
            List<WeatherAlert> alerts = new ArrayList<>();
            try {
                alerts = alertFetcher.fetchWeatherAlerts("강원도");
            } catch (Exception e) {
                log.warn("기상 특보 조회 실패, 빈 목록으로 처리: {}", e.getMessage());
            }
            
            return new WeatherSummary(info, alerts);
        } catch (Exception e) {
            log.error("날씨 요약 조회 실패", e);
            throw new RuntimeException("날씨 요약 조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기상청 API에서 날씨 데이터를 가져와서 DB에 저장
     * 
     * @param lat 위도
     * @param lon 경도
     * @return 저장된 날씨 예보 정보
     */
    public WeatherForecast fetchAndSave(double lat, double lon) {
        log.info("날씨 데이터 API 호출 및 저장 - 위도: {}, 경도: {}", lat, lon);
        
        try {
            // 기준 시각 계산 (1시간 전 기준)
            LocalDateTime now = LocalDateTime.now().minusHours(1);
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = getNearestBaseTime(now.getHour());

            // 위도/경도를 격자 좌표로 변환
            GridCoordinate grid = GridConverter.convertToGrid(lat, lon);

            // API에서 단기 예보 데이터 조회
            WeatherInfo info = forecastFetcher.fetchWeatherForecast(grid.nx, grid.ny);

            // WeatherForecast 도메인 객체 생성 및 저장
            WeatherForecast weatherForecast = new WeatherForecast();
            weatherForecast.setNx(String.valueOf(grid.nx));
            weatherForecast.setNy(String.valueOf(grid.ny));
            weatherForecast.setBaseDate(baseDate);
            weatherForecast.setBaseTime(baseTime);
            weatherForecast.setForecastTime(LocalDateTime.now());
            
            // WeatherInfo를 JSON으로 변환하여 저장
            String weatherData = String.format(
                "{\"temperature\":\"%s\",\"precipitationProbability\":\"%s\",\"sky\":\"%s\",\"windSpeed\":\"%s\"}",
                info.getTemperature(), info.getPrecipitationProbability(), info.getSky(), info.getWindSpeed()
            );
            weatherForecast.setWeatherData(weatherData);
            weatherForecast.setCreatedAt(LocalDateTime.now());

            return weatherForecastRepository.save(weatherForecast);
        } catch (Exception e) {
            log.error("날씨 데이터 API 호출 및 저장 실패", e);
            throw new RuntimeException("날씨 데이터 API 호출 및 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * DB에서 최신 날씨 데이터 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @return 최신 날씨 정보 (없으면 null)
     */
    public WeatherInfo getLatestFromDb(double lat, double lon) {
        log.info("DB에서 최신 날씨 데이터 조회 - 위도: {}, 경도: {}", lat, lon);
        
        // 위도/경도를 격자 좌표로 변환
        GridCoordinate grid = GridConverter.convertToGrid(lat, lon);

        // 격자 좌표 기준으로 최신 데이터 조회
        List<WeatherForecast> forecasts = weatherForecastRepository.findLatestByGrid(String.valueOf(grid.nx), String.valueOf(grid.ny));
        
        if (forecasts.isEmpty()) {
            log.warn("해당 좌표의 날씨 데이터가 없습니다 - nx: {}, ny: {}", grid.nx, grid.ny);
            return null;
        }

        // WeatherForecast를 WeatherInfo로 변환 (JSON 파싱)
        WeatherForecast forecast = forecasts.get(0);
        
        // 15분 이내의 데이터인지 확인
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime forecastTime = forecast.getCreatedAt();
        long minutesDiff = java.time.Duration.between(forecastTime, now).toMinutes();
        
        if (minutesDiff > 15) {
            log.info("DB 데이터가 {}분 전 데이터로 너무 오래되어 사용하지 않습니다", minutesDiff);
            return null;
        }
        
        log.info("DB에서 {}분 전 데이터 사용 (온도: {}도)", minutesDiff, forecast.getWeatherData());
        
        try {
            // 간단한 JSON 파싱 (실제로는 Jackson ObjectMapper 사용 권장)
            String weatherData = forecast.getWeatherData();
            if (weatherData != null && weatherData.contains("temperature")) {
                // JSON에서 값 추출 (간단한 방식)
                String temp = extractValue(weatherData, "temperature");
                String pop = extractValue(weatherData, "precipitationProbability");
                String sky = extractValue(weatherData, "sky");
                String wsd = extractValue(weatherData, "windSpeed");
                
                return new WeatherInfo(
                    Integer.parseInt(temp), 
                    Integer.parseInt(pop), 
                    Integer.parseInt(sky), 
                    Integer.parseInt(wsd)
                );
            }
        } catch (Exception e) {
            log.warn("날씨 데이터 파싱 실패: {}", forecast.getWeatherData(), e);
        }
        
        return null;
    }

    /**
     * JSON 문자열에서 특정 키의 값을 추출하는 헬퍼 메서드
     * 
     * @param json JSON 문자열
     * @param key 추출할 키
     * @return 추출된 값 (없으면 "0")
     */
    private String extractValue(String json, String key) {
        try {
            int startIndex = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
            int endIndex = json.indexOf("\"", startIndex);
            if (startIndex > key.length() + 3 && endIndex > startIndex) {
                return json.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            log.warn("JSON 파싱 실패: key={}, json={}", key, json, e);
        }
        return "0";
    }

    /**
     * 현재 시간에 가장 가까운 기상청 예보 기준 시각 반환
     * 기상청은 3시간마다 예보를 발표 (02, 05, 08, 11, 14, 17, 20, 23시)
     * 
     * @param hour 현재 시간 (0-23)
     * @return 예보 기준 시각 (HHMM 형식)
     */
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
