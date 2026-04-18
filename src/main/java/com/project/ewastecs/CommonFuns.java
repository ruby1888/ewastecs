package com.project.ewastecs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class CommonFuns {

    @Value("${app.upload.dir:uploads/ewastes}")
    private String uploadDir;

    /**
     * Saves a multipart file to the upload directory and returns the stored filename.
     */
    public String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(storedName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return storedName;
    }

    /**
     * Deletes a previously uploaded file by filename.
     */
    public void deleteFile(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {}
    }
}
