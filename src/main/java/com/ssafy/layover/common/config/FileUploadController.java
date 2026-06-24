package com.ssafy.layover.common.config;

import com.ssafy.layover.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return uploadToLocal(file, "이미지");
    }

    @PostMapping("/file")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return uploadToLocal(file, "파일");
    }

    private ResponseEntity<ApiResponse<String>> uploadToLocal(MultipartFile file, String label) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("파일이 비어있습니다."));
        }
        try {
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + ext;
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
            return ResponseEntity.ok(ApiResponse.success("/uploads/" + filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail(label + " 업로드 실패: " + e.getMessage()));
        }
    }
}
