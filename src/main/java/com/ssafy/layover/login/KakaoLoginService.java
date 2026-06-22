package com.ssafy.layover.login;

import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.jwt.JwtUtil;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.login.dto.KakaoUserInfo;
import com.ssafy.layover.login.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;
    
    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.admin-key}")
    private String adminKey;

    public String getKakaoAuthUrl() {
        return "https://kauth.kakao.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri;
    }

    public String getKakaoToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        params.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token",
                request,
                Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    public KakaoUserInfo getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                request,
                KakaoUserInfo.class
        );

        return response.getBody();
    }

    public Map<String, Object> processKakaoLogin(String code) {
        String kakaoAccessToken = getKakaoToken(code);
        KakaoUserInfo userInfo = getKakaoUserInfo(kakaoAccessToken);

        String kakaoId = String.valueOf(userInfo.getKakaoId());
        String email = userInfo.getEmail() != null ? userInfo.getEmail() : kakaoId + "@kakao.com";
        String nickname = userInfo.getNickname() != null ? userInfo.getNickname() : "카카오유저";

        boolean[] isNew = {false};

        userRepository.findByKakaoId(kakaoId).ifPresent(existing -> {
            if (existing.getDeletedAt() != null) {
                throw new RuntimeException("탈퇴한 회원입니다.");
            }
        });

        User user = userRepository.findByKakaoId(kakaoId).orElseGet(() -> {
        	isNew[0] = true;
        	User newUser = User.builder()
                    .username(nickname)
                    .email(email)
                    .kakaoId(kakaoId)
                    .build();
            return userRepository.save(newUser);
        });
        
        // 기존 유저인데 프로필 미입력인 경우도 needsProfile=true
        boolean needsProfile = isNew[0] || user.getRealName() == null;

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "needsProfile", needsProfile
                );
    }

    public void unlinkKakao(String kakaoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", kakaoId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        restTemplate.postForEntity(
                "https://kapi.kakao.com/v1/user/unlink",
                request,
                Map.class
        );
    }
}
