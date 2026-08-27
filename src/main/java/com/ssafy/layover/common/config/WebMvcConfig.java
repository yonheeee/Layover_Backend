package com.ssafy.layover.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /**
     * 로컬 저장(storage.type=local)으로 올라간 파일을 /uploads/** 로 서빙한다.
     *
     * <p>경로 문자열은 반드시 슬래시로 끝나야 한다. {@code Path.toUri()} 는
     * <b>실제로 존재하는 디렉터리에만</b> 끝 슬래시를 붙이는데, uploads/ 는
     * .gitignore 대상이라 새로 클론한 환경에는 없다. 그 상태로 서버를 켜면
     * 위치가 {@code .../uploads} 가 되어 요청이 {@code .../uploadsstamps/...}
     * 로 풀린다. 사진은 저장되는데 화면에서만 안 보이고, 서버를 다시 켜야
     * 고쳐지는 증상이 여기서 나온다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ignored) {
            // 디렉터리를 못 만들어도 아래 슬래시 보정으로 매핑 자체는 올바르게 유지된다.
        }

        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
