package link.sakkit.sakchat.engine;

import link.sakkit.sakchat.config.ConfigManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AsyncTasksEngine {
    private static final ExecutorService ASYNC_POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SakChat-Worker");
        thread.setDaemon(true);
        return thread;
    });

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConfigManager.LOGGER.info("[SAKChat] Shoutdown detected. Cleaning queue...");
            try {
                ASYNC_POOL.shutdown();
                if (!ASYNC_POOL.awaitTermination(2, TimeUnit.SECONDS)) {
                    ASYNC_POOL.shutdownNow();
                }
            } catch (InterruptedException e) {
                ASYNC_POOL.shutdownNow();
            }
        }, "SakChat-Shutdown-Cleanup"));
    }

    public static void runAsync(Runnable task) {
        if (ASYNC_POOL.isShutdown()) return;

        ASYNC_POOL.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                ConfigManager.LOGGER.error("[SAKChat] Errore nel task asincrono!", e);
            }
        });
    }

    public static void shutdown() {
        ASYNC_POOL.shutdown();
    }
}
