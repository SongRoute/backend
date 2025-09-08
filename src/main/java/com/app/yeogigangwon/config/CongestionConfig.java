package com.app.yeogigangwon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// CongestionConfig: 애플리케이션의 스케줄링 설정을 활성화하는 클래스
@Configuration
@EnableScheduling // 주기적으로 작업을 실행시키는 기능을 활성화
public class CongestionConfig {
}