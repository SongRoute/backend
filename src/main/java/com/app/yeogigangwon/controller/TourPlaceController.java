// 관광지 CRUD, 거리 기반 추천, 이동 시간 계산 기능
package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.domain.TourPlace;
import com.app.yeogigangwon.dto.LocationRequest;
import com.app.yeogigangwon.dto.TourPlaceDistance;
import com.app.yeogigangwon.dto.TravelTimeInfo;
import com.app.yeogigangwon.service.TourPlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class TourPlaceController {

    private final TourPlaceService tourPlaceService;

    /**
     * 사용자 위치 기반 가까운 관광지 조회 (거리순)
     * 
     * @param lat 위도
     * @param lon 경도
     * @param limit 추천할 관광지 개수 (기본값: 10)
     * @return 거리순으로 정렬된 관광지 목록
     */
    @GetMapping("/places/nearby")
    public ResponseEntity<List<TourPlaceDistance>> getNearbyPlaces(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("가까운 관광지 조회 요청 - 위치: ({}, {}), 제한: {}", lat, lon, limit);
        
        List<TourPlaceDistance> places = tourPlaceService.getNearbyPlaces(lat, lon, limit);
        return ResponseEntity.ok(places);
    }
    
    /**
     * 거리 기반으로 정렬된 관광지 추천 (교통수단 선택 가능)
     * 
     * @param lat 위도
     * @param lon 경도
     * @param limit 추천할 관광지 개수 (기본값: 10)
     * @param transport 교통수단 ("car" 또는 "walk", 기본값: "car")
     * @return 거리 기반으로 정렬된 관광지 목록
     */
    @GetMapping("/places/recommended")
    public ResponseEntity<List<TourPlaceDistance>> getRecommendedPlaces(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "car") String transport
    ) {
        log.info("거리 기반 관광지 추천 요청 - 위치: ({}, {}), 제한: {}, 교통수단: {}", lat, lon, limit, transport);
        
        List<TourPlaceDistance> places = tourPlaceService.getRecommendedPlacesByDistance(lat, lon, limit);
        
        // 교통수단에 따라 정렬 기준 변경
        if ("walk".equals(transport)) {
            places = places.stream()
                    .peek(tpd -> tpd.setTransportationMode("walk"))
                    .sorted((a, b) -> Integer.compare(a.getWalkingTime(), b.getWalkingTime()))
                    .collect(Collectors.toList());
        } else {
            places = places.stream()
                    .peek(tpd -> tpd.setTransportationMode("car"))
                    .sorted((a, b) -> Integer.compare(a.getDrivingTime(), b.getDrivingTime()))
                    .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(places);
    }
    
    /**
     * POST 방식으로 위치 정보를 받아서 가까운 관광지 조회
     * 
     * @param request 위치 정보 요청 DTO
     * @param limit 추천할 관광지 개수 (기본값: 10)
     * @return 거리순으로 정렬된 관광지 목록
     */
    @PostMapping("/places/nearby")
    public ResponseEntity<List<TourPlaceDistance>> getNearbyPlacesPost(
            @RequestBody LocationRequest request,
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("POST 방식 가까운 관광지 조회 요청 - 위치: ({}, {}), 제한: {}", 
                request.getLatitude(), request.getLongitude(), limit);
        
        List<TourPlaceDistance> places = tourPlaceService.getNearbyPlaces(
            request.getLatitude(), request.getLongitude(), limit
        );
        return ResponseEntity.ok(places);
    }
    
    /**
     * 새로운 관광지 정보 등록
     * 
     * @param tourPlace 등록할 관광지 정보
     * @return 저장된 관광지 정보
     */
    @PostMapping("/places")
    public ResponseEntity<TourPlace> createTourPlace(@RequestBody TourPlace tourPlace) {
        log.info("관광지 등록 요청: {}", tourPlace.getName());
        
        TourPlace saved = tourPlaceService.saveTourPlace(tourPlace);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 모든 관광지 정보 조회
     * 
     * @return 전체 관광지 목록
     */
    @GetMapping("/places")
    public ResponseEntity<List<TourPlace>> getAllPlaces() {
        log.info("전체 관광지 조회 요청");
        
        List<TourPlace> places = tourPlaceService.getAllTourPlaces();
        return ResponseEntity.ok(places);
    }
    
    /**
     * 두 관광지 간의 이동 시간 조회
     * 
     * @param place1Id 출발지 관광지 ID
     * @param place2Id 도착지 관광지 ID
     * @return 이동 시간 정보 (없으면 404 Not Found)
     */
    @GetMapping("/places/travel-time")
    public ResponseEntity<TravelTimeInfo> getTravelTimeBetweenPlaces(
            @RequestParam Long place1Id,
            @RequestParam Long place2Id
    ) {
        log.info("관광지 간 이동 시간 조회 요청 - 출발지: {}, 도착지: {}", place1Id, place2Id);
        
        TravelTimeInfo travelTime = tourPlaceService.getTravelTimeBetweenPlaces(place1Id, place2Id);
        if (travelTime == null) {
            log.warn("이동 시간 정보를 찾을 수 없습니다");
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(travelTime);
    }
    
    /**
     * 사용자 위치에서 특정 관광지까지의 이동 시간
     * 
     * @param lat 사용자 위도
     * @param lon 사용자 경도
     * @param placeId 관광지 ID
     * @return 이동 시간 정보 (없으면 404 Not Found)
     */
    @GetMapping("/places/travel-time-from-user")
    public ResponseEntity<TravelTimeInfo> getTravelTimeFromUserLocation(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam Long placeId
    ) {
        log.info("사용자 위치에서 관광지까지 이동 시간 조회 요청 - 사용자: ({}, {}), 관광지: {}", lat, lon, placeId);
        
        TravelTimeInfo travelTime = tourPlaceService.getTravelTimeFromUserLocation(lat, lon, placeId);
        if (travelTime == null) {
            log.warn("이동 시간 정보를 찾을 수 없습니다");
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(travelTime);
    }
}