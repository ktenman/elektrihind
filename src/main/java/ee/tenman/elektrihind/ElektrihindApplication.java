package ee.tenman.elektrihind;

import com.codeborne.selenide.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableRetry
@EnableCaching
public class ElektrihindApplication {

    static {
        Configuration.headless = true;
        Configuration.browser = "firefox";
    }

    public static void main(String[] args) {
        SpringApplication.run(ElektrihindApplication.class, args);
    }
}
