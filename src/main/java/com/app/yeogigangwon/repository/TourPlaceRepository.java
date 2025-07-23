package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.TourPlace;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TourPlaceRepository extends MongoRepository<TourPlace, String> {
    List<TourPlace> findByCategoryContainingIgnoreCase(String keyword);
}
