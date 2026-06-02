/* ======================================
 * File: Logger.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;

class Logger {
    static void info(String msg)  { System.out.println("[nob] " + msg); }
    static void warn(String msg)  { System.out.println("[nob] WARN " + msg); }
    static void error(String msg) { System.err.println("[nob] ERROR " + msg); }
}
