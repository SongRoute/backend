package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.KtoCongestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface KtoCongestionRepository extends JpaRepository<KtoCongestion, Long> {

    Optional<KtoCongestion> findByPlaceIdAndDate(String placeId, LocalDate date);

    Optional<KtoCongestion> findFirstByPlaceIdAndDateGreaterThanEqualOrderByDateAsc(String placeId, LocalDate date);
}
