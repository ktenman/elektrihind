package ee.tenman.elektrihind.apollo;

import ee.tenman.elektrihind.cache.CacheService;
import ee.tenman.elektrihind.telegram.ElektriTeemuTelegramService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Comparator.comparing;

@Service
@Slf4j
public class ReBookingService {

    @Getter
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
        Optional<ApolloKinoSession> existingSession = findExistingSession(session);
        if (existingSession.isPresent()) {
            log.info("Session already exists. Updating last interaction time");
            existingSession.get().getSelectedStarSeats().addAll(session.getSelectedStarSeats());
            updateLastInteractionTimes();
            return;
        }
        UUID sessionId = UUID.randomUUID();
        this.sessions.put(sessionId, session);
        cacheService.addRebookingSession(sessionId, session);
        updateLastInteractionTimes();
    }

    private Optional<ApolloKinoSession> findExistingSession(ApolloKinoSession session) {
        for (Entry<UUID, ApolloKinoSession> entry : sessions.entrySet()) {
            ApolloKinoSession existingSession = entry.getValue();
            if (session.getSelectedMovie().equals(existingSession.getSelectedMovie()) &&
                    session.getSelectedTime().equals(existingSession.getSelectedTime()) &&
                    session.getSelectedDate().equals(existingSession.getSelectedDate())) {
                return Optional.of(existingSession);
            }
        }
        return Optional.empty();
    }

    private void updateLastInteractionTimes() {
        log.info("Updating last interaction times");
        sessions.values().forEach(ApolloKinoSession::updateLastInteractionTime);
        cacheService.updateRebookingSessions(sessions);
        log.info("Updated last interaction times");
    }

    @Scheduled(cron = "0 */5 * * * *") // Runs every 5 minutes
    public void clearExpiredSessions() {
        List<UUID> idsToRemove = new ArrayList<>();
        for (Entry<UUID, ApolloKinoSession> sessionMapEntry : sessions.entrySet()) {
            if (isSessionExpired(sessionMapEntry.getValue())) {
                log.info("Removing session {}", sessionMapEntry.getKey());
                idsToRemove.add(sessionMapEntry.getKey());
            }
        }
        idsToRemove.forEach(sessions::remove);
        idsToRemove.forEach(cacheService::removeRebookingSession);
    }

    @Scheduled(cron = "* * * * * *") // Runs every minute
    public void rebook() {
        if (lock.tryLock()) {
            try {
                AtomicBoolean rebooked = new AtomicBoolean(false);
                sessions.entrySet().stream()
                        .sorted(comparing((Entry<UUID, ApolloKinoSession> o) -> o.getValue().getUpdatedAt()).reversed())
                        .filter(entry -> isReadyToReBook(entry.getValue()))
                        .forEach(entry -> {
                            ApolloKinoSession rebookedSession = book(entry.getValue());
                            sessions.remove(entry.getKey());
                            sessions.put(entry.getKey(), rebookedSession);
                            rebooked.set(true);
                        });
                if (rebooked.get()) {
                    updateLastInteractionTimes();
                }
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

    private ApolloKinoSession book(ApolloKinoSession session) {
        log.info("Rebooking session {}", session.getSessionId());

        Optional<Entry<File, List<String>>> bookedFile = apolloKinoService.book(session);
        if (bookedFile.isPresent()) {
            log.info("Re-booked session {}", session.getSessionId());
            String message = "Booked: " + session.getSelectedMovie() + " [" + session.getRowAndSeat() + "] on " +
                    session.getSelectedDate().format(ApolloKinoService.DATE_TIME_FORMATTER) + " at " +
                    session.getSelectedTime();
            log.info("Re-booked session {} - {}", session.getSessionId(), message);
            elektriTeemuTelegramService.sendToTelegram(message, session.getChatId());
            elektriTeemuTelegramService.sendFileToTelegram(bookedFile.get().getKey(), session.getChatId());
        }
        session.updateLastInteractionTime();

        return session;
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
        return Duration.between(session.getUpdatedAt(), LocalDateTime.now()).toSeconds() > 900;
    }

    public int getActiveBookingCount() {
        return sessions.size();
    }

    public void cancel(UUID bookingUuid) {
        sessions.remove(bookingUuid);
        cacheService.removeRebookingSession(bookingUuid);
        log.info("Cancelled booking session {}", bookingUuid);
    }

}
