package com.ssafy.layover.common;

import com.ssafy.layover.common.dto.ApiResponse;
import com.ssafy.layover.common.exception.DuplicateException;
import com.ssafy.layover.common.exception.ExternalApiException;
import com.ssafy.layover.common.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 서버가 정상 동작할 수 없는 상태.
     *
     * <p>예전에는 409 Conflict 에 예외 메시지를 그대로 실어 보냈다. 그래서
     * 시드를 넣지 않은 서버에서 사진을 저장하면 화면에
     * "character_seed.sql 을 적용했는지 확인하세요" 라는 내부 메시지가 그대로
     * 떴다. 프론트엔드는 409 를 "오늘 이미 방문한 장소"로 다루고 있어서
     * 상태 코드의 의미까지 겹쳤다.
     *
     * <p>중복은 {@link DuplicateException} 과 {@link DataIntegrityViolationException}
     * 이 맡는다. 여기로 오는 건 설정이나 데이터가 잘못된 서버 오류이므로
     * 500 으로 내보내고 원인은 로그에만 남긴다.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        log.error("Illegal state", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("서버 오류가 발생했습니다."));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicate(DuplicateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(e.getMessage()));
    }

    /**
     * DB 유니크 제약 위반.
     *
     * <p>동시 요청이 애플리케이션 중복 검사를 함께 통과했을 때 최종적으로
     * 걸러지는 지점이다. 예를 들어 스탬프 저장 버튼을 빠르게 두 번 누르면
     * uq_stamps_user_place_day 가 두 번째를 막는다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("[DB] 제약 조건 위반: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("이미 처리된 요청입니다."));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalApi(ExternalApiException e) {
        log.error("External API call failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "잘못된 요청입니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.fail("파일 용량이 너무 큽니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("서버 오류가 발생했습니다."));
    }
}
