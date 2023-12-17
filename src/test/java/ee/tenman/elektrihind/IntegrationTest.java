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

    private static final String CUSTOM_USERNAME = "user";
    private static final String CUSTOM_PASSWORD = "something";

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.2.3-alpine");
    private static final DockerImageName RABBITMQ_IMAGE = DockerImageName.parse("rabbitmq:3.9-management-alpine");

    private static final GenericContainer<?> RABBIT_MQ_CONTAINER = new GenericContainer<>(RABBITMQ_IMAGE)
            .withExposedPorts(5672, 15672);

    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", CUSTOM_PASSWORD)
            .withEnv("RABBITMQ_DEFAULT_USER", CUSTOM_USERNAME)
            .withEnv("RABBITMQ_DEFAULT_PASS", CUSTOM_PASSWORD);

    static {
        REDIS_CONTAINER.start();
        RABBIT_MQ_CONTAINER.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.data.redis.host=" + REDIS_CONTAINER.getHost(),
                "spring.data.redis.port=" + REDIS_CONTAINER.getFirstMappedPort(),
                "spring.data.redis.password=" + CUSTOM_PASSWORD,

                "spring.rabbitmq.host=" + RABBIT_MQ_CONTAINER.getHost(),
                "spring.rabbitmq.port=" + RABBIT_MQ_CONTAINER.getMappedPort(5672),
                "spring.rabbitmq.username=" + CUSTOM_USERNAME,
                "spring.rabbitmq.password=" + CUSTOM_PASSWORD
        );
    }
}

