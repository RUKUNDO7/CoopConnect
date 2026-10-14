package rw.coopconnect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * NFR-01: Secure file storage for produce pictures.
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("File upload directory initialized at: {}", uploadPath);
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        // Validate image extensions
        if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP image files are allowed.");
        }

        String filename = UUID.randomUUID() + extension;
        Path targetLocation = uploadPath.resolve(filename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + filename + ". Please try again!", ex);
        }
    }
}
