package ee.tenman.elektrihind.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ee.tenman.elektrihind.apollo.Cinema;
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
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ScreenConfiguration {

    private final ResourceLoader resourceLoader;

    @Resource
    private ObjectMapper objectMapper;

    private final Map<Cinema, Map<String, Screen>> screenMap = new TreeMap<>();

    public ScreenConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Screen getScreen(Cinema cinema, String name) {
        return Optional.ofNullable(screenMap.get(cinema).get(name))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Cinema %s or/and screen %s not found", cinema, name)));
    }

    @PostConstruct
    public void init() {
        try {
            Map<Cinema, List<Screen>> screenList = objectMapper.readValue(
                    resourceLoader.getResource("classpath:screens.json").getInputStream(),
                    new TypeReference<>() {
                    });
            screenList.forEach((k, v) -> screenMap.put(k, v.stream()
                    .collect(Collectors.toMap(Screen::name, Function.identity()))));
        } catch (IOException e) {
            log.error("Failed to read screan.json", e);
        }
    }

    public boolean isValidHall(Cinema cinema, String hallName) {
        return screenMap.get(cinema).containsKey(hallName);
    }

}
