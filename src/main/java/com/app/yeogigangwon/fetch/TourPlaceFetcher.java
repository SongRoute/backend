package com.app.yeogigangwon.fetch;

import com.app.yeogigangwon.domain.TourPlace;
import com.app.yeogigangwon.service.TourPlaceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourPlaceFetcher {

    private final TourPlaceService tourPlaceService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${tour.api.key}")
    private String tourApiKey; // ✅ 디코딩된 키 (.env에 그대로 넣음)

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void fetchAndSaveTourPlaces() {
        if (tourPlaceService.hasData()) {
            log.info("데이터베이스에 이미 관광지 정보가 존재하여 데이터 초기화를 건너뜁니다.");
            return;
        }

        log.info("관광지 데이터 수집 및 DB 저장을 시작합니다...");
        List<TourPlace> places = fetchTourPlacesFromApi();

        if (!places.isEmpty()) {
            tourPlaceService.saveAllTourPlaces(places);
            log.info("총 {}개의 관광지 데이터가 DB에 성공적으로 저장되었습니다.", places.size());
        } else {
            log.warn("외부 API로부터 수집된 관광지 데이터가 없습니다.");
        }
    }

    public List<TourPlace> fetchTourPlacesFromApi() {
        try {
            int pageNo = 1;
            int numOfRows = 1000; // ✅ 최대치
            List<TourPlace> allPlaces = new ArrayList<>();

            while (true) {
                String responseBody = callTourApiAsString(numOfRows, pageNo, 32);

                if (responseBody == null || responseBody.isBlank()) {
                    log.error("[TourAPI] 응답이 비어있습니다.");
                    break;
                }

                List<TourPlace> places;
                if (isJson(responseBody)) {
                    places = parseFromJson(responseBody);
                } else {
                    places = parseFromXml(responseBody);
                }

                if (places.isEmpty()) break; // 데이터 없으면 종료
                allPlaces.addAll(places);

                log.info("[TourAPI] {} 페이지 수집 완료, 누적 {}건", pageNo, allPlaces.size());

                // totalCount 확인해서 마지막 페이지인지 체크
                Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
                Map<String, Object> body = (Map<String, Object>) ((Map<String, Object>) parsed.get("response")).get("body");
                int totalCount = Integer.parseInt(body.get("totalCount").toString());
                if (pageNo * numOfRows >= totalCount) break;

                pageNo++;
            }

            return allPlaces;
        } catch (Exception e) {
            log.error("TourAPI 호출 또는 데이터 파싱 중 예외가 발생했습니다.", e);
            return List.of();
        }
    }

    // ================== API 호출 ==================
    private String callTourApiAsString(int numOfRows, int pageNo, int areaCode) {
        try {
            // 🔑 디코딩된 키를 .env에 넣어두고 여기서 퍼센트 인코딩
            String encodedKey = URLEncoder.encode(tourApiKey.trim(), StandardCharsets.UTF_8);

            String uriString = "https://apis.data.go.kr"
                    + "/B551011/KorService2/areaBasedList2"
                    + "?numOfRows=" + numOfRows
                    + "&pageNo=" + pageNo
                    + "&MobileOS=WEB"
                    + "&MobileApp=YeogiGangwon"
                    + "&_type=json"
                    + "&areaCode=" + areaCode
                    + "&serviceKey=" + encodedKey;

            URI uri = URI.create(uriString);

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML));
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            log.debug("[TourAPI] GET {}", uriString.replaceAll("serviceKey=[^&]+", "serviceKey=***"));

            ResponseEntity<String> res = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, String.class);
            return res.getBody() == null ? "" : res.getBody().trim();
        } catch (Exception e) {
            log.error("[TourAPI] 호출 실패", e);
            return "";
        }
    }

    // ================== JSON 파싱 ==================
    private List<TourPlace> parseFromJson(String body) throws Exception {
        Map<String, Object> root = objectMapper.readValue(body, new TypeReference<>() {});
        Map<String, Object> response = asMap(root.get("response"));
        Map<String, Object> rbody = asMap(response.get("body"));

        if (rbody == null || rbody.get("items") == null) return List.of();

        Object itemsObj = ((Map<?, ?>) rbody.get("items")).get("item");
        if (itemsObj == null) return List.of();

        List<Map<String, Object>> itemList;
        if (itemsObj instanceof List<?> l) {
            itemList = (List<Map<String, Object>>) l;
        } else if (itemsObj instanceof Map<?, ?> m) {
            itemList = List.of((Map<String, Object>) m);
        } else return List.of();

        return mapToTourPlaces(itemList);
    }

    // ================== XML 파싱 ==================
    private List<TourPlace> parseFromXml(String xml) throws Exception {
        Document doc = parseXml(xml);
        NodeList items = doc.getElementsByTagName("item");
        List<TourPlace> out = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) {
            Element el = (Element) items.item(i);
            String title = textContent(el, "title");
            String addr1 = textContent(el, "addr1");
            String mapx = textContent(el, "mapx");
            String mapy = textContent(el, "mapy");

            if (mapx == null || mapy == null) continue;

            TourPlace place = new TourPlace();
            place.setName(title);
            place.setDescription(addr1 != null ? addr1 : "주소 정보 없음");
            place.setCategory(classifyPlaceType(title));
            place.setCrowdLevel(0);

            try {
                place.setLatitude(Double.parseDouble(mapy));
                place.setLongitude(Double.parseDouble(mapx));
            } catch (NumberFormatException e) {
                log.warn("[TourAPI-XML] 위도/경도 파싱 실패: {}", title);
                continue;
            }
            out.add(place);
        }
        return out;
    }

    // ================== 공통 ==================
    private List<TourPlace> mapToTourPlaces(List<Map<String, Object>> itemList) {
        List<TourPlace> tourPlaces = new ArrayList<>();
        for (Map<String, Object> item : itemList) {
            if (item.get("mapy") == null || item.get("mapx") == null) continue;

            TourPlace place = new TourPlace();
            place.setName((String) item.get("title"));
            place.setDescription((String) item.getOrDefault("addr1", "주소 정보 없음"));
            place.setCategory(classifyPlaceType((String) item.get("title")));
            place.setCrowdLevel(0);

            try {
                place.setLatitude(Double.parseDouble(item.get("mapy").toString()));
                place.setLongitude(Double.parseDouble(item.get("mapx").toString()));
            } catch (NumberFormatException e) {
                log.warn("[TourAPI-JSON] 위도/경도 파싱 실패: {}", item.get("title"));
                continue;
            }
            tourPlaces.add(place);
        }
        return tourPlaces;
    }

    private boolean isJson(String body) { return body.startsWith("{") || body.startsWith("["); }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        DocumentBuilder b = f.newDocumentBuilder();
        return b.parse(new InputSource(new StringReader(xml)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) { return (o instanceof Map) ? (Map<String, Object>) o : null; }

    private String textContent(Element e, String tag) {
        NodeList nl = e.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent();
    }

    private String classifyPlaceType(String name) {
        if (name == null) return "실외";
        List<String> indoorKeywords = Arrays.asList(
                "박물관", "미술관", "전시", "실내", "온천", "사우나", "찜질방",
                "도서관", "카페", "공연장", "체험관", "아쿠아리움", "갤러리",
                "플라네타리움", "영화관", "실내수영장", "키즈카페", "놀이방"
        );
        String text = name.toLowerCase();
        for (String keyword : indoorKeywords) {
            if (text.contains(keyword.toLowerCase())) {
                return "실내";
            }
        }
        return "실외";
    }
}
