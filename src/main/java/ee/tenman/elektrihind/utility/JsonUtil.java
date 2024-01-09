package ee.tenman.elektrihind.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.SneakyThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ee.tenman.elektrihind.config.ObjectMapperConfiguration.OBJECT_MAPPER;

public class JsonUtil {

    @SneakyThrows(JsonProcessingException.class)
    public static <K, V> String serializeMap(Map<K, V> map) {
        return OBJECT_MAPPER.writeValueAsString(map);
    }

    @SneakyThrows(JsonProcessingException.class)
    public static <T> String serializeList(List<T> list) {
        return OBJECT_MAPPER.writeValueAsString(list);
    }

    @SneakyThrows(JsonProcessingException.class)
    public static <K, V> Map<K, V> deserializeMap(String json, TypeReference<Map<K, V>> typeReference) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        return OBJECT_MAPPER.readValue(json, typeReference);
    }

    @SneakyThrows(JsonProcessingException.class)
    public static <T> List<T> deserializeList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        return OBJECT_MAPPER.readValue(json, typeReference);
    }

}
