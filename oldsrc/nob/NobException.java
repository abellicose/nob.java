/* ======================================
 * File: NobException.java
 * Date: 2026-05-31
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob;

import java.lang.RuntimeException;
import java.lang.Throwable;

public class NobException extends RuntimeException {
    public NobException(String message) {
        super("[nob] " + message);
    }

    public NobException(String message, Throwable cause) {
        super("[nob] " + message, cause);
    }
}

