/* ======================================
 * File: NobException.java
 * Date: 2026-05-30
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.util;
// comments here
public class NobException extends Exception { 
    public NobException(String errorMessage) {
        super("[nob] " + errorMessage);
    }
}

