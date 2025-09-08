package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.CongestionDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// JpaRepository를 확장하여 기본 CRUD와 파생 쿼리를 제공
@Repository
public interface CongestionRepository extends JpaRepository<CongestionDomain, Long> { // Long 타입으로 변경

    // 특정 해변(beachId)의 혼잡도 기록을 주어진 시간 범위(start~end)에서 조회,
    // timestamp 기준 오름차순(과거 -> 최신 순)으로 정렬하여 반환함
    List<CongestionDomain> findByBeachIdAndTimestampBetweenOrderByTimestampAsc(String beachId, LocalDateTime start, LocalDateTime end);

    // 특정 해변(beachId)의 혼잡도 기록을 주어진 시간 범위(start~end)에서 조회,
    // timestamp 기준 내림차순(최신 -> 과거 순)으로 정렬하여 반환함
    List<CongestionDomain> findByBeachIdAndTimestampBetweenOrderByTimestampDesc(String beachId, LocalDateTime start, LocalDateTime end);
}