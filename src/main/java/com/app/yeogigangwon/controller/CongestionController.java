package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.dto.CongestionRequest;
import com.app.yeogigangwon.dto.CongestionResponse;
import com.app.yeogigangwon.service.CongestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/congestion")
@RequiredArgsConstructor
public class CongestionController {

    private final CongestionService congestionService;

    @PostMapping
    public ResponseEntity<CongestionResponse> analyze(@RequestBody CongestionRequest request) {
        return ResponseEntity.ok(congestionService.analyzeCongestion(request));
    }
}
