package com.hopngo.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.service-account-key:firebase-service-account.json}")
    private String serviceAccountKeyPath;

    @Value("${firebase.project-id:hopngo-platform}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;
                
                try {
                    // Try to load from classpath first
                    serviceAccount = new ClassPathResource(serviceAccountKeyPath).getInputStream();
                    logger.info("Loading Firebase service account from classpath: {}", serviceAccountKeyPath);
                } catch (IOException e) {
                    // Fallback to default credentials (useful for production with environment variables)
                    logger.warn("Could not load service account from classpath, using default credentials");
                    serviceAccount = null;
                }

                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setProjectId(projectId);

                if (serviceAccount != null) {
                    GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
                    optionsBuilder.setCredentials(credentials);
                } else {
                    // Use default credentials (from environment or metadata server)
                    optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
                }

                FirebaseApp.initializeApp(optionsBuilder.build());
                logger.info("Firebase Admin SDK initialized successfully for project: {}", projectId);
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Firebase Admin SDK", e);
            // Don't throw exception to allow service to start without FCM
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return FirebaseMessaging.getInstance();
            } else {
                logger.warn("Firebase not initialized, FCM will not be available");
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to create FirebaseMessaging bean", e);
            return null;
        }
    }

    @Bean
    public boolean isFirebaseEnabled() {
        return !FirebaseApp.getApps().isEmpty();
    }
}