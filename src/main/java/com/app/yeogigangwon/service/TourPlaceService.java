package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.TourPlace;
import com.app.yeogigangwon.fetch.TourPlaceFetcher;
import com.app.yeogigangwon.repository.TourPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class TourPlaceService {

    private final TourPlaceRepository tourPlaceRepository;
    private final TourPlaceFetcher tourPlaceFetcher;

    public void fetchAndSaveTourPlaces() {
        List<TourPlace> places = tourPlaceFetcher.fetchTourPlacesFromApi();
        tourPlaceRepository.saveAll(places);
    }

    public List<TourPlace> getAllTourPlaces() {
        return tourPlaceRepository.findAll();
    }
}
