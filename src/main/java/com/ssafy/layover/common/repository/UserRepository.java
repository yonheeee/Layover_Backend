package com.ssafy.layover.common.repository;

import com.ssafy.layover.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByRealNameAndBirthDateAndPhone(String realName, LocalDate birthDate, String phone);
}
