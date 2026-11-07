/* ======================================
 * File: Signatures.java
 * Date: 2026-11-07
 * Creator: Osama
 * Notice: (C) Copyright 2026 By Osama. All Rights Reserved
 * ====================================== */

package nob.util;

public final class Signatures {
    private Signatures() {}

    public static boolean isForeign(String rawId) {
        return rawId.contains(":");
    }
}

