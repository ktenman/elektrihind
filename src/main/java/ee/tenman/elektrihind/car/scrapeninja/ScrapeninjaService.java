package ee.tenman.elektrihind.car.scrapeninja;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
public class ScrapeninjaService {

    @Value("${scrapeninja.key}")
    private String apiKey;

    @Resource
    private ScrapeninjaClient scrapeninjaClient;

    public Map<String, String> scrape(String vinCode, String regNr, String captchaToken) {
        log.info("Scraping vinCode={}, regNr={}, captchaToken={}", vinCode, regNr, captchaToken);
        ScrapeninjaRequest request = new ScrapeninjaRequest();
        request.setUrl("https://eng.auto24.ee/ostuabi/?t=soiduki-andmete-paring&s=vin");
        request.setHeaders(new String[]{"Content-Type: application/x-www-form-urlencoded"});
        request.setMethod("POST");
        request.setData(String.format("vin=%s&reg_nr=%s&g-recaptcha-response=%s&vpc_reg_search=1", vinCode, regNr, captchaToken));

        ScrapeninjaResponse response = scrapeninjaClient.scrape(apiKey, request);
        Document doc = Jsoup.parse(response.getBody());

        Map<String, String> result = new TreeMap<>();
        Elements select = doc.select("tr");
        for (Element element : select) {
            String elementText = element.text();
            boolean korraline = elementText.contains("Korraline");
            if (korraline) {
                Elements select1 = element.select("td");
                select1.get(0).text();
                select1.get(4).text();
                result.put("Läbisõit", select1.get(4).text() + " (" + select1.get(0).text() + ")");
                break;
            }
            if (elementText.contains("Kütusekulu")) {
                Elements td = element.select("td");
                String key = td.get(0).text()
                        .replace("(l/ 100 km)", "")
                        .replace("(l/100 km)", "")
                        .trim();
                String value = td.get(1).text();
                result.put(key, value);
            }
        }
        log.info("Scraped vinCode={}, regNr={}, captchaToken={}, result={}", vinCode, regNr, captchaToken, result);
        return result;
    }
}
