package ee.tenman.elektrihind.car.vision;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class FileToBase64 {

    public static String encodeFileToBase64(String filePath) throws IOException {
        // Read file to byte array
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

        // Encode bytes to base64
        return Base64.getEncoder().encodeToString(fileContent);
    }

    public static void main(String[] args) throws IOException {
        String base64EncodedFile = encodeFileToBase64("tenmanee-4c14800add13.json");
        System.out.println(base64EncodedFile);
    }
}
