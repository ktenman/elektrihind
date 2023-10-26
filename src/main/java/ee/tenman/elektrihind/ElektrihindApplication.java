package ee.tenman.elektrihind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@EnableRetry
public class ElektrihindApplication {

    public static void main(String[] args) {
        SpringApplication.run(ElektrihindApplication.class, args);
    }

}
