package com.app.yeogigangwon.controller;

import com.app.yeogigangwon.domain.CctvInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/cctv")
public class CctvController {

    private final List<CctvInfo> cctvList = Stream.of(
            new CctvInfo("cctv001", "남항진", "강릉시", "http://220.95.232.18/camera/4_0.jpg"),
            new CctvInfo("cctv002", "강문", "강릉시", "http://220.95.232.18/camera/51_0.jpg"),
            new CctvInfo("cctv003", "경포", "강릉시", "http://220.95.232.18/camera/52_0.jpg"),
            new CctvInfo("cctv004", "소돌", "강릉시", "http://220.95.232.18/camera/53_0.jpg"),
            new CctvInfo("cctv005", "염전", "강릉시", "http://220.95.232.18/camera/54_0.jpg"),
            new CctvInfo("cctv006", "영진", "강릉시", "http://220.95.232.18/camera/55_0.jpg"),
            new CctvInfo("cctv007", "정동진", "강릉시", "http://220.95.232.18/camera/56_0.jpg"),
            new CctvInfo("cctv008", "공현진", "고성군", "http://220.95.232.18/camera/57_0.jpg"),
            new CctvInfo("cctv009", "교암", "고성군", "http://220.95.232.18/camera/58_0.jpg"),
            new CctvInfo("cctv010", "봉포", "속초시", "http://220.95.232.18/camera/59_0.jpg"),
            new CctvInfo("cctv011", "초도", "고성군", "http://220.95.232.18/camera/60_0.jpg"),
            new CctvInfo("cctv012", "영랑", "속초시", "http://220.95.232.18/camera/69_0.jpg"),
            new CctvInfo("cctv013", "하맹방", "삼척시", "http://220.95.232.18/camera/62_0.jpg"),
            new CctvInfo("cctv014", "원평", "삼척시", "http://220.95.232.18/camera/65_0.jpg"),
            new CctvInfo("cctv015", "문암·초곡", "삼척시", "http://220.95.232.18/camera/88_0.jpg")
    ).collect(Collectors.toList());

    @GetMapping("/list")
    public List<CctvInfo> getCctvList() {
        return cctvList;
    }
}