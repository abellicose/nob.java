/* ======================================
 * File: NobException.java
 * Date: 2026-11-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.api.error;

import java.lang.RuntimeException;
import java.lang.Throwable;

public class NobException extends RuntimeException {
    public NobException(String message) { super(message); }
    public NobException(String message, Throwable cause) { super(message, cause); }
}


