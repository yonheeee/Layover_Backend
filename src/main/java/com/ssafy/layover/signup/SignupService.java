package com.ssafy.layover.signup;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.common.service.EmailVerificationService;
import com.ssafy.layover.signup.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class SignupService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public ApiResponse<?> checkEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            return ApiResponse.fail("이미 사용 중인 이메일입니다.");
        }
        return ApiResponse.success("사용 가능한 이메일입니다.", null);
    }

    public ApiResponse<?> sendEmailCode(String email) {
        String code = String.format("%06d", new Random().nextInt(900000) + 100000);
        emailVerificationService.saveCode(email, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Layover] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n5분 이내에 입력해 주세요.");
        mailSender.send(message);

        return ApiResponse.success("인증 코드가 발송되었습니다.", null);
    }

    public ApiResponse<?> verifyEmailCode(String email, String code) {
        String result = emailVerificationService.verifyCode(email, code);
        return switch (result) {
            case "EXPIRED" -> ApiResponse.fail("인증 코드가 만료되었습니다.");
            case "INVALID" -> ApiResponse.fail("인증 코드가 일치하지 않습니다.");
            default -> ApiResponse.success("이메일 인증이 완료되었습니다.", null);
        };
    }

    public ApiResponse<?> signup(SignupRequest request) {
        if (!emailVerificationService.isVerified(request.getEmail())) {
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
        emailVerificationService.deleteVerification(request.getEmail());

        // email_verifications 테이블은 JPA 자동 삭제 안됨. DB에서 수동 실행 필요:
        // DROP TABLE email_verifications;
        return ApiResponse.success("회원가입이 완료되었습니다.", null);
    }
}
