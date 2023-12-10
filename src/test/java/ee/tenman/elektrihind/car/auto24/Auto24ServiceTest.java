package ee.tenman.elektrihind.car.auto24;

import ee.tenman.elektrihind.IntegrationTest;
import jakarta.annotation.Resource;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;


@IntegrationTest
class Auto24ServiceTest {

    @Resource
    private Auto24Service auto24Service;

    @Test
    void carDetails() throws IOException, InterruptedException {
        String vinCode = "JF1SH5LS5AG105986"; // Your VIN code
        String number = "876BCH"; // Your registration number

        HashMap<String, String> map = new HashMap<>();
        map.put("Reg nr", number);
        map.put("Vin", vinCode);

        String captchaResponse = auto24Service.getCaptchaToken();

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");

        String jsonPayload = String.format("{\n" +
                "    \"url\": \"https://eng.auto24.ee/ostuabi/?t=soiduki-andmete-paring&s=vin\",\n" +
                "    \"headers\": [\"Content-Type: application/x-www-form-urlencoded\"],\n" +
                "    \"method\": \"POST\",\n" +
                "    \"data\": \"vin=%s&reg_nr=%s&g-recaptcha-response=%s&vpc_reg_search=1\"\n" +
                "}", vinCode, number, captchaResponse);

        RequestBody body = RequestBody.create(mediaType, jsonPayload);
        Request request = new Request.Builder()
                .url("https://scrapeninja.p.rapidapi.com/scrape")
                .post(body)
                .addHeader("content-type", "application/json")
                .addHeader("X-RapidAPI-Key", "12a6ad6ddfmsh0908ecfaffe93f6p1e88d3jsn8e649a76825d") // Replace with your actual API key
                .addHeader("X-RapidAPI-Host", "scrapeninja.p.rapidapi.com")
                .build();

        Response response = client.newCall(request).execute();

        // Parse HTML using Jsoup
        Document doc = Jsoup.parse(response.body().string());

        String result = "";
        Elements select = doc.select("tr");
        for (Element element : select) {
            boolean korraline = element.text().contains("Korraline");
            if (korraline) {
                Elements select1 = element.select("td");
                select1.get(0).text();
                select1.get(4).text();
                result = select1.get(4).text() + " (" + select1.get(0).text() + ")";
                break;

            }
        }
        System.out.println(doc.title()); // Examp
    }
}
