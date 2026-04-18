package com.project.ewastecs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/ewastes}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve the upload path — try absolute first, then relative to CWD
        Path uploadPath = Paths.get(uploadDir);
        if (!uploadPath.isAbsolute()) {
            uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir);
        }
        uploadPath = uploadPath.toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        if (!location.endsWith("/")) location = location + "/";

        // /uploads/ewastes/filename.jpg → file in uploads/ewastes/ directory
        registry.addResourceHandler("/uploads/ewastes/**")
                .addResourceLocations(location)
                .setCachePeriod(0);

        // Backward compat: /uploads/filename.jpg also works
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(0);
    }
}
