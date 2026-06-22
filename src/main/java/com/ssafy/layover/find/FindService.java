package com.ssafy.layover.find;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.entity.User;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.common.service.EmailVerificationService;
import com.ssafy.layover.find.dto.FindIdRequest;
import com.ssafy.layover.find.dto.FindIdResponse;
import com.ssafy.layover.find.dto.FindPwResetRequest;
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
public class FindService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final JavaMailSender mailSender;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public ApiResponse<FindIdResponse> findId(FindIdRequest request) {
        User user = userRepository
                .findByRealNameAndBirthDateAndPhone(request.getRealName(), request.getBirthDate(), request.getPhone())
                .orElse(null);

        if (user == null) {
            return ApiResponse.fail("일치하는 회원 정보를 찾을 수 없습니다.");
        }

        if (user.getKakaoId() != null) {
            return ApiResponse.fail("카카오 계정으로 가입된 회원입니다. 카카오 로그인을 이용해 주세요.");
        }

        String[] parts = user.getEmail().split("@");
        String maskedLocal = parts[0].substring(0, Math.min(3, parts[0].length())) + "***";
        String maskedEmail = maskedLocal + "@" + parts[1];

        return ApiResponse.success(new FindIdResponse(maskedEmail, user.getCreatedAt()));
    }

    public ApiResponse<?> sendPasswordResetCode(String email) {
        if (!userRepository.existsByEmail(email)) {
            return ApiResponse.fail("등록되지 않은 이메일입니다.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.getKakaoId() != null) {
            return ApiResponse.fail("카카오 계정으로 가입된 회원입니다. 카카오 로그인을 이용해 주세요.");
        }

        String code = String.format("%06d", new Random().nextInt(900000) + 100000);
        emailVerificationService.saveCode(email, code);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Layover] 비밀번호 재설정 인증 코드");
        message.setText("인증 코드: " + code + "\n5분 이내에 입력해 주세요.");
        mailSender.send(message);

        return ApiResponse.success("인증 코드가 발송되었습니다.", null);
    }

    public ApiResponse<?> verifyPasswordResetCode(String email, String code) {
        String result = emailVerificationService.verifyCode(email, code);
        return switch (result) {
            case "EXPIRED" -> ApiResponse.fail("인증 코드가 만료되었습니다.");
            case "INVALID" -> ApiResponse.fail("인증 코드가 일치하지 않습니다.");
            default -> ApiResponse.success("이메일 인증이 완료되었습니다.", null);
        };
    }

    public ApiResponse<?> resetPassword(FindPwResetRequest request) {
        if (!emailVerificationService.isVerified(request.getEmail())) {
            return ApiResponse.fail("이메일 인증이 완료되지 않았습니다.");
        }

        userRepository.updatePasswordByEmail(
                request.getEmail(),
                bCryptPasswordEncoder.encode(request.getNewPassword())
        );
        emailVerificationService.deleteVerification(request.getEmail());

        return ApiResponse.success("비밀번호가 변경되었습니다.", null);
    }
}
