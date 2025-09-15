package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.TourPlace;
import com.app.yeogigangwon.dto.TourPlaceDistance;
import com.app.yeogigangwon.dto.TravelTimeInfo;
import com.app.yeogigangwon.fetch.KakaoMapApiClient;
import com.app.yeogigangwon.repository.TourPlaceRepository;
import com.app.yeogigangwon.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 관광지 정보 서비스
 * 관광지 CRUD, 거리 기반 추천, 이동 시간 계산 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourPlaceService {

    private final TourPlaceRepository tourPlaceRepository;
    private final KakaoMapApiClient kakaoMapApiClient;
    
    /**
     * 사용자 위치 기반으로 가까운 관광지 추천 (거리순)
     * 
     * @param userLat 사용자 위도
     * @param userLon 사용자 경도
     * @param limit 추천할 관광지 개수
     * @return 거리순으로 정렬된 관광지 목록
     */
    public List<TourPlaceDistance> getNearbyPlaces(double userLat, double userLon, int limit) {
        log.info("가까운 관광지 추천 - 사용자 위치: ({}, {}), 제한: {}", userLat, userLon, limit);
        
        List<TourPlace> allPlaces = tourPlaceRepository.findAll();
        
        return allPlaces.stream()
                .map(place -> {
                    // 관광지의 위도/경도 직접 사용
                    if (place.getLatitude() != null && place.getLongitude() != null) {
                        // 이동시간 정보 조회 (거리 계산 포함)
                        TravelTimeInfo travelTime = kakaoMapApiClient.getTravelTime(
                            userLat, userLon, place.getLatitude(), place.getLongitude()
                        );
                        
                        // 이동시간 정보가 있으면 사용, 없으면 거리 기반 추정
                        int drivingTime = travelTime != null ? travelTime.getDrivingTime() : 0;
                        int walkingTime = travelTime != null ? travelTime.getWalkingTime() : 0;
                        double distance = travelTime != null ? travelTime.getDrivingDistance() : 0.0;
                        
                        // TourPlaceDistance 객체 생성 (이동시간 정보 포함)
                        TourPlaceDistance tpd = new TourPlaceDistance();
                        tpd.setPlace(place);
                        tpd.setDistance(distance);
                        tpd.setDrivingTime(drivingTime);
                        tpd.setWalkingTime(walkingTime);
                        tpd.setTransportationMode("car"); // 기본값
                        
                        return tpd;
                    }
                    return null;
                })
                .filter(tpd -> tpd != null)
                .sorted((a, b) -> Double.compare(a.getDistance(), b.getDistance())) // 거리순 정렬
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * 거리 기반으로 정렬된 관광지 추천
     * 
     * @param userLat 사용자 위도
     * @param userLon 사용자 경도
     * @param limit 추천할 관광지 개수
     * @return 거리 기반으로 정렬된 관광지 목록
     */
    public List<TourPlaceDistance> getRecommendedPlacesByDistance(double userLat, double userLon, int limit) {
        log.info("거리 기반 관광지 추천 - 사용자 위치: ({}, {}), 제한: {}", userLat, userLon, limit);
        
        List<TourPlaceDistance> nearbyPlaces = getNearbyPlaces(userLat, userLon, limit * 2);
        
        return nearbyPlaces.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    

    
    /**
     * 관광지 정보 저장
     * 
     * @param tourPlace 저장할 관광지 정보
     * @return 저장된 관광지 정보
     */
    public TourPlace saveTourPlace(TourPlace tourPlace) {
        log.info("관광지 저장: {}", tourPlace.getName());
        return tourPlaceRepository.save(tourPlace);
    }
    
    /**
     * 모든 관광지 정보 조회
     * 
     * @return 전체 관광지 목록
     */
    public List<TourPlace> getAllTourPlaces() {
        log.info("전체 관광지 정보 조회");
        return tourPlaceRepository.findAll();
    }
    
    /**
     * 두 관광지 간의 이동 시간 조회
     * 
     * @param place1Id 출발지 관광지 ID
     * @param place2Id 도착지 관광지 ID
     * @return 이동 시간 정보 (없으면 null)
     */
    public TravelTimeInfo getTravelTimeBetweenPlaces(Long place1Id, Long place2Id) {
        log.info("관광지 간 이동 시간 조회 - 출발지: {}, 도착지: {}", place1Id, place2Id);
        
        TourPlace place1 = tourPlaceRepository.findById(place1Id).orElse(null);
        TourPlace place2 = tourPlaceRepository.findById(place2Id).orElse(null);
        
        if (place1 == null || place2 == null) {
            log.warn("관광지를 찾을 수 없습니다 - place1: {}, place2: {}", place1Id, place2Id);
            return null;
        }
        
        if (place1.getLatitude() == null || place1.getLongitude() == null || 
            place2.getLatitude() == null || place2.getLongitude() == null) {
            log.warn("관광지 좌표가 없습니다 - place1: ({}, {}), place2: ({}, {})", 
                place1.getLatitude(), place1.getLongitude(), place2.getLatitude(), place2.getLongitude());
            return null;
        }
        
        // 카카오맵 API로 이동 시간 조회
        log.info("TourPlaceService에서 kakaoMapApiClient.getTravelTime 호출 시작");
        TravelTimeInfo travelTime = kakaoMapApiClient.getTravelTime(
            place1.getLatitude(), place1.getLongitude(), place2.getLatitude(), place2.getLongitude()
        );
        log.info("TourPlaceService에서 kakaoMapApiClient.getTravelTime 호출 완료");
        
        // 출발지와 도착지 이름 설정
        travelTime.setOriginName(place1.getName());
        travelTime.setDestinationName(place2.getName());
        
        return travelTime;
    }
    
    /**
     * 사용자 위치에서 특정 관광지까지의 이동 시간
     * 
     * @param userLat 사용자 위도
     * @param userLon 사용자 경도
     * @param placeId 관광지 ID
     * @return 이동 시간 정보 (없으면 null)
     */
    public TravelTimeInfo getTravelTimeFromUserLocation(double userLat, double userLon, Long placeId) {
        log.info("사용자 위치에서 관광지까지 이동 시간 조회 - 사용자: ({}, {}), 관광지: {}", userLat, userLon, placeId);
        
        TourPlace place = tourPlaceRepository.findById(placeId).orElse(null);
        
        if (place == null) {
            log.warn("관광지를 찾을 수 없습니다 - placeId: {}", placeId);
            return null;
        }
        
        if (place.getLatitude() == null || place.getLongitude() == null) {
            log.warn("관광지 좌표가 없습니다 - place: ({}, {})", place.getLatitude(), place.getLongitude());
            return null;
        }
        
        // 카카오맵 API로 이동 시간 조회
        TravelTimeInfo travelTime = kakaoMapApiClient.getTravelTime(
            userLat, userLon, place.getLatitude(), place.getLongitude()
        );
        
        travelTime.setOriginName("사용자 위치");
        travelTime.setDestinationName(place.getName());
        
        return travelTime;
    }
}