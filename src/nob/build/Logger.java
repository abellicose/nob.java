/* ======================================
 * File: Logger.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

import nob.Nob;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.lang.IllegalStateException;
import java.lang.InterruptedException;

public class Logger {
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();

    public static void info(String msg) {
        queue("", msg);
    }

    public static void warn(String msg) {
        queue(" WARN", msg);
    }

    public static void error(String msg) {
        queue(" ERROR", msg);
    }

    public static void debug(String msg) {
        if (Nob.debug) 
            queue(" DEBUG", msg);
    }

    private static void queue(String prefix, String msg) {
        try {
            pool.submit(() -> System.out.println("[nob" + prefix + "] " + msg));
        } catch (Exception ignored) {}
    }

    public static void stop() {
        try {
            pool.shutdown();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
