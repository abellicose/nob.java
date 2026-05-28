/* ======================================
 * File: Util.java
 * Date: 2026-05-28
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

import java.lang.Exception;

public class Util {
    // returns if succeeded
    public static void NOBmkdirIfNotExists(Path path) throws Exception {
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }
}

