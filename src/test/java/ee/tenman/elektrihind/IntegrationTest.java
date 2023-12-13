package ee.tenman.elektrihind;

import ee.tenman.elektrihind.utility.GlobalConstants;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

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

class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String REDIS_PASSWORD = "something";
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.2.3-alpine");
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD);

    static {
        redisContainer.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.data.redis.host=" + redisContainer.getHost(),
                "spring.data.redis.port=" + redisContainer.getFirstMappedPort(),
                "spring.data.redis.password=" + REDIS_PASSWORD
        );
    }
}

