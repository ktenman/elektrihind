package ee.tenman.elektrihind.car.automaks;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static ee.tenman.elektrihind.car.automaks.AutomaksClient.CLIENT_NAME;

@FeignClient(name = CLIENT_NAME, url = "${automaks.url}")
public interface AutomaksClient {

    String CLIENT_NAME = "automaksClient";

    @PostMapping(value = "/tax/calculate")
    TaxResponse calculate(@RequestBody CarDetails carDetails);

}

