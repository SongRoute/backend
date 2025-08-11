package com.app.yeogigangwon.repository;

import com.app.yeogigangwon.entity.Congestion;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CongestionRepository extends MongoRepository<Congestion, String> {}
