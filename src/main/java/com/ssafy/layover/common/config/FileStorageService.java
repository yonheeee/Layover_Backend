package com.ssafy.layover.common.config;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String upload(MultipartFile file, String directory);
}
