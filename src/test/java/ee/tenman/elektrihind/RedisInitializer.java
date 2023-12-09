package ee.tenman.elektrihind;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.2.3-alpine");
    private static final GenericContainer<?> redisContainer = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379);

    static {
        redisContainer.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.data.redis.host=" + redisContainer.getHost(),
                "spring.data.redis.port=" + redisContainer.getFirstMappedPort()
        );
    }
}
