package com.education.amenity.management;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FirebaseStorageService {

    private final Storage storage;
    private final String bucketName;

    @Autowired
    public FirebaseStorageService(Storage storage,
                                  @Value("${firebase.storage.bucket}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Blob blob = storage.create(
                BlobInfo.newBuilder(bucketName, fileName)
                        .setContentType(file.getContentType())
                        .build(),
                file.getBytes(),
                Storage.BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PUBLIC_READ)
        );

        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }
}