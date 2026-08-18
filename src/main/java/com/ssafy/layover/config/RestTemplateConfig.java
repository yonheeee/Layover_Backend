package com.ssafy.layover.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 API 호출용 RestTemplate 설정.
 *
 * <p>코스 1회 생성은 카카오 도보/차량/대중교통 경로 API를 구간마다 동기 호출하므로
 * 요청 1건이 외부 호출 수십 회로 번진다. 타임아웃이 없으면 외부 API 한 곳만 느려져도
 * 톰캣 스레드가 무한 대기하고, 동시 접속이 몰리면 스레드 풀이 그대로 고갈된다.
 * 따라서 모든 외부 호출에 connect/response 타임아웃과 커넥션 풀을 강제한다.
 *
 * <ul>
 *   <li>{@code restTemplate} (기본): 카카오/TourAPI/TAGO/버스 등 빠른 응답을 기대하는 API용.</li>
 *   <li>{@code aiRestTemplate}: LLM 코스 추천용. 생성 시간이 길어 별도의 긴 타임아웃을 쓴다.</li>
 * </ul>
 */
@Configuration
public class RestTemplateConfig {

    @Value("${http.client.connect-timeout-seconds:3}")
    private long connectTimeoutSeconds;

    @Value("${http.client.read-timeout-seconds:5}")
    private long readTimeoutSeconds;

    @Value("${http.client.connection-request-timeout-seconds:2}")
    private long connectionRequestTimeoutSeconds;

    @Value("${http.client.max-total:100}")
    private int maxTotal;

    @Value("${http.client.max-per-route:30}")
    private int maxPerRoute;

    @Value("${ai.course.connect-timeout-seconds:3}")
    private long aiConnectTimeoutSeconds;

    @Value("${ai.course.read-timeout-seconds:10}")
    private long aiReadTimeoutSeconds;

    @Primary
    @Bean
    public RestTemplate restTemplate() {
        return build(connectTimeoutSeconds, readTimeoutSeconds, maxTotal, maxPerRoute);
    }

    /**
     * AI 코스 추천 전용. 응답 생성에 수 초가 걸리므로 기본 RestTemplate보다 긴 타임아웃을 사용한다.
     * 동시에, 무한 대기는 여전히 허용하지 않는다.
     */
    @Bean(name = "aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        return build(aiConnectTimeoutSeconds, aiReadTimeoutSeconds, 20, 20);
    }

    private RestTemplate build(long connectSeconds, long readSeconds, int poolMaxTotal, int poolMaxPerRoute) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(Math.max(1, connectSeconds)))
                .setSocketTimeout(Timeout.ofSeconds(Math.max(1, readSeconds)))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(Math.max(1, poolMaxTotal))
                .setMaxConnPerRoute(Math.max(1, poolMaxPerRoute))
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                // 풀에 여유 커넥션이 없을 때도 무한 대기하지 않는다.
                .setConnectionRequestTimeout(Timeout.ofSeconds(Math.max(1, connectionRequestTimeoutSeconds)))
                .setResponseTimeout(Timeout.ofSeconds(Math.max(1, readSeconds)))
                .build();

        HttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(TimeValue.ofMinutes(1))
                .evictExpiredConnections()
                .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
