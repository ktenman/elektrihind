package ee.tenman.elektrihind.utility;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.max;
import static java.util.Map.Entry.comparingByValue;

@Slf4j
public class PDFDataExtractor {
    public static Map<String, Integer> extractDateAndOdometer(String filePath) {
        Map<String, Integer> data = new HashMap<>();
        try {
            PdfReader reader = new PdfReader(filePath);
            PdfDocument pdfDoc = new PdfDocument(reader);

            String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getFirstPage());
            Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}) (\\d+) Tehnoülevaatus");
            Matcher matcher = pattern.matcher(pageText);

            while (matcher.find()) {
                String date = matcher.group(1);
                int odometer = Optional.ofNullable(matcher.group(2))
                        .map(s -> s.replaceAll("\\D", ""))
                        .filter(StringUtils::isNotBlank)
                        .map(Integer::parseInt)
                        .orElse(0);

                if (odometer > 0) {
                    data.put(date, odometer);
                }
            }

            pdfDoc.close();
            reader.close();
        } catch (IOException e) {
            log.error("Error extracting data from PDF", e);
        }
        return data;
    }

    public static Optional<Entry<String, Integer>> getLastOdometer(Map<String, Integer> extractedData) {
        if (extractedData.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(max(extractedData.entrySet(), comparingByValue()));
    }
}
