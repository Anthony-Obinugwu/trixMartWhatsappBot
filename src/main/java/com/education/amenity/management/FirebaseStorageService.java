package com.education.amenity.management;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Service
public class FirebaseStorageService {
    private Storage storage;

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    @PostConstruct
    public void initialize() throws IOException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ClassPathResource(credentialsPath).getInputStream());

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setStorageBucket(bucketName)
                .build();

        FirebaseApp.initializeApp(options);
        this.storage = StorageClient.getInstance().bucket().getStorage();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Blob blob = storage.create(
                BlobInfo.newBuilder(bucketName, fileName).build(),
                file.getBytes(),
                Storage.BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PUBLIC_READ) // Optional: Set ACL
        );

        // Return public URL with long-lived token
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);

        // OR for signed URLs (temporary access):
        // return storage.signUrl(blob.getBlobId(), 7, TimeUnit.DAYS).toString();
    }
}