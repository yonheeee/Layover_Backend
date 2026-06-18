package com.ssafy.layover.common.repository;

import com.ssafy.layover.common.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, String> {

    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    @Transactional
    void deleteByEmail(String email);

    @Transactional
    @Modifying
    @Query("UPDATE EmailVerification e SET e.verified = true WHERE e.email = :email")
    void updateVerifiedByEmail(@Param("email") String email);
}
