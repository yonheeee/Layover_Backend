package com.ssafy.layover.signup;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.repository.UserRepository;
import com.ssafy.layover.common.service.EmailVerificationService;
import com.ssafy.layover.signup.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SignupServiceTest {

    private UserRepository userRepository;
    private JavaMailSender mailSender;
    private EmailVerificationService verificationService;
    private SignupService signupService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        mailSender = mock(JavaMailSender.class);
        verificationService = new EmailVerificationService(null);
        ReflectionTestUtils.setField(verificationService, "storageType", "memory");
        signupService = new SignupService(
                userRepository,
                verificationService,
                mailSender,
                new BCryptPasswordEncoder()
        );
    }

    @Test
    void sendsAndVerifiesCodeThenSignsUp() {
        String email = "openapi-code-test@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        ApiResponse<?> available = signupService.checkEmail(email);
        assertThat(available.isSuccess()).isTrue();

        ApiResponse<?> sent = signupService.sendEmailCode(email);
        assertThat(sent.isSuccess()).isTrue();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        String messageText = messageCaptor.getValue().getText();
        String code = messageText.replaceFirst("(?s).*인증 코드: (\\d{6}).*", "$1");
        assertThat(code).hasSize(6);

        ApiResponse<?> verified = signupService.verifyEmailCode(email, code);
        assertThat(verified.isSuccess()).isTrue();

        ApiResponse<?> signedUp = signupService.signup(request(email));
        assertThat(signedUp.isSuccess()).isTrue();
        verify(userRepository).save(any());
        assertThat(verificationService.isVerified(email)).isFalse();
    }

    @Test
    void rejectsWrongCodeAndExpiredOrMissingCode() {
        String email = "openapi-invalid-code@example.com";

        signupService.sendEmailCode(email);

        ApiResponse<?> wrong = signupService.verifyEmailCode(email, "000000");
        assertThat(wrong.isSuccess()).isFalse();
        assertThat(wrong.getMessage()).isEqualTo("인증 코드가 일치하지 않습니다.");

        ApiResponse<?> missing = signupService.verifyEmailCode("openapi-missing@example.com", "123456");
        assertThat(missing.isSuccess()).isFalse();
        assertThat(missing.getMessage()).isEqualTo("인증 코드가 만료되었습니다.");
    }

    @Test
    void rejectsSignupBeforeEmailVerification() {
        String email = "openapi-unverified@example.com";

        ApiResponse<?> result = signupService.signup(request(email));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("이메일 인증이 완료되지 않았습니다.");
        verify(userRepository, never()).save(any());
    }

    private SignupRequest request(String email) {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", email);
        ReflectionTestUtils.setField(request, "password", "2026openapi!");
        ReflectionTestUtils.setField(request, "name", "심사용 계정");
        ReflectionTestUtils.setField(request, "birthDate", LocalDate.of(2000, 1, 1));
        ReflectionTestUtils.setField(request, "phone", "01012345678");
        return request;
    }
}
