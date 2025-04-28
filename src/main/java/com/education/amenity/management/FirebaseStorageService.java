package com.education.amenity.management;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    private final Storage storage;
    private final String bucketName;

    // Allowed file types for security
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/pdf",
            "text/plain"
    );

    @Autowired
    public FirebaseStorageService(Storage storage,
                                  @Value("${firebase.storage.bucket}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        // Validate input
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Security check
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    String.format("Invalid file type. Allowed types: %s", ALLOWED_MIME_TYPES)
            );
        }

        // Generate unique filename with original extension
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String fileName = UUID.randomUUID() + fileExtension;

        try {
            // Upload with metadata and caching
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName)
                    .setContentType(file.getContentType())
                    .setCacheControl("public, max-age=604800") // 1 week cache
                    .build();

            Blob blob = storage.create(
                    blobInfo,
                    file.getBytes(),
                    Storage.BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PUBLIC_READ)
            );

            // Return HTTPS URL (better than HTTP)
            return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);

        } catch (StorageException e) {
            throw new IOException("Failed to upload file to Firebase Storage", e);
        }
    }

    // Optional: Add file deletion method
    public void deleteFile(String fileUrl) throws IOException {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            storage.delete(bucketName, fileName);
        } catch (StorageException e) {
            throw new IOException("Failed to delete file from Firebase Storage", e);
        }
    }
}