package ee.tenman.elektrihind;

import ee.tenman.elektrihind.utility.GlobalConstants;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles(GlobalConstants.TEST_PROFILE)
@AutoConfigureWireMock(port = 0)
@ContextConfiguration(initializers = RedisInitializer.class)
public @interface IntegrationTest {
}
