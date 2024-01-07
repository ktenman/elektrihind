package ee.tenman.elektrihind.apollo;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class ReBookingService {

    private final ConcurrentHashMap<UUID, ApolloKinoSession> sessions = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();
    @Resource
    private ApolloKinoService apolloKinoService;

    public void add(ApolloKinoSession session) {
        sessions.put(UUID.randomUUID(), session);
    }

    @Scheduled(cron = "0 */5 * * * *") // Runs every 5 minutes
    public void clearExpiredSessions() {
        sessions.entrySet().removeIf(entry -> isSessionExpired(entry.getValue()));
    }

    @Scheduled(cron = "* * * * * *") // Runs every second
    public void rebook() {
        if (lock.tryLock()) {
            try {
                sessions.values()
                        .stream()
                        .filter(this::isReadyToReBook)
                        .forEach(this::book);
            } finally {
                lock.unlock();
            }
        } else {
            log.info("Skipping. Rebooking is locked");
        }
    }

    private void book(ApolloKinoSession session) {
        log.info("Rebooking session {}", session.getSessionId());
        try {
            Optional<File> bookedFile = apolloKinoService.book(session);
            if (bookedFile.isPresent()) {
                log.info("Booked session {}", session.getSessionId());
                session.updateLastInteractionTime();
            }
        } catch (Exception e) {
            log.error("Failed to rebook session {}", session.getSessionId(), e);
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
        return Duration.between(session.getLastUpdated(), LocalDateTime.now()).toSeconds() > 890;
    }
}
