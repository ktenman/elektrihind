package ee.tenman.elektrihind.apollo;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SessionManagementService {
    private static final long CLEANUP_RATE_MS = 600_000;
    private static final long SESSION_EXPIRY_DURATION_NANOS = 600;

    private final ConcurrentHashMap<Integer, ApolloKinoSession> sessions = new ConcurrentHashMap<>();

    public ApolloKinoSession createNewSession() {
        ApolloKinoSession session = new ApolloKinoSession(generateSessionId());
        sessions.put(session.getSessionId(), session);
        return session;
    }

    private Integer generateSessionId() {
        return sessions.values().stream()
                .max(Comparator.comparing(ApolloKinoSession::getSessionId))
                .map(ApolloKinoSession::getSessionId)
                .orElse(1) +
                RandomUtils.nextInt(99);
    }

    public Optional<ApolloKinoSession> getSession(Integer sessionId) {
        boolean sessionExpired = isSessionExpired(sessionId);
        if (sessionExpired) {
            log.error("Session expired: {}", sessionId);
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Scheduled(fixedRate = CLEANUP_RATE_MS)
    public void clearExpiredSessions() {
        sessions.entrySet().removeIf(entry -> isSessionExpired(entry.getValue().getSessionId()));
    }

    private boolean isSessionExpired(Integer sessionId) {
        ApolloKinoSession session = sessions.get(sessionId);
        if (session == null) {
            return true;
        }
        long nanos = Duration.between(session.getLastUpdated(), LocalDateTime.now()).toSeconds();
        return nanos >= SESSION_EXPIRY_DURATION_NANOS;
    }

    public void removeSession(Integer sessionId) {
        sessions.remove(sessionId);
    }
}
