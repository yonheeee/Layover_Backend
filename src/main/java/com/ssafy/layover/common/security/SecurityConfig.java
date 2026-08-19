package com.ssafy.layover.common.security;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ssafy.layover.common.jwt.JwtFilter;
import com.ssafy.layover.common.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${place.sync.public-enabled:false}")
    private boolean placeSyncPublicEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                    .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers("/api/signup/**", "/api/login/**", "/api/find/**",
                            "/api/trains/**", "/uploads/**", "/api/auth/refresh").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/places/**").permitAll();

                // 관광지 전체 동기화는 TourAPI/Kakao 일일 호출 한도를 소모하는 무거운 작업이다.
                // 예전에는 anyRequest().authenticated() 에만 걸려 있어 로그인만 하면 누구나 실행할 수 있었다.
                if (placeSyncPublicEnabled) {
                    // 로컬에서 DB를 비우고 재수집할 때만 켠다. 배포 환경에서는 false를 유지해야 한다.
                    auth.requestMatchers(HttpMethod.POST, "/api/admin/places/sync", "/api/places/sync").permitAll();
                } else {
                    auth.requestMatchers(HttpMethod.POST, "/api/admin/places/sync", "/api/places/sync")
                        .hasRole("ADMIN");
                }

                auth.requestMatchers(HttpMethod.GET, "/api/posts").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/posts/my").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                    .requestMatchers("/api/notices/**").permitAll()
                    .requestMatchers("/api/faq/**").permitAll()
                    .anyRequest().authenticated();
            })
            .addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
