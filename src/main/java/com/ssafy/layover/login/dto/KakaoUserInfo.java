package com.ssafy.layover.login.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoUserInfo {

    @JsonProperty("id")
    private Long kakaoId;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.email : null;
    }

    public String getNickname() {
        if (kakaoAccount == null || kakaoAccount.profile == null) return null;
        return kakaoAccount.profile.nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {
        private String email;
        private Profile profile;
    }

    @Getter
    @NoArgsConstructor
    public static class Profile {
        private String nickname;
    }
}
