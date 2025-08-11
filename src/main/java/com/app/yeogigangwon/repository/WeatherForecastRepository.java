package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.WeatherForecast;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WeatherForecastRepository extends MongoRepository<WeatherForecast, String> {
    Optional<WeatherForecast> findTopByNxAndNyOrderByBaseDateDescBaseTimeDesc(int nx, int ny);
    Optional<WeatherForecast> findTopByNxAndNyOrderByCreatedAtDesc(int nx, int ny);
}
