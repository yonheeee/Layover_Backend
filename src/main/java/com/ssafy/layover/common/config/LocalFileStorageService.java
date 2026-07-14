package com.ssafy.layover.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalFileStorageService implements FileStorageService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file, String directory) {
        try {
            String filename = StorageFileNames.randomName(file.getOriginalFilename());
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename));
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new FileStorageException("로컬 파일 저장 중 오류가 발생했습니다.", e);
        }
    }
}
