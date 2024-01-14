package ee.tenman.elektrihind.apollo;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
public class CountdownTimer {
    private final ScheduledExecutorService scheduler;
    private final CountDownLatch latch;
    private final AtomicInteger seconds;

    public CountdownTimer(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("Seconds cannot be negative");
        }
        this.seconds = new AtomicInteger(seconds);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.latch = new CountDownLatch(1);
    }

    public static void startTimer(int seconds) {
        log.info("Starting countdown timer for {} seconds", seconds);
        CountdownTimer countdown = new CountdownTimer(seconds);
        countdown.start();
        try {
            countdown.awaitCompletion();
        } catch (InterruptedException e) {
            log.error("Error", e);
            Thread.currentThread().interrupt();
        }
        log.info("Countdown complete. Program continues...");
    }

    private void start() {
        final Runnable task = () -> {
            int remainingSeconds = seconds.decrementAndGet();
            if (remainingSeconds > 0) {
                log.info("Time remaining: {} seconds", remainingSeconds);
            } else {
                log.info("Time's up!");
                scheduler.shutdown();
                latch.countDown();
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 1, SECONDS);
    }

    public void awaitCompletion() throws InterruptedException {
        latch.await();
    }
}
