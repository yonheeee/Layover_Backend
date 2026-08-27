package com.ssafy.layover.common.config;

import com.ssafy.layover.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestParam("file") MultipartFile file) {
        ResponseEntity<ApiResponse<String>> invalidResponse = validateImage(file);
        if (invalidResponse != null) return invalidResponse;
        return upload(file, "community/images", "이미지");
    }

    @PostMapping("/profile-image")
    public ResponseEntity<ApiResponse<String>> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        ResponseEntity<ApiResponse<String>> invalidResponse = validateImage(file);
        if (invalidResponse != null) return invalidResponse;
        return upload(file, "profile/images", "프로필 이미지");
    }

    /**
     * 스탬프 인증 사진(엽서).
     *
     * <p>예전에는 합성된 엽서를 dataURL 그대로 localStorage에 쌓았다. 장당
     * 300~500KB인데 localStorage 한도가 5~10MB라 20장쯤에서 저장이 조용히
     * 멈췄고, 기기를 바꾸면 도감이 통째로 사라졌다. 이제 여기에 올리고
     * URL만 들고 다닌다.
     *
     * <p>파일 저장은 트랜잭션 롤백에 참여하지 못하므로 스탬프 저장보다
     * 먼저 끝내고 URL만 넘긴다. 스탬프가 실패하면 고아 파일이 남지만,
     * 사진이 비어 있는 도감 항목보다는 낫다.
     */
    @PostMapping("/stamp-photo")
    public ResponseEntity<ApiResponse<String>> uploadStampPhoto(@RequestParam("file") MultipartFile file) {
        ResponseEntity<ApiResponse<String>> invalidResponse = validateImage(file);
        if (invalidResponse != null) return invalidResponse;
        return upload(file, "stamps/photos", "인증 사진");
    }

    @PostMapping("/file")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("파일이 비어 있습니다."));
        }
        return upload(file, "community/files", "파일");
    }

    private ResponseEntity<ApiResponse<String>> upload(MultipartFile file, String directory, String label) {
        try {
            return ResponseEntity.ok(ApiResponse.success(fileStorageService.upload(file, directory)));
        } catch (FileStorageException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(label + " 업로드에 실패했습니다: " + e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponse<String>> validateImage(MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("파일이 비어 있습니다."));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("이미지 파일만 업로드할 수 있습니다."));
        }
        return null;
    }
}
