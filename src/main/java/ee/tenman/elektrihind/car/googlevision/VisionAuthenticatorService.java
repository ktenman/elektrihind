package ee.tenman.elektrihind.car.googlevision;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import ee.tenman.elektrihind.utility.FileToBase64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Service
@Slf4j
public class VisionAuthenticatorService {

    private final GoogleCredentials credentials;

    public VisionAuthenticatorService(@Value("${vision.base64EncodedKey}") String base64EncodedKey) {
        if (base64EncodedKey == null || base64EncodedKey.isEmpty()) {
            log.warn("Google Credentials not initialized. Please provide a base64 encoded key in the 'vision.base64EncodedKey' property.");
            credentials = null;
            return;
        }
        try {
            byte[] decodedJsonBytes = FileToBase64.decode(base64EncodedKey);
            try (InputStream credentialsStream = new ByteArrayInputStream(decodedJsonBytes)) {
                credentials = GoogleCredentials.fromStream(credentialsStream)
                        .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));
                log.info("Google Credentials have been initialized successfully.");
            }
        } catch (IOException e) {
            log.error("Error initializing Google Credentials: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Google Credentials", e);
        }
    }

    public String getAccessToken() {
        try {
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            log.debug("Access token retrieved successfully.");
            return token.getTokenValue();
        } catch (IOException e) {
            log.error("Error retrieving access token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve access token", e);
        }
    }
}


