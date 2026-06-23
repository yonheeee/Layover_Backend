package com.ssafy.layover.train;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class KorailApiClient {

    private static final String BASE_URL =
            "https://openapis.korail.com/samples/public/call/run/travelerTrainRunInfo";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TrainResponse> fetchTrains(String stnNm, String date) {
        try {
        	String url = BASE_URL
        	        + "?cond%5Brun_ymd%3A%3AEQ%5D=" + date
        	        + "&cond%5Bstn_nm%3A%3AEQ%5D=" + URLEncoder.encode(stnNm, StandardCharsets.UTF_8)
        	        + "&numOfRows=100"
        	        + "&pageNo=1";

            log.info("코레일 API 요청: {}", url);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            log.info("코레일 API 응답 코드: {}", status);

            if (status == 302) {
                String location = conn.getHeaderField("Location");
                log.info("코레일 API 리다이렉트 → {}", location);
                conn.disconnect();

                conn = (HttpURLConnection) new URL(location).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                status = conn.getResponseCode();
                log.info("리다이렉트 후 응답 코드: {}", status);
            }

            if (status != 200) {
                log.error("코레일 API 실패 응답 코드: {}", status);
                return List.of();
            }

            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("코레일 API 응답 바디 (앞 200자): {}", body.substring(0, Math.min(200, body.length())));

            KorailRoot root = objectMapper.readValue(body, KorailRoot.class);

            if (root == null
                    || root.response() == null
                    || root.response().body() == null
                    || root.response().body().items() == null
                    || root.response().body().items().item() == null) {
                return List.of();
            }

            return root.response().body().items().item().stream()
                    .map(item -> new TrainResponse(
                            item.trn_no(),
                            toHhmm(item.trn_dptre_dt()),
                            toHhmm(item.trn_arvl_dt()),
                            item.arvl_stn_nm(),
                            item.dptre_stn_nm(),
                            item.stn_nm(),
                            item.mrnt_nm()
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("코레일 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String toHhmm(String datetime) {
        if (datetime == null || !datetime.contains(" ")) return "";
        String timePart = datetime.split(" ")[1];
        return timePart.length() >= 5 ? timePart.substring(0, 5) : timePart;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KorailRoot(KorailResponse response) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KorailResponse(KorailBody body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KorailBody(KorailItems items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KorailItems(List<KorailItem> item) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KorailItem(
            String arvl_stn_nm,
            String dptre_stn_nm,
            String stn_nm,
            String trn_no,
            String trn_dptre_dt,
            String trn_arvl_dt,
            String mrnt_nm
    ) {}
}