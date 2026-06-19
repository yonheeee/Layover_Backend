package com.ssafy.layover.common.repository;

import com.ssafy.layover.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByKakaoId(String kakaoId);

    Optional<User> findByRealNameAndBirthDateAndPhone(String realName, LocalDate birthDate, String phone);

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.email = :email")
    void updatePasswordByEmail(@Param("email") String email, @Param("passwordHash") String passwordHash);
}
