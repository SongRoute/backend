// backend/src/main/java/com/app/yeogigangwon/repository/CongestionRepository.java
package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.CongestionDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CongestionRepository extends JpaRepository<CongestionDomain, Long> { // Long 타입으로 변경

    // findByBeachIdAndTimestampBetweenOrderByTimestampAsc
    List<CongestionDomain> findByBeachIdAndTimestampBetweenOrderByTimestampAsc(String beachId, LocalDateTime start, LocalDateTime end);

    // findByBeachIdAndTimestampBetweenOrderByTimestampDesc
    List<CongestionDomain> findByBeachIdAndTimestampBetweenOrderByTimestampDesc(String beachId, LocalDateTime start, LocalDateTime end);
}