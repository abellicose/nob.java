/* ======================================
 * File: Logger.java
 * Date: 2026-06-02
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.build;
import nob.Nob;

public class Logger {
    public static void info(String msg)  { System.out.println("[nob] " + msg); }
    public static void warn(String msg)  { System.out.println("[nob WARN] " + msg); }
    public static void error(String msg) { System.err.println("[nob ERROR] " + msg); }
    public static void debug(String msg) { if (Nob.debug) System.out.println("[nob DEBUG] " + msg); }
}
