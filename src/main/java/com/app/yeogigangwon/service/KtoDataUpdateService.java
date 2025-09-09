package com.app.yeogigangwon.service;

import com.app.yeogigangwon.domain.KtoCongestion;
import com.app.yeogigangwon.domain.KtoPlaceMap;
import com.app.yeogigangwon.repository.KtoCongestionRepository;
import com.app.yeogigangwon.repository.KtoPlaceMapRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class KtoDataUpdateService {

    private final KtoCongestionRepository ktoCongestionRepository;
    private final KtoPlaceMapRepository ktoPlaceMapRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kto.api.key}")
    private String ktoApiKey; // ✅ 디코딩된 키 사용

    private static final List<String> GANGWON_SIGUNGU_CODES = List.of(
            "51110","51130","51150","51170","51190","51210","51230","51720",
            "51730","51750","51760","51770","51780","51790","51800","51810",
            "51820","51830"
    );

    private AtomicLong lastKtoIdCounter = null;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void updateKtoCongestionData() {
        log.info("[KTO] 스케줄 시작");
        try {
            List<KtoCongestion> latestData = fetchDataFromKtoApi();
            if (!latestData.isEmpty()) {
                ktoCongestionRepository.deleteAllInBatch();
                ktoCongestionRepository.saveAll(latestData);
                log.info("[KTO] 저장 완료: {}건", latestData.size());
            } else {
                log.warn("[KTO] API에서 수집된 데이터가 없습니다!");
            }
        } catch (Exception e) {
            log.error("[KTO] 업데이트 실패!", e);
        }
    }

    @Transactional
    public String getOrCreateInternalId(String placeName) {
        return ktoPlaceMapRepository.findByPlaceName(placeName)
                .map(KtoPlaceMap::getInternalId)
                .orElseGet(() -> {
                    if (lastKtoIdCounter == null) {
                        long maxId = ktoPlaceMapRepository.findAll().stream()
                                .map(p -> p.getInternalId().replace("kto_", ""))
                                .filter(s -> s.matches("\\d+"))
                                .mapToLong(Long::parseLong)
                                .max().orElse(0L);
                        lastKtoIdCounter = new AtomicLong(maxId);
                    }
                    String newId = "kto_" + lastKtoIdCounter.incrementAndGet();
                    ktoPlaceMapRepository.save(new KtoPlaceMap(placeName, newId));
                    log.info("[KTO] 신규 관광지 매핑: '{}' -> '{}'", placeName, newId);
                    return newId;
                });
    }

    /** 단일 호출 (저장 X, 테스트용) */
    public Map<String, Object> updateOnceForDebug(String sigungu, String tAtsNm) {
        try {
            String body = callKtoApiAsString(sigungu, 10, tAtsNm);
            if (isJson(body)) {
                return summarizeFromJson(body);
            } else {
                return summarizeFromXml(body);
            }
        } catch (Exception ex) {
            log.error("[KTO] once 호출 실패", ex);
            return Map.of(
                    "error", ex.getClass().getSimpleName() + ": " + nonNull(ex.getMessage()),
                    "signgu", sigungu,
                    "tAtsNm", tAtsNm
            );
        }
    }

    /** 단일 호출 원문 반환 */
    public String getRawOnce(String sigungu, String tAtsNm) {
        try {
            return callKtoApiAsString(sigungu, 10, tAtsNm);
        } catch (Exception ex) {
            log.error("[KTO] raw 호출 실패", ex);
            return "ERROR: " + ex.getClass().getSimpleName() + ": " + nonNull(ex.getMessage());
        }
    }

    // ================== 내부 수집 로직 ==================
    private List<KtoCongestion> fetchDataFromKtoApi() throws Exception {
        List<KtoCongestion> all = new ArrayList<>();
        for (String sigungu : GANGWON_SIGUNGU_CODES) {
            String body = callKtoApiAsString(sigungu, 500, null);
            List<KtoCongestion> list;
            if (isJson(body)) {
                list = parseItemsFromJson(body);
            } else {
                list = parseItemsFromXml(body);
            }
            all.addAll(list);
            Thread.sleep(80);
        }
        return all;
    }

    // ================== 핵심: API 호출 ==================
    private String callKtoApiAsString(String sigungu, int numOfRows, String tAtsNm) {
        // 🔑 디코딩된 키 사용
        String decodedKey = (ktoApiKey == null) ? "" : ktoApiKey.trim();

        // serviceKey는 반드시 퍼센트 인코딩
        String encodedKey = URLEncoder.encode(decodedKey, StandardCharsets.UTF_8);

        // tAtsNm(관광지명)은 있을 때만 인코딩해서 추가
        String nameParam = "";
        if (tAtsNm != null && !tAtsNm.isBlank()) {
            nameParam = "&tAtsNm=" + URLEncoder.encode(tAtsNm, StandardCharsets.UTF_8);
        }

        // 매뉴얼 순서대로 파라미터 붙여서 최종 URL 구성
        String uriString = "https://apis.data.go.kr"
                + "/B551011/TatsCnctrRateService/tatsCnctrRatedList"
                + "?serviceKey=" + encodedKey
                + "&pageNo=1"
                + "&numOfRows=" + numOfRows
                + "&MobileOS=WEB"
                + "&MobileApp=YeogiGangwon"
                + "&areaCd=51"
                + "&signguCd=" + sigungu
                + nameParam
                + "&_type=json";

        URI uri = URI.create(uriString);

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML));
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        log.debug("[KTO] GET {}", uriString.replaceAll("serviceKey=[^&]+", "serviceKey=***"));

        ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
        return res.getBody() == null ? "" : res.getBody().trim();
    }


    // ================== JSON/XML 파싱 ==================
    private boolean isJson(String body) { return body.startsWith("{") || body.startsWith("["); }

    private Map<String, Object> summarizeFromJson(String body) throws Exception {
        Map<String, Object> root = objectMapper.readValue(body, new TypeReference<>() {});
        Map<String, Object> response = asMap(root.get("response"));
        Map<String, Object> header = asMap(response.get("header"));
        Map<String, Object> rbody = asMap(response.get("body"));
        String resultCode = val(header, "resultCode");
        String totalCount = val(rbody, "totalCount");
        int itemsCount = countItemsFromJson(rbody);
        return Map.of(
                "format", "json",
                "resultCode", nonNull(resultCode),
                "totalCount", nonNull(totalCount),
                "itemsCount", itemsCount
        );
    }

    private int countItemsFromJson(Map<String, Object> rbody) {
        if (rbody == null) return 0;
        Object itemsObj = rbody.get("items");
        if (!(itemsObj instanceof Map)) return 0;
        Object item = ((Map<?, ?>) itemsObj).get("item");
        if (item instanceof List<?> l) return l.size();
        if (item instanceof Map<?, ?>) return 1;
        return 0;
    }

    private List<KtoCongestion> parseItemsFromJson(String body) throws Exception {
        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");
        Map<String, Object> root = objectMapper.readValue(body, new TypeReference<>() {});
        Map<String, Object> response = asMap(root.get("response"));
        Map<String, Object> rbody = asMap(response.get("body"));
        Object itemsObj = rbody == null ? null : rbody.get("items");
        if (!(itemsObj instanceof Map)) return List.of();

        Object item = ((Map<?, ?>) itemsObj).get("item");
        List<Map<String, Object>> list;
        if (item instanceof List<?> l) list = (List<Map<String, Object>>) l;
        else if (item instanceof Map<?, ?> m) list = List.of((Map<String, Object>) m);
        else return List.of();

        List<KtoCongestion> out = new ArrayList<>();
        for (Map<String, Object> it : list) {
            String name = String.valueOf(it.get("tAtsNm"));
            String base = String.valueOf(it.get("baseYmd"));
            String rate = String.valueOf(it.get("cnctrRate"));
            if (name == null || base == null || rate == null) continue;
            String placeId = getOrCreateInternalId(name);
            out.add(new KtoCongestion(placeId, LocalDate.parse(base, ymd), Double.parseDouble(rate)));
        }
        return out;
    }

    private Map<String, Object> summarizeFromXml(String xml) throws Exception {
        Document doc = parseXml(xml);
        if (doc.getElementsByTagName("cmmMsgHeader").getLength() > 0) {
            String errMsg  = textContent(doc, "errMsg");
            String authMsg = textContent(doc, "returnAuthMsg");
            String reason  = textContent(doc, "returnReasonCode");
            return Map.of(
                    "format", "xml",
                    "error", nonNull(authMsg),
                    "errMsg", nonNull(errMsg),
                    "resultCode", nonNull(reason)
            );
        }
        String resultCode = textContent(doc, "resultCode");
        String totalCount = textContent(doc, "totalCount");
        int itemsCount = doc.getElementsByTagName("item").getLength();
        return Map.of(
                "format", "xml",
                "resultCode", nonNull(resultCode),
                "totalCount", nonNull(totalCount),
                "itemsCount", itemsCount
        );
    }

    private List<KtoCongestion> parseItemsFromXml(String xml) throws Exception {
        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");
        List<KtoCongestion> out = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String name = textContent(el, "tAtsNm");
            String base = textContent(el, "baseYmd");
            String rate = textContent(el, "cnctrRate");
            if (name == null || base == null || rate == null) continue;
            String placeId = getOrCreateInternalId(name);
            out.add(new KtoCongestion(placeId, LocalDate.parse(base, ymd), Double.parseDouble(rate)));
        }
        return out;
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        DocumentBuilder b = f.newDocumentBuilder();
        return b.parse(new InputSource(new StringReader(xml)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) { return (o instanceof Map) ? (Map<String, Object>) o : null; }

    private String textContent(Document d, String tag) {
        NodeList nl = d.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent();
    }

    private String textContent(Element e, String tag) {
        NodeList nl = e.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent();
    }

    private String val(Map<String, Object> m, String k) { return m == null ? null : String.valueOf(m.get(k)); }

    private String nonNull(String s) { return s == null ? "" : s; }
}
