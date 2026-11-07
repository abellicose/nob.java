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

public final class Logger {
    private Logger() {}
    // TODO: TEMP CONSTRUCTOR, REMOVE THIS AFTER REMOVING SHUTDOWNHOOK
    private static final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static final String BG_BLACK   = "\u001B[40m";
    public static final String BG_RED     = "\u001B[41m";
    public static final String BG_GREEN   = "\u001B[42m";
    public static final String BG_YELLOW  = "\u001B[43m";
    public static final String BG_BLUE    = "\u001B[44m";
    public static final String BG_PURPLE  = "\u001B[45m";
    public static final String BG_CYAN    = "\u001B[46m";
    public static final String BG_WHITE   = "\u001B[47m";

    public static final String BOLD      = "\u001B[1m";
    public static final String DIM       = "\u001B[2m";
    public static final String ITALIC    = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String REVERSE   = "\u001B[7m";
    public static final String STRIKE    = "\u001B[9m";

    public static void info(String msg) {
        queue("", msg, BOLD + CYAN, "");
    }

    public static void warn(String msg) {
        queue(" WARN", msg, BOLD + YELLOW, YELLOW);
    }

    public static void error(String msg) {
        queue(" ERROR", msg, BOLD + WHITE + BG_RED, RED);
    }

    public static void debug(String msg) {
        if (Nob.debug)
            queue(" DEBUG", msg, DIM + PURPLE, DIM);
    }

    // tagCodes style the [nob …] tag; msgCodes style the message. info keeps the
    // message plain so normal output stays clean and warn/error/debug pop against it.
    private static void queue(String prefix, String msg, String tagCodes, String msgCodes) {
        try {
            pool.submit(() -> System.out.println(
                tagCodes + "[nob" + prefix + "]" + RESET + " " + msgCodes + msg + RESET));
        } catch (Exception ignored) {}
    }

    public static void stop() {
        System.out.println("Stopping logger pool");
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
