package com.ssafy.layover.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "real_name", length = 50)
    private String realName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "kakao_id", unique = true, length = 100)
    private String kakaoId;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Builder.Default
    @Column(name = "stamp_count", nullable = false)
    private int stampCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @Column(name = "role", nullable = false)
    private String role = "USER";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
