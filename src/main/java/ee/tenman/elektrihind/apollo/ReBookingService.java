package ee.tenman.elektrihind.apollo;

import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.telegram.ElektriTeemuTelegramService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class ReBookingService {

    private ConcurrentHashMap<UUID, ApolloKinoSession> sessions = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();
    @Resource
    private ApolloKinoService apolloKinoService;
    @Resource
    private ElektriTeemuTelegramService elektriTeemuTelegramService;
    @Resource
    private CacheService cacheService;

    @PostConstruct
    public void init() {
        this.sessions = new ConcurrentHashMap<>(cacheService.getRebookingSessions());
    }

    public void add(ApolloKinoSession session) {
        UUID sessionId = UUID.randomUUID();
        this.sessions.put(sessionId, session);
        cacheService.addRebookingSession(sessionId, session);
    }

    @Scheduled(cron = "0 */5 * * * *") // Runs every 5 minutes
    public void clearExpiredSessions() {
        List<UUID> idsToRemove = new ArrayList<>();
        for (Map.Entry<UUID, ApolloKinoSession> sessionMapEntry : sessions.entrySet()) {
            if (isSessionExpired(sessionMapEntry.getValue())) {
                log.info("Removing session {}", sessionMapEntry.getKey());
                idsToRemove.add(sessionMapEntry.getKey());
            }
        }
        idsToRemove.forEach(sessions::remove);
        idsToRemove.forEach(cacheService::removeRebookingSession);
    }

    @Scheduled(cron = "* * * * * *") // Runs every second
    public void rebook() {
        if (lock.tryLock()) {
            try {
                sessions.values()
                        .stream()
                        .filter(this::isReadyToReBook)
                        .forEach(this::book);
            } catch (Exception e) {
                log.error("Failed to rebook", e);
                clearExpiredSessions();
            } finally {
                lock.unlock();
            }
        } else {
            log.info("Skipping. Rebooking is locked");
        }
    }

    private void book(ApolloKinoSession session) {
        log.info("Rebooking session {}", session.getSessionId());

        Optional<File> bookedFile = apolloKinoService.book(session);
        if (bookedFile.isPresent()) {
            log.info("Booked session {}", session.getSessionId());
            session.updateLastInteractionTime();
            elektriTeemuTelegramService.sendToTelegram(
                    "Booked movie " + session.getSelectedMovie() + " at " + session.getSelectedDateTime(), session.getChatId());
            elektriTeemuTelegramService.sendFileToTelegram(bookedFile.get(), session.getChatId());
        }
    }

    private boolean isSessionExpired(ApolloKinoSession session) {
        if (session == null) {
            return true;
        }
        return session.getSelectedDateTime().isBefore(LocalDateTime.now());
    }

    private boolean isReadyToReBook(ApolloKinoSession session) {
        if (session == null) {
            return false;
        }
        return Duration.between(session.getLastUpdated(), LocalDateTime.now()).toSeconds() > 900;
    }
}
