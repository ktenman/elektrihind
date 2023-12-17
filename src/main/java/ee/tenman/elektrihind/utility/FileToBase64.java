package ee.tenman.elektrihind.utility;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Slf4j
public class FileToBase64 {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

    public static String encodeToBase64(String filePath) throws IOException {
        try {
            return encodeToBase64(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            log.error("Error reading file: {}", filePath, e);
            throw new IOException("Error reading file: " + filePath, e);
        }
    }

    public static String encodeToBase64(byte[] fileContent) {
        return BASE64_ENCODER.encodeToString(fileContent);
    }

    public static byte[] decode(String base64EncodedKey) {
        return BASE64_DECODER.decode(base64EncodedKey);
    }
}
