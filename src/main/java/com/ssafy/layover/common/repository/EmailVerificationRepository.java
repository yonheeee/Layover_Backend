package com.ssafy.layover.common.repository;

import com.ssafy.layover.common.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {

    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    void deleteByEmail(String email);
}
