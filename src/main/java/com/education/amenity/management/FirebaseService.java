package com.education.amenity.management;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;

@Service
public class FirebaseService {
    private Firestore firestore;

    @PostConstruct
    public void initialize() throws IOException {
        InputStream serviceAccount = new ClassPathResource("firebase-credentials.json").getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        FirebaseApp.initializeApp(options);
        this.firestore = FirestoreClient.getFirestore();
    }

    public Firestore getFirestore() {
        return firestore;
    }
}