package ee.tenman.elektrihind.telegram;

import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class CustomMultipartFile implements MultipartFile {

    private final File file;
    private final String originalFileName;

    public CustomMultipartFile(File file) {
        this.file = file;
        this.originalFileName = file.getName();
    }

    @Override
    public String getName() {
        return originalFileName; // Returns the original file name
    }

    @Override
    public String getOriginalFilename() {
        return originalFileName;
    }

    @Override
    public String getContentType() {
        return "application/octet-stream"; // or the actual content type
    }

    @Override
    public boolean isEmpty() {
        return (file == null || file.length() == 0);
    }

    @Override
    public long getSize() {
        return (file != null) ? file.length() : 0;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (InputStream in = new FileInputStream(this.file);
             OutputStream out = new FileOutputStream(dest)) {
            StreamUtils.copy(in, out);
        }
    }
}
