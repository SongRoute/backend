package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.KtoCongestion;
import com.app.yeogigangwon.repository.KtoCongestionRepository;
import com.app.yeogigangwon.repository.KtoPlaceMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KtoService {

    private final KtoCongestionRepository ktoCongestionRepository;
    private final KtoPlaceMapRepository ktoPlaceMapRepository;

    public Optional<Double> getRate(String key, LocalDate date) {
        return ktoCongestionRepository.findByPlaceIdAndDate(key, date).map(KtoCongestion::getRate);
    }

    // 관광지명으로 내부 ID 조회 후 오늘부터 가장 가까운 예측치 반환
    public Optional<Double> getRateByPlaceName(String placeName, LocalDate fromDate) {
        return ktoPlaceMapRepository.findByPlaceName(placeName)
                .flatMap(pm -> ktoCongestionRepository
                        .findFirstByPlaceIdAndDateGreaterThanEqualOrderByDateAsc(pm.getInternalId(), fromDate))
                .map(KtoCongestion::getRate);
    }
}
