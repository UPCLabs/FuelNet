package com.fuelnet.fuelnet.config;

import java.io.InputStream;

import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {

            InputStream serviceAccount = getClass().getClassLoader()
                    .getResourceAsStream("firebase-key.json");

            if (serviceAccount == null) {
                throw new RuntimeException("❌ firebase-key.json no encontrado");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            System.out.println("🔥 Firebase inicializado");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
