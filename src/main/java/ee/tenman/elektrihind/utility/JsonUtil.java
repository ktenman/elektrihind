package ee.tenman.elektrihind.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import ee.tenman.elektrihind.apollo.ApolloKinoSession;
import lombok.SneakyThrows;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static ee.tenman.elektrihind.config.ObjectMapperConfiguration.OBJECT_MAPPER;

public class JsonUtil {

    @SneakyThrows(JsonProcessingException.class)
    public static String serializeMap(Map<UUID, ApolloKinoSession> map) {
        return OBJECT_MAPPER.writeValueAsString(map);
    }

    @SneakyThrows(JsonProcessingException.class)
    public static Map<UUID, ApolloKinoSession> deserializeMap(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        return OBJECT_MAPPER.readValue(json, new TypeReference<>() {
        });
    }
}
