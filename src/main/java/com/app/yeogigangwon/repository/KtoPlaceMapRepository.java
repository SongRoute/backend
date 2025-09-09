package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.KtoPlaceMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KtoPlaceMapRepository extends JpaRepository<KtoPlaceMap, Long> {

    Optional<KtoPlaceMap> findByPlaceName(String placeName);

    Optional<KtoPlaceMap> findByInternalId(String internalId);
}
