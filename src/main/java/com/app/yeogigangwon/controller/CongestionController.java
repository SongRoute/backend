// backend/src/main/java/com/app/yeogigangwon/controller/CongestionController.java
package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.dto.CongestionDto;
import com.app.yeogigangwon.service.CongestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;

    @GetMapping("/status")
    public ResponseEntity<List<CongestionDto.CrowdStatus>> getCrowdStatus() {
        List<CongestionDto.CrowdStatus> statuses = congestionService.getCrowdStatus();
        return ResponseEntity.ok(statuses);
    }
}
