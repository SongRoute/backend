// backend/src/main/java/com/app/yeogigangwon/repository/CrowdRecordRepository.java
package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.domain.CongestionDomain;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CongestionRepository extends MongoRepository<CongestionDomain, String> {
    List<CongestionDomain> findByBeachIdAndTimestampBetweenOrderByTimestampAsc(String beachId, LocalDateTime start, LocalDateTime end);

    List<CongestionDomain> findByBeachIdAndTimestampBetweenOrderByTimestampDesc(String beachId, LocalDateTime start, LocalDateTime end);
}
