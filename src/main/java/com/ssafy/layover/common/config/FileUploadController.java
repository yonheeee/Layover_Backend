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
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("파일이 비어 있습니다."));
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("이미지 파일만 업로드할 수 있습니다."));
        }
        return upload(file, "community/images", "이미지");
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
}
