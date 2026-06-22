package com.ssafy.layover.train;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KorailApiClient {

    private static final String BASE_URL =
            "https://openapis.korail.com/samples/public/call/run/travelerTrainRunPlan";

    private final RestTemplate restTemplate;

    public List<TrainResponse> fetchTrains(String stnCd, String date) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("cond[dptre_stn_cd::EQ]", stnCd)
                    .queryParam("cond[run_ymd::EQ]", date)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 100)
                    .build()
                    .encode()
                    .toUri();

            log.info("코레일 API 요청: {}", uri);

            KorailRoot root = restTemplate.getForObject(uri, KorailRoot.class);

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
                            toHhmm(item.trn_plan_dptre_dt()),
                            toHhmm(item.trn_plan_arvl_dt()),
                            item.arvl_stn_nm(),
                            item.dptre_stn_nm(),
                            item.dptre_stn_cd()
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("코레일 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // "2026-06-22 05:55:00.0" → "05:55"
    private String toHhmm(String datetime) {
        if (datetime == null || !datetime.contains(" ")) return "";
        String timePart = datetime.split(" ")[1];
        return timePart.length() >= 5 ? timePart.substring(0, 5) : timePart;
    }

    // ── JSON 역직렬화용 내부 레코드 ──────────────────────────────────

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
            String dptre_stn_cd,
            String trn_no,
            String trn_plan_dptre_dt,
            String trn_plan_arvl_dt
    ) {}
}
