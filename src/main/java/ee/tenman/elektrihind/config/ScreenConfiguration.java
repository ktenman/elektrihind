package ee.tenman.elektrihind.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.tenman.elektrihind.apollo.Screen;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScreenConfiguration {

    private final ResourceLoader resourceLoader;

    @Resource
    private ObjectMapper objectMapper;

    private Map<String, Screen> screenMap;

    public ScreenConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Screen getScreen(String name) {
        return Optional.ofNullable(screenMap.get(name))
                .orElseThrow(() -> new IllegalArgumentException("Screen not found: " + name));
    }

    @PostConstruct
    public void init() {
        try {
            List<Screen> screenList = objectMapper.readValue(
                    resourceLoader.getResource("classpath:screens.json").getInputStream(),
                    new TypeReference<>() {
                    });
            screenMap = screenList.stream().collect(Collectors.toMap(Screen::getName, Function.identity()));
        } catch (IOException e) {
            log.error("Failed to read screan.json", e);
        }
    }

    public boolean isValidHall(String hallName) {
        return screenMap.containsKey(hallName);
    }
}
