package com.education.amenity.management.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:firebase-credentials.json}")
    private String credentialsPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        try {
            Resource resource = new ClassPathResource(credentialsPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Firebase credentials file not found: " + credentialsPath);
            }

            InputStream serviceAccount = resource.getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId("trixmartid") // Explicitly set project ID
                    .build();

            logger.info("Initializing FirebaseApp for project: {}", options.getProjectId());
            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            logger.error("Failed to initialize FirebaseApp", e);
            throw e;
        }
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        try {
            Firestore firestore = FirestoreClient.getFirestore(firebaseApp);
            logger.info("Firestore initialized successfully");
            return firestore;
        } catch (Exception e) {
            logger.error("Failed to initialize Firestore", e);
            throw new RuntimeException("Failed to initialize Firestore", e);
        }
    }

    @Bean
    public Storage firebaseStorage() throws IOException {
        try {
            Resource resource = new ClassPathResource(credentialsPath);
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .setProjectId("trixmartid")
                    .build()
                    .getService();
            logger.info("Google Cloud Storage initialized successfully");
            return storage;
        } catch (IOException e) {
            logger.error("Failed to initialize Google Cloud Storage", e);
            throw e;
        }
    }
}