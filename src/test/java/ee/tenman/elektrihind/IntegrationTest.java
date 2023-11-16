package ee.tenman.elektrihind;

import ee.tenman.elektrihind.util.GlobalConstants;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles(GlobalConstants.TEST_PROFILE)
@AutoConfigureWireMock(port = 0)
public @interface IntegrationTest {
}
