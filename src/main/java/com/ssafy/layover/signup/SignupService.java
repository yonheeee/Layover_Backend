package com.ssafy.layover.signup;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.entity.EmailVerification;
import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.repository.EmailVerificationRepository;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.signup.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public ApiResponse<?> checkEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            return ApiResponse.fail("이미 사용 중인 이메일입니다.");
        }
        return ApiResponse.success("사용 가능한 이메일입니다.", null);
    }

    public ApiResponse<?> sendEmailCode(String email) {
        emailVerificationRepository.deleteByEmail(email);

        String code = String.format("%06d", new Random().nextInt(900000) + 100000);
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        emailVerificationRepository.save(verification);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Layover] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n5분 이내에 입력해 주세요.");
        mailSender.send(message);

        return ApiResponse.success("인증 코드가 발송되었습니다.", null);
    }

    public ApiResponse<?> verifyEmailCode(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElse(null);

        if (verification == null) {
            return ApiResponse.fail("인증 코드를 먼저 발송해 주세요.");
        }
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            return ApiResponse.fail("인증 코드가 만료되었습니다.");
        }
        if (!verification.getCode().equals(code)) {
            return ApiResponse.fail("인증 코드가 일치하지 않습니다.");
        }

        emailVerificationRepository.updateVerifiedByEmail(email);
        return ApiResponse.success("이메일 인증이 완료되었습니다.", null);
    }

    public ApiResponse<?> signup(SignupRequest request) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail())
                .orElse(null);

        if (verification == null || !verification.isVerified()) {
            return ApiResponse.fail("이메일 인증이 완료되지 않았습니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getName())
                .realName(request.getName())
                .birthDate(request.getBirthDate())
                .phone(request.getPhone())
                .passwordHash(bCryptPasswordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);
        emailVerificationRepository.deleteByEmail(request.getEmail());

        return ApiResponse.success("회원가입이 완료되었습니다.", null);
    }
}
