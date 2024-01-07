package ee.tenman.elektrihind.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class HolidaysConfiguration {

    private final ResourceLoader resourceLoader;
    @Resource
    ObjectMapper objectMapper;

    @Getter
    private Set<String> holidays;

    public HolidaysConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.holidays = new HashSet<>();
    }

    @PostConstruct
    public void init() {
        try {
            // Adjusted TypeReference to match the JSON structure
            Map<String, Set<String>> holidayMap = objectMapper.readValue(
                    resourceLoader.getResource("classpath:holidays.json").getInputStream(),
                    new TypeReference<>() {
                    });
            holidays = holidayMap.get("holidays");
        } catch (IOException e) {
            log.error("Failed to read holidays.json", e);
        }
    }

}
