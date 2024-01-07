package ee.tenman.elektrihind.apollo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionManagementService {
    private static final long CLEANUP_RATE_MS = 3600000;
    private static final long SESSION_EXPIRY_DURATION_MIN = 10;

    private final ConcurrentHashMap<UUID, ApolloKinoSession> sessions = new ConcurrentHashMap<>();

    public ApolloKinoSession createNewSession() {
        ApolloKinoSession session = new ApolloKinoSession();
        sessions.put(session.getSessionId(), session);
        return session;
    }

    public ApolloKinoSession getSession(UUID sessionId) {
        if (isSessionExpired(sessionId)) {
            return null;
        }
        return sessions.get(sessionId);
    }

    @Scheduled(fixedRate = CLEANUP_RATE_MS)
    public void clearExpiredSessions() {
        sessions.entrySet().removeIf(entry -> isSessionExpired(entry.getKey()));
    }

    private boolean isSessionExpired(UUID sessionId) {
        ApolloKinoSession session = sessions.get(sessionId);
        if (session == null) {
            return true;
        }
        return Duration.between(session.getLastUpdated(), LocalDateTime.now()).toMinutes() >= SESSION_EXPIRY_DURATION_MIN;
    }
}
