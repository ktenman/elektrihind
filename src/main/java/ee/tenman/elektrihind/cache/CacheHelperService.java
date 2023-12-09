package ee.tenman.elektrihind.cache;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static ee.tenman.elektrihind.config.RedisConfig.MESSAGE_COUNTS_CACHE;

@Service
public class CacheHelperService {

    @Cacheable(value = MESSAGE_COUNTS_CACHE, key = "#date")
    public int getMessageCount(LocalDate date) {
        return 0; // Default value if not found in cache
    }

    @CachePut(value = MESSAGE_COUNTS_CACHE, key = "#date")
    public int incrementMessageCount(LocalDate date, int currentCount) {
        return currentCount + 1;
    }

}
