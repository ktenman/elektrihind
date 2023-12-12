package ee.tenman.elektrihind.car.easyocr;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "easyOcrClient", url = "http://85.253.165.86:61234")
public interface EasyOcrClient {

    @PostMapping("/upload")
    EasyOcrResponse decode(@RequestBody EasyOcrRequest request);

}
