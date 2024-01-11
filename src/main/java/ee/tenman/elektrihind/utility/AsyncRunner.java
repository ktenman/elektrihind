package ee.tenman.elektrihind.utility;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncRunner {

    @Async("customExecutor")
    public void run(Runnable task) {
        task.run();
    }
}
