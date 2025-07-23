package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.domain.TourPlace;
import com.app.yeogigangwon.repository.TourPlaceRepository;
import com.app.yeogigangwon.service.TourPlaceService;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TourPlaceController {

    private final TourPlaceRepository repository;
    private final TourPlaceService tourPlaceService;

    @GetMapping("/recommend")
    public List<TourPlace> recommend(@RequestParam String keyword) {
        return repository.findByCategoryContainingIgnoreCase(keyword);
    }

    @GetMapping("/fetch")
    public ResponseEntity<String> fetchAndSaveTourPlaces() {
        tourPlaceService.fetchAndSaveTourPlaces();
        return ResponseEntity.ok("관광지 데이터 저장 완료");
    }

}
